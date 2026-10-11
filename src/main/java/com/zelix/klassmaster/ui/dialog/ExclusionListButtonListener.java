package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ExclusionListButtonListener implements ActionListener {
    public final ParameterListDialog dialog;

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.dialog.mainAddBtn) {
                this.dialog.startAddParameter();
            } else if (source == this.dialog.mainModifyBtn) {
                this.dialog.modifySelectedParameter();
            } else if (source == this.dialog.mainDeleteBtn) {
                this.dialog.removeSelectedParameter();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ExclusionListButtonListener(ParameterListDialog parameterListDialog1) {
        this.dialog = parameterListDialog1;
    }
}
