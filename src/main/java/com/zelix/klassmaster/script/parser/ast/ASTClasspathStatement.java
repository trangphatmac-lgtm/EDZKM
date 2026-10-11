package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.classfile.hierarchy.ZkmClasspath;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.LogMessageReporter;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SystemEnvironmentConstants;
import com.zelix.klassmaster.util.ZkmStringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ASTClasspathStatement extends SummarizingStatementNode {
    public static char pathSeparator = File.pathSeparatorChar;
    public Integer multiReleaseVersion;
    public List classpathEntries = new ArrayList();

    @Override
    public final void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int ba = this.jjtGetNumChildren();
        int messageCount = scriptEnvironment1.getMessageCount();
        int warningCount = scriptEnvironment1.getWarningCount();
        int errorCount = scriptEnvironment1.getErrorCount();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            ZkmScriptNode zkmScriptNode = this.jjtGetChild(childrenElementIndex);
            if (zkmScriptNode instanceof ASTIntegerLiteral) {
                String value = ((ASTIntegerLiteral) zkmScriptNode).getValue();
                if (childrenElementIndex == 0) {
                    try {
                        int multiReleaseVersionValue = Integer.parseInt(value);
                        if (multiReleaseVersionValue < 9) {
                            scriptEnvironment1.logFatalError(
                                    "'"
                                            + this.getStatementName()
                                            + "' statement at line "
                                            + this.getStatementLine()
                                            + " has a multi-release version integer '"
                                            + value
                                            + "' which is not 9 or greater."
                            );
                        } else {
                            this.multiReleaseVersion = multiReleaseVersionValue;
                        }
                    } catch (NumberFormatException numberFormatException) {
                        scriptEnvironment1.logFatalError(
                                "'"
                                        + this.getStatementName()
                                        + "' statement at line "
                                        + this.getStatementLine()
                                        + " has an invalid multi-release version integer : '"
                                        + value
                                        + "'d."
                        );
                    }
                } else {
                    scriptEnvironment1.logFatalError(
                            "'"
                                    + this.getStatementName()
                                    + "' statement at line "
                                    + this.getStatementLine()
                                    + " had a multi-release version integer '"
                                    + value
                                    + "' that was not the first parameter of the statement : "
                                    + childrenElementIndex
                                    + "."
                    );
                }
            } else if (zkmScriptNode instanceof ZkmScriptASTStringLiteral) {
                ZkmScriptASTStringLiteral zkmScriptASTStringLiteral = (ZkmScriptASTStringLiteral) zkmScriptNode;
                String valueValue = zkmScriptASTStringLiteral.getValue();
                if (this.classpathEntries.contains(valueValue)) {
                    scriptEnvironment1.logWarning(
                            "\""
                                    + valueValue
                                    + "\" appears more than once in \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getStatementLine()
                                    + ". First occurrence will be used."
                    );
                } else {
                    this.classpathEntries.add(valueValue);
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

    public ASTClasspathStatement() {
        super(53);
    }

    @Override
    public void executeStatement(Object object, Object object1, Object object2, Object object3) throws ZkmException, IOException {
        int bc = (Integer) object2;
        int ba = (Integer) object3;
        int bb = (Integer) object1;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        PrintWriter printWriter = scriptEnvironment1.getLogWriter();
        LogMessageReporter logMessageReporter = new LogMessageReporter(scriptEnvironment1, ZkmScriptSimpleNode.getTimestampPrefix().length());
        ZkmClasspath zkmClasspath = scriptEnvironment1.getClasspathForVersion(this.multiReleaseVersion);
        StringBuilder stringBuilder = new StringBuilder();
        int elementIndex = 0;
        int bf = 0;

        for (List list1 = this.classpathEntries; bf < list1.size(); list1 = this.classpathEntries) {
            String string = (String) this.classpathEntries.get(elementIndex);
            if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) != pathSeparator) {
                stringBuilder.append(pathSeparator);
            }

            if (com.zelix.klassmaster.script.ExecutionObserver.CURRENT.get() != null) {
                File candidate = new File(string);
                if (!candidate.isAbsolute()) candidate = new File(scriptEnvironment1.getDefaultDirectory(), string);
                string = com.zelix.klassmaster.script.ExecutionObserver.inputPath(candidate).getPath();
            }
            stringBuilder.append(string);
            bf = ++elementIndex;
        }

        String string4 = stringBuilder.toString();
        String string6 = ZkmScriptSimpleNode.getTimestampPrefix()
                + " Setting classpath"
                + (this.multiReleaseVersion == null ? "" : " for multi-release version " + this.multiReleaseVersion);
        System.out.println(string6 + "...");
        ObservableHolder observableHolder = new ObservableHolder();
        ObservableHolder observableHolder1 = new ObservableHolder();
        boolean bl = ZkmClasspath.validateClasspath(string4, scriptEnvironment1.getDefaultDirectory(), observableHolder, logMessageReporter, observableHolder1);
        String value = (String) observableHolder.getValue();
        if (!bl) {
            scriptEnvironment1.logError(
                    "Invalid classpath in \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getStatementLine()
                            + " : "
                            + (String) observableHolder1.getValue()
            );
            PrintStream printStream = System.err;
            StringBuilder stringBuilder1 = new StringBuilder();
            int bg = ZkmScriptSimpleNode.getTimestampPrefix().length() + 1;
            Integer integer = 32;
            printStream.println(stringBuilder1.append(ZkmStringUtils.repeatChar(bg, integer)).append("Error detected while setting classpath.").toString());
        }

        String classpathValue = zkmClasspath.getClasspath();
        zkmClasspath.setClasspath(value);
        ObservableHolder observableHolder2 = new ObservableHolder();
        ObservableHolder observableHolder3 = new ObservableHolder();
        if (zkmClasspath.locateRuntimeClasses(observableHolder3, observableHolder2)) {
            if (!observableHolder3.isValueNull()) {
                printWriter.println(
                        ZkmScriptSimpleNode.getTimestampPrefix() + " Using \"" + (String) observableHolder3.getValue() + "\" as path to java.lang.Object (E)"
                );
            } else if (!observableHolder2.isValueNull()) {
                printWriter.println(
                        ZkmScriptSimpleNode.getTimestampPrefix() + " Using \"" + (String) observableHolder2.getValue() + "\" as path to java.lang.Object (F)"
                );
            }
        }

        String classpathValue2 = zkmClasspath.getClasspath();
        printWriter.println(string6 + " to \"" + ZkmClasspath.describeClasspath(classpathValue2) + "\"");
        observableHolder3.clearValue();
        observableHolder2.clearValue();
        ASTClasspathStatement aSTClasspathStatement1;
        ScriptEnvironment scriptEnvironment2;
        int be;
        int bh;
        int bi;
        String string7;
        if (!zkmClasspath.locateRuntimeClasses(observableHolder3, observableHolder2)) {
            scriptEnvironment1.logFatalError(
                    "Could not find java.lang.Object : '"
                            + classpathValue2
                            + "' : "
                            + "java.home"
                            + "='"
                            + SystemEnvironmentConstants.JAVA_HOME
                            + "' '"
                            + SystemEnvironmentConstants.JAVA_VM_VENDOR
                            + "' '"
                            + SystemEnvironmentConstants.JAVA_VM_VERSION
                            + "' '"
                            + SystemEnvironmentConstants.OS_NAME
                            + "' '"
                            + SystemEnvironmentConstants.OS_VERSION
                            + "'"
            );
            aSTClasspathStatement1 = this;
            scriptEnvironment2 = scriptEnvironment1;
            be = bb;
            bh = bc;
            bi = ba;
            string7 = "while executing";
        } else if (!observableHolder2.isValueNull()) {
            String valueValue = (String) observableHolder2.getValue();
            printWriter.println("\tAdding '" + valueValue + "' to classpath. Must be able to find java.lang.Object.");
            aSTClasspathStatement1 = this;
            scriptEnvironment2 = scriptEnvironment1;
            be = bb;
            bh = bc;
            bi = ba;
            string7 = "while executing";
        } else {
            aSTClasspathStatement1 = this;
            scriptEnvironment2 = scriptEnvironment1;
            be = bb;
            bh = bc;
            bi = ba;
            string7 = "while executing";
        }

        aSTClasspathStatement1.printMessageSummary(scriptEnvironment2, be, bh, bi, string7);
        if (scriptEnvironment1.isVerbose()) {
            printWriter.println(
                    "\tChanging classpath" + (this.multiReleaseVersion == null ? "" : " for multi-release version " + this.multiReleaseVersion) + "..."
            );
            printWriter.println("\tfrom\t\"" + classpathValue + "\"");
            printWriter.println("\tto\t\"" + zkmClasspath.getClasspathDescription() + "\"");
        }
    }

    @Override
    public String getStatementName() {
        return "classpath";
    }
}
