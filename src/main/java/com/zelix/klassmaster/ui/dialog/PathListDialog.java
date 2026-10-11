package com.zelix.klassmaster.ui.dialog;

import com.zelix.GuiResources;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.ui.component.AllFilesFilter;
import com.zelix.klassmaster.ui.component.FilePathList;
import com.zelix.klassmaster.ui.component.FileSelector;
import com.zelix.klassmaster.ui.component.SwingUtils;
import com.zelix.klassmaster.ui.component.TextFileFilter;
import com.zelix.klassmaster.ui.component.ZkmFileFilter;
import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class PathListDialog extends OwnedFrameDialogBase implements HelperDialogMarker, ListSelectionListener, ActionListener, KeyListener {
    public JButton exportBtn;
    public JButton helpBtn;
    public boolean closedByButton;
    public JLabel messageLabel;
    public JButton addBtn;
    public JButton removeBtn;
    public FilePathList pathList;
    public JButton okBtn;
    public static final String[] LAYOUT_CONSTRAINTS = new String[]{
            "upperBtnWidth=max(browseBtn.defaultWidth, removeBtn.defaultWidth, upBtn.defaultWidth, downBtn.defaultWidth)",
            "msgLbl.top=2",
            "msgLbl.left=5",
            "msgLbl.right=container.right-5",
            "pathLst.left=5",
            "pathLst.right=container.right-upperBtnWidth-10",
            "pathLst.top=msgLbl.bottom+2",
            "pathLst.bottom=container.bottom-5",
            "browseBtn.left=pathLst.right+5",
            "browseBtn.right=container.right-5",
            "browseBtn.bottom=removeBtn.top-3",
            "browseBtn.width=upperBtnWidth",
            "removeBtn.left=pathLst.right+5",
            "removeBtn.right=container.right-5",
            "removeBtn.bottom=upBtn.top-10",
            "removeBtn.width=upperBtnWidth",
            "upBtn.right=container.right-5",
            "upBtn.top=pathLst.centerY+5",
            "upBtn.width=upperBtnWidth",
            "downBtn.right=container.right-5",
            "downBtn.width=upperBtnWidth",
            "downBtn.top=upBtn.bottom+3"
    };
    public HashMap pathMap;
    public JButton cancelBtn;
    public DefaultListModel pathListModel;
    public JButton moveDownBtn;
    public JButton moveUpBtn;
    public JButton importBtn;

    @Override
    public abstract void cancelDialog() throws ZkmException, IOException;

    public List readPaths(BufferedReader bufferedReader, List list1) throws IOException {
        ArrayList arrayList = new ArrayList();

        String string;
        while ((string = bufferedReader.readLine()) != null) {
            string = string.trim();
            if (string.length() > 0 && !string.startsWith("//")) {
                if (string.length() >= 3 && string.startsWith("\"") && string.endsWith("\"")) {
                    string = string.substring(1, string.length() - 1);
                }

                File file1 = new File(string);
                if (file1.exists()) {
                    arrayList.add(file1);
                } else {
                    list1.add(string);
                }
            }
        }

        return arrayList;
    }

    @Override
    public abstract void buildContents(Object object, Object object1, Object object2, Object object3, Object object4, Object object5);

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            Object source = actionEvent.getSource();
            if (source == this.removeBtn) {
                this.removeSelectedPaths();
            } else if (source == this.moveUpBtn) {
                this.moveSelectedUp();
            } else if (source == this.moveDownBtn) {
                this.moveSelectedDown();
            } else if (source == this.addBtn) {
                this.browseToAddPath();
            } else if (source == this.okBtn) {
                this.acceptPaths();
            } else if (source == this.cancelBtn) {
                this.cancelDialog();
            } else if (source == this.importBtn) {
                this.importPaths();
            } else if (source == this.exportBtn) {
                this.exportPaths();
            } else if (source == this.helpBtn) {
                this.showHelp();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        try {
            Object source = keyEvent.getSource();
            if (keyEvent.getKeyCode() == 10) {
                if (source == this.removeBtn) {
                    this.removeSelectedPaths();
                } else if (source == this.moveUpBtn) {
                    this.moveSelectedUp();
                } else if (source == this.moveDownBtn) {
                    this.moveSelectedDown();
                } else if (source == this.addBtn) {
                    this.browseToAddPath();
                } else if (source == this.okBtn) {
                    this.acceptPaths();
                } else if (source == this.cancelBtn) {
                    this.cancelDialog();
                } else if (source == this.importBtn) {
                    this.importPaths();
                } else if (source == this.exportBtn) {
                    this.exportPaths();
                } else if (source == this.helpBtn) {
                    this.showHelp();
                } else if (source == this.pathList) {
                }
            } else if (keyEvent.getKeyCode() == 127 && source == this.pathList) {
                this.removeSelectedPaths();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public void updateButtonStates() {
        int size = this.pathListModel.getSize();
        List list1 = this.pathList.getSelectedValuesList();
        int sizeValue = this.pathListModel.getSize();
        int sizeValue3 = 0;

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            Object object = list1.get(elementIndex);
            int sizeValue2 = this.pathListModel.indexOf(object);
            if (sizeValue2 < sizeValue) {
                sizeValue = sizeValue2;
            }

            if (sizeValue2 > sizeValue3) {
                sizeValue3 = sizeValue2;
            }
        }

        if (this.pathList.getSelectedIndex() == -1) {
            this.removeBtn.setEnabled(false);
            this.moveUpBtn.setEnabled(false);
            this.moveDownBtn.setEnabled(false);
        } else {
            this.removeBtn.setEnabled(true);
            this.moveUpBtn.setEnabled(sizeValue3 > list1.size() - 1);
            JButton jButton = this.moveDownBtn;
            if (sizeValue + list1.size() < size) {
                jButton.setEnabled(true);
            } else {
                jButton.setEnabled(false);
            }
        }

        if (this.moveUpBtn.hasFocus()) {
            if (!this.moveUpBtn.isEnabled()) {
                if (this.moveDownBtn.isEnabled()) {
                    SwingUtils.requestFocusOnEdt(this.moveDownBtn);
                } else {
                    SwingUtils.requestFocusOnEdt(this.addBtn);
                }
            }
        } else if (this.moveDownBtn.hasFocus() && !this.moveDownBtn.isEnabled()) {
            if (this.moveUpBtn.isEnabled()) {
                SwingUtils.requestFocusOnEdt(this.moveUpBtn);
            } else {
                SwingUtils.requestFocusOnEdt(this.addBtn);
            }
        }
    }

    public PathListDialog(JFrame jFrame, String string, List list1, String string1, String string2, String string3) {
        super(jFrame, string, list1, string1, string2, string3);
        this.showCenteredOnOwner();
        SwingUtils.requestFocusOnEdt(this.okBtn);
    }

    public final void moveSelectedUp() {
        List list1 = this.pathList.getSelectedValuesList();
        int[] integerArray = new int[list1.size()];
        int[] selectedIndices = new int[list1.size()];

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            Object object = list1.get(elementIndex);
            int integerArrayElement = this.pathListModel.indexOf(object);
            integerArray[elementIndex] = integerArrayElement;
        }

        Arrays.sort(integerArray);
        int be = 0;

        for (int i = 0; i < integerArray.length; i++) {
            int selectedIndice = integerArray[i];
            if (selectedIndice > 0 && selectedIndice > be) {
                File file1 = (File) this.pathListModel.getElementAt(selectedIndice - 1);
                this.pathListModel.setElementAt(this.pathListModel.getElementAt(selectedIndice), selectedIndice - 1);
                this.pathListModel.setElementAt(file1, selectedIndice);
                selectedIndices[i] = selectedIndice - 1;
                be = selectedIndice - 1;
            } else {
                selectedIndices[i] = selectedIndice;
                be = selectedIndice + 1;
            }
        }

        this.pathList.clearSelection();
        this.pathList.setSelectedIndices(selectedIndices);
        this.pathList.ensureIndexIsVisible(selectedIndices[0]);
        this.updateButtonStates();
    }

    public void createMainButtons(String string, String toolTipText, StringBuffer stringBuffer, Container container1) {
        this.okBtn = new JButton(string);
        this.okBtn.setToolTipText(toolTipText);
        container1.add(this.okBtn, "okBtn");
        this.cancelBtn = new JButton("Cancel");
        this.cancelBtn.setToolTipText(GuiResources.getTooltipText("CANCEL_DIALOG"));
        container1.add(this.cancelBtn, "cancelBtn");
        this.importBtn = new JButton("Import");
        this.importBtn.setToolTipText(GuiResources.getTooltipText("IMPORT_DIALOG"));
        container1.add(this.importBtn, "importBtn");
        this.exportBtn = new JButton("Export");
        this.exportBtn.setToolTipText(GuiResources.getTooltipText("EXPORT_DIALOG"));
        container1.add(this.exportBtn, "exportBtn");
        this.helpBtn = new JButton("Help");
        this.helpBtn.setToolTipText(GuiResources.getTooltipText("SCREEN_HELP"));
        container1.add(this.helpBtn, "helpBtn");
        this.okBtn.addKeyListener(this);
        this.cancelBtn.addKeyListener(this);
        this.helpBtn.addKeyListener(this);
        this.okBtn.addActionListener(this);
        this.cancelBtn.addActionListener(this);
        this.importBtn.addActionListener(this);
        this.exportBtn.addActionListener(this);
        this.helpBtn.addActionListener(this);
        stringBuffer.append(
                "btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, importBtn.defaultHeight, exportBtn.defaultHeight, helpBtn.defaultHeight);btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, importBtn.defaultWidth, exportBtn.defaultWidth, helpBtn.defaultWidth);okBtn.centerX=container.width*10/100;okBtn.bottom=bottom=container.bottom-10;okBtn.height=btnHeight;okBtn.width=btnWidth;cancelBtn.centerX=container.width*30/100;cancelBtn.top=okBtn.top;cancelBtn.height=btnHeight;cancelBtn.width=btnWidth;importBtn.centerX=container.width*50/100;importBtn.top=okBtn.top;importBtn.height=btnHeight;importBtn.width=btnWidth;exportBtn.centerX=container.width*70/100;exportBtn.top=okBtn.top;exportBtn.height=btnHeight;exportBtn.width=btnWidth;helpBtn.centerX=container.width*90/100;helpBtn.top=okBtn.top;helpBtn.height=btnHeight;helpBtn.width=btnWidth"
        );
    }

    @Override
    public void closeFrame() throws ZkmException, IOException {
        super.closeFrame();
    }

    public final void importPaths() {
        final FileSelector fileSelector = new FileSelector(ZkmFileUtils.USER_DIR_FILE, false, 1, new ZkmFileFilter[]{new TextFileFilter(), new AllFilesFilter()}, true);
        if (fileSelector.showOpenDialog(this, "Select import file") == 1) {
            final File selectedFile = fileSelector.getSelectedFile();
            final ArrayList list = new ArrayList();
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader(selectedFile));
                this.setPaths(this.readPaths(bufferedReader, list), list);
                try {
                    bufferedReader.close();
                } catch (final IOException ioException) {
                }
            } catch (final IOException ioExceptionValue) {
                final MessageBoxDialog messageBoxDialog = new MessageBoxDialog(this, "File Error", "Couldn't open or read \"" + selectedFile.getAbsolutePath() + "\" : " + ioExceptionValue.getMessage());
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException ioExceptionValue2) {
                    }
                }
            }
        }
    }

    public final void moveSelectedDown() {
        List list1 = this.pathList.getSelectedValuesList();
        int[] integerArray = new int[list1.size()];
        int[] selectedIndices = new int[list1.size()];

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            Object object = list1.get(elementIndex);
            int integerArrayElement = this.pathListModel.indexOf(object);
            integerArray[elementIndex] = integerArrayElement;
        }

        Arrays.sort(integerArray);
        int bf = this.pathListModel.getSize() - 1;
        int bg = bf;

        for (int i = integerArray.length - 1; i >= 0; i += -1) {
            int selectedIndice = integerArray[i];
            if (selectedIndice < bf && selectedIndice < bg) {
                File file1 = (File) this.pathListModel.getElementAt(selectedIndice + 1);
                this.pathListModel.setElementAt(this.pathListModel.getElementAt(selectedIndice), selectedIndice + 1);
                this.pathListModel.setElementAt(file1, selectedIndice);
                selectedIndices[i] = selectedIndice + 1;
                bg = selectedIndice + 1;
            } else {
                selectedIndices[i] = selectedIndice;
                bg = selectedIndice - 1;
            }
        }

        this.pathList.clearSelection();
        this.pathList.setSelectedIndices(selectedIndices);
        this.pathList.ensureIndexIsVisible(selectedIndices[selectedIndices.length - 1]);
        this.updateButtonStates();
    }

    public abstract void browseToAddPath();

    public boolean addPath(String key) {
        File file1 = new File(key);
        boolean bl = this.pathMap.put(key, file1) == null;
        if (bl) {
            this.pathListModel.addElement(file1);
            this.pathList.setSelectedIndex(this.pathListModel.getSize() - 1);
            this.pathList.ensureIndexIsVisible(this.pathListModel.getSize() - 1);
        } else {
            int selectedIndex = this.pathListModel.indexOf(file1);
            this.pathList.setSelectedIndex(selectedIndex);
            this.pathList.ensureIndexIsVisible(selectedIndex);
        }

        return bl;
    }

    public final void exportPaths() {
        final FileSelector fileSelector = new FileSelector(ZkmFileUtils.USER_DIR_FILE, false, 1, new ZkmFileFilter[]{new TextFileFilter(), new AllFilesFilter()}, true);
        if (fileSelector.showOpenDialog(this, "Select export destination") == 1) {
            final File selectedFile = fileSelector.getSelectedFile();
            BufferedWriter bufferedWriter = null;
            try {
                final String pathsText = this.getPathsText();
                bufferedWriter = new BufferedWriter(new FileWriter(selectedFile));
                bufferedWriter.write(pathsText, 0, pathsText.length());
                try {
                    bufferedWriter.close();
                } catch (final IOException ioException) {
                }
            } catch (final IOException ioExceptionValue) {
                final MessageBoxDialog messageBoxDialog = new MessageBoxDialog(this, "File Error", "Couldn't save \"" + selectedFile.getAbsolutePath() + "\" : " + ioExceptionValue.getMessage());
            } finally {
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (final IOException ioExceptionValue2) {
                    }
                }
            }
        }
    }

    @Override
    public final void keyTyped(KeyEvent keyEvent) {
    }

    public final void removeSelectedPaths() {
        List list1 = this.pathList.getSelectedValuesList();
        int size = this.pathListModel.getSize();

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            Object object = list1.get(elementIndex);
            int sizeValue = this.pathListModel.indexOf(object);
            HashMap hashMap;
            if (sizeValue < size) {
                size = sizeValue;
                hashMap = this.pathMap;
            } else {
                hashMap = this.pathMap;
            }

            hashMap.remove(((File) object).getAbsolutePath());
            this.pathListModel.removeElement(object);
        }

        int sizeValue2 = this.pathListModel.getSize();
        if (sizeValue2 > size) {
            this.pathList.setSelectedIndex(size);
        } else if (sizeValue2 > 0) {
            this.pathList.setSelectedIndex(sizeValue2 - 1);
        } else {
            SwingUtils.requestFocusOnEdt(this.addBtn);
        }

        this.updateButtonStates();
    }

    public abstract void showHelp();

    public abstract void acceptPaths() throws ZkmException, IOException;

    public String getPathsText() {
        int size = this.pathListModel.getSize();
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < size; i++) {
            File file1 = (File) this.pathListModel.getElementAt(i);
            if (!file1.isAbsolute()) {
                stringBuilder.append(file1.getPath().trim());
            } else {
                stringBuilder.append(file1.getAbsolutePath().trim());
            }

            if (i < size - 1) {
                stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
            }
        }

        return stringBuilder.toString();
    }

    public void setPaths(List list1, List list2) {
        this.pathListModel.clear();
        this.pathMap.clear();
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            File file1 = (File) iterator.next();
            if (this.pathMap.put(file1.getAbsolutePath(), file1) == null) {
                this.pathListModel.addElement(file1);
            }
        }

        if (this.pathListModel.getSize() > 0) {
            this.pathList.setSelectedIndex(this.pathListModel.getSize() - 1);
            this.pathList.ensureIndexIsVisible(this.pathListModel.getSize() - 1);
        }

        if (!list2.isEmpty()) {
            int list2Size = list2.size();
            int bc = Math.min(20, list2Size);
            int bb = bc;
            SwingUtils.createMessageDialog(
                    this, "FILE ERROR: Files not found", "These strings did not match an existing file.", ZkmUtils.toQuotedListString(list2.subList(0, bb))
            );
        }
    }

    @Override
    public final void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (!listSelectionEvent.getValueIsAdjusting()) {
            this.updateButtonStates();
        }
    }

    @Override
    public final void keyReleased(KeyEvent keyEvent) {
    }
}
