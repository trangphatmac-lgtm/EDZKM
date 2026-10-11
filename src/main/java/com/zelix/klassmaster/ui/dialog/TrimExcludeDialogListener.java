package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrimExcludeDialogListener implements ActionListener {
    public final TrimExclusionsDialog trimExclusionsDialog;

    public TrimExcludeDialogListener(TrimExclusionsDialog trimExclusionsDialog1) {
        this.trimExclusionsDialog = trimExclusionsDialog1;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.trimExclusionsDialog.okBtn) {
                this.trimExclusionsDialog.acceptParameters();
            } else if (source == this.trimExclusionsDialog.testBtn) {
                this.trimExclusionsDialog.runTestTrim();
            } else if (source == this.trimExclusionsDialog.cancelBtn) {
                this.trimExclusionsDialog.cancelDialog();
            } else if (source == this.trimExclusionsDialog.helpBtn) {
                this.trimExclusionsDialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }
}
