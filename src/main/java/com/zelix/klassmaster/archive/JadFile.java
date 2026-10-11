package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.NumericStringUtil;
import com.zelix.klassmaster.util.OrderedIndexedMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.xml.ResourcePathTranslator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class JadFile {
    private static int[] flowControlTable;
    public static String sectionTerminator;
    public OrderedIndexedMap attributes = new OrderedIndexedMap();
    public String jadFilePath;
    public String jadFileName;
    public String expectedFinalSha256;

    public void requireAttribute(String key) throws ZkmException {
        if (!this.attributes.containsKey(key)) {
            throw new ZkmException("JAD file '" + this.jadFilePath + "' does not contain a " + key + " attribute.");
        }
    }


    static {
        setFlowControlTable(new int[4]);
        sectionTerminator = "&";
    }


    public String parseAttributeLine(String string) throws ZkmException {
        int endIndex = string.indexOf(":");
        if (endIndex > 0 && endIndex != string.length() - 1) {
            String key = string.substring(0, endIndex).trim();
            OrderedIndexedMap orderedIndexedMap;
            if (key.equalsIgnoreCase("MIDlet-Name")) {
                key = "MIDlet-Name";
                orderedIndexedMap = this.attributes;
            } else if (key.equalsIgnoreCase("MIDlet-Jar-URL")) {
                key = "MIDlet-Jar-URL";
                orderedIndexedMap = this.attributes;
            } else if (key.equalsIgnoreCase("MIDlet-Jar-Size")) {
                key = "MIDlet-Jar-Size";
                orderedIndexedMap = this.attributes;
            } else {
                orderedIndexedMap = this.attributes;
            }

            if (orderedIndexedMap.containsKey(key)) {
                throw new ZkmException("Two '" + key + "' headers in the JAD: '" + string + "'");
            }

            String second = string.substring(endIndex + 1).trim();
            this.attributes.put(key, second);
            return key;
        } else {
            throw new ZkmException("Invalid JAD attribute '" + string + "'");
        }
    }

    public JadFile(InputFileLocation inputFileLocation) throws ZkmException {
        if (inputFileLocation == null) {
            throw new IllegalArgumentException("Null JAD file");
        }

        this.jadFilePath = inputFileLocation.getQualifiedName();
        this.jadFileName = inputFileLocation.getFile().getName();
        if (inputFileLocation.getExpectedFinalSha256() != null) {
            this.expectedFinalSha256 = inputFileLocation.getExpectedFinalSha256().toLowerCase();
        }

        BufferedReader bufferedReader = null;

        try {
            String jadEncoding = HiddenOptionFlags.JAD_ENCODING;
            String jadFilePathValue;
            if (jadEncoding == null) {
                jadEncoding = "UTF-8";
                jadFilePathValue = this.jadFilePath;
            } else {
                jadFilePathValue = this.jadFilePath;
            }

            bufferedReader = ZkmFileUtils.openReaderForPath(jadFilePathValue, jadEncoding);

            String string1;
            while ((string1 = bufferedReader.readLine()) != null) {
                if (!string1.equals("") && !string1.equals(sectionTerminator)) {
                    this.parseAttributeLine(string1);
                }
            }

            this.requireAttribute("MIDlet-Name");
            this.requireAttribute("MIDlet-Jar-URL");
            this.requireAttribute("MIDlet-Jar-Size");
        } catch (IOException iOException1) {
            throw new ZkmException("IOException while reading JAD : '" + this.jadFilePath + "' : " + iOException1);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException iOException) {
                    throw new ZkmException("IOException while closing JAD : '" + this.jadFilePath + "' : " + iOException);
                }
            }
        }
    }

    public String getJadFilePath() {
        return this.jadFilePath;
    }

    public String getAttribute(Object key) {
        return (String) this.attributes.get(key);
    }

    
    
    public void writeTranslatedJad(File file1, EnumerableMap enumerableMap, EnumerableMap enumerableMap1, String string, MessageReporter messageReporter1) throws ZkmException {
        if (file1 == null) {
            throw new IllegalArgumentException("Null output JAD file");
        }

        boolean bl = false;
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(enumerableMap.size()));
        Iterator iterator = enumerableMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (!hashSet.add(entry.getValue())) {
                bl = true;
                break;
            }
        }

        PrintWriter printWriter = null;
        boolean bl1 = false ;

        try {
            bl1 = true;
            String jadEncoding = HiddenOptionFlags.JAD_ENCODING;
            if (jadEncoding == null) {
                jadEncoding = "UTF-8";
            }

            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file1), jadEncoding)));
            Enumeration enumeration = this.attributes.keyEnumeration();

            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                String attributeValue = this.getAttribute(key);
                if (key.equals("MIDlet-Icon")) {
                    printWriter.println(key + ":" + " " + ResourcePathTranslator.renameResourceParentPath(attributeValue, enumerableMap));
                } else if (key.startsWith("MIDlet-") && NumericStringUtil.isInteger(key.substring("MIDlet-".length()))) {
                    printWriter.println(ManifestHandler.translateMidletValue(key, attributeValue, enumerableMap, enumerableMap1));
                } else {
                    BooleanFlag booleanFlag = new BooleanFlag(false);
                    String string3 = AbstractManifestProcessor.translateClassListValue(
                            key, attributeValue, enumerableMap, enumerableMap1, booleanFlag, string, bl, messageReporter1
                    );
                    if (!booleanFlag.getValue() || HiddenOptionFlags.KEEP_UNCHANGED_MANIFEST_ENTRIES) {
                        printWriter.println(string3);
                    }
                }
            }

            printWriter.println();
            bl1 = false;
        } catch (IOException iOException) {
            throw new ZkmException("IOException while writing JAD : '" + file1.getAbsolutePath() + "' : " + iOException);
        } finally {
            if (bl1) {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        }

        printWriter.close();
    }

    public static void setFlowControlTable(int[] flowControlTableArgument) {
        flowControlTable = flowControlTableArgument;
    }

    public long getJarSize() throws JadFileException {
        String string = (String) this.attributes.get("MIDlet-Jar-Size");
        if (string != null) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException numberFormatException) {
                throw new JadFileException(
                        "JAD file '" + this.jadFilePath + "' has an invalid " + "MIDlet-Jar-Size" + " value of '" + string + "' : " + numberFormatException.toString()
                );
            }
        } else {
            throw new JadFileException("JAD file '" + this.jadFilePath + "' has no " + "MIDlet-Jar-Size" + " attribute");
        }
    }

    public static int[] getFlowControlTable() {
        return flowControlTable;
    }

    public String getJarFileName() throws ZkmException {
        String jarFileNameValue = (String) this.attributes.get("MIDlet-Jar-URL");
        if (jarFileNameValue != null) {
            int ba = jarFileNameValue.lastIndexOf("/");
            String jarFileName;
            if (ba > -1) {
                if (ba >= jarFileNameValue.length()) {
                    throw new ZkmException("JAD file '" + this.jadFilePath + "' has an invalid " + "MIDlet-Jar-URL" + " value of '" + jarFileNameValue + "'");
                }

                jarFileName = jarFileNameValue.substring(ba + 1);
            } else {
                jarFileName = jarFileNameValue;
            }

            int endIndex = jarFileName.indexOf("?");
            if (endIndex > -1) {
                jarFileName = jarFileName.substring(0, endIndex);
            }

            return jarFileName;
        } else {
            throw new ZkmException("JAD file '" + this.jadFilePath + "' has no " + "MIDlet-Jar-URL" + " attribute");
        }
    }

    public void setJarSize(long jarSize) {
        String string = (String) this.attributes.put("MIDlet-Jar-Size", String.valueOf(jarSize));
    }

    public void collectMidletClasses(String string, Map map1) {
        String string2 = " in '" + string + "'";
        Enumeration enumeration = this.attributes.keyEnumeration();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (key.startsWith("MIDlet-") && NumericStringUtil.isInteger(key.substring("MIDlet-".length()))) {
                String attributeValue = this.getAttribute(key);
                StringTokenizer stringTokenizer1 = new StringTokenizer(attributeValue, ",", true);
                int bb = 0;

                while (stringTokenizer1.hasMoreTokens()) {
                    String string8 = stringTokenizer1.nextToken().trim();
                    if (string8.equals(",")) {
                        bb++;
                    } else {
                        switch (bb) {
                            case 0:
                            case 1:
                                break;
                            case 2:
                                String keyValue = ZkmUtils.dotsToSlashes(string8);
                                ProgramClass programClass2 = ClassHierarchyNode.findProgramClass(keyValue);
                                if (programClass2 != null) {
                                    map1.put(programClass2, key + string2);
                                }
                                break;
                            default:
                                String[] strings = new String[]{"'" + key + "' has invalid value '" + attributeValue + "' : " + bb};
                                ZkmAssert.assertTrue(false, strings);
                        }
                    }
                }
            } else if (!key.equals("MIDlet-Icon")) {
                String attributeValue2 = this.getAttribute(key);
                StringTokenizer stringTokenizer = new StringTokenizer(attributeValue2, " ,", true);

                while (stringTokenizer.hasMoreTokens()) {
                    String string5 = stringTokenizer.nextToken();
                    if (string5.length() >= 3 && string5.indexOf(".") > -1 && string5.indexOf("/") == -1) {
                        int ba = string5.indexOf(".");
                        if (ba > 0 && ba < string5.length() - 1) {
                            String string1 = ".";
                            if (AbstractManifestProcessor.isDottedIdentifier(string5, string1)) {
                                String keyValue2 = ZkmUtils.dotsToSlashes(string5);
                                ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(keyValue2);
                                if (programClass1 != null) {
                                    map1.put(programClass1, key + string2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String getJadFileName() {
        return this.jadFileName;
    }
}
