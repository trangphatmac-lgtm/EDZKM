package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TrimOptionsActionListener implements ActionListener {
    public final DeleteAttributesDialog deleteAttributesDialog;

    public TrimOptionsActionListener(DeleteAttributesDialog deleteAttributesDialog1) {
        this.deleteAttributesDialog = deleteAttributesDialog1;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.deleteAttributesDialog.previousBtn) {
                this.deleteAttributesDialog.onPrevious();
            } else if (source == this.deleteAttributesDialog.okBtn) {
                this.deleteAttributesDialog.onOk();
            } else if (source == this.deleteAttributesDialog.cancelBtn) {
                this.deleteAttributesDialog.onCancel();
            } else if (source == this.deleteAttributesDialog.helpBtn) {
                this.deleteAttributesDialog.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }
}
