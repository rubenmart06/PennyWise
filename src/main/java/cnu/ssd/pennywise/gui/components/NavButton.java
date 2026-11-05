package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

/**
 * Sidebar navigation toggle button.
 */
public class NavButton extends JToggleButton {

    public NavButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(new Font(UiTheme.FONT, Font.BOLD, 14));
        setHorizontalAlignment(LEFT);
        setBorder(new EmptyBorder(11, 20, 11, 30));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = getWidth(), h = getHeight();
        RoundRectangle2D s = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, h, h);
        if (isSelected()) {
            g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x6FCBFA), 0, h, new Color(0x1E83D2)));
            g2.fill(s);
            g2.setClip(s);
            g2.setPaint(new Color(255, 255, 255, 100));
            g2.fillRect(0, 0, w, h / 2);
            g2.setClip(null);
            g2.setColor(new Color(0x0B5E9E));
            g2.draw(s);
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(255, 255, 255, 140));
            g2.fill(s);
        }
        g2.dispose();
        setForeground(isSelected() ? Color.WHITE : UiTheme.INK);
        super.paintComponent(g);
    }
}