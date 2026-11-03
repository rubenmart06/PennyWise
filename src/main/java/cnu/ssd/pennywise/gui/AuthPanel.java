package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.gui.components.AeroButton;
import cnu.ssd.pennywise.gui.components.AeroPanel;
import cnu.ssd.pennywise.gui.components.Backdrop;
import cnu.ssd.pennywise.gui.components.UiTheme;
import lombok.Getter;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * The entry screen (View / Boundary): login and sign-up forms.
 * <p>
 * Contains no business logic: it never talks to a Service or a Repository.
 * {@link com.pennywise.controller.AuthController} reads the fields below via
 * their getters and attaches its own listeners to {@link #getLoginButton()} and
 * {@link #getRegisterButton()}.
 */
public class AuthPanel extends Backdrop {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    @Getter
    private final JTextField loginUsernameField = textField(16);
    private final JPasswordField loginPasswordField = passwordField(16);
    private final AeroButton loginButton = new AeroButton("Log in", UiTheme.GREEN);
    private final AeroButton toRegisterButton = new AeroButton("No account? Sign up", UiTheme.SKY);

    private final JTextField registerUsernameField = textField(16);
    private final JTextField registerEmailField = textField(16);
    private final JPasswordField registerPasswordField = passwordField(16);
    private final JPasswordField registerConfirmPasswordField = passwordField(16);
    private final AeroButton registerButton = new AeroButton("Create account", UiTheme.GREEN);
    private final AeroButton toLoginButton = new AeroButton("Have an account? Log in", UiTheme.SKY);

    public AuthPanel() {
        super(new GridBagLayout(), true);

        AeroPanel box = new AeroPanel(new java.awt.BorderLayout(0, 14), 28);
        box.setPreferredSize(new Dimension(400, 470));

        JPanel head = new JPanel(new GridLayout(0, 1));
        head.setOpaque(false);
        JLabel logo = label("PennyWise", 36, true, UiTheme.SKY);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = label("Household Account Book", 14, false, UiTheme.MUTED);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        head.add(logo);
        head.add(sub);
        box.add(head, java.awt.BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildLoginCard(), "login");
        cards.add(buildRegisterCard(), "register");
        box.add(cards, java.awt.BorderLayout.CENTER);

        add(box);
    }

    private JPanel buildLoginCard() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.setOpaque(false);
        p.add(label("Username", 13, true, UiTheme.INK));
        p.add(loginUsernameField);
        p.add(label("Password", 13, true, UiTheme.INK));
        p.add(loginPasswordField);
        p.add(loginButton);
        p.add(toRegisterButton);
        toRegisterButton.addActionListener(e -> showRegister());
        return p;
    }

    private JPanel buildRegisterCard() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 8));
        p.setOpaque(false);
        p.add(label("Username", 13, true, UiTheme.INK));
        p.add(registerUsernameField);
        p.add(label("Email", 13, true, UiTheme.INK));
        p.add(registerEmailField);
        p.add(label("Password", 13, true, UiTheme.INK));
        p.add(registerPasswordField);
        p.add(label("Confirm password", 13, true, UiTheme.INK));
        p.add(registerConfirmPasswordField);
        p.add(registerButton);
        p.add(toLoginButton);
        toLoginButton.addActionListener(e -> showLogin());
        return p;
    }

    public void showLogin() {
        cardLayout.show(cards, "login");
    }

    public void showRegister() {
        cardLayout.show(cards, "register");
    }

    public void clearLoginFields() {
        loginUsernameField.setText("");
        loginPasswordField.setText("");
    }

    public void clearRegisterFields() {
        registerUsernameField.setText("");
        registerEmailField.setText("");
        registerPasswordField.setText("");
        registerConfirmPasswordField.setText("");
    }

    // ----- Getters the Controller wires listeners / reads values through -----

    public JPasswordField getLoginPasswordField() {
        return loginPasswordField;
    }

    public AeroButton getLoginButton() {
        return loginButton;
    }

    public JTextField getRegisterUsernameField() {
        return registerUsernameField;
    }

    public JTextField getRegisterEmailField() {
        return registerEmailField;
    }

    public JPasswordField getRegisterPasswordField() {
        return registerPasswordField;
    }

    public JPasswordField getRegisterConfirmPasswordField() {
        return registerConfirmPasswordField;
    }

    public AeroButton getRegisterButton() {
        return registerButton;
    }

    // ----- Styling helpers (same look as the prototype's field()/lbl()) -----

    private static JTextField textField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(new LineBorder(new java.awt.Color(0x9CCBEA), 1, true), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(new LineBorder(new java.awt.Color(0x9CCBEA), 1, true), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    private static JLabel label(String text, int size, boolean bold, java.awt.Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }
}
