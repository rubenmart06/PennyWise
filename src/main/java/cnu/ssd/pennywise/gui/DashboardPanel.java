package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.MainDashboardController;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.BudgetBar;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.BudgetLimit;
import cnu.ssd.pennywise.model.FinancialSummary;
import cnu.ssd.pennywise.model.Transaction;
import cnu.ssd.pennywise.model.TransactionType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The at-a-glance overview shown when a household is first opened (View /
 * Boundary): three summary cards (income/expense/net for the current month),
 * the budget bars from {@code CategoryBudgetPanel}'s data, and a short list
 * of the most recent transactions.
 * <p>
 * Contains no business logic: it only renders whatever
 * {@link #setSummary}/{@link #setBudgetOverview}/{@link #setRecentTransactions}
 * are given. {@link MainDashboardController} loads
 * that data (via {@code SwingWorker}) and pushes it in on every visit.
 */
public class DashboardPanel extends JPanel {

    private static final String[] RECENT_COLUMNS = {"Date", "Type", "Category", "Paid by", "Amount"};

    private final JLabel incomeValue = cardValue();
    private final JLabel expenseValue = cardValue();
    private final JLabel netValue = cardValue();

    private final JPanel budgetBarsPanel = new JPanel();
    private final JLabel noBudgetsLabel = mutedLabel("No budget limits set yet.");

    private final DefaultTableModel recentTableModel = new DefaultTableModel(RECENT_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable recentTable = new JTable(recentTableModel);

    private String primaryCurrencyCode = "";

    public DashboardPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(buildSummaryCards(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridLayout(1, 2, 14, 0));
        middle.setOpaque(false);
        middle.add(buildBudgetSection());
        middle.add(buildRecentSection());
        body.add(middle, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    private JPanel buildSummaryCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(summaryCard("Income this month", incomeValue, UiTheme.GREEN));
        row.add(summaryCard("Expenses this month", expenseValue, UiTheme.RED));
        row.add(summaryCard("Net balance", netValue, UiTheme.SKY));
        return row;
    }

    private AeroPanel summaryCard(String label, JLabel valueLabel, Color accent) {
        AeroPanel card = new AeroPanel(new GridLayout(0, 1, 0, 4), 16);
        JLabel caption = mutedLabel(label);
        card.add(caption);
        card.add(valueLabel);
        valueLabel.setForeground(accent);
        return card;
    }

    private AeroPanel buildBudgetSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        JLabel heading = sectionHeading("Budget overview");
        section.add(heading, BorderLayout.NORTH);

        budgetBarsPanel.setLayout(new BoxLayout(budgetBarsPanel, BoxLayout.Y_AXIS));
        budgetBarsPanel.setOpaque(false);
        section.add(budgetBarsPanel, BorderLayout.CENTER);
        return section;
    }

    private AeroPanel buildRecentSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(sectionHeading("Recent transactions"), BorderLayout.NORTH);
        section.add(AeroTableStyler.style(recentTable), BorderLayout.CENTER);
        return section;
    }

    /** The current month's totals, shown in the three summary cards. */
    public void setSummary(FinancialSummary summary, String primaryCurrencyCode) {
        this.primaryCurrencyCode = primaryCurrencyCode;
        incomeValue.setText(format(summary.getTotalIncome()));
        expenseValue.setText(format(summary.getTotalExpense()));
        netValue.setText(format(summary.getNetBalance()));
    }

    /** The household-wide and per-category limits, each as a labeled {@link BudgetBar}. */
    public void setBudgetOverview(List<BudgetLimit> budgetLimits) {
        budgetBarsPanel.removeAll();
        if (budgetLimits.isEmpty()) {
            budgetBarsPanel.add(noBudgetsLabel);
        }
        for (BudgetLimit limit : budgetLimits) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 4, 0));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 0, 2, 8);
            c.gridy = 0;
            c.gridx = 0;
            c.weightx = .4;
            c.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel(limit.getLabel() + "  (" + format(limit.getSpentAmount()) + " / "
                    + format(limit.getLimitAmount()) + ")");
            label.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
            label.setForeground(UiTheme.INK);
            row.add(label, c);
            c.gridx = 1;
            c.weightx = .6;
            BudgetBar bar = new BudgetBar(limit.getUsageRatio());
            row.add(bar, c);
            budgetBarsPanel.add(row);
        }
        budgetBarsPanel.revalidate();
        budgetBarsPanel.repaint();
    }

    /** The five most recent records, already limited to five by the Controller. */
    public void setRecentTransactions(List<Transaction> transactions) {
        recentTableModel.setRowCount(0);
        for (Transaction t : transactions) {
            String sign = t.getType() == TransactionType.EXPENSE ? "-" : "+";
            recentTableModel.addRow(new Object[]{
                    t.getDate().toString(),
                    t.getType() == TransactionType.EXPENSE ? "Expense" : "Income",
                    t.getCategory(),
                    t.getPayerUsername(),
                    sign + t.getConvertedAmount().toPlainString() + " " + primaryCurrencyCode
            });
        }
    }

    private String format(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).toPlainString() + " " + primaryCurrencyCode;
    }

    private static JLabel cardValue() {
        JLabel l = new JLabel("0.00");
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 22));
        return l;
    }

    private static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
        l.setForeground(UiTheme.MUTED);
        return l;
    }

    private static JLabel sectionHeading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        l.setForeground(UiTheme.DEEP);
        return l;
    }
}
