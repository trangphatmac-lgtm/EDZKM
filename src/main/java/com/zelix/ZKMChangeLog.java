package com.zelix;

import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.changelog.ChangeLogMethodEntry;
import com.zelix.klassmaster.changelog.LineSeparatorHolder;
import com.zelix.klassmaster.changelog.parser.ChangeLogParseException;
import com.zelix.klassmaster.changelog.parser.ChangeLogParser;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogTokenMgrError;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ZKMChangeLog extends LineSeparatorHolder {
    public int baselineFatalErrorCount;
    public int baselineWarningCount;
    public int baselineErrorCount;
    public int baselineMessageCount;
    public ChangeLogMapping changeLogMapping;
    public ScriptEnvironment scriptEnvironment = new ScriptEnvironment(
            null, null, "ZKM_log1.txt", (String) null, (String) null, (String) null, (String) null, (String) null, (String) null
    );
    public String logFileName;

    public List getOldClassNames() {
        try {
            return this.toDottedNames(this.changeLogMapping.getOldClassNames());
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldTypeName(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New type name must not be null.");
            }

            String string1 = this.changeLogMapping.toOriginalTypeName(string);
            return string1 == null ? string : ZkmUtils.slashesToDots(string1);
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getNewFieldName(String string, String nameArgument, String descriptor) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            if (this.changeLogMapping.hasClassMapping(string)) {
                if (this.changeLogMapping.containsFieldMapping(string, nameArgument, descriptor)) {
                    this.recordLogCounts();
                    String newFieldName = this.changeLogMapping.getNewFieldName(string, nameArgument, descriptor);
                    this.reportNewLogErrors();
                    return newFieldName == null ? nameArgument : newFieldName;
                } else {
                    return nameArgument;
                }
            } else {
                return nameArgument;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public void recordLogCounts() {
        this.baselineMessageCount = this.scriptEnvironment.getMessageCount();
        this.baselineWarningCount = this.scriptEnvironment.getWarningCount();
        this.baselineErrorCount = this.scriptEnvironment.getErrorCount();
        this.baselineFatalErrorCount = this.scriptEnvironment.getSeriousErrorCount();
    }

    public String getNewPackageName(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            }

            String newPackageName = this.changeLogMapping.getNewPackageName(string);
            return newPackageName == null ? string : ZkmUtils.slashesToDots(newPackageName);
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public List getOldMethodSignaturesForClass(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Old class name must not be null.");
        } else {
            return this.changeLogMapping.listMethodEntries(key);
        }
    }

    public boolean isOldFieldPresent(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            if (this.changeLogMapping.hasClassMapping(string)) {
                int endIndex = string1.indexOf(" ");
                if (endIndex > 0 && endIndex < string1.length() - 1) {
                    String descriptor = string1.substring(0, endIndex);
                    String nameArgumentValue = string1.substring(endIndex + 1);
                    return this.isOldFieldPresent(string, nameArgumentValue, descriptor);
                } else {
                    throw new IllegalArgumentException("'" + string1 + "' is not a valid field signature");
                }
            } else {
                return false;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ChangeLogMethodEntry parseMethodSignature(String string) {
        String string1 = string;
        string1 = string1.trim();
        int endIndex = string1.indexOf(" ");
        if (endIndex <= -1) {
            throw new IllegalArgumentException("'" + string1 + "' is not a valid method signature (1)");
        }

        String returnTypeValue = string1.substring(0, endIndex);
        String string3 = string1.substring(endIndex + 1);
        string3 = string3.trim();
        int bb = string3.indexOf("(");
        if (bb <= 0) {
            throw new IllegalArgumentException("'" + string1 + "' is not a valid method signature (2)");
        }

        String methodNameValue = string3.substring(0, bb);
        int endIndexValue = string3.indexOf(")", bb);
        if (endIndexValue <= -1) {
            throw new IllegalArgumentException("'" + string1 + "' is not a valid method signature (3)");
        }

        string3 = string3.substring(bb + 1, endIndexValue);
        ArrayList arrayList = new ArrayList();
        StringTokenizer stringTokenizer = new StringTokenizer(string3, ",");

        while (stringTokenizer.hasMoreTokens()) {
            arrayList.add(stringTokenizer.nextToken().trim());
        }

        String[] argumentTypesValue = new String[arrayList.size()];
        argumentTypesValue = ((java.lang.String[]) (arrayList.toArray(argumentTypesValue)));
        return new ChangeLogMethodEntry(this, returnTypeValue, methodNameValue, argumentTypesValue);
    }

    public String getNewMethodSignature(String string, String methodNameArgument, String[] argumentTypesArgument, String returnTypeArgument) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            if (methodNameArgument == null) {
                throw new IllegalArgumentException("Old method name must not be null.");
            }

            if (returnTypeArgument == null) {
                throw new IllegalArgumentException("Old method return type must not be null.");
            }

            ChangeLogMethodEntry changeLogMethodEntry = new ChangeLogMethodEntry(this, returnTypeArgument, methodNameArgument, argumentTypesArgument);
            if (this.changeLogMapping.hasClassMapping(string)) {
                if (this.changeLogMapping.containsMethodMapping(string, methodNameArgument, argumentTypesArgument, returnTypeArgument)) {
                    this.recordLogCounts();
                    String string3 = this.changeLogMapping.describeNewMethod(string, methodNameArgument, argumentTypesArgument, returnTypeArgument);
                    this.reportNewLogErrors();
                    return string3 == null ? changeLogMethodEntry.getSignatureString() : string3;
                } else {
                    return changeLogMethodEntry.getSignatureString();
                }
            } else {
                return changeLogMethodEntry.getSignatureString();
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public void reportNewLogErrors() {
        boolean bl = false;
        boolean bl1 = false;
        int baselineWarningCountValue;
        if (this.baselineFatalErrorCount >= this.scriptEnvironment.getSeriousErrorCount() && this.baselineErrorCount >= this.scriptEnvironment.getErrorCount()) {
            baselineWarningCountValue = this.baselineWarningCount;
        } else {
            bl = true;
            bl1 = true;
            baselineWarningCountValue = this.baselineWarningCount;
        }

        if (baselineWarningCountValue < this.scriptEnvironment.getWarningCount() || this.baselineMessageCount < this.scriptEnvironment.getMessageCount()) {
            bl1 = true;
        }

        if (bl1) {
            System.err.println(this.scriptEnvironment.getLoggedMessagesText());
        }

        if (bl) {
            System.exit(1);
        }
    }

    public String getNewFieldName(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            } else {
                int endIndex = string1.indexOf(" ");
                if (endIndex > 0 && endIndex < string1.length() - 1) {
                    String descriptor = string1.substring(0, endIndex);
                    String nameArgumentValue = string1.substring(endIndex + 1);
                    return this.changeLogMapping.hasClassMapping(string) ? this.getNewFieldName(string, nameArgumentValue, descriptor) : nameArgumentValue;
                } else {
                    throw new IllegalArgumentException("'" + string1 + "' is not a valid field signature");
                }
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ZKMChangeLog(String logFileNameArgument) throws Exception {
        this.scriptEnvironment.clearLoggedMessages();
        this.logFileName = logFileNameArgument;
        this.recordLogCounts();
        this.parseChangeLog();
        this.reportNewLogErrors();
    }

    public String getOldClassName(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            }

            String originalClassName = this.changeLogMapping.getOriginalClassName(string);
            return originalClassName == null ? string : ZkmUtils.slashesToDots(originalClassName);
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldMethodName(String string, String string1, String[] strings, String string2) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            }

            if (this.changeLogMapping.isNewClassName(string)) {
                if (this.changeLogMapping.hasOriginalMethod(string, string1, strings, string2)) {
                    this.recordLogCounts();
                    String originalMethodName = this.changeLogMapping.getOriginalMethodName(string, string1, strings, string2);
                    this.reportNewLogErrors();
                    return originalMethodName == null ? string1 : originalMethodName;
                } else {
                    return string1;
                }
            } else {
                return string1;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getNewClassName(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            String newClassName = this.changeLogMapping.getNewClassName(string);
            return newClassName == null ? string : ZkmUtils.slashesToDots(newClassName);
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public boolean isOldFieldPresent(String string, String nameArgument, String descriptor) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            } else {
                return ((this.changeLogMapping.hasClassMapping(string)) && (this.changeLogMapping.containsFieldMapping(string, nameArgument, descriptor)));
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getNewMethodName(String string, String string1, String[] strings, String string2) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            if (this.changeLogMapping.hasClassMapping(string)) {
                if (this.changeLogMapping.containsMethodMapping(string, string1, strings, string2)) {
                    this.recordLogCounts();
                    String newMethodName = this.changeLogMapping.getNewMethodName(string, string1, strings, string2);
                    this.reportNewLogErrors();
                    return newMethodName == null ? string1 : newMethodName;
                } else {
                    return string1;
                }
            } else {
                return string1;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldFieldName(String originalClassName, String string1, String string2) {
        try {
            if (originalClassName == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            } else {
                return this.changeLogMapping.getOriginalFieldName(originalClassName, string1, string2);
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getLogFileName() {
        return this.logFileName;
    }

    public boolean isOldClassPresent(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            } else {
                return this.changeLogMapping.hasClassMapping(string);
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public void parseChangeLog() throws Exception {
        BufferedReader bufferedReader = null;
        ChangeLogSimpleNode input = null;
        try {
            this.changeLogMapping = new ChangeLogMapping(this.logFileName, this.scriptEnvironment);
            final ChangeLogParser instance = ChangeLogParser.instance;
            final String changeLogEncoding = ChangeLogMapping.readChangeLogEncoding(new File(this.logFileName));
            if (changeLogEncoding != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.logFileName), changeLogEncoding));
            } else {
                bufferedReader = new BufferedReader(new FileReader(this.logFileName));
            }
            if (instance == null) {
                new ChangeLogParser(bufferedReader);
            } else {
                ChangeLogParser.ReInit(bufferedReader);
            }
            input = ChangeLogParser.Input();
            input.interpret(null, 30872, 34067, 41973, this.changeLogMapping);
            ChangeLogMapping changeLogMapping;
            if (input.jjtGetNumChildren() == 0) {
                System.exit(1);
                changeLogMapping = this.changeLogMapping;
            } else {
                this.changeLogMapping.derivePackageMappingsFromClasses();
                changeLogMapping = this.changeLogMapping;
            }
            changeLogMapping.markParsingComplete();
            if (input != null) {
                input.dump();
            }
            try {
                bufferedReader.close();
            } catch (final IOException ioException) {
            }
        } catch (final FileNotFoundException fileNotFoundException) {
            throw new Exception("File Error : Couldn't open file \"" + this.logFileName + "\"");
        } catch (final ChangeLogTokenMgrError changeLogTokenMgrError) {
            throw new Exception("Lexical error while reading \"" + this.logFileName + "\" : \"" + changeLogTokenMgrError.getMessage() + "\"");
        } catch (final ChangeLogParseException changeLogParseException) {
            throw new Exception("Parse error while reading \"" + this.logFileName + "\" : \"" + changeLogParseException.getMessage() + "\"");
        } finally {
            if (input != null) {
                input.dump();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException ioExceptionValue) {
                }
            }
        }
    }

    public List getNewClassNames() {
        try {
            return this.toDottedNames(this.changeLogMapping.getNewClassNames());
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public boolean isOldMethodPresent(String string, String string1, String[] strings, String string2) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            } else {
                return ((this.changeLogMapping.hasClassMapping(string)) && (this.changeLogMapping.containsMethodMapping(string, string1, strings, string2)));
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public List getNewPackageNames() {
        try {
            return this.toDottedNames(this.changeLogMapping.getRenamedPackageNames());
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getNewMethodSignature(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            ChangeLogMethodEntry changeLogMethodEntry = this.parseMethodSignature(string1);
            return this.changeLogMapping.hasClassMapping(string)
                    ? this.getNewMethodSignature(string, changeLogMethodEntry.methodName, changeLogMethodEntry.argumentTypes, changeLogMethodEntry.returnType)
                    : string1;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldMethodName(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            }

            ChangeLogMethodEntry changeLogMethodEntry = this.parseMethodSignature(string1);
            return this.changeLogMapping.isNewClassName(string)
                    ? this.getOldMethodName(string, changeLogMethodEntry.methodName, changeLogMethodEntry.argumentTypes, changeLogMethodEntry.returnType)
                    : changeLogMethodEntry.methodName;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public ArrayList toDottedNames(ArrayList arrayList) throws IOException {
        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            String string = ZkmUtils.slashesToDots((String) arrayList.get(elementIndex));
            arrayList.set(elementIndex, string);
        }

        return arrayList;
    }

    public String getNewMethodName(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            }

            ChangeLogMethodEntry changeLogMethodEntry = this.parseMethodSignature(string1);
            return this.changeLogMapping.hasClassMapping(string)
                    ? this.getNewMethodName(string, changeLogMethodEntry.methodName, changeLogMethodEntry.argumentTypes, changeLogMethodEntry.returnType)
                    : changeLogMethodEntry.methodName;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public boolean isOldPackagePresent(String string) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old package name must not be null.");
            } else {
                return this.changeLogMapping.hasPackageMapping(string);
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public List getOldFieldSignaturesForClass(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Old class name must not be null.");
        } else {
            return this.changeLogMapping.listFieldEntries(key);
        }
    }

    public boolean isOldMethodPresent(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("Old class name must not be null.");
            } else if (this.changeLogMapping.hasClassMapping(string)) {
                ChangeLogMethodEntry changeLogMethodEntry = this.parseMethodSignature(string1);
                return this.isOldMethodPresent(string, changeLogMethodEntry.methodName, changeLogMethodEntry.argumentTypes, changeLogMethodEntry.returnType);
            } else {
                return false;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public List getOldMethodSignatures(String string, String string1) {
        try {
            if (string == null) {
                throw new IllegalArgumentException("New class name must not be null.");
            } else {
                return this.changeLogMapping.findOriginalMethods(string, string1);
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public List getOldPackageNames() {
        try {
            return this.toDottedNames(this.changeLogMapping.getSortedPackageNames());
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }
}
