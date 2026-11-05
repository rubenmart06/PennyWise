package cnu.ssd.pennywise.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cnu.ssd.pennywise.exception.HouseholdException;
import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.HouseholdFormDialog;
import cnu.ssd.pennywise.gui.HouseholdHubPanel;
import cnu.ssd.pennywise.model.Household;
import cnu.ssd.pennywise.model.HouseholdKind;
import cnu.ssd.pennywise.model.Session;
import cnu.ssd.pennywise.service.HouseholdService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Drives {@link HouseholdHubPanel} and {@link HouseholdFormDialog}: loads the
 * current user's households, handles household creation, and routes between
 * the hub and (via {@link AuthController}) the login screen on logout, or
 * (via {@link MainDashboardController}) a selected household's dashboard. Every
 * call into {@link HouseholdService} runs on a {@link SwingWorker} so a slow
 * database round trip never freezes the Event Dispatch Thread.
 */
@Component
public class HouseholdController {

    private final HouseholdService householdService;
    private final Session session;
    private final AppFrame appFrame;
    private final AuthController authController;
    private final MainDashboardController mainDashboardController;
    private final HouseholdHubPanel hubPanel = new HouseholdHubPanel();


    /**
     * {@code @Lazy} on {@link MainDashboardController} breaks the otherwise
     * circular dependency, the same way it broke the Auth↔Household one:
     * Household hands off to the dashboard when a household is opened, the
     * dashboard hands back to Household on "switch household" and "log out".
     */
    public HouseholdController(HouseholdService householdService, Session session, AppFrame appFrame,
                               AuthController authController, @Lazy MainDashboardController mainDashboardController) {
        this.householdService = householdService;
        this.session = session;
        this.appFrame = appFrame;
        this.authController = authController;
        this.mainDashboardController = mainDashboardController;
        wireListeners();
    }

    /** Shows the hub for the currently logged-in user and loads their households into it. */
    public void openHouseholdHub() {
        hubPanel.setWelcomeMessage(session.getCurrentUser().getUsername());
        appFrame.show(hubPanel);
        loadHouseholds();
    }

    private void wireListeners() {
        hubPanel.setOnCreateHousehold(this::openCreateHouseholdDialog);
        hubPanel.setOnHouseholdSelected(this::handleHouseholdSelected);
        hubPanel.getLogoutButton().addActionListener(e -> logout());
    }

    private void loadHouseholds() {
        Long userId = session.getCurrentUser().getId();
        new SwingWorker<List<Household>, Void>() {
            @Override
            protected List<Household> doInBackground() {
                return householdService.getHouseholdsForUser(userId);
            }

            @Override
            protected void done() {
                try {
                    hubPanel.setHouseholds(get());
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(appFrame.getFrame(), "Could not load your households. Please try again.", "Load failed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void openCreateHouseholdDialog() {
        HouseholdFormDialog dialog = new HouseholdFormDialog(appFrame.getFrame());
        dialog.getCreateButton().addActionListener(e -> handleCreateHousehold(dialog));
        dialog.setVisible(true);
    }

    private void handleCreateHousehold(HouseholdFormDialog dialog) {
        Long creatorUserId = session.getCurrentUser().getId();
        String name = dialog.getNameField().getText();
        String currency = (String) dialog.getCurrencyComboBox().getSelectedItem();
        HouseholdKind kind = "Shared".equals(dialog.getKindComboBox().getSelectedItem())
                ? HouseholdKind.SHARED : HouseholdKind.INDIVIDUAL;

        dialog.getCreateButton().setEnabled(false);
        new SwingWorker<Household, Void>() {
            @Override
            protected Household doInBackground() {
                return householdService.createHousehold(creatorUserId, name, kind, currency);
            }

            @Override
            protected void done() {
                dialog.getCreateButton().setEnabled(true);
                try {
                    get();
                    dialog.dispose();
                    loadHouseholds();
                } catch (ExecutionException ex) {
                    String message;
                    if (ex.getCause() instanceof HouseholdException) {
                        message = ex.getCause().getMessage();
                    } else {
                        ex.getCause().printStackTrace();
                        message = "Something went wrong. Please try again.";
                    }
                    showError(dialog, message, "Invalid data");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleHouseholdSelected(Household household) {
        mainDashboardController.open(household);
    }

    /** Called by {@link MainDashboardController} when the user logs out from the dashboard. */
    public void logout() {
        session.logout();
        authController.showLoginScreen();
    }

    private void showError(java.awt.Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
