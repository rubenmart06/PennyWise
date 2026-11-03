package cnu.ssd.pennywise.gui.components;

import java.awt.*;

/**
 * Shared Frutiger Aero palette and paint helpers. Just static constants/utilities.
 */
public final class UiTheme {

    private UiTheme() {
    }

    public static final Color SKY = new Color(0x2BA3E8);
    public static final Color DEEP = new Color(0x0B5E9E);
    public static final Color GREEN = new Color(0x4DAF28);
    public static final Color RED = new Color(0xE0533D);
    public static final Color AMBER = new Color(0xF2A81D);
    public static final Color INK = new Color(0x12466B);
    public static final Color MUTED = new Color(0x5F86A3);
    public static final String FONT = "Segoe UI";

    public static Color mix(Color c, float f) {
        return new Color(
                (int) (c.getRed() + (255 - c.getRed()) * f),
                (int) (c.getGreen() + (255 - c.getGreen()) * f),
                (int) (c.getBlue() + (255 - c.getBlue()) * f));
    }

    public static Color dark(Color c, float f) {
        return new Color(
                (int) (c.getRed() * (1 - f)),
                (int) (c.getGreen() * (1 - f)),
                (int) (c.getBlue() * (1 - f)));
    }

    public static Graphics2D g2(Graphics g) {
        Graphics2D x = (Graphics2D) g.create();
        x.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return x;
    }
}
