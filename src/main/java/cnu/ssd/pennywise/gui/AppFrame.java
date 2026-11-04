package cnu.ssd.pennywise.gui;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.springframework.stereotype.Component;

/**
 * Wraps the single {@link JFrame} the whole application runs in. A Spring
 * singleton so every controller (Auth, Household, …) can be injected with it
 * and swap the visible screen by calling {@link #show(JComponent)}, instead of
 * each controller creating and owning its own window.
 */
@Component
public class AppFrame {

    private final JFrame frame = new JFrame("PennyWise — Household Account Book");

    public AppFrame() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 780);
        frame.setMinimumSize(new Dimension(1050, 700));
        frame.setLocationRelativeTo(null);
    }

    public JFrame getFrame() {
        return frame;
    }

    /** Swaps the frame's content pane for {@code content} and makes the frame visible. */
    public void show(JComponent content) {
        frame.setContentPane(content);
        frame.revalidate();
        frame.repaint();
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
    }
}
