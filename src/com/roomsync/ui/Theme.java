package com.roomsync.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;

/** Every colour, font and size used by the UI, plus the few custom painted components. */
public final class Theme {
    private Theme() {
    }

    // ---------- palette ----------
    public static final Color BACKGROUND = new Color(0xF1F7F7);
    public static final Color CARD = Color.WHITE;
    public static final Color CARD_ALT = new Color(0xF1F7F7);
    public static final Color SURFACE = new Color(0xD5E5E5);
    public static final Color TEXT = new Color(0x5D6B6B);
    public static final Color TEXT_STRONG = new Color(0x3F4C4C);   // darker slate for small text on pastel badges
    public static final Color TEXT_MUTED = new Color(0x788686);
    public static final Color TEXT_ON_PRIMARY = new Color(0xF1F7F7);
    public static final Color PRIMARY_HOVER = new Color(0x4C5959);
    public static final Color ACCENT = new Color(0xBDD7D8);
    public static final Color ACCENT_DEEP = new Color(0x9CC2C4);   // derived from ACCENT, for bar fills
    public static final Color WARM = new Color(0xF7CBCA);
    public static final Color ROSE_DEEP = new Color(0xEDA3A1);     // derived from WARM, for HIGH severity
    public static final Color DIVIDER = new Color(0xDDD3D3);
    public static final Color SHADOW = new Color(93, 107, 107, 7);  // one layer of the soft card shadow

    public static final Color SEVERITY_LOW = ACCENT;
    public static final Color SEVERITY_MEDIUM = WARM;
    public static final Color SEVERITY_HIGH = ROSE_DEEP;

