package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.ExportDialog;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.model.TransactionFilter;
import cnu.ssd.pennywise.service.ExportService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.springframework.stereotype.Component;

/**
 * Drives {@link ExportDialog}: lets the user pick .xlsx or .csv, builds the
 * file's bytes via {@link ExportService} (for whatever {@link TransactionFilter}
 * the Transactions screen currently has applied), and saves them to a file the
 * user picks via {@link JFileChooser}. The Service call runs on a
 * {@link SwingWorker} so a slow export never freezes the EDT.
 */
@Component
public class ExportController {

    private final ExportService exportService;
    private final HouseholdContext householdContext;
    private final AppFrame appFrame;

    public ExportController(ExportService exportService, HouseholdContext householdContext, AppFrame appFrame) {
        this.exportService = exportService;
        this.householdContext = householdContext;
        this.appFrame = appFrame;
    }

    /** Opens the format-choice dialog, then exports the household's transactions matching {@code filter}. */
    public void openExportDialog(TransactionFilter filter) {
        ExportDialog dialog = new ExportDialog(appFrame.getFrame());
        dialog.getExportButton().addActionListener(e -> handleExport(dialog, filter));
        dialog.setVisible(true);
    }

    private void handleExport(ExportDialog dialog, TransactionFilter filter) {
        boolean excel = dialog.isExcelSelected();
        Long householdId = householdContext.getCurrentHouseholdId();

        dialog.getExportButton().setEnabled(false);
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                return excel ? exportService.exportToExcel(householdId, filter) : exportService.exportToCsv(householdId, filter);
            }

            @Override
            protected void done() {
                dialog.getExportButton().setEnabled(true);
                byte[] bytes;
                try {
                    bytes = get();
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(dialog, "Could not generate the export file.");
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                dialog.dispose();
                saveToFile(bytes, excel);
            }
        }.execute();
    }

    private void saveToFile(byte[] bytes, boolean excel) {
        JFileChooser chooser = new JFileChooser();
        String extension = excel ? "xlsx" : "csv";
        chooser.setFileFilter(new FileNameExtensionFilter(
                excel ? "Excel workbook (*.xlsx)" : "CSV file (*.csv)", extension));
        chooser.setSelectedFile(new File("pennywise-transactions." + extension));

        int choice = chooser.showSaveDialog(appFrame.getFrame());
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
            JOptionPane.showMessageDialog(appFrame.getFrame(), "Exported to " + file.getAbsolutePath(),
                    "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(appFrame.getFrame(), "Could not write the file. Please try again.");
        }
    }

    private void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
