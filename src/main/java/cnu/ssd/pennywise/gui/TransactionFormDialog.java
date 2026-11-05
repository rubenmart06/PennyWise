package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.TransactionController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.Backdrop;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.Category;
import cnu.ssd.pennywise.model.Member;
import cnu.ssd.pennywise.model.TransactionType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * The "Add/Edit financial record" popup (View / Boundary).
 * <p>
 * Contains no business
 * logic: it only exposes its fields via getters and toggles its own layout
 * (shared-split section, percentage spinners, rate prompt) in response to its
 * own widgets, pure navigation/visibility, the same kind {@link AuthPanel}
 * already does when switching between its login and sign-up cards.
 * <p>
 * {@link TransactionController} reads every field
 * via its getters, drives the live conversion preview with
 * {@link #setConversionPreviewText}, and calls the Service.
 */
public class TransactionFormDialog extends JDialog {

    private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"Expense", "Income"});
    private final JTextField dateField = textField(LocalDate.now().toString(), 12);
    private final JTextField amountField = textField("", 10);
    private final JComboBox<String> currencyComboBox = new JComboBox<>(new String[]{"EUR", "USD", "KRW", "GBP", "JPY"});
    private final JLabel conversionPreviewLabel = new JLabel(" ");
    private final JComboBox<Category> categoryComboBox = new JComboBox<>();
    private List<Category> allCategories = List.of();
    private final JTextField descriptionField = textField("", 14);
    private final JComboBox<Member> payerComboBox = new JComboBox<>();
    private final JComboBox<String> scopeComboBox = new JComboBox<>(new String[]{"Individual", "Shared"});

    private final JComboBox<String> splitModeComboBox = new JComboBox<>(new String[]{"Split equally", "By percentage"});
    private final JPanel memberRowsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JPanel sharedPanel;
    private final List<MemberSplitRow> memberSplitRows = new ArrayList<>();

    private final JLabel ratePromptLabel = new JLabel(" ");
    private final JTextField rateField = textField("", 8);
    private final AeroButton saveRateButton = new AeroButton("Save rate", UiTheme.SKY);
    private final JPanel ratePromptPanel;
    private String promptedCurrencyCode;

    private final AeroButton createButton = new AeroButton("Save record", UiTheme.GREEN);
    private final AeroButton cancelButton = new AeroButton("Cancel", UiTheme.SKY);

    public TransactionFormDialog(Frame owner) {
        super(owner, "Add financial record", true);

        for (JComboBox<?> combo : List.of(typeComboBox, currencyComboBox, categoryComboBox, payerComboBox, scopeComboBox, splitModeComboBox)) {
            combo.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
            combo.setBackground(Color.WHITE);
        }
        currencyComboBox.setEditable(true);
        conversionPreviewLabel.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
        conversionPreviewLabel.setForeground(UiTheme.MUTED);
        dateField.setToolTipText("yyyy-mm-dd");

        sharedPanel = buildSharedPanel();
        sharedPanel.setVisible(false);
        ratePromptPanel = buildRatePromptPanel();
        ratePromptPanel.setVisible(false);

        Backdrop background = new Backdrop(new BorderLayout(0, 12), false);
        background.setBorder(new EmptyBorder(16, 16, 16, 16));

        AeroPanel form = new AeroPanel(new GridBagLayout(), 20);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        addRow(form, c, 0, "Type", typeComboBox);
        addRow(form, c, 1, "Date", dateField);
        addRow(form, c, 2, "Amount", amountField);
        addRow(form, c, 3, "Currency", currencyComboBox);
        addFullRow(form, c, 4, conversionPreviewLabel);
        addFullRow(form, c, 5, ratePromptPanel);
        addRow(form, c, 6, "Category", categoryComboBox);
        addRow(form, c, 7, "Description", descriptionField);
        addRow(form, c, 8, "Paid by", payerComboBox);
        addRow(form, c, 9, "Scope", scopeComboBox);
        addFullRow(form, c, 10, sharedPanel);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(createButton);

        background.add(form, BorderLayout.CENTER);
        background.add(buttonRow, BorderLayout.SOUTH);
        setContentPane(background);

        cancelButton.addActionListener(e -> dispose());
        typeComboBox.addItemListener(e -> refreshCategoryChoices());
        scopeComboBox.addItemListener(e -> {
            sharedPanel.setVisible("Shared".equals(scopeComboBox.getSelectedItem()));
            pack();
        });
        splitModeComboBox.addItemListener(e -> {
            boolean percentageMode = "By percentage".equals(splitModeComboBox.getSelectedItem());
            for (MemberSplitRow row : memberSplitRows) {
                row.getPercentageSpinner().setVisible(percentageMode);
            }
            pack();
        });

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildSharedPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(new TitledBorder(new LineBorder(new Color(0x9CCBEA), 1, true), "Shared expense split"));
        memberRowsPanel.setOpaque(false);
        panel.add(splitModeComboBox, BorderLayout.NORTH);
        panel.add(memberRowsPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRatePromptPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(new TitledBorder(new LineBorder(new Color(0x9CCBEA), 1, true), "New exchange rate needed"));
        ratePromptLabel.setFont(new Font(UiTheme.FONT, Font.PLAIN, 12));
        ratePromptLabel.setForeground(UiTheme.INK);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(rateField);
        row.add(saveRateButton);
        panel.add(ratePromptLabel, BorderLayout.NORTH);
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    /** Populates the payer combo and the shared-split member rows. Call before showing the dialog. */
    public void setMembers(List<Member> members) {
        payerComboBox.removeAllItems();
        memberRowsPanel.removeAll();
        memberSplitRows.clear();

        boolean percentageMode = "By percentage".equals(splitModeComboBox.getSelectedItem());
        for (Member member : members) {
            payerComboBox.addItem(member);

            JCheckBoxWithMember includeCheckBox = new JCheckBoxWithMember(member.getUser().getUsername());
            includeCheckBox.setOpaque(false);
            includeCheckBox.setSelected(true);
            JSpinner percentageSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            percentageSpinner.setVisible(percentageMode);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.setOpaque(false);
            row.add(includeCheckBox);
            row.add(percentageSpinner);
            memberRowsPanel.add(row);

            memberSplitRows.add(new MemberSplitRow(member, includeCheckBox, percentageSpinner));
        }
        memberRowsPanel.revalidate();
        memberRowsPanel.repaint();
        pack();
    }

    /** Gives the dialog the household's categories; the combo shows only those matching the chosen type. */
    public void setCategories(List<Category> categories) {
        this.allCategories = categories;
        refreshCategoryChoices();
    }

    /** Preselects a category by id (edit mode). Set the type first, since the combo is filtered by it. */
    public void selectCategoryById(Long categoryId) {
        for (int i = 0; i < categoryComboBox.getItemCount(); i++) {
            if (categoryComboBox.getItemAt(i).getId().equals(categoryId)) {
                categoryComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Repopulates the category combo with the categories whose type matches the type combo. */
    private void refreshCategoryChoices() {
        TransactionType type = "Income".equals(typeComboBox.getSelectedItem())
                ? TransactionType.INCOME : TransactionType.EXPENSE;
        categoryComboBox.removeAllItems();
        for (Category category : allCategories) {
            if (category.getType() == type) {
                categoryComboBox.addItem(category);
            }
        }
    }

    /** Preselects the payer combo by user id \u2014 a plain selection of an already-provided item, not business logic. */
    public void selectPayerById(Long userId) {
        for (int i = 0; i < payerComboBox.getItemCount(); i++) {
            Member member = payerComboBox.getItemAt(i);
            if (member.getUser().getId().equals(userId)) {
                payerComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    public void setConversionPreviewText(String text) {
        conversionPreviewLabel.setText(text);
    }

    /** Shows the inline "define a rate for X" prompt (use case V.5, alt path 4.1). */
    public void showRatePrompt(String currencyCode) {
        promptedCurrencyCode = currencyCode;
        ratePromptLabel.setText("No static rate for " + currencyCode + " yet \u2014 enter one (1 primary unit = ? " + currencyCode + "):");
        rateField.setText("");
        ratePromptPanel.setVisible(true);
        pack();
    }

    public void hideRatePrompt() {
        ratePromptPanel.setVisible(false);
        pack();
    }

    public String getPromptedCurrencyCode() {
        return promptedCurrencyCode;
    }

    // ----- Getters the Controller reads values / wires listeners through -----

    public JComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    public JTextField getDateField() {
        return dateField;
    }

    public JTextField getAmountField() {
        return amountField;
    }

    public JComboBox<String> getCurrencyComboBox() {
        return currencyComboBox;
    }

    public JComboBox<Category> getCategoryComboBox() {
        return categoryComboBox;
    }

    public JTextField getDescriptionField() {
        return descriptionField;
    }

    public JComboBox<Member> getPayerComboBox() {
        return payerComboBox;
    }

    public JComboBox<String> getScopeComboBox() {
        return scopeComboBox;
    }

    public JComboBox<String> getSplitModeComboBox() {
        return splitModeComboBox;
    }

    public List<MemberSplitRow> getMemberSplitRows() {
        return memberSplitRows;
    }

    public JTextField getRateField() {
        return rateField;
    }

    public AeroButton getSaveRateButton() {
        return saveRateButton;
    }

    public AeroButton getCreateButton() {
        return createButton;
    }

    public AeroButton getCancelButton() {
        return cancelButton;
    }

    /** One member's row in the shared-split section: whether they're included, and their percentage. */
    public static final class MemberSplitRow {
        private final Member member;
        private final JCheckBoxWithMember includeCheckBox;
        private final JSpinner percentageSpinner;

        private MemberSplitRow(Member member, JCheckBoxWithMember includeCheckBox, JSpinner percentageSpinner) {
            this.member = member;
            this.includeCheckBox = includeCheckBox;
            this.percentageSpinner = percentageSpinner;
        }

        public Member getMember() {
            return member;
        }

        public javax.swing.JCheckBox getIncludeCheckBox() {
            return includeCheckBox;
        }

        public JSpinner getPercentageSpinner() {
            return percentageSpinner;
        }
    }

    /** A plain JCheckBox; named subclass only so its purpose is clear at the call site above. */
    private static final class JCheckBoxWithMember extends javax.swing.JCheckBox {
        JCheckBoxWithMember(String text) {
            super(text);
        }
    }

    private static void addRow(AeroPanel form, GridBagConstraints c, int row, String labelText, JComponent field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        form.add(label(labelText), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
    }

    private static void addFullRow(AeroPanel form, GridBagConstraints c, int row, JComponent field) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        form.add(field, c);
        c.gridwidth = 1;
    }

    private static JTextField textField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
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
