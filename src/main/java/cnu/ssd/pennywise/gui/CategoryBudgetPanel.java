package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.CategoryBudgetController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.BudgetBar;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.BudgetLimit;
import cnu.ssd.pennywise.model.Category;
import cnu.ssd.pennywise.model.TransactionType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

/**
 * The categories & budgets view (View / Boundary): a table of categories
 * with Create/Edit/Delete actions, plus the household-wide global limit (with
 * its own {@link BudgetBar}) and a usage bar per category that has a limit.
 * Extracted and modularized from the prototype's {@code budgets()} page.
 * <p>
 * Contains no business logic: it only renders whatever {@link #setCategories}
 * and {@link #setBudgetOverview} are given.
 * {@link CategoryBudgetController} loads that data,
 * wires the buttons, and drives {@link CategoryFormDialog}.
 */
public class CategoryBudgetPanel extends JPanel {

    private static final String[] COLUMNS = {"Name", "Type", "Monthly limit"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final AeroButton addButton = new AeroButton("+ Add category", UiTheme.GREEN);
    private final AeroButton editButton = new AeroButton("Edit", UiTheme.SKY);
    private final AeroButton deleteButton = new AeroButton("Delete", UiTheme.RED);

    private final JLabel globalLimitUsageLabel = new JLabel(" ");
    private final BudgetBar globalLimitBar = new BudgetBar(0);
    private final javax.swing.JTextField globalLimitField = new javax.swing.JTextField(8);
    private final AeroButton saveGlobalLimitButton = new AeroButton("Save limit", UiTheme.SKY);

    private final JPanel categoryBarsPanel = new JPanel();

    private List<Category> categories = List.of();

    public CategoryBudgetPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JLabel title = new JLabel("Categories & Budgets");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(buildGlobalLimitSection(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridLayout(1, 2, 14, 0));
        middle.setOpaque(false);
        middle.add(buildCategoryTableSection());
        middle.add(buildCategoryBarsSection());
        body.add(middle, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
    }

    private AeroPanel buildGlobalLimitSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 8), 16);
        section.add(heading("Household-wide monthly limit"), BorderLayout.NORTH);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 2, 8);
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = .45;
        c.fill = GridBagConstraints.HORIZONTAL;
        row.add(globalLimitUsageLabel, c);
        c.gridx = 1;
        c.weightx = .55;
        row.add(globalLimitBar, c);
        section.add(row, BorderLayout.CENTER);

        globalLimitField.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        globalLimitField.setBorder(new CompoundBorder(new LineBorder(new Color(0x9CCBEA), 1, true), new EmptyBorder(5, 8, 5, 8)));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(new JLabel("New limit:"));
        actions.add(globalLimitField);
        actions.add(saveGlobalLimitButton);
        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private AeroPanel buildCategoryTableSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Categories"), BorderLayout.NORTH);
        section.add(AeroTableStyler.style(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private AeroPanel buildCategoryBarsSection() {
        AeroPanel section = new AeroPanel(new BorderLayout(0, 10), 16);
        section.add(heading("Usage this month"), BorderLayout.NORTH);
        categoryBarsPanel.setLayout(new BoxLayout(categoryBarsPanel, BoxLayout.Y_AXIS));
        categoryBarsPanel.setOpaque(false);
        section.add(categoryBarsPanel, BorderLayout.CENTER);
        return section;
    }

    /** Rebuilds the category table. Keeps the list so a selected row maps back to a {@link Category}. */
    public void setCategories(List<Category> categories) {
        this.categories = categories;
        tableModel.setRowCount(0);
        for (Category category : categories) {
            tableModel.addRow(new Object[]{
                    category.getName(),
                    category.getType() == TransactionType.INCOME ? "Income" : "Expense",
                    category.hasLimit() ? category.getLimitAmount().toPlainString() : "—"
            });
        }
    }

    /**
     * Shows the global limit's usage bar (first row whose label is "Global limit", if any) and
     * one usage row per remaining category limit.
     */
    public void setBudgetOverview(List<BudgetLimit> overview) {
        BudgetLimit global = overview.stream().filter(b -> b.getLabel().equals("Global limit")).findFirst().orElse(null);
        if (global != null) {
            globalLimitBar.setRatio(global.getUsageRatio());
            globalLimitUsageLabel.setText(global.getSpentAmount().toPlainString() + " / " + global.getLimitAmount().toPlainString());
        } else {
            globalLimitBar.setRatio(0);
            globalLimitUsageLabel.setText("No global limit set.");
        }

        categoryBarsPanel.removeAll();
        Map<String, BudgetLimit> byLabel = overview.stream()
                .filter(b -> !b.getLabel().equals("Global limit"))
                .collect(java.util.stream.Collectors.toMap(BudgetLimit::getLabel, b -> b));
        if (byLabel.isEmpty()) {
            JLabel none = new JLabel("No category limits set yet.");
            none.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
            none.setForeground(UiTheme.MUTED);
            categoryBarsPanel.add(none);
        }
        for (BudgetLimit limit : byLabel.values()) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 4, 0));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 0, 2, 8);
            c.gridy = 0;
            c.gridx = 0;
            c.weightx = .4;
            c.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel(limit.getLabel());
            label.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
            label.setForeground(UiTheme.INK);
            row.add(label, c);
            c.gridx = 1;
            c.weightx = .6;
            row.add(new BudgetBar(limit.getUsageRatio()), c);
            categoryBarsPanel.add(row);
        }
        categoryBarsPanel.revalidate();
        categoryBarsPanel.repaint();
    }

    /** The currently selected category, or {@code null} if nothing is selected. */
    public Category getSelectedCategory() {
        int row = table.getSelectedRow();
        return (row < 0 || row >= categories.size()) ? null : categories.get(row);
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

    public javax.swing.JTextField getGlobalLimitField() {
        return globalLimitField;
    }

    public AeroButton getSaveGlobalLimitButton() {
        return saveGlobalLimitButton;
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        l.setForeground(UiTheme.DEEP);
        return l;
    }
}
