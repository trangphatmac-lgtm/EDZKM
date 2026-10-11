package com.zelix.klassmaster.ui.dialog;

import com.zelix.GuiResources;
import com.zelix.klassmaster.config.PersistedStringList;
import com.zelix.klassmaster.config.WizardAppSettings;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ast.ASTRenameFilterParameter;
import com.zelix.klassmaster.script.parser.ast.ParameterListStatement;
import com.zelix.klassmaster.ui.ZkmMainWindow;
import com.zelix.klassmaster.ui.component.BorderedPanel;
import com.zelix.klassmaster.ui.component.ClassListEntry;
import com.zelix.klassmaster.ui.component.ConstraintLayout;
import com.zelix.klassmaster.ui.component.DisableableListItem;
import com.zelix.klassmaster.ui.component.ListItemWrapper;
import com.zelix.klassmaster.ui.component.RenameFilterModel;
import com.zelix.klassmaster.util.DialogCallback;
import com.zelix.klassmaster.util.OrderedIndexedMap;
import com.zelix.klassmaster.util.ParamEditorMarker;
import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public abstract class ExclusionWizardDialogBase extends ParameterListDialog implements ParamEditorMarker, ItemListener {
    public static String mainMethodSignature = "public static main(java.lang.String[])";
    public OrderedIndexedMap frameworkParams;
    public OrderedIndexedMap libraryParams;
    public JComboBox mainClassComboBox;
    public JComboBox applicationTypeComboBox;
    public OrderedIndexedMap defaultParams;
    public OrderedIndexedMap midletParams;
    public boolean updatingAppType;
    public DefaultComboBoxModel mainClassComboModel;
    public boolean updatingMainClass;
    public DefaultComboBoxModel appTypeComboModel;
    public WizardAppSettings wizardSettings;
    public List mainClassCandidates;

    public abstract String getMainClassParameter(ClassListEntry classListEntry);

    public void detectApplicationType() {
        int size = super.paramListModel.getSize();
        boolean bl;
        if (this.frameworkParams.size() <= size) {
            bl = true;
            int elementIndex = 0;
            int bg = 0;

            for (OrderedIndexedMap orderedIndexedMap2 = this.frameworkParams; bg < orderedIndexedMap2.size(); orderedIndexedMap2 = this.frameworkParams) {
                if (!super.paramIndexMap.containsKey(this.frameworkParams.getKeyAt(elementIndex))) {
                    bl = false;
                    break;
                }

                bg = ++elementIndex;
            }
        } else {
            bl = false;
        }

        if (bl) {
            this.updatingAppType = true;
            this.applicationTypeComboBox.setSelectedIndex(1);
            this.wizardSettings.setApplicationType(1);
            this.updatingAppType = false;
        } else {
            if (this.libraryParams.size() <= size) {
                bl = true;
                int elementIndexValue = 0;
                int be = 0;

                for (OrderedIndexedMap orderedIndexedMap = this.libraryParams; be < orderedIndexedMap.size(); orderedIndexedMap = this.libraryParams) {
                    if (!super.paramIndexMap.containsKey(this.libraryParams.getKeyAt(elementIndexValue))) {
                        bl = false;
                        break;
                    }

                    be = ++elementIndexValue;
                }
            } else {
                bl = false;
            }

            if (bl) {
                this.updatingAppType = true;
                this.applicationTypeComboBox.setSelectedIndex(0);
                this.wizardSettings.setApplicationType(0);
                this.updatingAppType = false;
            } else {
                if (this.midletParams.size() <= size) {
                    bl = true;
                    int elementIndexValue2 = 0;
                    int bf = 0;

                    for (OrderedIndexedMap orderedIndexedMap1 = this.midletParams; bf < orderedIndexedMap1.size(); orderedIndexedMap1 = this.midletParams) {
                        if (!super.paramIndexMap.containsKey(this.midletParams.getKeyAt(elementIndexValue2))) {
                            bl = false;
                            break;
                        }

                        bf = ++elementIndexValue2;
                    }
                } else {
                    bl = false;
                }

                if (bl) {
                    this.updatingAppType = true;
                    this.applicationTypeComboBox.setSelectedIndex(2);
                    this.wizardSettings.setApplicationType(2);
                    this.updatingAppType = false;
                } else {
                    this.updatingAppType = true;
                    this.applicationTypeComboBox.setSelectedIndex(3);
                    this.wizardSettings.setApplicationType(3);
                    this.updatingAppType = false;
                }
            }
        }
    }

    public ExclusionWizardDialogBase(
            ZkmMainWindow zkmMainWindow,
            List list1,
            WizardAppSettings wizardAppSettings,
            ScriptEnvironment scriptEnvironment1,
            DialogCallback dialogCallback1,
            int paramKindArgument
    ) {
        super(zkmMainWindow, scriptEnvironment1, dialogCallback1, paramKindArgument);
        this.wizardSettings = wizardAppSettings;
        this.mainClassCandidates = list1;
    }

    public void removeParameters(OrderedIndexedMap orderedIndexedMap) {
        if (!super.paramList.isSelectionEmpty()) {
            super.lastSelectedParameter = ((DisableableListItem) super.paramList.getSelectedValue()).toString();
        }

        int elementIndex = 0;
        int elementIndexValue = elementIndex;

        for (int orderedIndexedMapSize = orderedIndexedMap.size(); elementIndexValue < orderedIndexedMapSize; orderedIndexedMapSize = orderedIndexedMap.size()) {
            String keyAt = (String) orderedIndexedMap.getKeyAt(elementIndex);
            if (super.paramIndexMap.containsKey(keyAt)) {
                ListItemWrapper listItemWrapper = (ListItemWrapper) super.paramIndexMap.remove(keyAt);
                super.removingItem = true;
                super.paramListModel.removeElement(listItemWrapper);
                super.removingItem = false;
            }

            elementIndexValue = ++elementIndex;
        }

        if (super.paramList.getSelectedIndex() == -1) {
            super.mainModifyBtn.setEnabled(false);
            super.mainDeleteBtn.setEnabled(false);
            super.explanationArea.setText("");
        }
    }

    public abstract String[] getExtraPanelLayoutSpec();

    @Override
    public boolean canAccept() throws ZkmException, IOException {
        return true;
    }

    public int findMainClassIndex(Object object) {
        int size = this.mainClassComboModel.getSize();

        for (int i = 1; i < size; i++) {
            if (((ClassListEntry) this.mainClassComboModel.getElementAt(i)).getClassFile().getDottedClassName().equals(object)) {
                return i;
            }
        }

        return -1;
    }

    public final void syncMainClassSelection() {
        if (!this.updatingMainClass && this.mainClassComboBox != null) {
            switch (this.mainClassComboBox.getSelectedIndex()) {
                default:
                    if (!super.paramIndexMap.containsKey(this.getMainClassParameter((ClassListEntry) this.mainClassComboBox.getSelectedItem()))) {
                        this.mainClassComboBox.setSelectedIndex(0);
                    }
                case -1:
                case 0:
            }
        }
    }

    @Override
    public PersistedStringList getParameterStore() {
        return this.wizardSettings;
    }

    public final void buildMainClassSelector(Container container1, String string, String toolTipText) throws ZkmException, IOException {
        JLabel jLabel = new JLabel(string);
        container1.add(jLabel, "excludeMainClassLbl");
        this.mainClassComboModel = new DefaultComboBoxModel();
        this.mainClassComboBox = new JComboBox(this.mainClassComboModel);
        container1.add(this.mainClassComboBox, "mainClassComboBox");
        this.mainClassComboBox.addItem("<None>");
        if (this.mainClassCandidates != null && this.mainClassCandidates.size() != 0) {
            int elementIndex = 0;
            int elementIndexValue = elementIndex;

            for (List list1 = this.mainClassCandidates; elementIndexValue < list1.size(); list1 = this.mainClassCandidates) {
                this.mainClassComboBox.addItem(this.mainClassCandidates.get(elementIndex));
                elementIndexValue = ++elementIndex;
            }
        } else {
            this.mainClassComboBox.setEnabled(false);
        }

        if (this.wizardSettings.hasMainClass() && this.wizardSettings.isSaved()) {
            String mainClassName = this.wizardSettings.getMainClassName();
            int selectedIndex = this.findMainClassIndex(mainClassName);
            if (selectedIndex > -1) {
                this.mainClassComboBox.setSelectedIndex(selectedIndex);
            } else {
                this.removeParameter(this.buildMainClassParameter(mainClassName, this.wizardSettings.isMainClassApplication()));
            }
        }

        this.mainClassComboBox.addItemListener(this);
        this.mainClassComboBox.setToolTipText(toolTipText);
    }

    @Override
    public void onParametersChanged() {
        this.detectApplicationType();
        this.syncMainClassSelection();
    }

    public void loadPresetParameters() throws ZkmException, IOException {
        this.defaultParams = new OrderedIndexedMap();
        ParameterListStatement parameterListStatement;
        if (super.paramKind == 1) {
            parameterListStatement = NameExclusionSet.loadDefaultExcludes(super.scriptEnvironment);
        } else {
            parameterListStatement = TrimProcessor.loadDefaultTrimExclude(super.scriptEnvironment);
        }

        if (parameterListStatement != null) {
            Enumeration enumeration = parameterListStatement.getRenameFilterParameters();

            while (enumeration.hasMoreElements()) {
                ASTRenameFilterParameter aSTRenameFilterParameter = (ASTRenameFilterParameter) enumeration.nextElement();
                RenameFilterModel renameFilterModel = new RenameFilterModel(aSTRenameFilterParameter);
                this.defaultParams.put(aSTRenameFilterParameter.toString(), renameFilterModel);
            }
        }

        this.libraryParams = new OrderedIndexedMap();
        if (super.paramKind == 1) {
            parameterListStatement = NameExclusionSet.parsePublicExcludes(super.scriptEnvironment);
        } else {
            parameterListStatement = TrimProcessor.parsePublicApiTrimExclude(super.scriptEnvironment);
        }

        if (parameterListStatement != null) {
            Enumeration enumeration1 = parameterListStatement.getRenameFilterParameters();

            while (enumeration1.hasMoreElements()) {
                ASTRenameFilterParameter aSTRenameFilterParameter1 = (ASTRenameFilterParameter) enumeration1.nextElement();
                RenameFilterModel renameFilterModel1 = new RenameFilterModel(aSTRenameFilterParameter1);
                this.libraryParams.put(aSTRenameFilterParameter1.toString(), renameFilterModel1);
            }
        }

        this.frameworkParams = new OrderedIndexedMap();
        if (super.paramKind == 1) {
            parameterListStatement = NameExclusionSet.parsePublicProtectedExcludes(super.scriptEnvironment);
        } else {
            parameterListStatement = TrimProcessor.parsePublicProtectedApiTrimExclude(super.scriptEnvironment);
        }

        if (parameterListStatement != null) {
            Enumeration enumeration2 = parameterListStatement.getRenameFilterParameters();

            while (enumeration2.hasMoreElements()) {
                ASTRenameFilterParameter aSTRenameFilterParameter2 = (ASTRenameFilterParameter) enumeration2.nextElement();
                RenameFilterModel renameFilterModel2 = new RenameFilterModel(aSTRenameFilterParameter2);
                this.frameworkParams.put(aSTRenameFilterParameter2.toString(), renameFilterModel2);
            }
        }

        this.midletParams = new OrderedIndexedMap();
        if (super.paramKind == 1) {
            parameterListStatement = NameExclusionSet.parseMidletExcludes(super.scriptEnvironment);
        } else {
            parameterListStatement = TrimProcessor.parseMidletTrimExclude(super.scriptEnvironment);
        }

        if (parameterListStatement != null) {
            Enumeration enumeration3 = parameterListStatement.getRenameFilterParameters();

            while (enumeration3.hasMoreElements()) {
                ASTRenameFilterParameter aSTRenameFilterParameter3 = (ASTRenameFilterParameter) enumeration3.nextElement();
                RenameFilterModel renameFilterModel3 = new RenameFilterModel(aSTRenameFilterParameter3);
                this.midletParams.put(aSTRenameFilterParameter3.toString(), renameFilterModel3);
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        try {
            if (itemEvent.getSource() == this.applicationTypeComboBox) {
                if (this.updatingAppType) {
                    return;
                }

                int indexOf = this.appTypeComboModel.getIndexOf(itemEvent.getItem());
                if (itemEvent.getStateChange() == 1) {
                    this.wizardSettings.setApplicationType(indexOf);
                }

                switch (indexOf) {
                    case 0:
                        if (itemEvent.getStateChange() == 1) {
                            this.addParameters(this.libraryParams);
                            this.detectApplicationType();
                        } else {
                            this.removeParameters(this.libraryParams);
                        }
                        break;
                    case 1:
                        if (itemEvent.getStateChange() == 1) {
                            this.addParameters(this.frameworkParams);
                            this.detectApplicationType();
                        } else {
                            this.removeParameters(this.frameworkParams);
                        }
                        break;
                    case 2:
                        if (itemEvent.getStateChange() == 1) {
                            this.addParameters(this.midletParams);
                            this.detectApplicationType();
                        } else {
                            this.removeParameters(this.midletParams);
                        }
                    case 3:
                }
            } else if (itemEvent.getSource() == this.mainClassComboBox) {
                if (this.updatingMainClass) {
                    return;
                }

                int indexOfValue = this.mainClassComboModel.getIndexOf(itemEvent.getItem());
                switch (indexOfValue) {
                    case -1:
                    case 0:
                        break;
                    default:
                        String mainClassParameter = this.getMainClassParameter((ClassListEntry) this.mainClassComboModel.getElementAt(indexOfValue));
                        if (itemEvent.getStateChange() == 1) {
                            this.addParameter(mainClassParameter);
                        } else {
                            this.updatingMainClass = true;
                            this.removeParameter(mainClassParameter);
                            this.updatingMainClass = false;
                        }
                }
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public abstract void buildExtraComponents(Container container1) throws ZkmException, IOException;

    @Override
    public void saveParameters() {
        ArrayList arrayList = new ArrayList();
        Enumeration enumeration = super.paramListModel.elements();

        while (enumeration.hasMoreElements()) {
            DisableableListItem disableableListItem = (DisableableListItem) enumeration.nextElement();
            if (!disableableListItem.isDisabled()) {
                arrayList.add(disableableListItem.getItem());
            }
        }

        this.wizardSettings.setEntries(arrayList);
        switch (this.mainClassComboBox.getSelectedIndex()) {
            case -1:
            case 0:
                Object object = null;
                this.wizardSettings.setMainClass(false, (String) object, false);
                break;
            default:
                ClassListEntry classListEntry = (ClassListEntry) this.mainClassComboBox.getSelectedItem();
                WizardAppSettings wizardAppSettings = this.wizardSettings;
                boolean application = classListEntry.isApplication();
                String className = classListEntry.getClassName();
                wizardAppSettings.setMainClass(true, className, application);
        }
    }

    public void addParameters(OrderedIndexedMap orderedIndexedMap) {
        int elementIndex = 0;
        int bb = 0;

        for (int orderedIndexedMapSize = orderedIndexedMap.size(); bb < orderedIndexedMapSize; orderedIndexedMapSize = orderedIndexedMap.size()) {
            String keyAt = (String) orderedIndexedMap.getKeyAt(elementIndex);
            if (!super.paramIndexMap.containsKey(keyAt)) {
                try {
                    RenameFilterModel renameFilterModel = this.parseParameter(keyAt);
                    if (renameFilterModel.toScriptText().equals(keyAt)) {
                        DisableableListItem disableableListItem = new DisableableListItem(keyAt, false);
                        super.paramIndexMap.put(keyAt, disableableListItem);
                        super.paramListModel.addElement(disableableListItem);
                    }
                } catch (AssertionFailedException assertionFailedException) {
                    throw assertionFailedException;
                } catch (Throwable throwable) {
                }
            }

            bb = ++elementIndex;
        }

        if (super.paramList.getSelectedIndex() == -1 && super.lastSelectedParameter != null && super.paramIndexMap.containsKey(super.lastSelectedParameter)) {
            super.paramList.setSelectedIndex(super.paramIndexMap.indexOfKey(super.lastSelectedParameter));
        }

        super.lastSelectedParameter = null;
        if (super.paramListModel.size() > 0) {
            super.paramList.ensureIndexIsVisible(super.paramListModel.size() - 1);
        }
    }

    @Override
    public void initToolTips() {
        super.mainAddBtn.setToolTipText(GuiResources.getTooltipText("ADD_EXCLUSION_PARAM"));
        super.mainModifyBtn.setToolTipText(GuiResources.getTooltipText("MODIFY_EXCLUSION_PARAM"));
        super.mainDeleteBtn.setToolTipText(GuiResources.getTooltipText("DELETE_EXCLUSION_PARAM"));
        super.paramList.setToolTipText(GuiResources.getTooltipText("CURRENT_EXCLUSION_PARAMS"));
        super.explanationArea.setToolTipText(GuiResources.getTooltipText("EXPLAIN_SELECTED_EXCLUSION_PARAM"));
    }

    public abstract String buildMainClassParameter(String string, boolean bl);

    @Override
    public final void loadParameters(Object object) throws ZkmException, IOException {
        Container container1 = (Container) object;
        this.loadPresetParameters();
        BorderedPanel borderedPanel = new BorderedPanel();
        ConstraintLayout constraintLayout1 = new ConstraintLayout(borderedPanel);
        borderedPanel.setLayout(constraintLayout1);
        borderedPanel.add(new JLabel("Application type:"), "applicationTypeLbl");
        this.appTypeComboModel = new DefaultComboBoxModel();
        this.applicationTypeComboBox = new JComboBox(this.appTypeComboModel);
        this.applicationTypeComboBox.addItem("Non-extensible class library");
        this.applicationTypeComboBox.addItem("Extensible framework");
        this.applicationTypeComboBox.addItem("J2ME MIDlet");
        this.applicationTypeComboBox.addItem("Self contained application or applet");
        borderedPanel.add(this.applicationTypeComboBox, "applicationTypeComboBox");
        this.applicationTypeComboBox.setToolTipText(GuiResources.getTooltipText("EXCLUSION_APPLICATION_TYPE"));
        Enumeration enumeration = this.defaultParams.keyEnumeration();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();

            try {
                RenameFilterModel renameFilterModel = this.parseParameter(key);
                if (renameFilterModel.toScriptText().equals(key)) {
                    this.addParameterItem(key, true);
                }
            } catch (AssertionFailedException assertionFailedException1) {
                throw assertionFailedException1;
            } catch (Throwable throwable1) {
            }
        }

        if (this.wizardSettings.isSaved()) {
            enumeration = this.wizardSettings.enumerateEntries();

            while (enumeration.hasMoreElements()) {
                String keyValue = (String) enumeration.nextElement();

                try {
                    RenameFilterModel renameFilterModel1 = this.parseParameter(keyValue);
                    if (renameFilterModel1.toScriptText().equals(keyValue)) {
                        this.addParameter(keyValue);
                    }
                } catch (AssertionFailedException assertionFailedException) {
                    throw assertionFailedException;
                } catch (Throwable throwable) {
                }
            }
        }

        this.detectApplicationType();
        this.applicationTypeComboBox.addItemListener(this);
        this.buildExtraComponents(borderedPanel);
        constraintLayout1.setConstraints(this.getExtraPanelLayoutSpec());
        container1.add(borderedPanel, "extraPnl");
    }
}
