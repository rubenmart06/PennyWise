package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.CurrencyController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.AeroTableStyler;
import cnu.ssd.pennywise.gui.components.UiTheme;
import cnu.ssd.pennywise.model.ExchangeRate;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * The exchange-rate settings view (View / Boundary): a table of a household's
 * static rates plus Add/Modify buttons.
 * <p>
 * Contains no business logic: it only renders whatever {@link #setRates} is
 * given. {@link CurrencyController} wires the two
 * buttons and uses {@link javax.swing.JOptionPane} input dialogs to gather the
 * currency code/rate before calling the Service, kept out of this class
 * since it's an event-handling concern, not the View's own rendering.
 */
public class SettingsPanel extends JPanel {

    private static final String[] COLUMNS = {"Currency", "Static rate"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel subtitleLabel = new JLabel();
    private final AeroButton addRateButton = new AeroButton("+ Add rate", UiTheme.GREEN);
    private final AeroButton modifyRateButton = new AeroButton("Modify rate", UiTheme.SKY);

    public SettingsPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);

        JLabel title = new JLabel("Settings");
        title.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        title.setForeground(UiTheme.DEEP);
        add(title, BorderLayout.NORTH);

        AeroPanel panel = new AeroPanel(new BorderLayout(0, 10), 16);
        subtitleLabel.setFont(new Font(UiTheme.FONT, Font.BOLD, 16));
        subtitleLabel.setForeground(UiTheme.DEEP);
        panel.add(subtitleLabel, BorderLayout.NORTH);
        panel.add(AeroTableStyler.style(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addRateButton);
        buttons.add(modifyRateButton);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
    }

    public void setPrimaryCurrencyCode(String code) {
        subtitleLabel.setText("Exchange rates — primary currency: " + code);
    }

    public void setRates(List<ExchangeRate> rates) {
        tableModel.setRowCount(0);
        for (ExchangeRate rate : rates) {
            tableModel.addRow(new Object[]{rate.getCurrencyCode(), rate.getRate().toPlainString()});
        }
    }

    /** The currency code of the selected row, or {@code null} if nothing is selected. */
    public String getSelectedCurrencyCode() {
        int row = table.getSelectedRow();
        return row < 0 ? null : (String) tableModel.getValueAt(row, 0);
    }

    public AeroButton getAddRateButton() {
        return addRateButton;
    }

    public AeroButton getModifyRateButton() {
        return modifyRateButton;
    }
}
