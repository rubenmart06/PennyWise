package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.MainDashboardPanel;
import cnu.ssd.pennywise.gui.components.ComingSoonPanel;
import cnu.ssd.pennywise.model.Household;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.model.HouseholdKind;
import org.springframework.stereotype.Component;

/**
 * Drives {@link MainDashboardPanel}: builds its sidebar cards out of the other
 * controllers' panels, switches between them, and bridges the
 * household hub into the dashboard itself via {@link #open}.
 */
@Component
public class MainDashboardController {

    private static final String DASHBOARD = "Dashboard";
    private static final String TRANSACTIONS = "Transactions";
    private static final String HOUSEHOLD = "Household";
    private static final String DEBTS = "Debts";
    private static final String BUDGETS = "Budgets";
    private static final String REPORTS = "Reports";
    private static final String SETTINGS = "Settings";

    private final AppFrame appFrame;
    private final HouseholdContext householdContext;
    private final HouseholdController householdController;
    private final TransactionController transactionController;
    private final CurrencyController currencyController;
    private final MainDashboardPanel frame = new MainDashboardPanel();

    public MainDashboardController(AppFrame appFrame, HouseholdContext householdContext,
                                   HouseholdController householdController, TransactionController transactionController,
                                   CurrencyController currencyController) {
        this.appFrame = appFrame;
        this.householdContext = householdContext;
        this.householdController = householdController;
        this.transactionController = transactionController;
        this.currencyController = currencyController;

        buildCards();
        wireListeners();
    }

    private void buildCards() {
        frame.addCard(DASHBOARD, new ComingSoonPanel(DASHBOARD));
        frame.addCard(TRANSACTIONS, transactionController.getPanel());
        frame.addCard(HOUSEHOLD, new ComingSoonPanel(HOUSEHOLD));
        frame.addCard(DEBTS, new ComingSoonPanel("Debt settlement"));
        frame.addCard(BUDGETS, new ComingSoonPanel("Categories & Budgets"));
        frame.addCard(REPORTS, new ComingSoonPanel(REPORTS));
        frame.addCard(SETTINGS, currencyController.getPanel());
    }

    private void wireListeners() {
        frame.getNavButtons().forEach((section, button) -> button.addActionListener(e -> navigateTo(section)));
        frame.getSwitchHouseholdButton().addActionListener(e -> {
            householdContext.clear();
            householdController.openHouseholdHub();
        });
        frame.getLogoutButton().addActionListener(e -> {
            householdContext.clear();
            householdController.logout();
        });
    }

    /** Called by {@link HouseholdController} when a household is opened from the hub. */
    public void open(Household household) {
        householdContext.setCurrentHousehold(household);
        String kindLabel = household.getKind() == HouseholdKind.SHARED ? "Shared" : "Individual";
        frame.setHouseholdName(household.getName() + " (" + kindLabel + " \u00b7 " + household.getPrimaryCurrency() + ")");
        appFrame.show(frame);
        navigateTo(TRANSACTIONS);
    }

    private void navigateTo(String section) {
        frame.showCard(section);
        if (TRANSACTIONS.equals(section)) {
            transactionController.refresh();
        } else if (SETTINGS.equals(section)) {
            currencyController.refresh();
        }
    }
}
