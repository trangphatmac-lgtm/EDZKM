package com.zelix.klassmaster.ui.dialog;

import com.zelix.klassmaster.ui.component.DisableableListItem;
import com.zelix.klassmaster.util.ZkmUtils;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ExclusionListSelectionListener implements ListSelectionListener {
    public final ParameterListDialog dialog;

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        try {
            if (!this.dialog.removingItem) {
                if (this.dialog.paramList.getSelectedIndex() > -1) {
                    DisableableListItem disableableListItem = (DisableableListItem) this.dialog.paramList.getSelectedValue();
                    if (!disableableListItem.isDisabled()) {
                        this.dialog.mainModifyBtn.setEnabled(true);
                        this.dialog.mainDeleteBtn.setEnabled(true);
                    } else {
                        this.dialog.mainModifyBtn.setEnabled(false);
                        this.dialog.mainDeleteBtn.setEnabled(false);
                    }

                    String item = (String) disableableListItem.getItem();
                    String text = this.dialog.parseParameter(item).buildDescription(this.dialog.paramKind);
                    this.dialog.explanationArea.setText(text);
                    this.dialog.explanationArea.setCaretPosition(0);
                }
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ExclusionListSelectionListener(ParameterListDialog parameterListDialog1) {
        this.dialog = parameterListDialog1;
    }
}
