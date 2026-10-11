package com.zelix.klassmaster.ui.component;

import javax.swing.JEditorPane;

public class SetEditorTextTask implements Runnable {
    public final JEditorPane editorPane;
    public final String text;

    public SetEditorTextTask(JEditorPane jEditorPane, String textArgument) {
        this.editorPane = jEditorPane;
        this.text = textArgument;
    }

    @Override
    public void run() {
        SwingUtils.applyHtmlText(this.editorPane, this.text);
    }
}
