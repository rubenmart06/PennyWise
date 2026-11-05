package cnu.ssd.pennywise.controller;

import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.ReportingPanel;
import cnu.ssd.pennywise.model.FinancialSummary;
import cnu.ssd.pennywise.model.HouseholdContext;
import cnu.ssd.pennywise.service.ReportService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutionException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.springframework.stereotype.Component;

/**
 * Drives {@link ReportingPanel}: loads the current household's financial
 * summary for the selected period/member and renders
 * it. Every call into {@link ReportService} runs on a {@link SwingWorker} so
 * a slow database round trip never freezes the EDT.
 */
@Component
public class ReportingController {

    private final ReportService reportService;
    private final HouseholdContext householdContext;
    private final AppFrame appFrame;
    private final ReportingPanel panel = new ReportingPanel();

    public ReportingController(ReportService reportService, HouseholdContext householdContext, AppFrame appFrame) {
        this.reportService = reportService;
        this.householdContext = householdContext;
        this.appFrame = appFrame;
        panel.getApplyButton().addActionListener(e -> loadSummary());
    }

    public JComponent getPanel() {
        return panel;
    }

    /** Populates the member selector and loads the default (current-month, all-members) summary. */
    public void refresh() {
        panel.setMembers(householdContext.getCurrentHousehold().getMembers());
        loadSummary();
    }

    private void loadSummary() {
        Long householdId = householdContext.getCurrentHouseholdId();
        String primaryCurrency = householdContext.getCurrentHousehold().getPrimaryCurrency();

        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = parseOrNull(panel.getFromDateText());
            toDate = parseOrNull(panel.getToDateText());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(panel, "Enter dates as yyyy-mm-dd.", "Invalid data", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Long memberUserId = panel.getSelectedMemberUserId();

        panel.getApplyButton().setEnabled(false);
        new SwingWorker<FinancialSummary, Void>() {
            @Override
            protected FinancialSummary doInBackground() {
                return reportService.getSummary(householdId, fromDate, toDate, memberUserId);
            }

            @Override
            protected void done() {
                panel.getApplyButton().setEnabled(true);
                try {
                    panel.setSummary(get(), primaryCurrency);
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    JOptionPane.showMessageDialog(appFrame.getFrame(), "Could not load the report. Please try again.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private LocalDate parseOrNull(String text) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.isEmpty() ? null : LocalDate.parse(trimmed);
    }
}
