package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExcludeDialogActionListener implements ActionListener {
    public final NameExclusionsDialog dialog;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.dialog.okBtn) {
                this.dialog.acceptParameters();
            } else if (source == this.dialog.cancelBtn) {
                this.dialog.cancelDialog();
            } else if (source == this.dialog.helpBtn) {
                this.dialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ExcludeDialogActionListener(NameExclusionsDialog nameExclusionsDialog) {
        this.dialog = nameExclusionsDialog;
    }
}
