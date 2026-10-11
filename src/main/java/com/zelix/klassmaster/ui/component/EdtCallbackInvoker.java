package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ObservableModel;

import java.io.IOException;
import javax.swing.SwingUtilities;

public class EdtCallbackInvoker {
    public EdtCallbackInvoker(EdtCallback edtCallback, ObservableModel observableModel1, Object changeArg1, Object changeArg2, Object changeArg3) throws ZkmException, IOException {
        if (SwingUtilities.isEventDispatchThread()) {
            this.invokeCallback(edtCallback, observableModel1, changeArg1, changeArg2, changeArg3);
        } else {
            SwingUtilities.invokeLater(new ChangeNotifyTask(this, edtCallback, observableModel1, changeArg1, changeArg2, changeArg3));
        }
    }

    public final void invokeCallback(EdtCallback edtCallback, ObservableModel observableModel1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        edtCallback.handleChangeOnEdt(observableModel1, object, object1, object2);
    }
}
