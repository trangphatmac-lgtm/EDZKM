package com.zelix.klassmaster.ui.dialog;

import com.zelix.GuiResources;
import com.zelix.klassmaster.ui.component.RenameFilterModel;
import com.zelix.klassmaster.util.DialogCallback;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class FieldExcludeParameterDialog extends ExcludeParameterDialogBase {
    public PackageNameParamPanel packagePanel;
    public ExcludeParamTabPanel classPanel;
    public FieldSpecifierPanel fieldPanel;

    @Override
    public final void showHelp() {
        if (super.exclusionType == 1) {
            GuiResources.showHelpTopic("029");
        } else if (super.exclusionType == 2) {
            GuiResources.showHelpTopic("023");
        } else if (super.exclusionType == 3) {
            GuiResources.showHelpTopic("043");
        }
    }

    @Override
    public void buildTabs() {
        boolean bl3;
        int exclusionTypeValue;
        if (super.exclusionType == 1) {
            bl3 = true;
            exclusionTypeValue = super.exclusionType;
        } else {
            bl3 = false;
            exclusionTypeValue = super.exclusionType;
        }

        int exclusionTypeValue2 = exclusionTypeValue;
        boolean bl = bl3;
        RenameFilterModel renameFilterModel = super.filterModel;
        FieldExcludeParameterDialog fieldExcludeParameterDialog1 = this;
        int exclusionTypeValue3 = exclusionTypeValue2;
        boolean showHigherLevelOption = bl;
        RenameFilterModel renameFilterModel1 = renameFilterModel;
        FieldExcludeParameterDialog fieldExcludeParameterDialog2 = fieldExcludeParameterDialog1;
        this.packagePanel = new PackageNameParamPanel(fieldExcludeParameterDialog2, renameFilterModel1, showHigherLevelOption, exclusionTypeValue3);
        if (super.exclusionType != 3) {
            bl3 = true;
            exclusionTypeValue = super.exclusionType;
        } else {
            bl3 = false;
            exclusionTypeValue = super.exclusionType;
        }

        exclusionTypeValue2 = exclusionTypeValue;
        bl = bl3;
        renameFilterModel = super.filterModel;
        fieldExcludeParameterDialog1 = this;
        int exclusionTypeValue4 = exclusionTypeValue2;
        boolean showHigherLevelExclude = bl;
        RenameFilterModel renameFilterModel2 = renameFilterModel;
        FieldExcludeParameterDialog fieldExcludeParameterDialog3 = fieldExcludeParameterDialog1;
        this.classPanel = new ClassSpecifierPanel(fieldExcludeParameterDialog3, renameFilterModel2, showHigherLevelExclude, exclusionTypeValue4);
        this.fieldPanel = new FieldSpecifierPanel(this, super.filterModel, super.exclusionType);
        JTabbedPane jTabbedPane = super.tabbedPane;
        PackageNameParamPanel packageNameParamPanel1 = this.packagePanel;
        String tooltipText = GuiResources.getTooltipText("CONTAINING_PACKAGE_PART_OF_PARAM");
        PackageNameParamPanel packageNameParamPanel = packageNameParamPanel1;
        jTabbedPane.addTab("Containing package", (Icon) null, packageNameParamPanel, tooltipText);
        jTabbedPane = super.tabbedPane;
        ExcludeParamTabPanel excludeParamTabPanel1 = this.classPanel;
        String tooltipTextValue = GuiResources.getTooltipText("CONTAINING_CLASS_PART_OF_PARAM");
        ExcludeParamTabPanel excludeParamTabPanel = excludeParamTabPanel1;
        jTabbedPane.addTab("Containing class", (Icon) null, excludeParamTabPanel, tooltipTextValue);
        jTabbedPane = super.tabbedPane;
        FieldSpecifierPanel fieldSpecifierPanel1 = this.fieldPanel;
        String tooltipTextValue2 = GuiResources.getTooltipText("FIELD_PART_OF_PARAM");
        FieldSpecifierPanel fieldSpecifierPanel = fieldSpecifierPanel1;
        jTabbedPane.addTab("Field", (Icon) null, fieldSpecifierPanel, tooltipTextValue2);
        super.tabbedPane.setSelectedComponent(this.fieldPanel);
    }

    public FieldExcludeParameterDialog(JFrame jFrame, String string, RenameFilterModel renameFilterModel, int ba, DialogCallback dialogCallback1) {
        super(jFrame, string, renameFilterModel, ba, dialogCallback1);
    }
}
