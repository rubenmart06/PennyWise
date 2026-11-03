package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Translucent rounded "glass" container.
 */
public class AeroPanel extends JPanel {

    public AeroPanel(LayoutManager lm, int pad) {
        super(lm);
        setOpaque(false);
        setBorder(new EmptyBorder(pad, pad, pad, pad));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(0, 60, 110, 28));
        g2.fill(new RoundRectangle2D.Float(3, 5, w - 6, h - 6, 24, 24));
        RoundRectangle2D s = new RoundRectangle2D.Float(1, 1, w - 5, h - 6, 24, 24);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 235), 0, h, new Color(0xE4, 0xF4, 0xFF, 200)));
        g2.fill(s);
        g2.setClip(s);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 150), 0, h * .35f, new Color(255, 255, 255, 0)));
        g2.fillRect(0, 0, w, (int) (h * .35f));
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 240));
        g2.setStroke(new java.awt.BasicStroke(1.6f));
        g2.draw(s);
        g2.setColor(new Color(43, 143, 214, 90));
        g2.setStroke(new java.awt.BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0, 0, w - 3, h - 4, 25, 25));
        g2.dispose();
    }
}
