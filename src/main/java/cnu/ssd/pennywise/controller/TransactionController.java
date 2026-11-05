package cnu.ssd.pennywise.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import cnu.ssd.pennywise.exception.RateNotConfiguredException;
import cnu.ssd.pennywise.exception.TransactionException;
import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.TransactionFormDialog;
import cnu.ssd.pennywise.gui.TransactionPanel;
import cnu.ssd.pennywise.model.*;
import cnu.ssd.pennywise.service.CurrencyService;
import cnu.ssd.pennywise.service.TransactionService;
import org.springframework.stereotype.Component;

/**
 * Drives {@link TransactionPanel} and {@link TransactionFormDialog}: loads the
 * current household's transactions, drives the dialog's live currency-conversion
 * preview, and handles record creation, including the "no rate configured
 * yet" prompt. Every call into
 * {@link TransactionService}/{@link CurrencyService} runs on a
 * {@link SwingWorker} so a slow database round trip never freezes the EDT.
 */
@Component
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrencyService currencyService;
    private final HouseholdContext householdContext;
    private final Session session;
    private final AppFrame appFrame;
    private final TransactionPanel panel = new TransactionPanel();

    public TransactionController(TransactionService transactionService, CurrencyService currencyService,
                                 HouseholdContext householdContext, Session session, AppFrame appFrame) {
        this.transactionService = transactionService;
        this.currencyService = currencyService;
        this.householdContext = householdContext;
        this.session = session;
        this.appFrame = appFrame;
        panel.getAddButton().addActionListener(e -> openCreateDialog());
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Reloads the panel's table from the current household. */
    public void refresh() {
        Long householdId = householdContext.getCurrentHouseholdId();
        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();

        panel.getAddButton().setEnabled(false);
        new SwingWorker<List<Transaction>, Void>() {
            @Override
            protected List<Transaction> doInBackground() {
                return transactionService.getTransactionsForHousehold(householdId);
            }

            @Override
            protected void done() {
                panel.getAddButton().setEnabled(true);
                try {
                    panel.setTransactions(get(), primaryCurrency);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(appFrame.getFrame(), "Could not load transactions. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void openCreateDialog() {
        Household household = householdContext.getCurrentHousehold();
        TransactionFormDialog dialog = new TransactionFormDialog(appFrame.getFrame());
        dialog.setMembers(household.getMembers());
        dialog.selectPayerById(session.getCurrentUser().getId());
        dialog.getCurrencyComboBox().setSelectedItem(household.getPrimaryCurrency());

        wireConversionPreview(dialog, household);
        dialog.getCreateButton().addActionListener(e -> handleCreateTransaction(dialog, household));
        dialog.setVisible(true);
    }

    private void wireConversionPreview(TransactionFormDialog dialog, Household household) {
        Runnable updatePreview = () -> {
            BigDecimal amount = parsePositiveAmount(dialog.getAmountField().getText());
            if (amount == null) {
                dialog.setConversionPreviewText(" ");
                dialog.hideRatePrompt();
                return;
            }
            String currency = currencyOf(dialog);
            dialog.hideRatePrompt();

            new SwingWorker<BigDecimal, Void>() {
                @Override
                protected BigDecimal doInBackground() {
                    return currencyService.convertToPrimary(household.getId(), currency, amount);
                }

                @Override
                protected void done() {
                    try {
                        BigDecimal converted = get();
                        dialog.setConversionPreviewText("\u2248 " + converted + " " + household.getPrimaryCurrency());
                    } catch (ExecutionException ex) {
                        if (ex.getCause() instanceof RateNotConfiguredException rnc) {
                            dialog.setConversionPreviewText("No exchange rate set for " + rnc.getCurrencyCode() + ".");
                            dialog.showRatePrompt(rnc.getCurrencyCode());
                        } else {
                            ex.getCause().printStackTrace();
                            dialog.setConversionPreviewText("Could not preview the conversion.");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        };

        dialog.getAmountField().getDocument().addDocumentListener(onChange(updatePreview));
        dialog.getCurrencyComboBox().addItemListener(e -> updatePreview.run());
        dialog.getSaveRateButton().addActionListener(e -> handleSaveRateFromDialog(dialog, household, updatePreview));
    }

    private void handleSaveRateFromDialog(TransactionFormDialog dialog, Household household, Runnable updatePreview) {
        BigDecimal rate = parsePositiveAmount(dialog.getRateField().getText());
        if (rate == null) {
            JOptionPane.showMessageDialog(dialog, "Enter a valid positive rate.", "Invalid data", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String code = dialog.getPromptedCurrencyCode();

        dialog.getSaveRateButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                currencyService.setRate(household.getId(), code, rate);
                return null;
            }

            @Override
            protected void done() {
                dialog.getSaveRateButton().setEnabled(true);
                try {
                    get();
                    dialog.hideRatePrompt();
                    updatePreview.run();
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    JOptionPane.showMessageDialog(dialog, "Could not save the exchange rate.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleCreateTransaction(TransactionFormDialog dialog, Household household) {
        TransactionType type = "Income".equals(dialog.getTypeComboBox().getSelectedItem())
                ? TransactionType.INCOME : TransactionType.EXPENSE;

        LocalDate date;
        try {
            date = LocalDate.parse(dialog.getDateField().getText().trim());
        } catch (DateTimeParseException ex) {
            showError(dialog, "Enter the date as yyyy-mm-dd.");
            return;
        }

        BigDecimal amount = parsePositiveAmount(dialog.getAmountField().getText());
        if (amount == null) {
            showError(dialog, "Amount must be a positive number.");
            return;
        }

        String currency = currencyOf(dialog);
        String category = dialog.getCategoryField().getText();
        String description = dialog.getDescriptionField().getText();

        Member payer = (Member) dialog.getPayerComboBox().getSelectedItem();
        if (payer == null) {
            showError(dialog, "Select who paid.");
            return;
        }

        boolean shared = "Shared".equals(dialog.getScopeComboBox().getSelectedItem());
        TransactionScope scope = shared ? TransactionScope.SHARED : TransactionScope.INDIVIDUAL;
        List<Long> sharedMemberIds = null;
        Map<Long, BigDecimal> percentages = null;
        if (shared) {
            sharedMemberIds = new ArrayList<>();
            boolean percentageMode = "By percentage".equals(dialog.getSplitModeComboBox().getSelectedItem());
            if (percentageMode) {
                percentages = new LinkedHashMap<>();
            }
            for (TransactionFormDialog.MemberSplitRow row : dialog.getMemberSplitRows()) {
                if (row.getIncludeCheckBox().isSelected()) {
                    Long userId = row.getMember().getUser().getId();
                    sharedMemberIds.add(userId);
                    if (percentageMode) {
                        percentages.put(userId, BigDecimal.valueOf((Integer) row.getPercentageSpinner().getValue()));
                    }
                }
            }
        }

        Long householdId = household.getId();
        Long payerUserId = payer.getUser().getId();
        List<Long> finalSharedMemberIds = sharedMemberIds;
        Map<Long, BigDecimal> finalPercentages = percentages;

        dialog.getCreateButton().setEnabled(false);
        new SwingWorker<Transaction, Void>() {
            @Override
            protected Transaction doInBackground() {
                return transactionService.createTransaction(householdId, payerUserId, type, date, category,
                        description, currency, amount, scope, finalSharedMemberIds, finalPercentages);
            }

            @Override
            protected void done() {
                dialog.getCreateButton().setEnabled(true);
                try {
                    get();
                    dialog.dispose();
                    refresh();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof RateNotConfiguredException rnc) {
                        dialog.setConversionPreviewText("No exchange rate set for " + rnc.getCurrencyCode() + ".");
                        dialog.showRatePrompt(rnc.getCurrencyCode());
                    } else if (ex.getCause() instanceof TransactionException) {
                        showError(dialog, ex.getCause().getMessage());
                    } else {
                        ex.getCause().printStackTrace();
                        showError(dialog, "Something went wrong. Please try again.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private String currencyOf(TransactionFormDialog dialog) {
        Object selected = dialog.getCurrencyComboBox().getEditor().getItem();
        return String.valueOf(selected).trim().toUpperCase();
    }

    private BigDecimal parsePositiveAmount(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.signum() > 0 ? value : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Invalid data", JOptionPane.ERROR_MESSAGE);
    }

    /** Adapts the three-method {@link DocumentListener} to a single callback. */
    private static DocumentListener onChange(Runnable callback) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                callback.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                callback.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                callback.run();
            }
        };
    }
}
