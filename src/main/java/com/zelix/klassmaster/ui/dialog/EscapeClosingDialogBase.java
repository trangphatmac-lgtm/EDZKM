package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.ui.component.SwingUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public abstract class EscapeClosingDialogBase extends JDialog {
    public JFrame ownerFrame_JFrame;
    public static final String LINE_SEPARATOR = SwingUtils.LINE_SEPARATOR;

    @Override
    public JRootPane createRootPane() {
        JRootPane jRootPane = new JRootPane();
        KeyStroke keyStroke = KeyStroke.getKeyStroke("ESCAPE");
        Action action = this.createEscapeAction();
        jRootPane.getInputMap(2).put(keyStroke, "ESCAPE");
        jRootPane.getActionMap().put("ESCAPE", action);
        return jRootPane;
    }

    public final void showCentered() {
        this.showCenteredWithOffset(0, 0, false);
    }

    public final void showCenteredWithOffset(int xOffset, int yOffset, boolean relativeToOwner) {
        Dimension dialogSize = this.getSize();
        if (relativeToOwner && this.ownerFrame_JFrame.isVisible()) {
            Point ownerLocation = this.ownerFrame_JFrame.getLocationOnScreen();
            Dimension ownerSize = this.ownerFrame_JFrame.getSize();
            int x = ownerSize.width / 2 - dialogSize.width / 2 + ownerLocation.x;
            int y = ownerSize.height / 2 - dialogSize.height / 2 + ownerLocation.y;
            this.setLocation(Math.max(0, x) + xOffset, Math.max(0, y) + yOffset);
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - dialogSize.width / 2;
            int y = screenSize.height / 2 - dialogSize.height / 2;
            this.setLocation(Math.max(0, x) + xOffset, Math.max(0, y) + yOffset);
        }

        SwingUtils.setVisibleOnEdt(this);
    }

    public final void showCenteredOnOwner() {
        this.showCenteredWithOffset(0, 0, true);
    }

    public EscapeClosingDialogBase(JFrame jFrame, String string, boolean bl) {
        super(jFrame, string, bl);
        this.ownerFrame_JFrame = jFrame;
        DialogCloseListener dialogCloseListener = new DialogCloseListener(this);
        this.addWindowListener(dialogCloseListener);
    }

    public Action createEscapeAction() {
        return new EscapeCloseAction(this);
    }

    public void closeDialog() throws ZkmException, IOException {
        this.setVisible(false);
        this.dispose();
    }
}
