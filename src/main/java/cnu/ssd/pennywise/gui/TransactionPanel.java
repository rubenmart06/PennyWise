package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.Transaction;
import cnu.ssd.pennywise.model.TransactionScope;
import cnu.ssd.pennywise.model.TransactionType;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The transaction history view (View / Boundary): a table of records plus a
 * "+ Add record" button.
 * <p>
 * Contains no business logic: it only renders whatever {@link #setTransactions}
 * is given, in the household's primary currency passed alongside it.
 */
public class TransactionPanel extends JPanel {

    private static final String[] COLUMNS = {"Date", "Type", "Category", "Description", "Paid by", "Scope", "Amount"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final AeroButton addButton = new AeroButton("+ Add record", UiTheme.GREEN);

    public TransactionPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Transactions");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        top.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        top.add(actions, BorderLayout.EAST);

        AeroPanel list = new AeroPanel(new BorderLayout(0, 10), 16);
        JLabel listTitle = new JLabel("Transaction history");
        listTitle.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        listTitle.setForeground(UiTheme.DEEP);
        list.add(listTitle, BorderLayout.NORTH);
        list.add(AeroTableStyler.style(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
    }

    /** Rebuilds the table from the given transactions, formatting amounts in {@code primaryCurrencyCode}. */
    public void setTransactions(List<Transaction> transactions, String primaryCurrencyCode) {
        tableModel.setRowCount(0);
        for (Transaction t : transactions) {
            String sign = t.getType() == TransactionType.EXPENSE ? "-" : "+";
            String amount = String.format("%s%.2f %s", sign, t.getConvertedAmount(), primaryCurrencyCode);
            tableModel.addRow(new Object[]{
                    t.getDate().toString(),
                    t.getType() == TransactionType.EXPENSE ? "Expense" : "Income",
                    t.getCategory(),
                    t.getDescription(),
                    t.getPayerUsername(),
                    t.getScope() == TransactionScope.SHARED ? "Shared" : "Individual",
                    amount
            });
        }
    }

    public AeroButton getAddButton() {
        return addButton;
    }
}
