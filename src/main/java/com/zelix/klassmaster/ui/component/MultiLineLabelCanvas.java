package com.zelix.klassmaster.ui.component;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.StringTokenizer;
import java.util.Vector;

public class MultiLineLabelCanvas extends Canvas {
    private static final String WORD_DELIMITERS = " \n\r";
    public static int lineSpacing = 1;
    public boolean linesWrapped;
    public Vector lines;
    public String text = "Zelix KlassMaster could not find the class java.lang.Object. Please select the archive file that contains the Java bootstrap classes that you wish Zelix KlassMaster to use.";
    public int wrapWidth = 250;
    public FontMetrics fontMetrics;
    public int ascent;
    public int descent;

    @Override
    public void invalidate() {
        super.invalidate();
        this.linesWrapped = false;
    }

    @Override
    public synchronized void setBounds(int ba, int bb, int bc, int bd) {
        super.setBounds(ba, bb, bc, bd);
        this.wrapWidth = this.getSize().width;
    }

    public int getTextHeight() {
        if (!this.linesWrapped) {
            this.wrapLines();
        }

        int textHeight;
        if (this.lines == null) {
            textHeight = 0;
        } else {
            textHeight = this.lines.size() * (this.ascent + this.descent + lineSpacing);
        }

        return textHeight;
    }

    @Override
    public Dimension getPreferredSize() {
        return this.getMinimumSize();
    }

    @Override
    public void update(Graphics graphics) {
        this.paint(graphics);
    }

    public void wrapLines() {
        this.lines = new Vector();
        StringTokenizer tokens = new StringTokenizer(this.text, WORD_DELIMITERS, true);
        StringBuffer lineText = new StringBuffer();
        int lineWidth = 0;
        int tokenCount = tokens.countTokens();

        for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
            String tokenText = tokens.nextToken();
            if (tokenText.equals("\n") || tokenText.equals("\r")) {
                this.lines.addElement(lineText.toString());
                lineText = new StringBuffer();
                lineWidth = 0;
            } else {
                int tokenWidth = this.fontMetrics.stringWidth(tokenText);
                lineWidth += tokenWidth;
                if (lineWidth <= this.wrapWidth) {
                    lineText.append(tokenText);
                } else {
                    this.lines.addElement(lineText.toString());
                    lineText = new StringBuffer();
                    if (!tokenText.equals(" ")) {
                        lineText.append(tokenText);
                        lineWidth = tokenWidth;
                    } else {
                        lineWidth = 0;
                    }
                }
            }

            if (tokenIndex == tokenCount - 1) {
                this.lines.addElement(lineText.toString());
            }
            this.linesWrapped = true;
        }
    }

    @Override
    public void validate() {
        super.validate();
        this.repaint();
    }

    public MultiLineLabelCanvas(Font font) {
        this.setFont(font);
        this.fontMetrics = this.getFontMetrics(this.getFont());
        this.ascent = this.fontMetrics.getAscent();
        this.descent = this.fontMetrics.getDescent();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(this.wrapWidth, this.getTextHeight());
    }

    @Override
    public void paint(Graphics graphics) {
        if (!this.linesWrapped) {
            this.wrapLines();
        }

        Dimension dimension = this.getSize();
        int heightValue = dimension.height;
        int widthValue = dimension.width;
        graphics.clearRect(0, 0, widthValue, heightValue);
        int ascent = this.ascent;
        int elementIndex = 0;
        int bf = 0;

        for (Vector vector = this.lines; bf < vector.size(); vector = this.lines) {
            String string = (String) this.lines.elementAt(elementIndex);
            int ascentValue = ascent;
            graphics.drawString(string, 0, ascentValue);
            ascent += this.ascent + this.descent + lineSpacing;
            bf = ++elementIndex;
        }
    }
}
