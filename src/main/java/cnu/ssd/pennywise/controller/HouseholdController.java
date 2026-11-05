package cnu.ssd.pennywise.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

import cnu.ssd.pennywise.exception.HouseholdException;
import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.HouseholdFormDialog;
import cnu.ssd.pennywise.gui.HouseholdHubPanel;
import cnu.ssd.pennywise.gui.HouseholdPanel;
import cnu.ssd.pennywise.model.*;
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
    private final HouseholdContext householdContext;
    private final Session session;
    private final AppFrame appFrame;
    private final AuthController authController;
    private final MainDashboardController mainDashboardController;
    private final HouseholdHubPanel hubPanel = new HouseholdHubPanel();
    private final HouseholdPanel householdPanel = new HouseholdPanel();

    /**
     * {@code @Lazy} on {@link MainDashboardController} breaks the otherwise
     * circular dependency, the same way it broke the Auth↔Household one:
     * Household hands off to the dashboard when a household is opened, the
     * dashboard hands back to Household on "switch household" and "log out".
     */
    public HouseholdController(HouseholdService householdService, HouseholdContext householdContext, Session session,
                               AppFrame appFrame, AuthController authController,
                               @Lazy MainDashboardController mainDashboardController) {
        this.householdService = householdService;
        this.householdContext = householdContext;
        this.session = session;
        this.appFrame = appFrame;
        this.authController = authController;
        this.mainDashboardController = mainDashboardController;
        wireListeners();
        wireHouseholdPanelListeners();
    }

    /** The in-household members/invitations view driven by this controller (requirement 4\u20135). */
    public JComponent getHouseholdPanel() {
        return householdPanel;
    }

    /**
     * Reloads the open household (with its current members) from the database, refreshes the
     * context so other screens (e.g. the transaction form's payer list) see the change, and
     * redraws the member table.
     */
    public void refreshHouseholdPanel() {
        Long householdId = householdContext.getCurrentHouseholdId();
        new SwingWorker<Household, Void>() {
            @Override
            protected Household doInBackground() {
                return householdService.getHousehold(householdId);
            }

            @Override
            protected void done() {
                try {
                    Household fresh = get();
                    householdContext.setCurrentHousehold(fresh);
                    householdPanel.setMembers(fresh.getMembers());
                } catch (ExecutionException ex) {
                    ex.getCause().printStackTrace();
                    showError(householdPanel, "Could not load the household members. Please try again.", "Load failed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void wireHouseholdPanelListeners() {
        householdPanel.getRemoveMemberButton().addActionListener(e -> handleRemoveMember());
        householdPanel.getAddMemberButton().addActionListener(e -> handleAddMember());
        householdPanel.getUsernameField().addActionListener(e -> handleAddMember());
    }

    private void handleAddMember() {
        Long householdId = householdContext.getCurrentHouseholdId();
        Long requesterUserId = session.getCurrentUser().getId();
        String username = householdPanel.getUsernameField().getText();

        householdPanel.getAddMemberButton().setEnabled(false);
        new SwingWorker<Member, Void>() {
            @Override
            protected Member doInBackground() {
                return householdService.addMember(householdId, requesterUserId, username);
            }

            @Override
            protected void done() {
                householdPanel.getAddMemberButton().setEnabled(true);
                try {
                    Member added = get();
                    householdPanel.getUsernameField().setText("");
                    refreshHouseholdPanel();
                    JOptionPane.showMessageDialog(householdPanel,
                            added.getUser().getUsername() + " was added to this household.",
                            "Member added", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof HouseholdException) {
                        showError(householdPanel, ex.getCause().getMessage(), "Could not add member");
                    } else {
                        ex.getCause().printStackTrace();
                        showError(householdPanel, "Something went wrong. Please try again.", "Error");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleRemoveMember() {
        Member member = householdPanel.getSelectedMember();
        if (member == null) {
            showError(householdPanel, "Select a member to remove first.", "No member selected");
            return;
        }
        Long householdId = householdContext.getCurrentHouseholdId();
        Long requesterUserId = session.getCurrentUser().getId();
        Long userId = member.getUser().getId();

        int confirm = JOptionPane.showConfirmDialog(householdPanel,
                "Remove " + member.getUser().getUsername() + " from this household?", "Confirm removal",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        householdPanel.getRemoveMemberButton().setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                householdService.removeMember(householdId, requesterUserId, userId);
                return null;
            }

            @Override
            protected void done() {
                householdPanel.getRemoveMemberButton().setEnabled(true);
                try {
                    get();
                    refreshHouseholdPanel();
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof HouseholdException) {
                        showError(householdPanel, ex.getCause().getMessage(), "Could not remove member");
                    } else {
                        ex.getCause().printStackTrace();
                        showError(householdPanel, "Something went wrong. Please try again.", "Error");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
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
