package cnu.ssd.pennywise.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/**
 * Glossy pie chart with a legend.
 */
public class PieChart extends JComponent {

    private static final Color[] COLORS = {
            new Color(0x2BA3E8), new Color(0x4DAF28), new Color(0xF2A81D),
            new Color(0xE0533D), new Color(0x8E6FD8), new Color(0x1FB8B0)
    };

    private double[] values = {1};
    private String[] names = {""};

    public void setData(String[] names, double[] values) {
        this.names = names;
        this.values = values;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int d = Math.min(getHeight() - 20, getWidth() / 2), x = 20, y = (getHeight() - d) / 2;
        double total = 0;
        for (double v : values) {
            total += v;
        }
        if (total <= 0) {
            g2.dispose();
            return;
        }
        double angle = 90;
        for (int i = 0; i < values.length; i++) {
            double extent = -360 * values[i] / total;
            Color c = COLORS[i % COLORS.length];
            g2.setPaint(new GradientPaint(x, y, UiTheme.mix(c, .35f), x, y + d, UiTheme.dark(c, .1f)));
            g2.fill(new Arc2D.Double(x, y, d, d, angle, extent, Arc2D.PIE));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Arc2D.Double(x, y, d, d, angle, extent, Arc2D.PIE));
            angle += extent;
        }
        g2.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 140), 0, y + d / 2f, new Color(255, 255, 255, 0)));
        g2.fill(new Ellipse2D.Double(x + d * .1, y + 4, d * .8, d * .5));

        int lx = x + d + 30, ly = y + 20;
        for (int i = 0; i < values.length; i++) {
            g2.setColor(COLORS[i % COLORS.length]);
            g2.fillRoundRect(lx, ly + i * 28, 16, 16, 6, 6);
            g2.setColor(UiTheme.INK);
            g2.setFont(new Font(UiTheme.FONT, Font.PLAIN, 14));
            g2.drawString(names[i] + "  " + Math.round(100 * values[i] / total) + "%", lx + 26, ly + i * 28 + 13);
        }
        g2.dispose();
    }
}
