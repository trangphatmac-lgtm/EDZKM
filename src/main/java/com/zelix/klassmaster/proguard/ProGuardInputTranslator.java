package com.zelix.klassmaster.proguard;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.config.parser.ProGuardConfigParseException;
import com.zelix.klassmaster.proguard.config.parser.ProGuardConfigParser;
import com.zelix.klassmaster.proguard.config.parser.ast.ASTBomClause;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.ObjectStack;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.OrderedIndexedMap;
import com.zelix.klassmaster.util.SystemEnvironmentConstants;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ProGuardInputTranslator {
    private static int[] optionPositions;

    public static String loadZkmScriptTemplate(ObservableHolder observableHolder, String string, Set set1) throws ZkmException, IOException {
        String string2 = HiddenOptionFlags.SCRIPT_ENCODING != null ? HiddenOptionFlags.SCRIPT_ENCODING : ZkmFileUtils.DEFAULT_ENCODING;
        if (HiddenOptionFlags.DEFAULT_DIR != null) {
            set1.add(HiddenOptionFlags.DEFAULT_DIR);
            String string1 = readZkmScriptInDir(HiddenOptionFlags.DEFAULT_DIR, string2, observableHolder);
            if (string1 != null) {
                return string1;
            }
        }

        Set set2;
        String userDir;
        if (string != null) {
            set1.add(string);
            String string3 = readZkmScriptInDir(string, string2, observableHolder);
            if (string3 != null) {
                return string3;
            }

            set2 = set1;
            userDir = ZkmFileUtils.USER_DIR;
        } else {
            set2 = set1;
            userDir = ZkmFileUtils.USER_DIR;
        }

        set2.add(userDir);
        String string4 = readZkmScriptInDir(ZkmFileUtils.USER_DIR, string2, observableHolder);
        return string4 != null ? string4 : null;
    }

    public static String readZkmScriptInDir(String string, String string1, ObservableHolder observableHolder) throws ZkmException, IOException {
        File file1 = new File(string, "ZKMScript.txt");
        if (file1.exists() && !file1.isDirectory()) {
            observableHolder.setValue(file1.getAbsolutePath());
            LineCommentStripper lineCommentStripper = new LineCommentStripper();
            String string2 = ZkmFileUtils.readFileTransformed(file1, string1, lineCommentStripper);
            int endIndex = string2.indexOf("/*");
            if (endIndex > -1) {
                StringBuilder stringBuilder = new StringBuilder();

                while (endIndex > -1) {
                    stringBuilder.append(string2.substring(0, endIndex));
                    int bb = string2.indexOf("*/", endIndex + 2);
                    if (bb > -1) {
                        string2 = string2.substring(bb + 2);
                        endIndex = string2.indexOf("/*");
                    } else {
                        endIndex = -1;
                    }
                }

                stringBuilder.append(string2);
                return stringBuilder.toString();
            } else {
                return string2;
            }
        } else {
            return null;
        }
    }

    public static int findLastFileOptionEnd(String string, int integerArgument, ObservableHolder observableHolder) throws ZkmException, IOException {
        int integerValue = findNextFileOptionEnd(string, integerArgument, observableHolder);
        boolean bl = true;

        while (integerValue > 0 && bl) {
            int characterIndex = ZkmStringUtils.skipWhitespace(string, integerValue);
            if (characterIndex != -1 && string.charAt(characterIndex) == '-') {
                integerValue = findNextFileOptionEnd(string, integerValue, observableHolder);
            } else {
                bl = false;
            }
        }

        return integerValue;
    }

    public static String translateToZkmScript(final String text, final Properties properties) throws ZkmException, IOException {
        final ObservableHolder observableHolder = new ObservableHolder();
        final StringWriter out = new StringWriter();
        final ObservableHolder observableHolder2 = new ObservableHolder();
        final File writeStringToTempFile = ZkmFileUtils.writeStringToTempFile(text, observableHolder2);
        if (!observableHolder2.isValueNull()) {
            return (String) observableHolder2.getValue();
        }
        try {
            return parseConfigFile(writeStringToTempFile, new PrintWriter(out), observableHolder, properties).buildZkmScript("ProGuardInputTranslator", true, true, new ArrayList());
        } catch (final AssertionFailedException assertionFailedExceptionValue) {
            throw assertionFailedExceptionValue;
        } catch (final Throwable throwableValue) {
            out.flush();
            final String string = out.toString();
            final File file = new File("ZKM_PG_log.txt");
            PrintWriter printWriter = null;
            String s2;
            try {
                s2 = "ERROR: Error while translating input from ProGuard to Zelix KlassMaster format : '" + throwableValue.getMessage() + "' (B)";
                printWriter = new PrintWriter(new FileWriter(file));
                printWriter.println(string);
                printWriter.println(s2);
                s2 = s2 + ZkmAssert.lineSeparator + "See '" + file.getAbsolutePath() + "' for more detail. Any error message line numbers will refer to the content appearing two lines after the '" + "Expanded and preprocessed ProGuard style input" + "' line.";
                printWriter.flush();
                printWriter.close();
            } catch (final IOException ioException) {
                s2 = "ERROR: Error while translating input from ProGuard to Zelix KlassMaster format : '" + throwableValue.getMessage() + "' (C)";
            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
            return s2;
        } finally {
            try {
                out.close();
            } catch (final IOException ioExceptionValue) {
            }
            try {
                if (observableHolder.getValue() != null) {
                    ((Reader) observableHolder.getValue()).close();
                }
            } catch (final IOException ioExceptionValue2) {
            }
        }
    }

    public static void clearOptionPositions() {
        optionPositions = null;
    }

    public static String expandIncludes(
            String string, Properties properties1, Properties properties2, ObjectStack objectStack, Map map1, PrintWriter printWriter
    ) throws ProGuardIncludeException, IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int ba = 0;

        for (int endIndex = string.indexOf("-include"); endIndex > -1; endIndex = string.indexOf("-include", ba)) {
            String string4;
            int endIndexValue;
            String string5;
            if (endIndex - 1 > ba) {
                String string1 = string.substring(ba, endIndex);
                stringBuilder.append(string1);
                stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
                string4 = string;
                endIndexValue = endIndex;
                string5 = "-include";
            } else {
                string4 = string;
                endIndexValue = endIndex;
                string5 = "-include";
            }

            int beginIndex = ZkmStringUtils.skipWhitespace(string4, endIndexValue + string5.length());
            if (beginIndex == -1) {
                throw new ProGuardIncludeException("No contents for '-include' in file '" + ((File) objectStack.peek()).getAbsolutePath() + "'");
            }

            int beginIndexValue = beginIndex;
            char bd = string.charAt(beginIndexValue);
            boolean bl = false;

            for (boolean whitespace = Character.isWhitespace(bd); !whitespace || bd == ' ' && bl; whitespace = Character.isWhitespace(bd)) {
                if (bd == '"') {
                    bl = !bl;
                }

                bd = string.charAt(++beginIndexValue);
            }

            String pathnameValue = string.substring(beginIndex, beginIndexValue);
            if (pathnameValue.startsWith("\"") && pathnameValue.endsWith("\"")) {
                pathnameValue = pathnameValue.substring(1, pathnameValue.length() - 1);
            }

            if (pathnameValue.indexOf("\"") > -1) {
                throw new ProGuardIncludeException(
                        "Broken string literal in '-include' in file '" + ((File) objectStack.peek()).getAbsolutePath() + "' : '" + pathnameValue + "'"
                );
            }

            String string3 = readIncludeFile(pathnameValue, properties1, properties2, objectStack, map1, printWriter);
            stringBuilder.append(string3);
            if (!string3.endsWith(HiddenOptionFlags.LINE_SEPARATOR)) {
                stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
            }

            ba = beginIndexValue + 1;
        }

        stringBuilder.append(string.substring(ba, string.length()));
        return stringBuilder.toString();
    }

    public static String readIncludeFile(final String pathname, final Properties properties, final Properties properties2, final ObjectStack objectStack, final Map map, final PrintWriter printWriter) throws ProGuardIncludeException, IOException {
        final StringBuilder stringBuilderValue = new StringBuilder();
        final StringBuilder stringBuilderValue2 = new StringBuilder();
        BufferedReader bufferedReader = null;
        final File file = new File(pathname);
        printWriter.println("Expanding '-include' '" + file.getAbsolutePath() + "'");
        try {
            if (objectStack.search(file) > -1) {
                throw new ProGuardIncludeException("Recursive '-include' of file '" + file.getAbsolutePath() + "' in file '" + ((File) objectStack.peek()).getAbsolutePath() + "'");
            }
            objectStack.push(file);
            try {
                bufferedReader = new BufferedReader(new FileReader(file));
            } catch (final IOException ioException) {
                throw new ProGuardIncludeException("File error while processing '-include' of file '" + file.getAbsolutePath() + "'" + ((objectStack.size() > 1) ? (" in file '" + ((File) objectStack.peekAt(1)).getAbsolutePath() + "'") : "") + " : '" + ioException.getMessage() + "'");
            }
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                stringBuilderValue.append(str);
                stringBuilderValue.append(HiddenOptionFlags.LINE_SEPARATOR);
                final int index = str.indexOf("#");
                if (index > -1) {
                    str = str.substring(0, index);
                }
                final String trim = str.trim();
                if (trim.length() > 0) {
                    stringBuilderValue2.append(trim);
                    stringBuilderValue2.append(HiddenOptionFlags.LINE_SEPARATOR);
                }
            }
            try {
                bufferedReader.close();
            } catch (final IOException ioExceptionValue) {
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException ioExceptionValue2) {
                }
            }
        }
        map.put(file, stringBuilderValue.toString());
        final String expandIncludes = expandIncludes(substituteProperties(stringBuilderValue2.toString(), properties, properties2), properties, properties2, objectStack, map, printWriter);
        final File file2 = (File) objectStack.pop();
        return expandIncludes;
    }

    public static boolean isQuoteTerminator(int ba) {
        return ba == 40 || ba == 13 || ba == 10 || ba == SystemEnvironmentConstants.PATH_SEPARATOR_CHAR;
    }

    public static int findNextFileOptionEnd(String string, Integer integer, ObservableHolder observableHolder) throws ZkmException, IOException {
        int[] integerArray = new int[ProGuardOptionNames.FILE_ARG_OPTIONS.length];

        for (int i = 0; i < integerArray.length; i++) {
            integerArray[i] = string.indexOf(ProGuardOptionNames.FILE_ARG_OPTIONS[i], integer);
        }

        int integerArrayElement = -1;

        for (int i = 0; i < integerArray.length; i++) {
            if (integerArray[i] > -1 && (integerArrayElement == -1 || integerArray[i] < integerArrayElement)) {
                integerArrayElement = integerArray[i];
                observableHolder.setValue(ProGuardOptionNames.FILE_ARG_OPTIONS[i]);
            }
        }

        return integerArrayElement > -1 ? integerArrayElement + ((String) observableHolder.getValue()).length() : integerArrayElement;
    }

    public static int[] getOptionPositions() {
        return optionPositions;
    }

    public static String substituteProperties(String string, Properties properties1, Properties properties2) {
        StringBuilder stringBuilder = new StringBuilder((int) (string.length() * 1.5));
        int ba = 0;
        int bd = string.indexOf("<", 0);

        while (true) {
            int endIndex = bd;
            if (bd <= -1) {
                stringBuilder.append(string.substring(ba));
                return stringBuilder.toString();
            }

            stringBuilder.append(string.substring(ba, endIndex));
            int endIndexValue = string.indexOf(">", endIndex + "<".length());
            if (endIndexValue > -1 && endIndexValue > endIndex + "<".length()) {
                String string1 = string.substring(endIndex + "<".length(), endIndexValue);
                String property = null;
                if (properties1 != null) {
                    property = properties1.getProperty(string1);
                }

                if (property == null) {
                    property = properties2.getProperty(string1);
                }

                if (property != null) {
                    stringBuilder.append(property);
                    ba = endIndexValue + ">".length();
                } else {
                    stringBuilder.append("<");
                    ba = endIndex + "<".length();
                }
            } else {
                stringBuilder.append("<");
                ba = endIndex + "<".length();
            }

            bd = string.indexOf("<", ba);
        }
    }

    public static String quoteFileArguments(String string) throws ZkmException, IOException {
        int endIndex = string.length();
        StringBuilder stringBuilder = new StringBuilder();
        int beginIndex = 0;
        ObservableHolder observableHolder = new ObservableHolder();

        for (int i = findLastFileOptionEnd(string, 0, observableHolder); i > -1; i = findLastFileOptionEnd(string, beginIndex, observableHolder)) {
            String string1 = string.substring(beginIndex, i).trim();
            stringBuilder.append(string.substring(beginIndex, i));
            int endIndexValue = ZkmStringUtils.skipWhitespace(string, i);
            if (endIndexValue == -1) {
                beginIndex = endIndex;
                break;
            }

            stringBuilder.append(string.substring(i, endIndexValue));
            int characterIndex = endIndexValue;
            char bf = string.charAt(characterIndex);
            char bg = 0;
            if (string.length() > characterIndex + 1) {
                bg = string.charAt(characterIndex + 1);
            }

            if (bf == '\'') {
                bf = '"';
            }

            boolean bl = false;
            boolean bl1 = true;

            while (characterIndex < endIndex && (!Character.isWhitespace(bf) || bf == ' ' && bl)) {
                if (bf == '"' && !isQuoteTerminator(bg)) {
                    bl = !bl;
                }

                if (bl1) {
                    char bh = stringBuilder.charAt(stringBuilder.length() - 1);
                    if (bf != '"' && bh != '"' && bh != ')') {
                        stringBuilder.append("\"");
                        bl = true;
                    }

                    bl1 = false;
                }

                if (bf != SystemEnvironmentConstants.PATH_SEPARATOR_CHAR && bf != ',' && bf != '(' && bf != ')') {
                    if (bf == '"' && !bl) {
                        bl1 = true;
                    }

                    if (bf != '"' || !isQuoteTerminator(bg) || string1.equals("-flattenpackagehierarchy") && stringBuilder.charAt(stringBuilder.length() - 1) == '"'
                    ) {
                        stringBuilder.append(bf);
                    }
                } else {
                    if (stringBuilder.charAt(stringBuilder.length() - 1) != '"' && bl) {
                        stringBuilder.append("\"");
                    }

                    bl = false;
                    stringBuilder.append(bf == SystemEnvironmentConstants.PATH_SEPARATOR_CHAR ? "~" : bf);
                    bl1 = true;
                }

                if (++characterIndex < endIndex) {
                    bf = string.charAt(characterIndex);
                    if (bf == '\'') {
                        bf = '"';
                    }
                }

                if (string.length() > characterIndex + 1) {
                    bg = string.charAt(characterIndex + 1);
                }
            }

            if (stringBuilder.charAt(stringBuilder.length() - 1) != '"' && stringBuilder.charAt(stringBuilder.length() - 1) != ')') {
                stringBuilder.append("\"");
            }

            beginIndex = characterIndex;
        }

        stringBuilder.append(string.substring(beginIndex, endIndex));
        return stringBuilder.toString();
    }

    public static void exitWithError(String string, String string1, PrintWriter printWriter) {
        PrintStream printStream;
        if (printWriter != null) {
            printWriter.println(string);
            printWriter.close();
            printStream = System.err;
        } else {
            printStream = System.err;
        }

        printStream.println(string);
        System.err.println("See '" + string1 + "' for more detail.");
        System.exit(1);
    }

    public static ProGuardConfigTranslator parseConfigFile(File file1, PrintWriter printWriter, ObservableHolder observableHolder, Properties properties1) throws ZkmException, ProGuardConfigParseException, IOException {
        OrderedIndexedMap orderedIndexedMap = new OrderedIndexedMap();
        printWriter.println("");
        printWriter.println("Temporary input file '" + file1.getAbsolutePath() + "' contents.");
        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        printWriter.println(ZkmFileUtils.readFileAsString(file1));
        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        printWriter.println("");
        String string = quoteFileArguments(
                readIncludeFile(file1.getAbsolutePath(), properties1, System.getProperties(), new ObjectStack(), orderedIndexedMap, printWriter)
        );
        printWriter.println("");
        printWriter.println("Expanded and preprocessed ProGuard style input. Any line numbers in error messages will refer to this content.");
        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        printWriter.println(string.replace('~', ZkmFileUtils.PATH_SEPARATOR_CHAR));
        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        StringReader stringReader = new StringReader(string);
        observableHolder.setValue(stringReader);
        ASTBomClause aSTBomClause = (ASTBomClause) new ProGuardConfigParser(stringReader).BomClause();
        ProGuardConfigTranslator proGuardConfigTranslator = new ProGuardConfigTranslator(printWriter);
        aSTBomClause.translate(null, proGuardConfigTranslator);
        return proGuardConfigTranslator;
    }

    public static String translateCommandLine(
            String[] strings,
            Properties properties1,
            ObservableHolder observableHolder,
            ObservableHolder observableHolder1,
            ObservableHolder observableHolder2,
            BooleanFlag booleanFlag,
            String string,
            PrintWriter printWriter,
            boolean bl
    ) throws ZkmException, IOException {
        printWriter.println("");
        printWriter.println("Raw input");
        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

        for (int i = 0; i < strings.length; i++) {
            printWriter.println(strings[i]);
        }

        printWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < strings.length; i++) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(" ");
            }

            if (strings[i].length() > 0 && strings[i].charAt(0) == '@') {
                stringBuilder.append("-include");
                stringBuilder.append(" ");
                stringBuilder.append(strings[i].substring(1));
            } else {
                stringBuilder.append(strings[i]);
            }
        }

        ObservableHolder observableHolder3 = new ObservableHolder();
        File file1 = ZkmFileUtils.writeStringToTempFile(stringBuilder.toString(), observableHolder3);
        if (file1 == null) {
            String string1 = "ERROR: Error while attempting to create temporary file (A) : '" + (String) observableHolder3.getValue() + "'.";
            if (!bl) {
                throw new RuntimeException(string1);
            }

            exitWithError(string1, string, printWriter);
        }

        ObservableHolder observableHolder4 = new ObservableHolder();
        ProGuardConfigTranslator proGuardConfigTranslator = null;

        try {
            proGuardConfigTranslator = parseConfigFile(file1, printWriter, observableHolder4, properties1);
        } catch (AssertionFailedException assertionFailedException) {
            throw assertionFailedException;
        } catch (Throwable throwable) {
            String string3 = "ERROR: Error while translating input from ProGuard to Zelix KlassMaster format : '"
                    + (throwable.getMessage() != null ? throwable.getMessage() : throwable)
                    + "' (A)'. See '"
                    + string
                    + "' for more detail.";
            if (!bl) {
                throw new RuntimeException(string3);
            }

            exitWithError(string3, string, printWriter);
        } finally {
            try {
                if (observableHolder4.getValue() != null) {
                    ((Reader) observableHolder4.getValue()).close();
                }
            } catch (IOException iOException) {
            }
        }

        String string2 = null;
        ObservableHolder observableHolder5 = new ObservableHolder();
        HashSet hashSet = ZkmUtils.createHashSet(13);
        String string4 = proGuardConfigTranslator.findOutputDirectory(hashSet);
        String string5 = ProGuardConfigTranslator.resolveZkmLogDir(string4);
        System.setProperty("ZKM_LOG_DIR", string5);
        System.setProperty("PROGUARD_LOG_DIR", proGuardConfigTranslator.getProGuardLogDir());
        System.setProperty("PROGUARD_SAVE_DIR", proGuardConfigTranslator.getSaveDirectory());

        try {
            string2 = loadZkmScriptTemplate(observableHolder5, string4, hashSet);
        } catch (IOException iOException1) {
            String string7 = "FILE ERROR: Error while attempting to open and read : '"
                    + (observableHolder5.isValueNull() ? "ZKMScript.txt" : new File((String) observableHolder5.getValue(), "ZKMScript.txt").getAbsolutePath())
                    + "' : '"
                    + iOException1.getMessage();
            if (!bl) {
                throw new RuntimeException(string7);
            }

            exitWithError(string7, string, printWriter);
        }

        String string6 = "Searched for 'ZKMScript.txt' in : " + ZkmUtils.toQuotedListString(hashSet);
        proGuardConfigTranslator.logMessage(string6);
        if (string2 != null && string2.length() > 0) {
            proGuardConfigTranslator.loadTemplateStatements(string2, (String) observableHolder5.getValue());
        }

        String text = proGuardConfigTranslator.buildZkmScript("ProGuardInputTranslator", true, true, new ArrayList());
        observableHolder.setValue(proGuardConfigTranslator.getPrintSeedsFile());
        observableHolder1.setValue(proGuardConfigTranslator.getPrintUsageFile());
        observableHolder2.setValue(proGuardConfigTranslator.getBaseDirectory());
        booleanFlag.setValue(proGuardConfigTranslator.isVerbose());
        observableHolder3.setValue(null);
        File file2 = ZkmFileUtils.writeStringToTempFile(text, observableHolder3);
        if (file2 == null) {
            String string8 = "ERROR: Error while attempting to create temporary file (B) : '" + (String) observableHolder3.getValue() + "'.";
            if (!bl) {
                throw new RuntimeException(string8);
            }

            exitWithError(string8, string, printWriter);
        }

        return file2.getAbsolutePath();
    }

    static {
        clearOptionPositions();
    }

    private ProGuardInputTranslator() {
    }
}
