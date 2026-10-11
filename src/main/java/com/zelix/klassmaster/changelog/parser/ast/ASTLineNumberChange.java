package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;
import java.util.ArrayList;

public class ASTLineNumberChange extends ChangeLogSimpleNode {
    public ArrayList oldLineNumbers;
    public Integer newLineNumber;
    public AbstractChangeLog changeLog;

    @Override
    public void interpret(ChangeLogNode changeLogNode, int ba, int be, int bf, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        this.changeLog = abstractChangeLog;
        int bb = this.jjtGetNumChildren();
        this.oldLineNumbers = new ArrayList(bb - 1);
        int childrenElementIndex = 0;

        while (childrenElementIndex < bb) {
            this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
            if (ba >= 0) {
                childrenElementIndex++;
            }
        }

        if (abstractChangeLog.acceptsLineNumberChanges()) {
            ASTClassChange aSTClassChange = (ASTClassChange) changeLogNode;

            for (int elementIndex = 0; elementIndex < this.oldLineNumbers.size(); elementIndex++) {
                aSTClassChange.addLineNumberChange((Integer) this.oldLineNumbers.get(elementIndex), this.newLineNumber);
            }
        }
    }

    public void addOldLineNumber(String string) {
        this.oldLineNumbers.add(this.changeLog.getCachedInteger(Integer.parseInt(string)));
    }

    public void setNewLineNumber(String newLineNumberArgument) {
        this.newLineNumber = this.changeLog.getCachedInteger(Integer.parseInt(newLineNumberArgument));
    }

    public ASTLineNumberChange() {
        super(35);
    }
}
