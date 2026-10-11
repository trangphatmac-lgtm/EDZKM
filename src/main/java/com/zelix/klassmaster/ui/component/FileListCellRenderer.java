package com.zelix.klassmaster.ui.component;

import java.awt.Component;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

public class FileListCellRenderer extends DefaultListCellRenderer {
    public final FilePathList fileList;
    public FileIconProvider iconProvider;

    public FileListCellRenderer(FilePathList filePathList) {
        this.fileList = filePathList;
        this.iconProvider = new FileIconProvider();
    }

    @Override
    public Component getListCellRendererComponent(JList jList, Object object, int ba, boolean bl, boolean bl1) {
        super.getListCellRendererComponent(jList, object, ba, bl, bl1);
        String text = object.toString();
        String textValue = text;
        if (textValue.equals(".")) {
            textValue = this.fileList.workingDirPath;
        } else if (textValue.equals("..")) {
            textValue = this.fileList.workingDir.getParent();
            if (textValue == null) {
                textValue = this.fileList.workingDirPath;
            }
        }

        File file1 = new File(textValue);
        Icon icon1 = this.iconProvider.getFileIcon(file1);
        if (icon1 != null) {
            this.setIcon(icon1);
        }

        if (text.equals(".") || text.equals("..")) {
            this.setText(text);
        } else if (this.fileList.showFullPaths) {
            this.setText(file1.getAbsolutePath());
        } else {
            this.setText(file1.getName());
        }

        return this;
    }
}
