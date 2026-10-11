package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.archive.DirectoryFileLister;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.config.ChangeLogInputFile;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.ObfuscateOptions;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.LogMessageReporter;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.proguard.ProGuardMappingTranslator;
import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingParseException;
import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingParser;
import com.zelix.klassmaster.proguard.mapping.parser.ast.ProGuardMappingASTInput;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.NoOpCallback;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SystemEnvironmentConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public abstract class ObfuscateStatementBase extends KeyValueStatementNode {
    public void applyRandomize(ObfuscateOptions obfuscateOptions1) {
        obfuscateOptions1.z = false;
        List list1 = super.parameterValues.getValues("randomize");
        if (list1 != null && list1.size() > 0) {
            String string = (String) list1.get(0);
            if (string != null && string.equals("true")) {
                obfuscateOptions1.z = true;
            }
        }
    }

    public Reader readChangeLogFile(File file1) throws ZkmException, IOException {
        String string = ChangeLogMapping.readChangeLogEncoding(file1);
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();

        try {
            if (string != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file1), string));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file1)));
            }

            String string1;
            while ((string1 = bufferedReader.readLine()) != null) {
                stringBuffer.append(string1);
                stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
            }
        } finally {
            bufferedReader.close();
        }

        return new StringReader(stringBuffer.toString());
    }

    public abstract void applyStatementOptions(ObfuscateOptions obfuscateOptions1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException;

    public String translateProGuardMapping(String mappingFileName, ScriptEnvironment scriptEnvironment1) throws ZkmException {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = ZkmFileUtils.openReaderForPath(mappingFileName);
            new ProGuardMappingParser(bufferedReader);
            ProGuardMappingASTInput proGuardMappingASTInput = (ProGuardMappingASTInput) ProGuardMappingParser.Input();
            ProGuardMappingTranslator proGuardMappingTranslator = new ProGuardMappingTranslator(mappingFileName);
            proGuardMappingASTInput.translate(null, proGuardMappingTranslator);
            String text = proGuardMappingTranslator.buildChangeLog();
            ObservableHolder observableHolder = new ObservableHolder();
            File file1 = ZkmFileUtils.writeStringToTempFile(text, observableHolder);
            if (file1 != null) {
                scriptEnvironment1.logMessage(
                        "Mapping file '" + mappingFileName + "' into " + "Zelix KlassMaster" + " format : '" + (String) observableHolder.getValue() + "'"
                );
                return file1.getAbsolutePath();
            }

            scriptEnvironment1.logSeriousError_v(
                    "Error creating temporary file while translating '"
                            + mappingFileName
                            + "' into "
                            + "Zelix KlassMaster"
                            + " format : '"
                            + (String) observableHolder.getValue()
                            + "'"
            );
        } catch (IOException iOException1) {
            scriptEnvironment1.logSeriousError_v(
                    "Error translating '" + mappingFileName + "' into " + "Zelix KlassMaster" + " format : '" + iOException1.getMessage() + "' (A)"
            );
        } catch (ProGuardMappingParseException proGuardMappingParseException) {
            scriptEnvironment1.logSeriousError_v(
                    "Error translating '" + mappingFileName + "' into " + "Zelix KlassMaster" + " format : '" + proGuardMappingParseException.getMessage() + "' (B)"
            );
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException iOException) {
            }
        }

        return mappingFileName;
    }

    public abstract void executeObfuscation(
            List list1,
            List list2,
            ObfuscateOptions obfuscateOptions1,
            ScriptEnvironment scriptEnvironment1,
            NoOpCallback noOpCallback,
            MessageReporter messageReporter1
    ) throws ZkmException, IOException;

    public boolean isProGuardMappingFile(String string, ScriptEnvironment scriptEnvironment1) {
        try {
            String string1 = ZkmFileUtils.readFileAsString(new File(string));
            return string1.indexOf("->") != -1 && string1.indexOf("=>") == -1;
        } catch (IOException iOException) {
            scriptEnvironment1.logSeriousError_v("Error reading file '" + string + "'");
            return false;
        }
    }

    public String getFirstParameterValue(Object key) {
        List list1 = super.parameterValues.getValues(key);
        if (list1 != null && list1.size() > 0) {
            String string = (String) list1.get(0);
            return string != null && string.length() > 0 ? string : null;
        } else {
            return null;
        }
    }

    public void checkChangeLogFileClash(String string, String string1, String string2, ScriptEnvironment scriptEnvironment1) {
        if (string.equals(string1) || !ZkmFileUtils.caseSensitiveFileSystem && string.equalsIgnoreCase(string1)) {
            scriptEnvironment1.logFatalError(
                    "Change log file \""
                            + string
                            + "\" clashes with "
                            + string2
                            + " file name in \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getStatementLine()
            );
        }
    }

    public abstract String getProgressVerb();

    public void applyAggressiveMethodRenaming(ObfuscateOptions obfuscateOptions1) {
        obfuscateOptions1.w = false;
        List list1 = super.parameterValues.getValues("aggressiveMethodRenaming");
        if (list1 != null && list1.size() > 0) {
            String string = (String) list1.get(0);
            if (string != null && string.equals("true")) {
                obfuscateOptions1.w = true;
            }
        }
    }

    public ObfuscateStatementBase() {
        super(56);
    }

    @Override
    public final void executeStatement(Object object, Object object1, Object object2, Object object3) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int warningCount = (Integer) object2;
        int errorCount = (Integer) object3;
        int messageCount = (Integer) object1;
        ClassRepository classRepository1 = scriptEnvironment1.getClassRepository();
        if (!classRepository1.isOpened()) {
            scriptEnvironment1.logFatalError(
                    "Attempt to use \"" + this.getStatementName() + "\" statement at line " + this.getStatementLine() + " before opening classes"
            );
        } else if (classRepository1.hasNoClassesOpened()) {
            scriptEnvironment1.logFatalError(
                    "Attempt to use \"" + this.getStatementName() + "\" statement at line " + this.getStatementLine() + " with no classes opened"
            );
        } else if (!classRepository1.isOpenedWithoutErrors()) {
            scriptEnvironment1.logFatalError(
                    "Attempt to use \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getStatementLine()
                            + " with unusable classes : "
                            + classRepository1.getOpenErrorMessage()
            );
        } else if (!classRepository1.hasProgramClasses()) {
            scriptEnvironment1.logMessage(
                    "Only module-info.class classes opened. Statement \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getStatementLine()
                            + " will have no effect."
            );
            return;
        }

        PrintWriter printWriter = scriptEnvironment1.getLogWriter();
        LogMessageReporter logMessageReporter = new LogMessageReporter(scriptEnvironment1, ZkmScriptSimpleNode.getTimestampPrefix().length());
        NoOpCallback noOpCallback = NoOpCallback.getInstance();
        String string = ZkmScriptSimpleNode.getTimestampPrefix() + " " + this.getProgressVerb() + " classes...";
        printWriter.println(string);
        System.out.println(string);
        ObfuscateOptions obfuscateOptions1 = new ObfuscateOptions();
        this.applyChangeLogFileIn(obfuscateOptions1, scriptEnvironment1);
        this.applyChangeLogFileOut(obfuscateOptions1, scriptEnvironment1);
        this.applyAggressiveMethodRenaming(obfuscateOptions1);
        this.applyRandomize(obfuscateOptions1);
        this.applyAllClassesOpened(obfuscateOptions1);
        this.applyKeepInnerClassInfo(obfuscateOptions1);
        List list1 = scriptEnvironment1.getExcludeStatements();
        List list2 = scriptEnvironment1.getUnexcludeStatements();
        this.applyStatementOptions(obfuscateOptions1, scriptEnvironment1);
        ObfuscateStatementBase obfuscateStatementBase1;
        ScriptEnvironment scriptEnvironment2;
        int messageCountValue;
        int warningCountValue;
        int errorCountValue;
        String string2;
        if (scriptEnvironment1.isVerbose()) {
            StringBuffer stringBuffer = new StringBuffer();
            if (obfuscateOptions1.e) {
                stringBuffer.append("\tlooseChangeLogFileIn=");
            } else {
                stringBuffer.append("\tchangeLogFileIn=");
            }

            if (obfuscateOptions1.g != null && obfuscateOptions1.g.length != 0) {
                int inputChangeLogIndex = 0;
                int inputChangeLogIndexValue = inputChangeLogIndex;

                for (ChangeLogInputFile[] changeLogInputFiles = obfuscateOptions1.g; inputChangeLogIndexValue < changeLogInputFiles.length; changeLogInputFiles = obfuscateOptions1.g) {
                    if (inputChangeLogIndex > 0) {
                        stringBuffer.append(", ");
                    }

                    stringBuffer.append("\"" + obfuscateOptions1.g[inputChangeLogIndex].getFileName() + "\"");
                    inputChangeLogIndexValue = ++inputChangeLogIndex;
                }
            } else {
                stringBuffer.append("\"\"");
            }

            printWriter.println(stringBuffer.toString());
            printWriter.println("\tchangeLogFileOut=\"" + obfuscateOptions1.f + "\"");
            printWriter.println("\taggressiveMethodRenaming=" + obfuscateOptions1.w);
            printWriter.println("\trandomize=" + obfuscateOptions1.z);
            printWriter.println("\tallClassesOpened=" + obfuscateOptions1.A);
            String string1 = null;
            switch (obfuscateOptions1.b) {
                case 0:
                    string1 = "true";
                    break;
                case 1:
                    string1 = "false";
                    break;
                case 2:
                    string1 = "ifNameNotObfuscated";
            }

            printWriter.println("\tkeepInnerClassInfo=" + string1);
            obfuscateStatementBase1 = this;
            scriptEnvironment2 = scriptEnvironment1;
            messageCountValue = messageCount;
            warningCountValue = warningCount;
            errorCountValue = errorCount;
            string2 = "in parse of";
        } else {
            obfuscateStatementBase1 = this;
            scriptEnvironment2 = scriptEnvironment1;
            messageCountValue = messageCount;
            warningCountValue = warningCount;
            errorCountValue = errorCount;
            string2 = "in parse of";
        }

        obfuscateStatementBase1.printMessageSummary(scriptEnvironment2, messageCountValue, warningCountValue, errorCountValue, string2);
        messageCount = scriptEnvironment1.getMessageCount();
        warningCount = scriptEnvironment1.getWarningCount();
        errorCount = scriptEnvironment1.getErrorCount();
        int seriousErrorCount = scriptEnvironment1.getSeriousErrorCount();
        this.executeObfuscation(list1, list2, obfuscateOptions1, scriptEnvironment1, noOpCallback, logMessageReporter);
        if (obfuscateOptions1.changeLogWriter != null) {
            obfuscateOptions1.changeLogWriter.close();
            obfuscateOptions1.changeLogWriter = null;
        }

        this.printMessageSummary(scriptEnvironment1, messageCount, warningCount, errorCount, "while executing");
        if (scriptEnvironment1.getSeriousErrorCount() > seriousErrorCount) {
            logMessageReporter.reportFatalErrorWithDetail(
                    "FATAL ERROR:",
                    "Serious Errors detected during execution of '" + this.getStatementName() + "' statement.",
                    scriptEnvironment1.getSeriousErrorsText()
            );
        }
    }

    public void applyChangeLogFileIn(ObfuscateOptions obfuscateOptions1, ScriptEnvironment scriptEnvironment1) throws ZkmException {
        obfuscateOptions1.g = null;
        List list1 = null;
        if (super.parameterValues.containsKey("changeLogFileIn")) {
            list1 = super.parameterValues.getValues("changeLogFileIn");
        } else if (super.parameterValues.containsKey("looseChangeLogFileIn")) {
            list1 = super.parameterValues.getValues("looseChangeLogFileIn");
            obfuscateOptions1.e = true;
        }

        ArrayList arrayList = new ArrayList();
        if (list1 != null) {
            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                String fileName = (String) list1.get(elementIndex);
                if (fileName != null && fileName.length() > 0) {
                    String string1 = ZkmFileUtils.normalizeSeparators(fileName);
                    File file1;
                    if (ZkmFileUtils.isRelativePath(string1)) {
                        file1 = new File(scriptEnvironment1.getDefaultDirectory(), string1);
                    } else {
                        file1 = new File(string1);
                    }

                    if (file1.isDirectory()) {
                        scriptEnvironment1.logFatalError(
                                "Input change log file cannot be a directory : \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getStatementLine()
                                        + " : \""
                                        + file1.getAbsolutePath()
                                        + "\""
                        );
                    }

                    String absolutePath = file1.getAbsolutePath();
                    String logFileName = scriptEnvironment1.getLogFileName();
                    this.checkChangeLogFileClash(absolutePath, logFileName, "ZKM log", scriptEnvironment1);
                    String trimLogFileName = scriptEnvironment1.getTrimLogFileName();
                    this.checkChangeLogFileClash(absolutePath, trimLogFileName, "trim log", scriptEnvironment1);
                    ChangeLogInputFile changeLogInputFile;
                    if (scriptEnvironment1.isProGuardMappingInputEnabled() && this.isProGuardMappingFile(absolutePath, scriptEnvironment1)) {
                        String fileNameValue = this.translateProGuardMapping(absolutePath, scriptEnvironment1);
                        changeLogInputFile = new ChangeLogInputFile(fileNameValue);
                        file1 = new File(fileNameValue);
                        absolutePath = file1.getAbsolutePath();
                    } else {
                        changeLogInputFile = new ChangeLogInputFile(fileName);
                    }

                    try {
                        changeLogInputFile.setReader(this.readChangeLogFile(file1));
                        arrayList.add(changeLogInputFile);
                    } catch (IOException iOException) {
                        scriptEnvironment1.logFatalError(
                                "Change log file \""
                                        + absolutePath
                                        + "\" could not be opened in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getStatementLine()
                                        + " : "
                                        + iOException
                                        + " : (1)"
                        );
                    }
                }
            }

            if (arrayList.size() > 0) {
                obfuscateOptions1.g = ((com.zelix.klassmaster.config.ChangeLogInputFile[]) (arrayList.toArray(new ChangeLogInputFile[arrayList.size()])));
            }
        }
    }

    public void applyKeepInnerClassInfo(ObfuscateOptions obfuscateOptions1) {
        obfuscateOptions1.b = 1;
        List list1 = super.parameterValues.getValues("keepInnerClassInfo");
        if (list1 != null && list1.size() > 0) {
            String string = (String) list1.get(0);
            if (string != null) {
                if (string.equals("true")) {
                    obfuscateOptions1.b = 0;
                } else if (string.equals("ifNameNotObfuscated")) {
                    obfuscateOptions1.b = 2;
                }
            }
        }
    }

    public void applyAllClassesOpened(ObfuscateOptions obfuscateOptions1) {
        obfuscateOptions1.A = true;
        List list1 = super.parameterValues.getValues("allClassesOpened");
        if (list1 != null && list1.size() > 0) {
            String string = (String) list1.get(0);
            if (string != null && string.equals("false")) {
                obfuscateOptions1.A = false;
            }
        }
    }

    @Override
    public boolean isParameterAccepted(Object object, Object object1) {
        DelegatingParameterNode delegatingParameterNode = (DelegatingParameterNode) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        String parameterName = delegatingParameterNode.getParameterName();
        ListMultimap listMultimap;
        if (super.parameterValues.containsKey("changeLogFileIn")) {
            if (parameterName.equals("looseChangeLogFileIn")) {
                scriptEnvironment1.logError(
                        "\"changeLogFileIn\" and \"looseChangeLogFileIn\" appear in \""
                                + this.getStatementName()
                                + "\" statement at line "
                                + this.getStatementLine()
                                + ". They are mutually exclusive. Only \""
                                + "changeLogFileIn"
                                + "\" will be used. (3)"
                );
                return false;
            }

            listMultimap = super.parameterValues;
        } else {
            listMultimap = super.parameterValues;
        }

        if (listMultimap.containsKey("looseChangeLogFileIn") && parameterName.equals("changeLogFileIn")) {
            scriptEnvironment1.logError(
                    "\"looseChangeLogFileIn\" and \"changeLogFileIn\" appear in \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getStatementLine()
                            + ". They are mutually exclusive. Only \""
                            + "looseChangeLogFileIn"
                            + "\" will be used. (4)"
            );
            return false;
        } else {
            return true;
        }
    }

    public void applyChangeLogFileOut(ObfuscateOptions obfuscateOptions1, ScriptEnvironment scriptEnvironment1) {
        List list1 = super.parameterValues.getValues("changeLogFileOut");
        if (list1 == null) {
            list1 = super.parameterValues.getValues("changeLogFile");
        }

        obfuscateOptions1.changeLogWriter = null;
        if (list1 != null && list1.size() > 0) {
            obfuscateOptions1.f = ((String) list1.get(0)).trim();
        } else {
            obfuscateOptions1.f = "ChangeLog.txt";
        }

        if (obfuscateOptions1.f.length() > 0) {
            String absolutePathValue = ZkmFileUtils.normalizeSeparators(obfuscateOptions1.f);
            File file1 = null;

            try {
                if (ZkmFileUtils.isRelativePath(absolutePathValue)) {
                    file1 = new File(scriptEnvironment1.getDefaultDirectory(), absolutePathValue);
                } else {
                    file1 = new File(absolutePathValue);
                }

                String absolutePath;
                String fileSeparator;
                if (file1.isDirectory()) {
                    scriptEnvironment1.logFatalError(
                            "Change log file cannot be a directory : \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getStatementLine()
                                    + " : \""
                                    + file1.getAbsolutePath()
                                    + "\""
                    );
                    absolutePath = absolutePathValue;
                    fileSeparator = SystemEnvironmentConstants.FILE_SEPARATOR;
                } else {
                    absolutePath = absolutePathValue;
                    fileSeparator = SystemEnvironmentConstants.FILE_SEPARATOR;
                }

                int endIndex = absolutePath.lastIndexOf(fileSeparator);
                if (endIndex > 0) {
                    String string1 = absolutePathValue.substring(0, endIndex);
                    DirectoryFileLister.ensureDirectoryExists(string1);
                    absolutePath = file1.getAbsolutePath();
                } else {
                    absolutePath = file1.getAbsolutePath();
                }

                String absolutePathValue2 = absolutePath;
                String logFileName = scriptEnvironment1.getLogFileName();
                this.checkChangeLogFileClash(absolutePathValue2, logFileName, "ZKM log", scriptEnvironment1);
                String trimLogFileName = scriptEnvironment1.getTrimLogFileName();
                this.checkChangeLogFileClash(absolutePathValue2, trimLogFileName, "trim log", scriptEnvironment1);
                String changeLogEncoding = AbstractChangeLog.getChangeLogEncoding();
                if (changeLogEncoding != null) {
                    obfuscateOptions1.changeLogWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file1), changeLogEncoding), 2048));
                    obfuscateOptions1.v = changeLogEncoding;
                } else {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file1), "UTF-8");
                    obfuscateOptions1.changeLogWriter = new PrintWriter(new BufferedWriter(outputStreamWriter, 2048));
                    obfuscateOptions1.v = "UTF-8";
                }
            } catch (IOException iOException) {
                scriptEnvironment1.logFatalError(
                        "Change log file \""
                                + file1.getAbsolutePath()
                                + "\" could not be opened in \""
                                + this.getStatementName()
                                + "\" statement at line "
                                + this.getStatementLine()
                                + " : "
                                + iOException
                                + " : (2)"
                );
            }
        } else {
            obfuscateOptions1.f = null;
        }
    }
}
