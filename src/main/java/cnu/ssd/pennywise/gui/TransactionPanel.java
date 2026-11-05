package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.*;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
    private static final String ALL_MEMBERS = "All members";
    private static final String ALL_CATEGORIES = "All categories";

    private final JTextField fromDateField = dateField();
    private final JTextField toDateField = dateField();
    private final JComboBox<Object> categoryComboBox = new JComboBox<>();
    private final JComboBox<Object> memberComboBox = new JComboBox<>();
    private final JTextField keywordField = smallField();
    private final AeroButton searchButton = new AeroButton("Search", UiTheme.SKY);
    private final AeroButton clearFiltersButton = new AeroButton("Clear", UiTheme.DEEP);

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final AeroButton addButton = new AeroButton("+ Add record", UiTheme.GREEN);
    private final AeroButton editButton = new AeroButton("Edit", UiTheme.SKY);
    private final AeroButton deleteButton = new AeroButton("Delete", UiTheme.RED);
    private final AeroButton exportButton = new AeroButton("Export", UiTheme.DEEP);

    private List<Transaction> transactions = List.of();

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
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(exportButton);
        actions.add(addButton);
        top.add(actions, BorderLayout.EAST);

        AeroPanel list = new AeroPanel(new BorderLayout(0, 10), 16);
        JLabel listTitle = new JLabel("Transaction history");
        listTitle.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        listTitle.setForeground(UiTheme.DEEP);
        list.add(listTitle, BorderLayout.NORTH);
        list.add(AeroTableStyler.style(table), BorderLayout.CENTER);
        list.add(buildFilterBar(), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
    }

    /** Two stacked rows (a single FlowLayout row wrapped and clipped its last button on narrow windows). */
    private AeroPanel buildFilterBar() {
        categoryComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        memberComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        keywordField.setToolTipText("Matches the description or category");

        JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        rowOne.setOpaque(false);
        rowOne.add(fieldLabel("From"));
        rowOne.add(fromDateField);
        rowOne.add(fieldLabel("To"));
        rowOne.add(toDateField);
        rowOne.add(fieldLabel("Category"));
        rowOne.add(categoryComboBox);
        rowOne.add(fieldLabel("Member"));
        rowOne.add(memberComboBox);

        JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        rowTwo.setOpaque(false);
        rowTwo.add(fieldLabel("Keyword"));
        rowTwo.add(keywordField);
        rowTwo.add(searchButton);
        rowTwo.add(clearFiltersButton);

        AeroPanel bar = new AeroPanel(new java.awt.GridLayout(0, 1, 0, 2), 10);
        bar.add(rowOne);
        bar.add(rowTwo);
        return bar;
    }

    /** Populates the category filter combo with "All categories" plus each household category. */
    public void setCategories(List<Category> categories) {
        categoryComboBox.removeAllItems();
        categoryComboBox.addItem(ALL_CATEGORIES);
        for (Category category : categories) {
            categoryComboBox.addItem(category);
        }
    }

    /** Populates the member filter combo with "All members" plus each household member. Call before first use. */
    public void setMembers(List<Member> members) {
        memberComboBox.removeAllItems();
        memberComboBox.addItem(ALL_MEMBERS);
        for (Member member : members) {
            memberComboBox.addItem(member);
        }
    }

    /** Rebuilds the table from the given transactions, formatting amounts in {@code primaryCurrencyCode}. */
    public void setTransactions(List<Transaction> transactions, String primaryCurrencyCode) {
        this.transactions = transactions;
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

    /** Clears every filter field back to "no filter". */
    public void clearFilters() {
        fromDateField.setText("");
        toDateField.setText("");
        if (categoryComboBox.getItemCount() > 0) {
            categoryComboBox.setSelectedIndex(0);
        }
        keywordField.setText("");
        if (memberComboBox.getItemCount() > 0) {
            memberComboBox.setSelectedIndex(0);
        }
    }

    /** The currently selected transaction, or {@code null} if nothing is selected. */
    public Transaction getSelectedTransaction() {
        int row = table.getSelectedRow();
        return (row < 0 || row >= transactions.size()) ? null : transactions.get(row);
    }

    public String getFromDateText() {
        return fromDateField.getText();
    }

    public String getToDateText() {
        return toDateField.getText();
    }

    /** The selected category's name, or {@code null} if "All categories" is selected. */
    public String getCategoryFilterText() {
        Object selected = categoryComboBox.getSelectedItem();
        return (selected instanceof Category category) ? category.getName() : null;
    }

    public String getKeywordFilterText() {
        return keywordField.getText();
    }

    /** The selected member's user id, or {@code null} if "All members" is selected. */
    public Long getSelectedMemberFilterUserId() {
        Object selected = memberComboBox.getSelectedItem();
        return (selected instanceof Member member) ? member.getUser().getId() : null;
    }

    public AeroButton getAddButton() {
        return addButton;
    }

    public AeroButton getEditButton() {
        return editButton;
    }

    public AeroButton getDeleteButton() {
        return deleteButton;
    }

    public AeroButton getExportButton() {
        return exportButton;
    }

    public AeroButton getSearchButton() {
        return searchButton;
    }

    public AeroButton getClearFiltersButton() {
        return clearFiltersButton;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 12));
        l.setForeground(UiTheme.INK);
        return l;
    }

    private static JTextField dateField() {
        JTextField f = new JTextField(10);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
        f.setToolTipText("yyyy-mm-dd");
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x9CCBEA), 1, true), new EmptyBorder(4, 6, 4, 6)));
        return f;
    }

    private static JTextField smallField() {
        JTextField f = new JTextField(10);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x9CCBEA), 1, true), new EmptyBorder(4, 6, 4, 6)));
        return f;
    }
}
