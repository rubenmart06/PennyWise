package cnu.ssd.pennywise.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Applies the glass table look (zebra rows, aqua gradient header,
 * green/red amount coloring for strings starting with "+"/"-") to a plain
 * {@link JTable}, and wraps it in a matching {@link JScrollPane}.
 */
public final class AeroTableStyler {

    private AeroTableStyler() {
    }

    public static JScrollPane style(JTable table) {
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setOpaque(false);
        table.setFont(new Font(UiTheme.FONT, Font.PLAIN, 13));
        table.setSelectionBackground(new Color(0xBFE6FF));
        table.setSelectionForeground(UiTheme.INK);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, f, r, c);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setOpaque(true);
                String s = String.valueOf(v);
                setForeground(s.startsWith("+") ? new Color(0x2F8A1A) : s.startsWith("-") ? new Color(0xC8402B) : UiTheme.INK);
                if (!sel) {
                    setBackground(r % 2 == 0 ? new Color(255, 255, 255, 215) : new Color(0xEAF6FF));
                }
                return this;
            }
        });

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean f, int r, int c) {
                JLabel l = new JLabel(String.valueOf(v)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D x = (Graphics2D) g;
                        x.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x7FCBF7), 0, getHeight(), new Color(0x2B8FD6)));
                        x.fillRect(0, 0, getWidth(), getHeight());
                        x.setPaint(new Color(255, 255, 255, 80));
                        x.fillRect(0, 0, getWidth(), getHeight() / 2);
                        super.paintComponent(g);
                    }
                };
                l.setOpaque(false);
                l.setForeground(Color.WHITE);
                l.setFont(new Font(UiTheme.FONT, Font.BOLD, 13));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                l.setPreferredSize(new Dimension(10, 34));
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(new LineBorder(new Color(0xB5DAF2), 1, true));
        return sp;
    }
}
