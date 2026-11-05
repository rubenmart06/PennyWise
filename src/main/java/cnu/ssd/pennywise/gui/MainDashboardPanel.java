package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.MainDashboardController;
import cnu.ssd.pennywise.gui.components.*;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The application shell shown once a household is open (View / Boundary):
 * glossy header bar, glass sidebar navigation and a {@link CardLayout} content
 * area.
 * <p>
 * Contains no business logic: {@link MainDashboardController}
 * builds the cards ({@link #addCard}), switches between them ({@link #showCard}),
 * and wires every button returned by this class's getters.
 */
public class MainDashboardPanel extends Backdrop {

    /** Sidebar section labels, in display order. */
    public static final List<String> SECTIONS = List.of(
            "Dashboard", "Transactions", "Household", "Debts", "Budgets", "Reports", "Settings");

    private final JLabel householdLabel = new JLabel();
    private final AeroButton switchHouseholdButton = new AeroButton("Switch household", UiTheme.GREEN);
    private final AeroButton logoutButton = new AeroButton("Log out", UiTheme.DEEP);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    public MainDashboardPanel() {
        super(new BorderLayout(14, 14), false);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        cardsPanel.setOpaque(false);
        add(cardsPanel, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new HeaderBar();
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(10, 22, 10, 16));

        JLabel logo = new JLabel("PennyWise");
        logo.setFont(new Font(UiTheme.FONT, Font.BOLD, 26));
        logo.setForeground(Color.WHITE);
        header.add(logo, BorderLayout.WEST);

        householdLabel.setFont(new Font(UiTheme.FONT, Font.BOLD, 13));
        householdLabel.setForeground(Color.WHITE);

        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(householdLabel);
        right.add(switchHouseholdButton);
        right.add(logoutButton);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private AeroPanel buildSidebar() {
        AeroPanel sidebar = new AeroPanel(new BorderLayout(), 12);
        JPanel list = new JPanel(new java.awt.GridLayout(0, 1, 0, 6));
        list.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        for (String section : SECTIONS) {
            NavButton button = new NavButton(section);
            group.add(button);
            navButtons.put(section, button);
            list.add(button);
        }
        sidebar.add(list, BorderLayout.NORTH);
        return sidebar;
    }

    /** Adds a module's panel to the content area under {@code name} (one of {@link #SECTIONS}). */
    public void addCard(String name, JComponent panel) {
        cardsPanel.add(panel, name);
    }

    /** Switches the visible card and highlights the matching nav button (pure navigation, not business logic). */
    public void showCard(String name) {
        cardLayout.show(cardsPanel, name);
        NavButton button = navButtons.get(name);
        if (button != null) {
            button.setSelected(true);
        }
    }

    public void setHouseholdName(String text) {
        householdLabel.setText(text);
    }

    public Map<String, NavButton> getNavButtons() {
        return navButtons;
    }

    public AeroButton getSwitchHouseholdButton() {
        return switchHouseholdButton;
    }

    public AeroButton getLogoutButton() {
        return logoutButton;
    }

    /** The glossy gradient header bar.*/
    private static final class HeaderBar extends JPanel {
        HeaderBar() {
            super(new BorderLayout(12, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = UiTheme.g2(g);
            int w = getWidth(), h = getHeight();
            java.awt.geom.RoundRectangle2D r = new java.awt.geom.RoundRectangle2D.Float(1, 1, w - 3, h - 3, 26, 26);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x5CC4FF), 0, h, new Color(0x1B78C4)));
            g2.fill(r);
            g2.setClip(r);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 170), 0, h / 2f, new Color(255, 255, 255, 20)));
            g2.fillRect(0, 0, w, h / 2);
            g2.setClip(null);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.draw(r);
            g2.dispose();
        }
    }
}
