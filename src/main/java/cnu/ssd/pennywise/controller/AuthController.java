package cnu.ssd.pennywise.controller;

import java.awt.Dimension;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import cnu.ssd.pennywise.exception.AuthException;
import cnu.ssd.pennywise.gui.AuthPanel;
import cnu.ssd.pennywise.model.Session;
import cnu.ssd.pennywise.model.User;
import cnu.ssd.pennywise.service.AuthService;
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
    private final AuthPanel authPanel = new AuthPanel();

    private JFrame frame;

    public AuthController(AuthService authService, Session session) {
        this.authService = authService;
        this.session = session;
        wireListeners();
    }

    /** Builds the main {@link JFrame}, sets {@link AuthPanel} as its content, and shows it. */
    public void startApplication() {
        frame = new JFrame("PennyWise — Household Account Book");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(authPanel);
        frame.setSize(1200, 780);
        frame.setMinimumSize(new Dimension(1050, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
                    JOptionPane.showMessageDialog(frame, "Welcome back, " + user.getUsername() + "!",
                            "Login successful", JOptionPane.INFORMATION_MESSAGE);
                    // Phase 2 replaces this line with opening the Household Hub screen.
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
                    JOptionPane.showMessageDialog(frame, "Account created for " + user.getUsername()
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
        return ex.getCause() instanceof AuthException
                ? ex.getCause().getMessage()
                : "Something went wrong. Please try again.";
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
