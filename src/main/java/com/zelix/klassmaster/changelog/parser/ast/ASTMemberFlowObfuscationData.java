package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;
import java.util.List;

public class ASTMemberFlowObfuscationData extends FlowObfuscationDataNode {
    @Override
    public void applyToChangeLog(Object object) throws ZkmException, IOException {
        if (super.className != null) {
            List list1 = super.values;
            String classNameValue = super.className;
            super.changeLog.addMemberFlowObfuscationData(classNameValue, list1);
        }
    }

    public ASTMemberFlowObfuscationData() {
        super(43);
    }
}
