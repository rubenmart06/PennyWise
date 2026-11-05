package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.DebtController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.Debt;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The debt-settlement view (View / Boundary): the household's minimal set of
 * "who owes whom" pairs, each with a "Mark as settled"
 * action.
 * <p>
 * Contains no business logic — the optimization itself lives in
 * {@code DebtSettlementService}. This panel only renders whatever
 * {@link #setDebts} is given, and exposes the selected row via
 * {@link #getSelectedDebt()} so {@link DebtController}
 * can call the Service.
 */
public class DebtSettlementPanel extends JPanel {

    private static final String[] COLUMNS = {"From", "To", "Amount"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final AeroButton settleButton = new AeroButton("Mark as settled", UiTheme.GREEN);
    private final JLabel emptyLabel = new JLabel("Every shared expense is settled up — nothing owed.");

    private List<Debt> debts = List.of();
    private String primaryCurrencyCode = "";

    public DebtSettlementPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JLabel title = new JLabel("Debt settlement");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        AeroPanel panel = new AeroPanel(new BorderLayout(0, 10), 16);
        JLabel heading = new JLabel("Optimized debts");
        heading.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        heading.setForeground(UiTheme.DEEP);
        panel.add(heading, BorderLayout.NORTH);

        emptyLabel.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        emptyLabel.setForeground(UiTheme.MUTED);
        emptyLabel.setVisible(false);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(AeroTableStyler.style(table), BorderLayout.CENTER);
        center.add(emptyLabel, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(settleButton);
        panel.add(actions, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
    }

    /** Rebuilds the table from the optimized debt list, formatting amounts in the household's primary currency. */
    public void setDebts(List<Debt> debts, String primaryCurrencyCode) {
        this.debts = debts;
        this.primaryCurrencyCode = primaryCurrencyCode;
        tableModel.setRowCount(0);
        for (Debt debt : debts) {
            tableModel.addRow(new Object[]{
                    debt.getFromUsername(),
                    debt.getToUsername(),
                    debt.getAmount().toPlainString() + " " + primaryCurrencyCode
            });
        }
        emptyLabel.setVisible(debts.isEmpty());
        table.setVisible(!debts.isEmpty());
    }

    /** The currently selected debt, or {@code null} if nothing is selected. */
    public Debt getSelectedDebt() {
        int row = table.getSelectedRow();
        return (row < 0 || row >= debts.size()) ? null : debts.get(row);
    }

    public AeroButton getSettleButton() {
        return settleButton;
    }
}
