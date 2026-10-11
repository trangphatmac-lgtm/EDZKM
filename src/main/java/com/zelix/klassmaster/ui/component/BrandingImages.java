package com.zelix.klassmaster.ui.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

/** Native desktop branding, rendered without the legacy product artwork. */
public final class BrandingImages {
    private static final Color ACCENT = new Color(104, 79, 190);
    private static final Image BANNER = createImage(264, 60, "EDZKM", 42, false);
    private static final Image WINDOW_ICON = new BaseMultiResolutionImage(
            createImage(16, 16, "E", 12, true),
            createImage(32, 32, "E", 24, true),
            createImage(64, 64, "E", 48, true));

    private BrandingImages() {
    }

    public static Image banner() {
        return BANNER;
    }

    public static Image windowIcon() {
        return WINDOW_ICON;
    }

    private static BufferedImage createImage(int width, int height, String text, int fontSize, boolean icon) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(ACCENT);
            if (icon) {
                graphics.fillRoundRect(0, 0, width, height, width / 3, height / 3);
                graphics.setColor(Color.WHITE);
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(text, (width - metrics.stringWidth(text)) / 2,
                    (height - metrics.getHeight()) / 2 + metrics.getAscent());
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
