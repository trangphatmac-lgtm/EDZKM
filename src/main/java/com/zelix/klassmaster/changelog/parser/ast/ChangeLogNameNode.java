package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.ChangeLogNameHolder;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public abstract class ChangeLogNameNode extends ChangeLogSimpleNode implements ChangeLogNameHolder {
    public String name;

    @Override
    public void setParsedName(Object parsedName) {
        this.name = (String) parsedName;
    }

    public abstract void passNameToParent(Object object);

    public ChangeLogNameNode(int ba) {
        super(ba);
    }

    @Override
    public final void interpret(ChangeLogNode changeLogNode, int bd, int ba, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        String[] opaqueStrings = ChangeLogSimpleNode.getOpaqueStrings();
        int bb = this.jjtGetNumChildren();
        String[] opaqueStringsValue = opaqueStrings;
        int childrenElementIndex = 0;

        while (true) {
            if (childrenElementIndex < bb) {
                if (ba >= 0) {
                    this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
                    childrenElementIndex++;
                    continue;
                }

                this.passNameToParent(opaqueStringsValue[1]);
                break;
            }

            this.passNameToParent(changeLogNode);
            break;
        }
    }
}
