package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.DebtSettlementPanel;
import cnu.ssd.pennywise.model.Debt;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.service.DebtSettlementService;
import cnu.ssd.pennywise.exception.TransactionException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.springframework.stereotype.Component;

/**
 * Drives {@link DebtSettlementPanel}: loads the current household's optimized
 * debts and handles "mark as settled". Every call into
 * {@link DebtSettlementService} runs on a {@link SwingWorker} so a slow
 * database round trip never freezes the EDT.
 */
@Component
public class DebtController {

    private final DebtSettlementService debtSettlementService;
    private final HouseholdContext householdContext;
    private final AppFrame appFrame;
    private final DebtSettlementPanel panel = new DebtSettlementPanel();

    public DebtController(DebtSettlementService debtSettlementService, HouseholdContext householdContext, AppFrame appFrame) {
        this.debtSettlementService = debtSettlementService;
        this.householdContext = householdContext;
        this.appFrame = appFrame;
        panel.getSettleButton().addActionListener(e -> handleSettle());
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Reloads the optimized debt list for the current household. */
    public void refresh() {
        Long householdId = householdContext.getCurrentHouseholdId();
        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();

        new SwingWorker<List<Debt>, Void>() {
            @Override
            protected List<Debt> doInBackground() {
                return debtSettlementService.getOptimizedDebts(householdId);
            }

            @Override
            protected void done() {
                try {
                    panel.setDebts(get(), primaryCurrency);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(appFrame.getFrame(), "Could not load debts. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleSettle() {
        Debt debt = panel.getSelectedDebt();
        if (debt == null) {
            showError(panel, "Select a debt to mark as settled first.");
            return;
        }
        Long householdId = householdContext.getCurrentHouseholdId();
        int confirm = JOptionPane.showConfirmDialog(panel,
                debt.getFromUsername() + " paid " + debt.getToUsername() + " " + debt.getAmount() + "?",
                "Confirm settlement", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        panel.getSettleButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                debtSettlementService.settleDebt(householdId, debt.getFromUserId(), debt.getToUserId(), debt.getAmount());
                return null;
            }

            @Override
            protected void done() {
                panel.getSettleButton().setEnabled(true);
                try {
                    get();
                    refresh();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof TransactionException) {
                        showError(panel, ex.getCause().getMessage());
                    } else {
                        ex.getCause().printStackTrace();
                        showError(panel, "Something went wrong. Please try again.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
