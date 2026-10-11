package com.zelix.klassmaster.ui.dialog;

import javax.swing.JFrame;

public class ShowMessageDialogTask implements Runnable {
    public final JFrame ownerFrame;
    public final String title;
    public final String message;
    public final String details;

    @Override
    public void run() {
        new CopyableMessageDialog(this.ownerFrame, this.title, this.message, this.details);
    }

    public ShowMessageDialogTask(JFrame jFrame, String titleArgument, String messageArgument, String detailsArgument) {
        this.ownerFrame = jFrame;
        this.title = titleArgument;
        this.message = messageArgument;
        this.details = detailsArgument;
    }
}
