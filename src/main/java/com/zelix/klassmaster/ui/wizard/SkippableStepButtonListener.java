package com.zelix.klassmaster.ui.wizard;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SkippableStepButtonListener implements ActionListener {
    public final TrimOptionsWizardDialog dialog;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.dialog.previousBtn) {
                this.dialog.onPrevious();
            } else if (source == this.dialog.okBtn) {
                this.dialog.onOk();
            } else if (source == this.dialog.skipBtn) {
                this.dialog.skipStep();
            } else if (source == this.dialog.cancelBtn) {
                this.dialog.onCancel();
            } else if (source == this.dialog.helpBtn) {
                this.dialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public SkippableStepButtonListener(TrimOptionsWizardDialog trimOptionsWizardDialog) {
        this.dialog = trimOptionsWizardDialog;
    }
}
