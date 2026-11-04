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
import org.springframework.stereotype.Component;

/**
 * Drives {@link HouseholdHubPanel} and {@link HouseholdFormDialog}: loads the
 * current user's households, handles household creation, and routes between
 * the hub and (via {@link AuthController}) the login screen on logout. Every
 * call into {@link HouseholdService} runs on a {@link SwingWorker} so a slow
 * database round trip never freezes the Event Dispatch Thread.
 */
@Component
public class HouseholdController {

    private final HouseholdService householdService;
    private final Session session;
    private final AppFrame appFrame;
    private final AuthController authController;
    private final HouseholdHubPanel hubPanel = new HouseholdHubPanel();

    public HouseholdController(HouseholdService householdService, Session session, AppFrame appFrame,
                               AuthController authController) {
        this.householdService = householdService;
        this.session = session;
        this.appFrame = appFrame;
        this.authController = authController;
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
        hubPanel.getLogoutButton().addActionListener(e -> handleLogout());
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
        // Phase 3 replaces this with opening the household's own dashboard (MainDashboardFrame).
        JOptionPane.showMessageDialog(appFrame.getFrame(),
                "Opened \"" + household.getName() + "\". Its dashboard will be built in a later phase.",
                "Household opened", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleLogout() {
        session.logout();
        authController.showLoginScreen();
    }

    private void showError(java.awt.Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
