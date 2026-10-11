package com.zelix.klassmaster.ui.dialog;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MessageDialogKeyListener extends KeyAdapter {
    public final CopyableMessageDialog copyableMessageDialog;

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        Object source = keyEvent.getSource();
        if (keyEvent.getKeyCode() == 10) {
            if (source == this.copyableMessageDialog.okBtn) {
                this.copyableMessageDialog.closeDialog();
            } else if (source == this.copyableMessageDialog.copyBtn) {
                this.copyableMessageDialog.copyMessageToClipboard();
            }
        }
    }

    public MessageDialogKeyListener(CopyableMessageDialog copyableMessageDialog1) {
        this.copyableMessageDialog = copyableMessageDialog1;
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
