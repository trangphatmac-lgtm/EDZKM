package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class LiteralParamStatementNode extends SummarizingStatementNode {
    public Map namedValues = ZkmUtils.createHashMap();
    public List literalValues = new ArrayList();

    public LiteralParamStatementNode() {
        super(57);
    }

    @Override
    public final void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int ba = this.jjtGetNumChildren();
        int messageCount = scriptEnvironment1.getMessageCount();
        int warningCount = scriptEnvironment1.getWarningCount();
        int errorCount = scriptEnvironment1.getErrorCount();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            ZkmScriptNode zkmScriptNode = this.jjtGetChild(childrenElementIndex);
            if (zkmScriptNode instanceof ZkmScriptASTStringLiteral) {
                ZkmScriptASTStringLiteral zkmScriptASTStringLiteral = (ZkmScriptASTStringLiteral) zkmScriptNode;
                String value = zkmScriptASTStringLiteral.getValue();
                if (this.literalValues.contains(value)) {
                    scriptEnvironment1.logWarning(
                            "\""
                                    + value
                                    + "\" appears more than once in \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getStatementLine()
                                    + ". First occurrence will be used."
                    );
                } else {
                    this.literalValues.add(value);
                }
            } else if (zkmScriptNode instanceof SingleValueParameterNode) {
                SingleValueParameterNode singleValueParameterNode1 = (SingleValueParameterNode) zkmScriptNode;
                ZkmScriptNode zkmScriptNode1 = singleValueParameterNode1.jjtGetChild(0);
                zkmScriptNode1.execute(this, scriptEnvironment1);
                SingleValueParameterNode singleValueParameterNode = (SingleValueParameterNode) zkmScriptNode1;
                String valueValue = ((java.lang.String) (this.namedValues.put(singleValueParameterNode.getParameterName(), singleValueParameterNode.getValue(0))));
                if (valueValue != null) {
                    scriptEnvironment1.logWarning(
                            "\""
                                    + singleValueParameterNode.getParameterName()
                                    + "\" appears more than once in \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getStatementLine()
                                    + ".  First occurrence will be used."
                    );
                    this.namedValues.put(singleValueParameterNode.getParameterName(), valueValue);
                }
            } else {
                scriptEnvironment1.logFatalError(
                        this.getClass().getName() + " had " + zkmScriptNode.getClass().getName() + " as child " + childrenElementIndex + " at line " + this.getStatementLine() + "."
                );
            }
        }

        Integer integer1 = errorCount;
        Integer integer = warningCount;
        this.executeStatement(scriptEnvironment1, messageCount, integer, integer1);
    }
}
