package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import java.util.Random;
import javax.swing.JPanel;

/**
 * Sky gradient + sun glow + bokeh bubbles, with optional green hills along the
 * bottom.
 */
public class Backdrop extends JPanel {

    private final boolean hills;

    public Backdrop(LayoutManager lm, boolean hills) {
        super(lm);
        this.hills = hills;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        g2.setPaint(new GradientPaint(0, 0, new Color(0x3FB1F2), 0, h, new Color(0xE3F6FF)));
        g2.fillRect(0, 0, w, h);
        g2.setPaint(new RadialGradientPaint(w * .85f, h * .05f, Math.max(w, h) * .55f, new float[]{0f, 1f},
                new Color[]{new Color(255, 255, 255, 210), new Color(255, 255, 255, 0)}));
        g2.fillRect(0, 0, w, h);
        Random r = new Random(7);
        for (int i = 0; i < 16; i++) {
            int d = 40 + r.nextInt(130), x = (int) (r.nextFloat() * w), y = (int) (r.nextFloat() * h * .8f);
            g2.setColor(new Color(255, 255, 255, 16 + r.nextInt(28)));
            g2.fillOval(x, y, d, d);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawOval(x, y, d, d);
        }
        if (hills) {
            Path2D p = new Path2D.Float();
            p.moveTo(0, h * .88f);
            p.curveTo(w * .25f, h * .74f, w * .5f, h * 1.02f, w * .75f, h * .84f);
            p.curveTo(w * .88f, h * .77f, w * .95f, h * .8f, w, h * .78f);
            p.lineTo(w, h);
            p.lineTo(0, h);
            p.closePath();
            g2.setPaint(new GradientPaint(0, h * .75f, new Color(0x9BE35A), 0, h, new Color(0x3C9A28)));
            g2.fill(p);
        }
        g2.dispose();
    }
}
