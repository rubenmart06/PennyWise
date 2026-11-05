package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.DashboardPanel;
import cnu.ssd.pennywise.gui.MainDashboardPanel;
import cnu.ssd.pennywise.gui.components.ComingSoonPanel;
import cnu.ssd.pennywise.model.*;
import cnu.ssd.pennywise.service.BudgetService;
import cnu.ssd.pennywise.service.ReportService;
import cnu.ssd.pennywise.service.TransactionService;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Drives {@link MainDashboardPanel}: builds its sidebar cards out of the other
 * controllers' panels, switches between them, and bridges the
 * household hub into the dashboard itself via {@link #open}.
 * Also loads {@link DashboardPanel}'s own data directly (summary, budget bars,
 * recent transactions) since that view has no dedicated Controller of its own
 * — it's a read-only composite of data the other Services already expose.
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
    private final CategoryBudgetController categoryBudgetController;
    private final DebtController debtController;
    private final ReportingController reportingController;
    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private final ReportService reportService;
    private final MainDashboardPanel frame = new MainDashboardPanel();
    private final DashboardPanel dashboardPanel = new DashboardPanel();

    public MainDashboardController(AppFrame appFrame, HouseholdContext householdContext,
                                   HouseholdController householdController, TransactionController transactionController,
                                   CurrencyController currencyController, CategoryBudgetController categoryBudgetController,
                                   DebtController debtController, ReportingController reportingController,
                                   BudgetService budgetService, TransactionService transactionService,
                                   ReportService reportService) {
        this.appFrame = appFrame;
        this.householdContext = householdContext;
        this.householdController = householdController;
        this.transactionController = transactionController;
        this.currencyController = currencyController;
        this.categoryBudgetController = categoryBudgetController;
        this.debtController = debtController;
        this.reportingController = reportingController;
        this.budgetService = budgetService;
        this.transactionService = transactionService;
        this.reportService = reportService;

        buildCards();
        wireListeners();
    }

    private void buildCards() {
        frame.addCard(DASHBOARD, dashboardPanel);
        frame.addCard(TRANSACTIONS, transactionController.getPanel());
        frame.addCard(HOUSEHOLD, householdController.getHouseholdPanel());
        frame.addCard(DEBTS, debtController.getPanel());
        frame.addCard(BUDGETS, categoryBudgetController.getPanel());
        frame.addCard(REPORTS, reportingController.getPanel());
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
        frame.setHouseholdName(household.getName() + " (" + kindLabel + " · " + household.getPrimaryCurrency() + ")");
        appFrame.show(frame);
        navigateTo(DASHBOARD);
    }

    private void navigateTo(String section) {
        frame.showCard(section);
        switch (section) {
            case DASHBOARD -> refreshDashboard();
            case TRANSACTIONS -> transactionController.refresh();
            case HOUSEHOLD -> householdController.refreshHouseholdPanel();
            case DEBTS -> debtController.refresh();
            case BUDGETS -> categoryBudgetController.refresh();
            case REPORTS -> reportingController.refresh();
            case SETTINGS -> currencyController.refresh();
            default -> {
            }
        }
    }

    private void refreshDashboard() {
        Long householdId = householdContext.getCurrentHouseholdId();
        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                FinancialSummary summary = reportService.getSummary(householdId, monthStart, LocalDate.now(), null);
                List<BudgetLimit> overview = budgetService.getBudgetOverview(householdId);
                List<Transaction> recent = transactionService.getTransactionsForHousehold(householdId).stream()
                        .sorted(Comparator.comparing(Transaction::getDate).reversed())
                        .limit(5)
                        .collect(Collectors.toList());
                return new Object[]{summary, overview, recent};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] result = get();
                    dashboardPanel.setSummary((FinancialSummary) result[0], primaryCurrency);
                    dashboardPanel.setBudgetOverview((List<BudgetLimit>) result[1]);
                    dashboardPanel.setRecentTransactions((List<Transaction>) result[2]);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
