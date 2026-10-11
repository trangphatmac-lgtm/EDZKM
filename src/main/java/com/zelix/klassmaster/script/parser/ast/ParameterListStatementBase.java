package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ParameterListStatementBase extends SummarizingStatementNode {
    public List parameters = new ArrayList();

    public abstract void printStartMessage(Object object);

    public void processParameters(int ba, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            ZkmScriptNode zkmScriptNode = this.jjtGetChild(childrenElementIndex);
            this.parameters.add(zkmScriptNode);
            zkmScriptNode.execute(this, scriptEnvironment1);
        }
    }

    public ParameterListStatementBase(int nodeTypeId) {
        super(nodeTypeId);
    }

    @Override
    public void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int messageCount = scriptEnvironment1.getMessageCount();
        int warningCount = scriptEnvironment1.getWarningCount();
        int errorCount = scriptEnvironment1.getErrorCount();
        this.printStartMessage(scriptEnvironment1);
        int bd = this.jjtGetNumChildren();
        this.processParameters(bd, scriptEnvironment1);
        Integer integer1 = errorCount;
        Integer integer = warningCount;
        this.executeStatement(scriptEnvironment1, messageCount, integer, integer1);
    }
}
