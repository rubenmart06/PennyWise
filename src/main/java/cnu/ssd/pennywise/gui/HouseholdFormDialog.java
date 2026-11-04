package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.HouseholdController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.Backdrop;
import cnu.ssd.pennywise.gui.components.UiTheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * The "New household" popup (View / Boundary).
 * <p>
 * Contains no business logic: it
 * only exposes its fields and buttons via getters,
 * {@link HouseholdController} reads them, calls the
 * Service, and disposes this dialog itself.
 */
public class HouseholdFormDialog extends JDialog {

    private final JTextField nameField = textField(16);
    private final JComboBox<String> currencyComboBox = new JComboBox<>(new String[]{"EUR", "KRW", "USD"});
    private final JComboBox<String> kindComboBox = new JComboBox<>(new String[]{"Individual", "Shared"});
    private final AeroButton createButton = new AeroButton("Create household", UiTheme.GREEN);
    private final AeroButton cancelButton = new AeroButton("Cancel", UiTheme.SKY);

    public HouseholdFormDialog(Frame owner) {
        super(owner, "New household", true);

        currencyComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        kindComboBox.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));

        Backdrop background = new Backdrop(new BorderLayout(0, 12), false);
        background.setBorder(new EmptyBorder(16, 16, 16, 16));

        AeroPanel form = new AeroPanel(new GridBagLayout(), 20);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, 0, "Name", nameField);
        addRow(form, c, 1, "Primary currency", currencyComboBox);
        addRow(form, c, 2, "Kind", kindComboBox);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(createButton);

        background.add(form, BorderLayout.CENTER);
        background.add(buttonRow, BorderLayout.SOUTH);

        setContentPane(background);
        cancelButton.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(owner);
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

    public JComboBox<String> getCurrencyComboBox() {
        return currencyComboBox;
    }

    public JComboBox<String> getKindComboBox() {
        return kindComboBox;
    }

    public AeroButton getCreateButton() {
        return createButton;
    }

    public AeroButton getCancelButton() {
        return cancelButton;
    }

    // ----- Styling helpers -----

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
