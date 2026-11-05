package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Glossy progress bar (green → amber → red as the limit approaches).
 */
public class BudgetBar extends JComponent {

    private double ratio;

    public BudgetBar(double ratio) {
        this.ratio = ratio;
        setPreferredSize(new Dimension(200, 22));
        setOpaque(false);
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = getWidth() - 2, h = Math.min(getHeight() - 2, 20), y = (getHeight() - h) / 2;
        g2.setColor(new Color(0xCFE6F5));
        g2.fillRoundRect(1, y, w, h, h, h);
        g2.setColor(new Color(0x8DBBD8));
        g2.drawRoundRect(1, y, w, h, h, h);

        Color c = ratio >= 1 ? UiTheme.RED : ratio >= .75 ? UiTheme.AMBER : UiTheme.GREEN;
        int fw = Math.max(h, (int) (w * Math.min(1, ratio)));
        RoundRectangle2D s = new RoundRectangle2D.Float(1, y, fw, h, h, h);
        g2.setPaint(new GradientPaint(0, y, UiTheme.mix(c, .35f), 0, y + h, UiTheme.dark(c, .1f)));
        g2.fill(s);
        g2.setClip(s);
        g2.setPaint(new Color(255, 255, 255, 110));
        g2.fillRect(1, y, fw, h / 2);
        g2.setClip(null);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font(UiTheme.FONT, Font.BOLD, 11));
        String t = (int) (ratio * 100) + "%";
        g2.drawString(t, Math.max(8, fw / 2 - 8), y + h - 6);
        g2.dispose();
    }
}
