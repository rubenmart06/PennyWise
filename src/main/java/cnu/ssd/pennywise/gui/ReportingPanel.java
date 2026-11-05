package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.ReportingController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.PieChart;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.FinancialSummary;
import cnu.ssd.pennywise.model.Member;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

/**
 * The reporting view (View / Boundary): a period (from/to date) and member
 * selector, summary cards, a category breakdown {@link PieChart} and a
 * per-member totals table.
 * <p>
 * Contains no business logic: it only renders whatever {@link #setSummary}
 * is given. {@link ReportingController} reads the
 * period/member selection via getters, calls {@code ReportService}, and
 * pushes the resulting {@link FinancialSummary} back in.
 */
public class ReportingPanel extends JPanel {

    private static final String[] MEMBER_TOTAL_COLUMNS = {"Member", "Income", "Expense", "Net"};
    private static final String ALL_MEMBERS = "All members";

    private final javax.swing.JTextField fromDateField;
    private final javax.swing.JTextField toDateField;
    private final JComboBox<Object> memberComboBox = new JComboBox<>();
    private final AeroButton applyButton = new AeroButton("Apply", UiTheme.SKY);

    private final JLabel incomeValue = cardValue();
    private final JLabel expenseValue = cardValue();
    private final JLabel netValue = cardValue();

    private final PieChart pieChart = new PieChart();
    private final DefaultTableModel memberTableModel = new DefaultTableModel(MEMBER_TOTAL_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable memberTable = new JTable(memberTableModel);

    private String primaryCurrencyCode = "";

    public ReportingPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        fromDateField = dateField(LocalDate.now().withDayOfMonth(1).toString());
        toDateField = dateField(LocalDate.now().toString());

        JLabel title = new JLabel("Reports");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(buildFilterBar(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 14));
        middle.setOpaque(false);
        middle.add(buildSummaryCards(), BorderLayout.NORTH);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.add(buildPieSection());
        bottom.add(buildMemberSection());
        middle.add(bottom, BorderLayout.CENTER);

        body.add(middle, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private AeroPanel buildFilterBar() {
        AeroPanel bar = new AeroPanel(new FlowLayout(FlowLayout.LEFT, 10, 0), 14);
        bar.add(fieldLabel("From"));
        bar.add(fromDateField);
        bar.add(fieldLabel("To"));
        bar.add(toDateField);
        bar.add(fieldLabel("Member"));
        memberComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        bar.add(memberComboBox);
        bar.add(applyButton);
        return bar;
    }

    private JPanel buildSummaryCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(summaryCard("Total income", incomeValue, UiTheme.GREEN));
        row.add(summaryCard("Total expenses", expenseValue, UiTheme.RED));
        row.add(summaryCard("Net balance", netValue, UiTheme.SKY));
        return row;
    }

    private AeroPanel summaryCard(String label, JLabel valueLabel, Color accent) {
        AeroPanel card = new AeroPanel(new GridLayout(0, 1, 0, 4), 16);
        card.add(mutedLabel(label));
        valueLabel.setForeground(accent);
        card.add(valueLabel);
        return card;
    }

    private AeroPanel buildPieSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Spending by category"), BorderLayout.NORTH);
        pieChart.setPreferredSize(new java.awt.Dimension(340, 240));
        section.add(pieChart, BorderLayout.CENTER);
        return section;
    }

    private AeroPanel buildMemberSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Per-member totals"), BorderLayout.NORTH);
        section.add(AeroTableStyler.style(memberTable), BorderLayout.CENTER);
        return section;
    }

    /** Populates the member combo with "All members" plus each household member. Call before first use. */
    public void setMembers(List<Member> members) {
        memberComboBox.removeAllItems();
        memberComboBox.addItem(ALL_MEMBERS);
        for (Member member : members) {
            memberComboBox.addItem(member);
        }
    }

    /** Renders the loaded summary: cards, pie chart (expenses only) and per-member table. */
    public void setSummary(FinancialSummary summary, String primaryCurrencyCode) {
        this.primaryCurrencyCode = primaryCurrencyCode;
        incomeValue.setText(format(summary.getTotalIncome()));
        expenseValue.setText(format(summary.getTotalExpense()));
        netValue.setText(format(summary.getNetBalance()));

        List<FinancialSummary.CategoryTotal> categoryTotals = summary.getCategoryTotals();
        String[] names = new String[Math.max(1, categoryTotals.size())];
        double[] values = new double[Math.max(1, categoryTotals.size())];
        if (categoryTotals.isEmpty()) {
            names[0] = "No expenses";
            values[0] = 1;
        } else {
            for (int i = 0; i < categoryTotals.size(); i++) {
                names[i] = categoryTotals.get(i).getCategory();
                values[i] = categoryTotals.get(i).getAmount().doubleValue();
            }
        }
        pieChart.setData(names, values);

        memberTableModel.setRowCount(0);
        for (FinancialSummary.MemberTotal mt : summary.getMemberTotals()) {
            memberTableModel.addRow(new Object[]{
                    mt.getUsername(),
                    "+" + mt.getIncome().toPlainString(),
                    "-" + mt.getExpense().toPlainString(),
                    (mt.getNet().signum() >= 0 ? "+" : "") + mt.getNet().toPlainString()
            });
        }
    }

    private String format(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).toPlainString() + " " + primaryCurrencyCode;
    }

    public String getFromDateText() {
        return fromDateField.getText();
    }

    public String getToDateText() {
        return toDateField.getText();
    }

    /** The selected member's user id, or {@code null} if "All members" is selected. */
    public Long getSelectedMemberUserId() {
        Object selected = memberComboBox.getSelectedItem();
        return (selected instanceof Member member) ? member.getUser().getId() : null;
    }

    public AeroButton getApplyButton() {
        return applyButton;
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

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        l.setForeground(UiTheme.DEEP);
        return l;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 13));
        l.setForeground(UiTheme.INK);
        return l;
    }

    private static javax.swing.JTextField dateField(String text) {
        javax.swing.JTextField f = new javax.swing.JTextField(text, 11);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        f.setToolTipText("yyyy-mm-dd");
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x9CCBEA), 1, true), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }
}
