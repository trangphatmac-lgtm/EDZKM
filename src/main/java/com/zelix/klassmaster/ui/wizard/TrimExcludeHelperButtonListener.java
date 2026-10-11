package com.zelix.klassmaster.ui.wizard;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrimExcludeHelperButtonListener implements ActionListener {
    public final TrimExcludeHelperDialog dialog;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.dialog.previousBtn) {
                this.dialog.goToPreviousStep();
            } else if (source == this.dialog.okBtn) {
                this.dialog.acceptParameters();
            } else if (source == this.dialog.skipBtn) {
                this.dialog.skipStep();
            } else if (source == this.dialog.cancelBtn) {
                this.dialog.cancelDialog();
            } else if (source == this.dialog.helpBtn) {
                this.dialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public TrimExcludeHelperButtonListener(TrimExcludeHelperDialog trimExcludeHelperDialog) {
        this.dialog = trimExcludeHelperDialog;
    }
}
