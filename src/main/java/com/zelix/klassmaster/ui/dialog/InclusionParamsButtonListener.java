package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InclusionParamsButtonListener implements ActionListener {
    public final InclusionParametersDialog inclusionParametersDialog;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == InclusionParametersDialog.accessPreviousButton(this.inclusionParametersDialog)) {
                this.inclusionParametersDialog.goPrevious();
            } else if (source == this.inclusionParametersDialog.okBtn) {
                this.inclusionParametersDialog.acceptParameters();
            } else if (source == this.inclusionParametersDialog.cancelBtn) {
                this.inclusionParametersDialog.cancelDialog();
            } else if (source == this.inclusionParametersDialog.helpBtn) {
                this.inclusionParametersDialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public InclusionParamsButtonListener(InclusionParametersDialog inclusionParametersDialog1) {
        this.inclusionParametersDialog = inclusionParametersDialog1;
    }
}
