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
import cnu.ssd.pennywise.service.BudgetService;
import cnu.ssd.pennywise.service.CurrencyService;
import cnu.ssd.pennywise.service.TransactionService;
import org.springframework.context.annotation.Lazy;
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
    private final BudgetService budgetService;
    private final HouseholdContext householdContext;
    private final Session session;
    private final AppFrame appFrame;
    private final ExportController exportController;
    private final TransactionPanel panel = new TransactionPanel();

    public TransactionController(TransactionService transactionService, CurrencyService currencyService,
                                 BudgetService budgetService, HouseholdContext householdContext, Session session,
                                 AppFrame appFrame, @Lazy ExportController exportController) {
        this.transactionService = transactionService;
        this.currencyService = currencyService;
        this.budgetService = budgetService;
        this.householdContext = householdContext;
        this.session = session;
        this.appFrame = appFrame;
        this.exportController = exportController;
        panel.getAddButton().addActionListener(e -> openCreateDialog());
        panel.getEditButton().addActionListener(e -> openEditDialog());
        panel.getDeleteButton().addActionListener(e -> handleDelete());
        panel.getSearchButton().addActionListener(e -> applyFilters());
        panel.getClearFiltersButton().addActionListener(e -> {
            panel.clearFilters();
            applyFilters();
        });
        panel.getExportButton().addActionListener(e -> exportController.openExportDialog(currentFilter()));
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Reloads the panel: repopulates the member filter from the current household, then re-applies its filters. */
    public void refresh() {
        panel.setMembers(householdContext.getCurrentHousehold().getMembers());
        Long householdId = householdContext.getCurrentHouseholdId();
        new SwingWorker<List<Category>, Void>() {
            @Override
            protected List<Category> doInBackground() {
                return budgetService.getCategories(householdId);
            }

            @Override
            protected void done() {
                try {
                    panel.setCategories(get());
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    panel.setCategories(List.of());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                applyFilters();
            }
        }.execute();
    }

    private void applyFilters() {
        Long householdId = householdContext.getCurrentHouseholdId();
        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();
        TransactionFilter filter;
        try {
            filter = currentFilter();
        } catch (DateTimeParseException ex) {
            showError(panel, "Enter dates as yyyy-mm-dd.");
            return;
        }

        panel.getAddButton().setEnabled(false);
        new SwingWorker<List<Transaction>, Void>() {
            @Override
            protected List<Transaction> doInBackground() {
                return transactionService.searchTransactions(householdId, filter);
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

    private TransactionFilter currentFilter() {
        LocalDate fromDate = parseOrNull(panel.getFromDateText());
        LocalDate toDate = parseOrNull(panel.getToDateText());
        return new TransactionFilter(fromDate, toDate, panel.getCategoryFilterText(),
                panel.getSelectedMemberFilterUserId(), panel.getKeywordFilterText());
    }

    private LocalDate parseOrNull(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? null : LocalDate.parse(trimmed);
    }

    private void openCreateDialog() {
        loadCategoriesThen(categories -> {
            Household household = householdContext.getCurrentHousehold();
            TransactionFormDialog dialog = new TransactionFormDialog(appFrame.getFrame());
            dialog.setMembers(household.getMembers());
            dialog.setCategories(categories);
            dialog.selectPayerById(session.getCurrentUser().getId());
            dialog.getCurrencyComboBox().setSelectedItem(household.getPrimaryCurrency());

            wireConversionPreview(dialog, household);
            dialog.getCreateButton().addActionListener(e -> handleCreateTransaction(dialog, household));
            dialog.setVisible(true);
        });
    }

    private void openEditDialog() {
        Transaction existing = panel.getSelectedTransaction();
        if (existing == null) {
            showError(panel, "Select a transaction to edit first.");
            return;
        }
        if (existing.getCategoryId() == null) {
            showError(panel, "Debt settlement records are generated automatically and can't be edited.");
            return;
        }
        loadCategoriesThen(categories -> {
            Household household = householdContext.getCurrentHousehold();
            TransactionFormDialog dialog = new TransactionFormDialog(appFrame.getFrame());
            dialog.setMembers(household.getMembers());
            dialog.setCategories(categories);
            populateForEdit(dialog, existing);

            wireConversionPreview(dialog, household);
            dialog.getCreateButton().addActionListener(e -> handleUpdateTransaction(dialog, household, existing.getId()));
            dialog.setVisible(true);
        });
    }

    /** Loads the household's categories on a worker, then hands them to {@code next} on the EDT. */
    private void loadCategoriesThen(java.util.function.Consumer<List<Category>> next) {
        Long householdId = householdContext.getCurrentHouseholdId();
        panel.getAddButton().setEnabled(false);
        new SwingWorker<List<Category>, Void>() {
            @Override
            protected List<Category> doInBackground() {
                return budgetService.getCategories(householdId);
            }

            @Override
            protected void done() {
                panel.getAddButton().setEnabled(true);
                try {
                    List<Category> categories = get();
                    if (categories.isEmpty()) {
                        showError(panel, "This household has no categories yet. Create one in the Budgets section first.");
                        return;
                    }
                    next.accept(categories);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(appFrame.getFrame(), "Could not load categories. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void populateForEdit(TransactionFormDialog dialog, Transaction existing) {
        dialog.getTypeComboBox().setSelectedItem(existing.getType() == TransactionType.INCOME ? "Income" : "Expense");
        dialog.selectCategoryById(existing.getCategoryId());
        dialog.getDateField().setText(existing.getDate().toString());
        dialog.getAmountField().setText(existing.getOriginalAmount().toPlainString());
        dialog.getCurrencyComboBox().setSelectedItem(existing.getCurrencyCode());
        dialog.getDescriptionField().setText(existing.getDescription());
        dialog.selectPayerById(existing.getPayerUserId());

        boolean shared = existing.isShared();
        dialog.getScopeComboBox().setSelectedItem(shared ? "Shared" : "Individual");
        if (shared) {
            Map<Long, BigDecimal> shareByUserId = new LinkedHashMap<>();
            for (SplitDetail split : existing.getSplits()) {
                shareByUserId.put(split.getUserId(), split.getShareAmount());
            }
            for (TransactionFormDialog.MemberSplitRow row : dialog.getMemberSplitRows()) {
                Long userId = row.getMember().getUser().getId();
                row.getIncludeCheckBox().setSelected(shareByUserId.containsKey(userId));
            }
        }
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
                        dialog.setConversionPreviewText("≈ " + converted + " " + household.getPrimaryCurrency());
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
        TransactionInput input = readInput(dialog);
        if (input == null) {
            return;
        }

        dialog.getCreateButton().setEnabled(false);
        new SwingWorker<Transaction, Void>() {
            @Override
            protected Transaction doInBackground() {
                return transactionService.createTransaction(household.getId(), input.payerUserId, input.type, input.date,
                        input.categoryId, input.description, input.currency, input.amount, input.scope,
                        input.sharedMemberIds, input.percentages);
            }

            @Override
            protected void done() {
                dialog.getCreateButton().setEnabled(true);
                try {
                    Transaction created = get();
                    dialog.dispose();
                    refresh();
                    warnIfBudgetExceeded(household.getId(), created);
                } catch (ExecutionException ex) {
                    handleTransactionError(ex, dialog);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleUpdateTransaction(TransactionFormDialog dialog, Household household, Long transactionId) {
        TransactionInput input = readInput(dialog);
        if (input == null) {
            return;
        }

        dialog.getCreateButton().setEnabled(false);
        new SwingWorker<Transaction, Void>() {
            @Override
            protected Transaction doInBackground() {
                return transactionService.updateTransaction(transactionId, input.payerUserId, input.type, input.date,
                        input.categoryId, input.description, input.currency, input.amount, input.scope,
                        input.sharedMemberIds, input.percentages);
            }

            @Override
            protected void done() {
                dialog.getCreateButton().setEnabled(true);
                try {
                    Transaction updated = get();
                    dialog.dispose();
                    applyFilters();
                    warnIfBudgetExceeded(household.getId(), updated);
                } catch (ExecutionException ex) {
                    handleTransactionError(ex, dialog);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleDelete() {
        Transaction existing = panel.getSelectedTransaction();
        if (existing == null) {
            showError(panel, "Select a transaction to delete first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(panel, "Delete this transaction?", "Confirm delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        panel.getDeleteButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                transactionService.deleteTransaction(existing.getId());
                return null;
            }

            @Override
            protected void done() {
                panel.getDeleteButton().setEnabled(true);
                try {
                    get();
                    applyFilters();
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(panel, "Could not delete the transaction. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Checks the just-saved expense against its category/global limit and shows a non-blocking warning (requirement 12). */
    private void warnIfBudgetExceeded(Long householdId, Transaction transaction) {
        if (transaction.getType() != TransactionType.EXPENSE) {
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return budgetService.checkLimitExceeded(householdId, transaction.getCategory(), transaction.getConvertedAmount());
            }

            @Override
            protected void done() {
                try {
                    String warning = get();
                    if (warning != null) {
                        JOptionPane.showMessageDialog(appFrame.getFrame(), warning, "Budget limit exceeded",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleTransactionError(ExecutionException ex, TransactionFormDialog dialog) {
        if (ex.getCause() instanceof RateNotConfiguredException rnc) {
            dialog.setConversionPreviewText("No exchange rate set for " + rnc.getCurrencyCode() + ".");
            dialog.showRatePrompt(rnc.getCurrencyCode());
        } else if (ex.getCause() instanceof TransactionException) {
            showError(dialog, ex.getCause().getMessage());
        } else {
            ex.getCause().printStackTrace();
            showError(dialog, "Something went wrong. Please try again.");
        }
    }

    private TransactionInput readInput(TransactionFormDialog dialog) {
        TransactionType type = "Income".equals(dialog.getTypeComboBox().getSelectedItem())
                ? TransactionType.INCOME : TransactionType.EXPENSE;

        LocalDate date;
        try {
            date = LocalDate.parse(dialog.getDateField().getText().trim());
        } catch (DateTimeParseException ex) {
            showError(dialog, "Enter the date as yyyy-mm-dd.");
            return null;
        }

        BigDecimal amount = parsePositiveAmount(dialog.getAmountField().getText());
        if (amount == null) {
            showError(dialog, "Amount must be a positive number.");
            return null;
        }

        String currency = currencyOf(dialog);
        Category category = (Category) dialog.getCategoryComboBox().getSelectedItem();
        if (category == null) {
            showError(dialog, "Select a category.");
            return null;
        }
        String description = dialog.getDescriptionField().getText();

        Member payer = (Member) dialog.getPayerComboBox().getSelectedItem();
        if (payer == null) {
            showError(dialog, "Select who paid.");
            return null;
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

        return new TransactionInput(type, date, category.getId(), description, currency, amount, payer.getUser().getId(),
                scope, sharedMemberIds, percentages);
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

    /** Plain holder for a validated form's fields, shared by create and update. */
    private static final class TransactionInput {
        final TransactionType type;
        final LocalDate date;
        final Long categoryId;
        final String description;
        final String currency;
        final BigDecimal amount;
        final Long payerUserId;
        final TransactionScope scope;
        final List<Long> sharedMemberIds;
        final Map<Long, BigDecimal> percentages;

        TransactionInput(TransactionType type, LocalDate date, Long categoryId, String description, String currency,
                         BigDecimal amount, Long payerUserId, TransactionScope scope, List<Long> sharedMemberIds,
                         Map<Long, BigDecimal> percentages) {
            this.type = type;
            this.date = date;
            this.categoryId = categoryId;
            this.description = description;
            this.currency = currency;
            this.amount = amount;
            this.payerUserId = payerUserId;
            this.scope = scope;
            this.sharedMemberIds = sharedMemberIds;
            this.percentages = percentages;
        }
    }
}
