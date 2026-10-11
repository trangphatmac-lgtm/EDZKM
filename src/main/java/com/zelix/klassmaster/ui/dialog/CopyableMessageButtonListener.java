package com.zelix.klassmaster.ui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CopyableMessageButtonListener implements ActionListener {
    public final CopyableMessageDialog dialog;

    public CopyableMessageButtonListener(CopyableMessageDialog copyableMessageDialog1) {
        this.dialog = copyableMessageDialog1;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Object source = actionEvent.getSource();
        if (source == this.dialog.okBtn) {
            this.dialog.closeDialog();
        } else if (source == this.dialog.copyBtn) {
            this.dialog.copyMessageToClipboard();
        }
    }
}
