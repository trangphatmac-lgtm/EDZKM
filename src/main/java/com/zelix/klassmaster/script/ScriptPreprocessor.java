package com.zelix.klassmaster.script;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

public class ScriptPreprocessor {
    public static final String USER_DIR = System.getProperty("user.dir");
    public File includeDirectory;
    public final String delimiter = "%";
    public final String directiveStart = '<' + this.delimiter;
    public final String directiveEnd = this.delimiter + '>';
    public Properties properties;
    public String processedText;

    public ScriptPreprocessor(String string, String string1, Properties properties1, String string2) throws ZkmException {
        this(string, null, properties1, null, null);
    }

    public boolean isBlank(String string) {
        int ba = string.length();

        for (int characterIndex = 0; characterIndex < ba; characterIndex++) {
            if (string.charAt(characterIndex) != ' ') {
                return false;
            }
        }

        return true;
    }

    public BufferedReader createReader() {
        return new BufferedReader(new StringReader(this.processedText));
    }

    public String describeIncludeChain(ArrayList arrayList) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int elementIndex = arrayList.size() - 1; elementIndex >= 0; elementIndex += -1) {
            String string = (String) arrayList.get(elementIndex);
            if (elementIndex == arrayList.size() - 1) {
                stringBuffer.append("'" + string + "'");
            } else {
                stringBuffer.append(" included in '" + string + "'");
            }
        }

        return stringBuffer.toString();
    }

    public String processIncludeDirective(String string, ArrayList arrayList) throws ZkmException {
        int endIndex = string.indexOf("include");
        if (endIndex == -1) {
            throw new ZkmProcessingException(
                    "PREPROCESSOR ERROR: Missing 'include' within '"
                            + this.directiveStart
                            + string
                            + this.directiveEnd
                            + "' in file "
                            + this.describeIncludeChain(arrayList)
            );
        }

        if (!this.isBlank(string.substring(0, endIndex))) {
            throw new ZkmProcessingException(
                    "PREPROCESSOR ERROR: Invalid 'include' directive '"
                            + this.directiveStart
                            + string
                            + this.directiveEnd
                            + "'. String '"
                            + string.substring(0, endIndex)
                            + "' is extraneous in file "
                            + this.describeIncludeChain(arrayList)
            );
        }

        int endIndexValue = string.indexOf("\"", endIndex + "include".length());
        if (endIndexValue > -1) {
            if (this.isBlank(string.substring(endIndex + "include".length(), endIndexValue))) {
                int endIndexValue2 = string.indexOf("\"", endIndexValue + "\"".length());
                if (endIndexValue2 > -1) {
                    String absolutePathValue = string.substring(endIndexValue + "\"".length(), endIndexValue2);
                    if (!this.isBlank(string.substring(endIndexValue2 + "\"".length()))) {
                        throw new ZkmProcessingException(
                                "PREPROCESSOR ERROR: Invalid 'include' directive '"
                                        + this.directiveStart
                                        + string
                                        + this.directiveEnd
                                        + "'. String '"
                                        + string.substring(endIndexValue2 + "\"".length())
                                        + "' is extraneous in file "
                                        + this.describeIncludeChain(arrayList)
                        );
                    }

                    String absolutePath;
                    if (ZkmFileUtils.isRelativePath(absolutePathValue)) {
                        if (this.includeDirectory != null) {
                            File file1 = new File(this.includeDirectory, absolutePathValue);
                            if (!file1.exists()) {
                                File file2 = new File(USER_DIR, absolutePathValue);
                                if (!file2.exists()) {
                                    throw new ZkmProcessingException(
                                            "PREPROCESSOR ERROR: Could not find either '"
                                                    + file1.getAbsolutePath()
                                                    + "' or '"
                                                    + file2.getAbsolutePath()
                                                    + "' as specified in '"
                                                    + "include"
                                                    + "' directive '"
                                                    + this.directiveStart
                                                    + string
                                                    + this.directiveEnd
                                                    + "' in file "
                                                    + this.describeIncludeChain(arrayList)
                                    );
                                }

                                absolutePath = file2.getAbsolutePath();
                            } else {
                                absolutePath = file1.getAbsolutePath();
                            }
                        } else {
                            File file3 = new File(USER_DIR, absolutePathValue);
                            absolutePath = file3.getAbsolutePath();
                        }
                    } else {
                        absolutePath = absolutePathValue;
                    }

                    return this.preprocessFile(absolutePath, ZkmUtils.copyArrayList(arrayList));
                } else {
                    throw new ZkmProcessingException(
                            "PREPROCESSOR ERROR: Missing closing '\"' in 'include' directive '"
                                    + this.directiveStart
                                    + string
                                    + this.directiveEnd
                                    + "' in file "
                                    + this.describeIncludeChain(arrayList)
                    );
                }
            } else {
                throw new ZkmProcessingException(
                        "PREPROCESSOR ERROR: Invalid 'include' directive '"
                                + this.directiveStart
                                + string
                                + this.directiveEnd
                                + "'. String '"
                                + string.substring(endIndex + "include".length(), endIndexValue)
                                + "' is extraneous in file "
                                + this.describeIncludeChain(arrayList)
                );
            }
        } else {
            throw new ZkmProcessingException(
                    "PREPROCESSOR ERROR: Missing opening '\"' in 'include' directive '"
                            + this.directiveStart
                            + string
                            + this.directiveEnd
                            + "' in file "
                            + this.describeIncludeChain(arrayList)
            );
        }
    }

    public String substituteProperties(String text) {
        StringBuffer result = new StringBuffer((int) (text.length() * 1.2));
        int nextOffset = 0;
        int openingDelimiter;
        while ((openingDelimiter = text.indexOf(this.delimiter, nextOffset)) > -1) {
            result.append(text.substring(nextOffset, openingDelimiter));
            if (!(openingDelimiter != 0 && text.charAt(openingDelimiter - 1) == '<')
                    && openingDelimiter < text.length() - 3
                    && text.charAt(openingDelimiter + 1) != '>') {
                int closingDelimiter = text.indexOf(this.delimiter, openingDelimiter + this.delimiter.length());
                if (closingDelimiter > -1 && closingDelimiter > openingDelimiter + this.delimiter.length()) {
                    boolean directiveBoundary = openingDelimiter > this.delimiter.length()
                            && text.charAt(openingDelimiter - this.delimiter.length()) == '<'
                            && text.length() - closingDelimiter >= this.delimiter.length()
                            && text.charAt(closingDelimiter + this.delimiter.length()) == '>';
                    if (!directiveBoundary) {
                        String propertyName = text.substring(openingDelimiter + this.delimiter.length(), closingDelimiter);
                        String propertyValue = null;
                        if (this.properties != null) {
                            propertyValue = this.properties.getProperty(propertyName);
                        }
                        if (propertyValue == null) {
                            propertyValue = System.getProperty(propertyName);
                        }
                        if (propertyValue != null) {
                            result.append(propertyValue);
                            nextOffset = closingDelimiter + this.delimiter.length();
                        } else {
                            result.append(this.delimiter);
                            nextOffset = openingDelimiter + this.delimiter.length();
                        }
                        continue;
                    }
                }
            }
            result.append(this.delimiter);
            nextOffset = openingDelimiter + this.delimiter.length();
        }
        result.append(text.substring(nextOffset));
        return result.toString();
    }

    public String preprocessFile(String string, ArrayList arrayList) throws ZkmException {
        arrayList.add(string);
        StringBuffer stringBuffer = new StringBuffer(1028);
        BufferedReader bufferedReader = null;

        try {
            ExecutionObserver observer = ExecutionObserver.CURRENT.get();
            String source = observer == null ? null : observer.source(new File(string));
            bufferedReader = source == null ? ZkmFileUtils.openReaderForPath(ExecutionObserver.inputPath(new File(string)).getPath(), HiddenOptionFlags.SCRIPT_ENCODING) : new BufferedReader(new StringReader(source));
            StringBuffer stringBuffer1 = new StringBuffer();
            int be = bufferedReader.read();

            while (true) {
                int ba = be;
                if (be == -1) {
                    String textValue = stringBuffer1.toString();
                    textValue = this.substituteProperties(textValue);
                    int bb = 0;
                    String textValue2 = textValue;

                    int bc;
                    for (String directiveStartValue = this.directiveStart; (bc = textValue2.indexOf(directiveStartValue, bb)) > -1; directiveStartValue = this.directiveStart) {
                        stringBuffer.append(textValue.substring(bb, bc));
                        int endIndex = textValue.indexOf(this.directiveEnd, bc);
                        if (endIndex <= -1) {
                            throw new ZkmProcessingException(
                                    "PREPROCESSOR ERROR: '"
                                            + this.directiveStart
                                            + "' without a matching '"
                                            + this.directiveEnd
                                            + "' in file "
                                            + this.describeIncludeChain(arrayList)
                            );
                        }

                        String string2 = this.processIncludeDirective(textValue.substring(bc + this.directiveStart.length(), endIndex), arrayList);
                        stringBuffer.append(string2);
                        bb = endIndex + this.directiveEnd.length();
                        textValue2 = textValue;
                    }

                    stringBuffer.append(textValue.substring(bb));
                    return stringBuffer.toString();
                }

                stringBuffer1.append((char) ba);
                be = bufferedReader.read();
            }
        } catch (FileNotFoundException fileNotFoundException) {
            throw new ZkmProcessingException("PREPROCESSOR ERROR: Couldn't find file " + this.describeIncludeChain(arrayList));
        } catch (IOException iOException1) {
            throw new ZkmProcessingException("PREPROCESSOR ERROR: " + iOException1.toString() + " in " + this.describeIncludeChain(arrayList));
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException iOException) {
                }
            }
        }
    }

    public ScriptPreprocessor(String string, String string1, Properties properties1, String string2, String string3) throws ZkmException {
        this.properties = properties1;
        if (!ZkmUtils.getHashedProperty(
                        "b90e12da2adf99f479ecad90a530ea33b8701422fbbe250b5d5f8edb8f7096c8b61dba24a593c4406c81f08cf9f129d95040be9487d1c7f1b0a53713dd37cb2d",
                        "true",
                        properties1
                )
                .equals("false")) {
            HiddenOptionFlags.skipFrameCompatibilityCheck = true;
        } else {
            HiddenOptionFlags.skipFrameCompatibilityCheck = false;
        }

        ExecutionObserver observer = ExecutionObserver.CURRENT.get();
        if (observer != null) this.includeDirectory = observer.includeDirectory();
        ArrayList arrayList = new ArrayList();
        this.processedText = this.preprocessFile(string, arrayList);
    }

    public String getProcessedText() {
        return this.processedText;
    }

    public ScriptPreprocessor(String string, Properties properties1) throws ZkmException {
        this(string, null, properties1, null);
    }
}
