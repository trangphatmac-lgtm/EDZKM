package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.ChangeLogNameHolder;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;
import java.util.Vector;

public class ChangeLogASTNameList extends ChangeLogSimpleNode implements ChangeLogNameHolder {
    public Vector nameParts = new Vector();

    public String buildDottedName() {
        StringBuffer stringBuffer = new StringBuffer();
        int namePartsSize = this.nameParts.size();

        for (int elementIndex = 0; elementIndex < namePartsSize; elementIndex++) {
            stringBuffer.append((String) this.nameParts.elementAt(elementIndex));
            if (elementIndex < namePartsSize - 1) {
                stringBuffer.append(".");
            }
        }

        return stringBuffer.toString();
    }

    public ChangeLogASTNameList() {
        super(22);
    }

    @Override
    public void setParsedName(Object parsedName) {
        this.nameParts.addElement(parsedName);
    }

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int ba, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int bb = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < bb; childrenElementIndex++) {
            ChangeLogNode changeLogNode1 = this;
            if (ba > 0) {
                changeLogNode1 = this.jjtGetChild(childrenElementIndex);
            }

            changeLogNode1.interpret(this, 30872, 34067, 41973, abstractChangeLog);
        }

        ChangeLogNameHolder changeLogNameHolder = (ChangeLogNameHolder) changeLogNode;
        changeLogNameHolder.setParsedName(this.buildDottedName());
    }
}
