package cnu.ssd.pennywise.controller;

import java.awt.Dimension;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cnu.ssd.pennywise.exception.AuthException;
import cnu.ssd.pennywise.gui.AppFrame;
import cnu.ssd.pennywise.gui.AuthPanel;
import cnu.ssd.pennywise.model.Session;
import cnu.ssd.pennywise.model.User;
import cnu.ssd.pennywise.service.AuthService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Drives {@link AuthPanel}: reads its fields, validates them client-side, calls
 * {@link AuthService} directly, and shows the result via {@link JOptionPane}. Every call into the
 * Service runs on a {@link SwingWorker} so a slow database round trip never
 * freezes the Event Dispatch Thread.
 */
@Component
public class AuthController {

    private final AuthService authService;
    private final Session session;
    private final AppFrame appFrame;
    private final HouseholdController householdController;
    private final AuthPanel authPanel = new AuthPanel();

    /**
     * {@code @Lazy} on {@link HouseholdController} breaks the otherwise circular
     * dependency: Auth hands off to Household on login, Household hands back to
     * Auth on logout. Spring resolves this side through a lazy-init proxy instead
     * of eagerly constructing both beans at once.
     */
    public AuthController(AuthService authService, Session session, AppFrame appFrame,
                          @Lazy HouseholdController householdController) {
        this.authService = authService;
        this.session = session;
        this.appFrame = appFrame;
        this.householdController = householdController;
        wireListeners();
    }

    /** Shows {@link AuthPanel} in the shared {@link AppFrame}. */
    public void startApplication() {
        appFrame.show(authPanel);
    }

    /** Called by {@link HouseholdController} on logout, to return to the login screen. */
    public void showLoginScreen() {
        authPanel.showLogin();
        appFrame.show(authPanel);
    }

    private void wireListeners() {
        authPanel.getLoginButton().addActionListener(e -> handleLogin());
        authPanel.getRegisterButton().addActionListener(e -> handleRegister());
    }

    private void handleLogin() {
        String username = authPanel.getLoginUsernameField().getText();
        String password = new String(authPanel.getLoginPasswordField().getPassword());

        authPanel.getLoginButton().setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                authPanel.getLoginButton().setEnabled(true);
                try {
                    User user = get();
                    session.login(user);
                    authPanel.clearLoginFields();
                    householdController.openHouseholdHub();
                } catch (ExecutionException ex) {
                    showError(authErrorMessage(ex), "Login failed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void handleRegister() {
        String username = authPanel.getRegisterUsernameField().getText();
        String email = authPanel.getRegisterEmailField().getText();
        String password = new String(authPanel.getRegisterPasswordField().getPassword());
        String confirmPassword = new String(authPanel.getRegisterConfirmPasswordField().getPassword());

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.", "Invalid data");
            return;
        }

        authPanel.getRegisterButton().setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return authService.register(username, email, password);
            }

            @Override
            protected void done() {
                authPanel.getRegisterButton().setEnabled(true);
                try {
                    User user = get();
                    authPanel.clearRegisterFields();
                    JOptionPane.showMessageDialog(appFrame.getFrame(), "Account created for " + user.getUsername()
                                    + ". You can log in now.", "Registration successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    authPanel.showLogin();
                } catch (ExecutionException ex) {
                    showError(authErrorMessage(ex), "Registration failed");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private String authErrorMessage(ExecutionException ex) {
        if (ex.getCause() instanceof AuthException) {
            return ex.getCause().getMessage();
        }
        ex.getCause().printStackTrace();
        return "Something went wrong. Please try again.";
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(appFrame.getFrame(), message, title, JOptionPane.ERROR_MESSAGE);
    }
}
