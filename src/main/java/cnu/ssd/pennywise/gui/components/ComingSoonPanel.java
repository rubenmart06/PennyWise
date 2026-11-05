package cnu.ssd.pennywise.gui.components;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A plain glass panel saying a section isn't built yet. Used for the sidebar
 * sections {@code MainDashboardFrame} has a nav button for but whose own
 * screen/controller hasn't been implemented yet (Dashboard,
 * Household, Debts, Budgets, Reports)
 */
public class ComingSoonPanel extends AeroPanel {

    public ComingSoonPanel(String sectionName) {
        super(new BorderLayout(), 24);
        JLabel label = new JLabel(sectionName + " is coming in a later phase.", SwingConstants.CENTER);
        label.setFont(new Font(UiTheme.FONT, Font.PLAIN, 15));
        label.setForeground(UiTheme.MUTED);
        add(label, BorderLayout.CENTER);
    }
}
