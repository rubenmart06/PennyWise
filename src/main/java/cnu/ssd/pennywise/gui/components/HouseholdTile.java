package cnu.ssd.pennywise.gui.components;

import cnu.ssd.pennywise.model.Household;
import cnu.ssd.pennywise.model.HouseholdKind;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/**
 * One selectable card on {@code HouseholdHubPanel},
 * Use {@link #HouseholdTile()} for the dashed "+ New household" tile instead.
 */
public class HouseholdTile extends JButton {

    private final Household household;
    private final boolean addTile;

    /** A real household tile. */
    public HouseholdTile(Household household) {
        this.household = household;
        this.addTile = false;
        init();
    }

    /** The dashed "+ New household" tile. */
    public HouseholdTile() {
        this.household = null;
        this.addTile = true;
        init();
    }

    public Household getHousehold() {
        return household;
    }

    private void init() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(270, 150));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = UiTheme.g2(g);
        int w = getWidth(), h = getHeight();
        boolean over = getModel().isRollover();
        RoundRectangle2D s = new RoundRectangle2D.Float(2, 2, w - 6, h - 8, 26, 26);
        g2.setColor(new Color(0, 60, 110, 30));
        g2.fill(new RoundRectangle2D.Float(4, 6, w - 6, h - 8, 26, 26));
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, over ? 250 : 225), 0, h,
                over ? new Color(0xCB, 0xEC, 0xFF, 240) : new Color(0xE4, 0xF4, 0xFF, 205)));
        g2.fill(s);
        g2.setClip(s);
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 160), 0, h * .4f, new Color(255, 255, 255, 0)));
        g2.fillRect(0, 0, w, (int) (h * .4f));
        g2.setClip(null);

        if (addTile) {
            g2.setColor(UiTheme.SKY);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 7f}, 0f));
            g2.draw(s);
            g2.setFont(new java.awt.Font(UiTheme.FONT, java.awt.Font.BOLD, 46));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("+", (w - fm.stringWidth("+")) / 2, h / 2 + 4);
            g2.setFont(new java.awt.Font(UiTheme.FONT, java.awt.Font.BOLD, 14));
            fm = g2.getFontMetrics();
            g2.setColor(UiTheme.DEEP);
            g2.drawString("New household", (w - fm.stringWidth("New household")) / 2, h / 2 + 34);
        } else {
            g2.setColor(over ? UiTheme.SKY : new Color(255, 255, 255, 240));
            g2.setStroke(new BasicStroke(over ? 2.4f : 1.6f));
            g2.draw(s);

            Color c = household.getKind() == HouseholdKind.SHARED ? UiTheme.GREEN : UiTheme.SKY;
            g2.setPaint(new GradientPaint(0, 20, UiTheme.mix(c, .35f), 0, 66, UiTheme.dark(c, .1f)));
            g2.fillOval(20, 20, 46, 46);
            g2.setPaint(new GradientPaint(0, 20, new Color(255, 255, 255, 170), 0, 43, new Color(255, 255, 255, 10)));
            g2.fill(new Ellipse2D.Double(25, 22, 36, 22));
            String initial = household.getName().isEmpty() ? "?" : household.getName().substring(0, 1).toUpperCase();
            g2.setColor(Color.WHITE);
            g2.setFont(new java.awt.Font(UiTheme.FONT, java.awt.Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial, 43 - fm.stringWidth(initial) / 2, 50);

            g2.setColor(UiTheme.INK);
            g2.setFont(new java.awt.Font(UiTheme.FONT, java.awt.Font.BOLD, 18));
            g2.drawString(household.getName(), 78, 40);

            String kindLabel = household.getKind() == HouseholdKind.SHARED ? "Shared" : "Individual";
            g2.setColor(UiTheme.MUTED);
            g2.setFont(new java.awt.Font(UiTheme.FONT, java.awt.Font.PLAIN, 12));
            g2.drawString(kindLabel + " \u00b7 " + household.getMemberCount()
                    + (household.getMemberCount() == 1 ? " member" : " members"), 78, 60);

            g2.setColor(new Color(43, 143, 214, 60));
            g2.drawLine(20, 88, w - 24, 88);
            g2.setColor(UiTheme.MUTED);
            g2.drawString("Primary currency: " + household.getPrimaryCurrency(), 20, 108);
        }
        g2.dispose();
    }
}
