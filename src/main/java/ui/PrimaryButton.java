package ui;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

public class PrimaryButton extends JButton {
    public static final Color PRIMARY_COLOR = new Color(0xFF6633);
    public static final Color TEXT_COLOR = Color.WHITE;
    private static final int ARC = 12;
    private static final Insets BORDER_INSETS = new Insets(6, 16, 6, 16);

    public PrimaryButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(TEXT_COLOR);
        if (getFont() != null) {
            setFont(getFont().deriveFont(Font.BOLD));
        }
        setBorder(BorderFactory.createEmptyBorder(
                BORDER_INSETS.top, BORDER_INSETS.left, BORDER_INSETS.bottom, BORDER_INSETS.right));
        setMargin(new Insets(4, 12, 4, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(PRIMARY_COLOR);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.dispose();

        Color originalForeground = getForeground();
        if (!isEnabled()) {
            setForeground(TEXT_COLOR);
        }

        super.paintComponent(g);
        setForeground(originalForeground);

        if (!isEnabled() && getText() != null) {
            Graphics2D textGraphics = (Graphics2D) g.create();
            textGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            textGraphics.setColor(TEXT_COLOR);
            FontMetrics fm = textGraphics.getFontMetrics(getFont());
            int textWidth = fm.stringWidth(getText());
            int textX = (getWidth() - textWidth) / 2;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            textGraphics.drawString(getText(), textX, textY);
            textGraphics.dispose();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (getFont() != null) {
            setFont(getFont().deriveFont(Font.BOLD));
        }
        setForeground(TEXT_COLOR);
    }
}
