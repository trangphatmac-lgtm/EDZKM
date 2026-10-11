package com.zelix.klassmaster.ui.wizard;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class BuildHelperTrimExclKeyListener extends KeyAdapter {
    public final TrimExclusionsWizardDialog trimExclusionsDialog;

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        try {
            if (keyEvent.getKeyCode() == 10) {
                Object source = keyEvent.getSource();
                if (keyEvent.getSource() == this.trimExclusionsDialog.previousBtn) {
                    this.trimExclusionsDialog.goToPreviousStep();
                } else if (source == this.trimExclusionsDialog.okBtn) {
                    this.trimExclusionsDialog.acceptParameters();
                } else if (source == this.trimExclusionsDialog.testBtn) {
                    this.trimExclusionsDialog.runTestTrim();
                } else if (source == this.trimExclusionsDialog.skipBtn) {
                    this.trimExclusionsDialog.skipStep();
                } else if (source == this.trimExclusionsDialog.cancelBtn) {
                    this.trimExclusionsDialog.cancelDialog();
                } else if (source == this.trimExclusionsDialog.helpBtn) {
                    this.trimExclusionsDialog.showHelp();
                }
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public BuildHelperTrimExclKeyListener(TrimExclusionsWizardDialog trimExclusionsWizardDialog) {
        this.trimExclusionsDialog = trimExclusionsWizardDialog;
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
