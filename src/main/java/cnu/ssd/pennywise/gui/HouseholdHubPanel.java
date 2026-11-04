package cnu.ssd.pennywise.gui;

import cnu.ssd.pennywise.controller.HouseholdController;
import cnu.ssd.pennywise.gui.components.*;
import cnu.ssd.pennywise.model.Household;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The household hub screen (View / Boundary): shows every household the current
 * user belongs to as a grid of tiles, plus a "+ New household" tile.
 * <p>
 * Contains no business logic: it never talks to a Service or a Repository. It
 * only renders whatever {@link #setHouseholds(List)} is given, and forwards
 * clicks to whichever listeners {@link HouseholdController}
 * has registered.
 */
public class HouseholdHubPanel extends Backdrop {

    private final JLabel welcomeLabel = label("Welcome back. Choose a household to open its dashboard.", 14, false, UiTheme.MUTED);
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    private final AeroButton logoutButton = new AeroButton("Log out", UiTheme.DEEP);

    private Consumer<Household> onHouseholdSelected = household -> {
    };
    private Runnable onCreateHousehold = () -> {
    };

    public HouseholdHubPanel() {
        super(new GridBagLayout(), true);

        AeroPanel box = new AeroPanel(new BorderLayout(0, 18), 28);
        box.setPreferredSize(new Dimension(920, 600));

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 2));
        head.setOpaque(false);
        head.add(label("Your households", 30, true, UiTheme.DEEP));
        head.add(welcomeLabel);

        grid.setOpaque(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(grid, BorderLayout.NORTH);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.add(logoutButton);

        box.add(head, BorderLayout.NORTH);
        box.add(wrap, BorderLayout.CENTER);
        box.add(south, BorderLayout.SOUTH);
        add(box);
    }

    /** Rebuilds the tile grid from the given households, plus a trailing "+ New household" tile. */
    public void setHouseholds(List<Household> households) {
        grid.removeAll();
        for (Household household : households) {
            HouseholdTile tile = new HouseholdTile(household);
            tile.addActionListener(e -> onHouseholdSelected.accept(household));
            grid.add(tile);
        }
        HouseholdTile addTile = new HouseholdTile();
        addTile.addActionListener(e -> onCreateHousehold.run());
        grid.add(addTile);
        grid.revalidate();
        grid.repaint();
    }

    public void setWelcomeMessage(String username) {
        welcomeLabel.setText("Welcome back, " + username + ". Choose a household to open its dashboard.");
    }

    public void setOnHouseholdSelected(Consumer<Household> listener) {
        this.onHouseholdSelected = listener;
    }

    public void setOnCreateHousehold(Runnable listener) {
        this.onCreateHousehold = listener;
    }

    public AeroButton getLogoutButton() {
        return logoutButton;
    }

    private static JLabel label(String text, int size, boolean bold, java.awt.Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UiTheme.FONT, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }
}
