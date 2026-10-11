package com.zelix.klassmaster.ui.wizard;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class WizardStepEnterKeyListener extends KeyAdapter {
    public final TrimOptionsWizardDialog dialog;

    public WizardStepEnterKeyListener(TrimOptionsWizardDialog trimOptionsWizardDialog) {
        this.dialog = trimOptionsWizardDialog;
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        try {
            if (keyEvent.getKeyCode() == 10) {
                Object source = keyEvent.getSource();
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
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
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
