package cnu.ssd.pennywise.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cnu.ssd.pennywise.exception.CurrencyException;
import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.SettingsPanel;
import cnu.ssd.pennywise.model.ExchangeRate;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.service.CurrencyService;
import org.springframework.stereotype.Component;

/**
 * Drives {@link SettingsPanel}: loads the current household's exchange rates
 * and handles adding/modifying one. Every call into {@link CurrencyService}
 * runs on a {@link SwingWorker} so a slow database round trip never freezes
 * the EDT.
 */
@Component
public class CurrencyController {

    private final CurrencyService currencyService;
    private final HouseholdContext householdContext;
    private final AppFrame appFrame;
    private final SettingsPanel panel = new SettingsPanel();

    public CurrencyController(CurrencyService currencyService, HouseholdContext householdContext, AppFrame appFrame) {
        this.currencyService = currencyService;
        this.householdContext = householdContext;
        this.appFrame = appFrame;
        wireListeners();
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Reloads the panel's rate table from the current household. */
    public void refresh() {
        panel.setPrimaryCurrencyCode(householdContext.getCurrentHousehold().getPrimaryCurrency());
        Long householdId = householdContext.getCurrentHouseholdId();

        setButtonsEnabled(false);
        new SwingWorker<List<ExchangeRate>, Void>() {
            @Override
            protected List<ExchangeRate> doInBackground() {
                return currencyService.getRatesForHousehold(householdId);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    panel.setRates(get());
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError("Could not load exchange rates. Please try again.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void wireListeners() {
        panel.getAddRateButton().addActionListener(e -> promptAndSaveRate(null));
        panel.getModifyRateButton().addActionListener(e -> {
            String code = panel.getSelectedCurrencyCode();
            if (code == null) {
                JOptionPane.showMessageDialog(appFrame.getFrame(), "Select a currency row to modify first.",
                        "No selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            promptAndSaveRate(code);
        });
    }

    private void promptAndSaveRate(String existingCode) {
        String code = existingCode;
        if (code == null) {
            code = JOptionPane.showInputDialog(appFrame.getFrame(), "Currency code (e.g. JPY):");
            if (code == null || code.isBlank()) {
                return;
            }
            code = code.trim().toUpperCase();
        }

        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();
        String rateText = JOptionPane.showInputDialog(appFrame.getFrame(),
                "1 " + primaryCurrency + " = ? " + code, existingCode == null ? "" : "");
        if (rateText == null) {
            return;
        }
        BigDecimal rate;
        try {
            rate = new BigDecimal(rateText.trim());
        } catch (NumberFormatException ex) {
            showError("Enter a valid numeric rate.");
            return;
        }

        String finalCode = code;
        Long householdId = householdContext.getCurrentHouseholdId();
        setButtonsEnabled(false);
        new SwingWorker<ExchangeRate, Void>() {
            @Override
            protected ExchangeRate doInBackground() {
                return currencyService.setRate(householdId, finalCode, rate);
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    get();
                    refresh();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof CurrencyException) {
                        showError(ex.getCause().getMessage());
                    } else {
                        ex.getCause().printStackTrace();
                        showError("Something went wrong. Please try again.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        panel.getAddRateButton().setEnabled(enabled);
        panel.getModifyRateButton().setEnabled(enabled);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(appFrame.getFrame(), message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
