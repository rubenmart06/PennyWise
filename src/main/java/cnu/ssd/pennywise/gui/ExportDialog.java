package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.ExportController;
import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.Backdrop;
import cnu.ssd.pennywise.gui.components.UiTheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

/**
 * The "Export transactions" popup (View / Boundary): choose .xlsx or .csv.
 * Contains no business logic: {@link ExportController}
 * reads {@link #isExcelSelected()}, calls {@code ExportService}, and shows the
 * save-file chooser itself.
 */
public class ExportDialog extends JDialog {

    private final JRadioButton excelOption = new JRadioButton("Excel workbook (.xlsx)", true);
    private final JRadioButton csvOption = new JRadioButton("CSV file (.csv)");
    private final AeroButton exportButton = new AeroButton("Export", UiTheme.GREEN);
    private final AeroButton cancelButton = new AeroButton("Cancel", UiTheme.SKY);

    public ExportDialog(Frame owner) {
        super(owner, "Export transactions", true);

        Backdrop background = new Backdrop(new BorderLayout(0, 12), false);
        background.setBorder(new EmptyBorder(16, 16, 16, 16));

        AeroPanel form = new AeroPanel(new GridLayout(0, 1, 0, 10), 20);
        JLabel label = new JLabel("Choose a file format:");
        label.setFont(new Font(UiTheme.FONT, Font.BOLD, 14));
        label.setForeground(UiTheme.INK);
        form.add(label);

        ButtonGroup group = new ButtonGroup();
        group.add(excelOption);
        group.add(csvOption);
        for (JRadioButton option : new JRadioButton[]{excelOption, csvOption}) {
            option.setOpaque(false);
            option.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        }
        form.add(excelOption);
        form.add(csvOption);

        javax.swing.JPanel buttonRow = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(exportButton);

        background.add(form, BorderLayout.CENTER);
        background.add(buttonRow, BorderLayout.SOUTH);
        setContentPane(background);

        cancelButton.addActionListener(e -> dispose());
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isExcelSelected() {
        return excelOption.isSelected();
    }

    public AeroButton getExportButton() {
        return exportButton;
    }

    public AeroButton getCancelButton() {
        return cancelButton;
    }
}
