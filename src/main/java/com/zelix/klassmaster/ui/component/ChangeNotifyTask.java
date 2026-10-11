package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.util.ObservableModel;
import com.zelix.klassmaster.util.ZkmUtils;

public class ChangeNotifyTask implements Runnable {
    public EdtCallbackInvoker invoker;
    public EdtCallback callback;
    public ObservableModel observableModel;
    public Object changeArg1;
    public Object changeArg2;
    public Object changeArg3;

    public ChangeNotifyTask(
            EdtCallbackInvoker edtCallbackInvoker, EdtCallback edtCallback, ObservableModel observableModel1, Object changeArg1Argument, Object changeArg2Argument, Object changeArg3Argument
    ) {
        this.invoker = edtCallbackInvoker;
        this.callback = edtCallback;
        this.observableModel = observableModel1;
        this.changeArg1 = changeArg1Argument;
        this.changeArg2 = changeArg2Argument;
        this.changeArg3 = changeArg3Argument;
    }

    @Override
    public void run() {
        try {
            this.invoker.invokeCallback(this.callback, this.observableModel, this.changeArg1, this.changeArg2, this.changeArg3);
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }
}
