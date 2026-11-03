package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Glossy "glass" pill button.
 */
public class AeroButton extends JButton {

    private final Color base;

    public AeroButton(String text, Color color) {
        super(text);
        this.base = color;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font(UiTheme.FONT, Font.BOLD, 13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 18, 8, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = getWidth(), h = getHeight();
        boolean down = getModel().isArmed(), over = getModel().isRollover();
        RoundRectangle2D s = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, h, h);
        Color c = isEnabled() ? base : new Color(0xA9B8C4);
        g2.setPaint(new java.awt.GradientPaint(0, 0, UiTheme.mix(c, over ? .4f : .25f), 0, h,
                down ? UiTheme.dark(c, .3f) : UiTheme.dark(c, .12f)));
        g2.fill(s);
        g2.setClip(s);
        g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(255, 255, 255, 190), 0, h / 2f,
                new Color(255, 255, 255, 25)));
        g2.fillRect(0, 0, w, h / 2);
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 170));
        g2.draw(new RoundRectangle2D.Float(2, 2, w - 5, h - 5, h - 2, h - 2));
        g2.setColor(UiTheme.dark(c, .45f));
        g2.draw(s);
        g2.dispose();
        super.paintComponent(g);
    }
}
