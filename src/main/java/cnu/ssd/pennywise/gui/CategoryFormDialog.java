package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.CategoryBudgetController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.Backdrop;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.Category;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * The "Create/Edit category" popup (View / Boundary), extracted and
 * modularized in the same style as {@link HouseholdFormDialog} and
 * {@link TransactionFormDialog}. Contains no business logic: it only exposes
 * its fields via getters — {@link CategoryBudgetController}
 * reads them, calls {@code BudgetService}, and disposes this dialog itself.
 * <p>
 * Used for both "create" and "edit": call {@link #populateFrom(Category)} to
 * preload an existing category's values before showing the dialog.
 */
public class CategoryFormDialog extends JDialog {

    private final JTextField nameField = textField(16);
    private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Expense", "Income"});
    private final JCheckBox hasLimitCheckBox = new JCheckBox("Set a monthly limit");
    private final JTextField limitField = textField(10);
    private final AeroButton saveButton;
    private final AeroButton cancelButton = new AeroButton("Cancel", UiTheme.SKY);

    public CategoryFormDialog(Frame owner, boolean editMode) {
        super(owner, editMode ? "Edit category" : "New category", true);
        saveButton = new AeroButton(editMode ? "Save changes" : "Create category", UiTheme.GREEN);

        typeComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        limitField.setEnabled(false);
        hasLimitCheckBox.setOpaque(false);
        hasLimitCheckBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        hasLimitCheckBox.addItemListener(e -> limitField.setEnabled(hasLimitCheckBox.isSelected()));

        Backdrop background = new Backdrop(new BorderLayout(0, 12), false);
        background.setBorder(new EmptyBorder(16, 16, 16, 16));

        AeroPanel form = new AeroPanel(new GridBagLayout(), 20);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        addRow(form, c, 0, "Name", nameField);
        addRow(form, c, 1, "Type", typeComboBox);
        addRow(form, c, 2, "", hasLimitCheckBox);
        addRow(form, c, 3, "Limit amount", limitField);

        javax.swing.JPanel buttonRow = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);

        background.add(form, BorderLayout.CENTER);
        background.add(buttonRow, BorderLayout.SOUTH);
        setContentPane(background);

        cancelButton.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(owner);
    }

    /** Preloads this dialog's fields from an existing category, for "edit" mode. */
    public void populateFrom(Category category) {
        nameField.setText(category.getName());
        typeComboBox.setSelectedItem(category.getType() == cnu.ssd.pennywise.model.TransactionType.INCOME ? "Income" : "Expense");
        boolean hasLimit = category.hasLimit();
        hasLimitCheckBox.setSelected(hasLimit);
        limitField.setEnabled(hasLimit);
        limitField.setText(hasLimit ? category.getLimitAmount().toPlainString() : "");
    }

    private static void addRow(AeroPanel form, GridBagConstraints c, int row, String labelText, java.awt.Component field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        form.add(label(labelText), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
    }

    // ----- Getters the Controller reads values / wires listeners through -----

    public JTextField getNameField() {
        return nameField;
    }

    public JComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    public JCheckBox getHasLimitCheckBox() {
        return hasLimitCheckBox;
    }

    public JTextField getLimitField() {
        return limitField;
    }

    public AeroButton getSaveButton() {
        return saveButton;
    }

    public AeroButton getCancelButton() {
        return cancelButton;
    }

    private static JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x9CCBEA), 1, true), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, Font.BOLD, 13));
        l.setForeground(UiTheme.INK);
        return l;
    }
}