    // ---------- type ----------
    public static final String FONT_FAMILY = pickFont();
    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 26);
    public static final Font HEADING = new Font(FONT_FAMILY, Font.BOLD, 17);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font BIG_NUMBER = new Font(FONT_FAMILY, Font.BOLD, 30);
    public static final Font BRAND = new Font(FONT_FAMILY, Font.BOLD, 19);

    // ---------- sizes ----------
    public static final int RADIUS = 14;
    public static final int BUTTON_RADIUS = 10;
    public static final int SHADOW_SIZE = 6;
    public static final int PAD = 24;
    public static final int GAP = 12;
    public static final int SMALL_GAP = 6;
    public static final int BAR_HEIGHT = 10;
    public static final int LABEL_SLACK = 6;
    public static final int PAGE_PAD_X = 48;
    public static final int WINDOW_WIDTH = 1080;
    public static final int WINDOW_HEIGHT = 780;
    public static final int FIELD_COLUMNS = 18;
    public static final int MATCH_TEXT_WIDTH = 190;
    public static final int WIDE_TEXT_WIDTH = 640;

    public static final String FOOTER_NOTE =
            "Recommendations are suggestions only and do not guarantee hostel or room allocation.";

    private static String pickFont() {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        if (Arrays.asList(names).contains("Segoe UI")) return "Segoe UI";
        if (Arrays.asList(names).contains("Inter")) return "Inter";
        return Font.SANS_SERIF;
    }

    /** Applies global defaults so dialogs and fields match the theme. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // the default look and feel is fine too
        }
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BODY);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("TextArea.font", BODY);
    }

    // ---------- simple factories ----------

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        // A little slack on the right: layout and painting can measure text a pixel or two apart.
        l.setBorder(new EmptyBorder(0, 0, 0, LABEL_SLACK));
        return l;
    }

    public static JLabel title(String text) {
        return label(text, TITLE, TEXT);
    }

    public static JLabel heading(String text) {
        return label(text, HEADING, TEXT);
    }

    public static JLabel body(String text) {
        return label(text, BODY, TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, SMALL, TEXT_MUTED);
    }

    /** Text that wraps at the given width. */
    public static JLabel wrap(String text, Font font, Color color, int width) {
        return label("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>", font, color);
    }

    public static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static Component space(int height) {
        Component c = Box.createVerticalStrut(height);
        ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Transparent vertical stack with left aligned children. */
    public static JPanel column(Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : items) add(p, c);
        return p;
    }

    public static void add(JPanel stack, Component c) {
        if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        // Plain labels in a stack may use the full width, so text is never cut off by a measuring mismatch.
        if (c instanceof JLabel && !(c instanceof Pill)) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        stack.add(c);
    }

    /** Transparent horizontal row, flush left, items vertically centred with a small gap between them. */
    public static JPanel row(Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : items) addToRow(p, c);
        return p;
    }

    public static void addToRow(JPanel row, Component c) {
        if (row.getComponentCount() > 0) row.add(Box.createHorizontalStrut(SMALL_GAP));
        if (c instanceof JComponent) ((JComponent) c).setAlignmentY(Component.CENTER_ALIGNMENT);
        row.add(c);
    }

    /** A card with a heading and stacked content. */
    public static Card card(String heading, Component... items) {
        Card card = new Card(CARD);
        if (heading != null) {
            add(card, heading(heading));
            add(card, space(GAP));
        }
        for (Component c : items) add(card, c);
        return card;
    }

    public static JTextField textField() {
        JTextField f = new JTextField(FIELD_COLUMNS);
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField(FIELD_COLUMNS);
        styleField(f);
        return f;
    }

    private static void styleField(JTextField f) {
        f.setFont(BODY);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBackground(CARD);
        f.setBorder(fieldBorder());
        f.setMaximumSize(new Dimension(320, f.getPreferredSize().height));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(new LineBorder(DIVIDER, 1, true), new EmptyBorder(8, 10, 8, 10));
    }

    /** Thin footer shown under result screens. */
    public static JComponent footer() {
        JLabel l = label(FOOTER_NOTE, SMALL, TEXT_MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BACKGROUND);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER), new EmptyBorder(10, PAD, 12, PAD)));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    /** A full screen: scrolling content with page padding, and an optional footer note. */
    public static JPanel page(JComponent content, boolean withFooter) {
        JPanel holder = new WidthTrackingPanel();
        holder.setBackground(BACKGROUND);
        holder.setBorder(new EmptyBorder(PAD, PAGE_PAD_X, PAD * 2, PAGE_PAD_X));
        holder.add(content, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.add(scroll, BorderLayout.CENTER);
        if (withFooter) page.add(footer(), BorderLayout.SOUTH);
        return page;
    }

    public static Color severityColor(String severity) {
        switch (severity) {
            case "HIGH": return SEVERITY_HIGH;
            case "MEDIUM": return SEVERITY_MEDIUM;
            default: return SEVERITY_LOW;
        }
    }

    private static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Scroll view that always matches the window width, so pages only ever scroll vertically. */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 32; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ---------- custom painted components ----------

    /** Rounded card with a soft shadow. Lays its children out vertically. */
    public static class Card extends JPanel {
        private final Color fill;
        private final boolean shadow;

        public Card(Color fill) {
            this(fill, true, PAD);
        }

        public Card(Color fill, boolean shadow, int padding) {
            this.fill = fill;
            this.shadow = shadow;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            int s = shadow ? SHADOW_SIZE : 0;
            setBorder(new EmptyBorder(s + padding, s + padding, s + padding, s + padding));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight(), s = shadow ? SHADOW_SIZE : 0;
            // Stacked translucent layers, nudged down a little, read as a soft shadow.
            for (int i = 0; i < s; i++) {
                g2.setColor(SHADOW);
                g2.fillRoundRect(i, i + 2, w - 2 * i, h - 2 * i - 2, RADIUS + s - i, RADIUS + s - i);
            }
            g2.setColor(fill);
            g2.fillRoundRect(s, s, w - 2 * s, h - 2 * s, RADIUS, RADIUS);
            g2.dispose();
        }
    }

    /** Flat rounded button: PRIMARY (slate), SECONDARY (soft surface) or NAV (transparent until hovered or selected). */
    public static class Button extends JButton {
        public static final int PRIMARY = 0, SECONDARY = 1, NAV = 2;
        private final int kind;

        public Button(String text, int kind) {
            super(text);
            this.kind = kind;
            setFont(kind == NAV ? BODY : BODY_BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            boolean hover = getModel().isRollover() && isEnabled();
            Color bg, fg;
            if (!isEnabled()) {
                bg = kind == NAV ? null : DIVIDER;
                fg = TEXT_MUTED;
            } else if (kind == PRIMARY) {
                bg = hover ? PRIMARY_HOVER : TEXT;
                fg = TEXT_ON_PRIMARY;
            } else if (kind == SECONDARY) {
                bg = hover ? ACCENT : SURFACE;
                fg = TEXT;
            } else {
                bg = isSelected() ? ACCENT : hover ? CARD_ALT : null;
                fg = TEXT;
            }
            if (bg != null) {
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BUTTON_RADIUS, BUTTON_RADIUS);
            }
            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Rounded progress bar for 0 to 100 values. */
    public static class Bar extends JComponent {
        private final double value;

        public Bar(double value, int width) {
            this.value = Math.max(0, Math.min(100, value));
            setPreferredSize(new Dimension(width, BAR_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, BAR_HEIGHT));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(CARD_ALT);
            g2.fillRoundRect(0, 0, w, h, h, h);
            int filled = (int) Math.round(w * value / 100);
            if (filled > 0) {
                g2.setColor(ACCENT_DEEP);
                g2.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
            }
            g2.dispose();
        }
    }

    /** Small rounded label used for badges and occupancy chips. */
    public static class Pill extends JLabel {
        private final Color fill;

        public Pill(String text, Color fill) {
            super(text);
            this.fill = fill;
            setFont(SMALL_BOLD);
            setForeground(TEXT_STRONG);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
