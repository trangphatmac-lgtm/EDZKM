package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.exceptions.ZkmRuntimeException;
import com.zelix.klassmaster.ui.dialog.CopyableMessageDialog;
import com.zelix.klassmaster.ui.dialog.EscapeClosableDialog;
import com.zelix.klassmaster.ui.dialog.MessageBoxDialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SwingUtils {
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    public static EscapeClosableDialog createMessageDialog(JFrame jFrame, String string, String string1, String textArgument) {
        return textArgument.length() > 150 ? new CopyableMessageDialog(jFrame, string, string1, textArgument) : new MessageBoxDialog(jFrame, string, textArgument);
    }

    public static boolean isNearBlack(Color color) {
        return color.getRed() + color.getBlue() + color.getGreen() < 50;
    }

    public static void setVisibleOnEdt(Window window1) {
        if (SwingUtilities.isEventDispatchThread()) {
            window1.setVisible(true);
        } else {
            try {
                SwingUtilities.invokeAndWait(new SetWindowVisibleTask(window1));
            } catch (InterruptedException interruptedException) {
                throw new ZkmRuntimeException(interruptedException.getMessage() + " " + window1.getClass().getName());
            } catch (InvocationTargetException invocationTargetException) {
                throw new ZkmRuntimeException(invocationTargetException.getTargetException().getMessage() + " " + window1.getClass().getName());
            }
        }
    }

    public static void setHtmlText(JEditorPane jEditorPane, String textArgument) {
        jEditorPane.setContentType("text/html");
        jEditorPane.setText(textArgument);
        jEditorPane.setCaretPosition(0);
    }

    public static boolean hasContrast(Color color, Color color1, int ba) {
        int red = color.getRed();
        int blue = color.getBlue();
        int green = color.getGreen();
        int redValue = color1.getRed();
        int blueValue = color1.getBlue();
        int greenValue = color1.getGreen();
        int bh = Math.abs(red - redValue);
        bh += Math.abs(blue - blueValue);
        bh += Math.abs(green - greenValue);
        int bj = Math.abs(red - redValue);
        int bi = bj;
        bj = Math.abs(blue - blueValue);
        if (bj > bi) {
            bi = bj;
        }

        bj = Math.abs(green - greenValue);
        if (bj > bi) {
            bi = bj;
        }

        return bh >= ba || bi > ba * 2 / 3;
    }

    public static void setHtmlTextOnEdt(JEditorPane jEditorPane, String textArgument) {
        if (SwingUtilities.isEventDispatchThread()) {
            setHtmlText(jEditorPane, textArgument);
        } else {
            SwingUtilities.invokeLater(new SetEditorTextTask(jEditorPane, textArgument));
        }
    }

    public static void packOnEdt(Window window1) {
        if (SwingUtilities.isEventDispatchThread()) {
            window1.pack();
        } else {
            try {
                SwingUtilities.invokeAndWait(new WindowPackTask(window1));
            } catch (InterruptedException interruptedException) {
                throw new ZkmRuntimeException(interruptedException.getMessage() + " " + window1.getClass().getName());
            } catch (InvocationTargetException invocationTargetException) {
                throw new ZkmRuntimeException(invocationTargetException.getTargetException().getMessage() + " " + window1.getClass().getName());
            }
        }
    }

    public static boolean isNearWhite(Color color) {
        int ba = color.getRed() + color.getBlue() + color.getGreen();
        return 765 - ba < 50;
    }

    public static void requestFocusCompat(Component component1) {
        try {
            component1.getClass().getMethod("requestFocusInWindow", EMPTY_CLASS_ARRAY).invoke(component1, EMPTY_CLASS_ARRAY);
        } catch (Throwable throwable) {
            component1.requestFocus();
        }
    }

    public static void applyHtmlText(JEditorPane jEditorPane, String textArgument) {
        setHtmlText(jEditorPane, textArgument);
    }

    public static boolean isDark(Color color) {
        return color.getRed() + color.getBlue() + color.getGreen() < 255;
    }

    public static boolean hasStrongContrast(Color color, Color color1) {
        return hasContrast(color, color1, 150);
    }

    public static void requestFocusOnEdt(Component component1) {
        if (SwingUtilities.isEventDispatchThread()) {
            requestFocusCompat(component1);
        } else {
            SwingUtilities.invokeLater(new FocusRequestRunnable(component1));
        }
    }

    public static void requestFocusNow(Component component1) {
        requestFocusCompat(component1);
    }

    private SwingUtils() {
    }
}
