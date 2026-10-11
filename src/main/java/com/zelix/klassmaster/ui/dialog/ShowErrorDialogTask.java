package com.zelix.klassmaster.ui.dialog;

import java.awt.Frame;

public class ShowErrorDialogTask implements Runnable {
    public final Frame ownerFrame;
    public final String title;
    public final String message;

    public ShowErrorDialogTask(Frame frame1, String titleArgument, String messageArgument) {
        this.ownerFrame = frame1;
        this.title = titleArgument;
        this.message = messageArgument;
    }

    @Override
    public void run() {
        new MessageBoxDialog(this.ownerFrame, this.title, this.message);
    }
}
