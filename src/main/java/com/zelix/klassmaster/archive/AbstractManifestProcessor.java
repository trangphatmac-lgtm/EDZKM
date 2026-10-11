package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.ArrayListMultiMap;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.xml.ResourcePathTranslator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public abstract class AbstractManifestProcessor {
    public static String sectionTerminator = "&";
    public ArrayListMultiMap headers = new ArrayListMultiMap();
    public boolean endOfStream;

    public static String translateClassListValue(
            String string,
            String string1,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            BooleanFlag booleanFlag,
            String string2,
            boolean bl,
            MessageReporter messageReporter1
    ) {
        String string3 = string + ":" + " ";
        booleanFlag.setValue(false);
        String[] strings = string1.split("\\s*[,;:\"'=\\s]\\s*");
        BooleanFlag booleanFlag1 = new BooleanFlag();
        HashMap hashMap = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList();

        for (String string4 : strings) {
            String key = string4.trim();
            int ba = key.indexOf(46);
            if (key.length() >= 3 && ba > 0 && ba < key.length() - 1 && key.indexOf(47) == -1 && isDottedIdentifier(key, ".")) {
                String value = translateClassOrPathName(key, enumerableMap1, booleanFlag1);
                if (booleanFlag1.getValue()) {
                    hashMap.put(key, value);
                    arrayList.add(key);
                } else {
                    String valueValue = translateDottedClassName(key, enumerableMap, booleanFlag1);
                    if (booleanFlag1.getValue()) {
                        hashMap.put(key, valueValue);
                        arrayList.add(key);
                    }
                }
            }
        }

        ArrayList arrayList1 = new ArrayList();
        int bd = 0;
        Iterator iterator = arrayList.iterator();

        while (iterator.hasNext()) {
            String searchText = (String) iterator.next();
            int endIndex = string1.indexOf(searchText, bd);
            String string12 = string1.substring(bd, endIndex);
            if (string12.length() > 0) {
                arrayList1.add(string12);
            }

            arrayList1.add(searchText);
            bd = endIndex + searchText.length();
        }

        if (bd < string1.length()) {
            String string10 = string1.substring(bd, string1.length());
            arrayList1.add(string10);
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        HashSet hashSet1 = ZkmUtils.createHashSet();
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl6 = false;
        String keyValue2 = null;
        boolean bl7 = false;
        boolean bl1 = false;
        boolean bl2 = false;
        int arrayList1Size = arrayList1.size();

        for (int elementIndex = 0; elementIndex < arrayList1Size; elementIndex++) {
            String keyValue = (String) arrayList1.get(elementIndex);
            if (hashMap.containsKey(keyValue)) {
                String string9 = (String) ZkmUtils.mapOrSelf(keyValue, hashMap);
                boolean bl3 = hashSet.add(keyValue);
                boolean bl4 = !hashSet1.add(string9) && bl3 && !HiddenOptionFlags.ALLOW_DUPLICATE_MANIFEST_ENTRIES;
                bl1 = bl1 || bl4;
                boolean bl5 = string9.length() == 0;
                bl7 = bl7 || bl5;
                if (!bl5 && !bl4) {
                    if (keyValue2 != null && (keyValue2.indexOf(59) > -1 || bl6 || stringBuilder.length() == 0)) {
                        stringBuilder.append(keyValue2);
                        keyValue2 = null;
                    }

                    stringBuilder.append(string9);
                    bl6 = true;
                }
            } else if (elementIndex < arrayList1Size - 1) {
                bl2 = true;
                keyValue2 = keyValue;
            } else {
                if (keyValue2 != null && (keyValue2.indexOf(59) > -1 || bl6 || stringBuilder.length() == 0)) {
                    if (keyValue2.trim().endsWith(",") && keyValue.trim().startsWith(",")) {
                        int endIndexValue = keyValue2.lastIndexOf(44);
                        keyValue2 = keyValue2.substring(0, endIndexValue) + (endIndexValue < keyValue2.length() - 2 ? keyValue2.substring(endIndexValue + 1) : "");
                    }

                    stringBuilder.append(keyValue2);
                    keyValue2 = null;
                    bl6 = false;
                }

                stringBuilder.append(keyValue);
            }
        }

        String string14 = stringBuilder.toString();
        booleanFlag.setValue(string14.length() == 0 || string14.length() == 1 && string14.equals(";"));
        if (bl && bl2 && (bl7 || bl1)) {
            messageReporter1.reportWarning(
                    "WARNING:",
                    "Package names were collapsed which may have caused invalid translation of the '"
                            + string
                            + "' attribute line in manifest file in '"
                            + TempFileManager.replaceTempPaths(string2)
                            + "' : '"
                            + string1
                            + (string14.length() > 0 ? "' to '" + string14 + "'" : "'. Attribute now empty.")
            );
        }

        return string3 + string14;
    }

    public final String translateClassListAttribute(
            String key,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            BooleanFlag booleanFlag,
            String string1,
            boolean bl,
            MessageReporter messageReporter1
    ) {
        String unwrappedHeaderValue = this.getUnwrappedHeaderValue(key);
        return this.wrapManifestLine(translateClassListValue(key, unwrappedHeaderValue, enumerableMap, enumerableMap1, booleanFlag, string1, bl, messageReporter1));
    }

    public String parseHeaderLine(String string) throws ZkmException {
        int endIndex = string.indexOf(":");
        if (endIndex > 0 && endIndex != string.length() - 1) {
            String key = string.substring(0, endIndex).trim();
            key = this.normalizeHeaderName(key);
            if (!this.headers.containsKey(key)) {
                String string2 = string.substring(endIndex + 1);
                ArrayListMultiMap arrayListMultiMap;
                if (string2.length() > 0) {
                    if (string2.charAt(0) == ' ') {
                        string2 = string2.substring(1);
                        arrayListMultiMap = this.headers;
                    } else {
                        arrayListMultiMap = this.headers;
                    }
                } else {
                    arrayListMultiMap = this.headers;
                }

                arrayListMultiMap.addValue(key, string2);
                return key;
            } else if (key.equals("Name")) {
                throw new ZkmException("Two 'Name' headers in the one manifest section.  Header '" + string + "' must be preceded by a blank line.");
            } else {
                throw new ZkmException("Two '" + key + "' headers in the one manifest section: '" + string + "'");
            }
        } else {
            throw new ZkmException("Invalid Manifest attribute '" + string + "' - " + ZkmUtils.toHexArrayLiteral(string.getBytes()));
        }
    }

    public static String translateDottedClassName(String string, EnumerableMap enumerableMap, BooleanFlag booleanFlag) {
        return lookupRenamedName(string, enumerableMap, true, booleanFlag);
    }

    public static String translateClassOrPathName(String string, EnumerableMap enumerableMap, BooleanFlag booleanFlag) {
        boolean bl;
        if (string.indexOf(47) > 0) {
            if (string.indexOf(46) != -1) {
                booleanFlag.setValue(false);
                return string;
            }

            bl = true;
        } else {
            bl = false;
        }

        return lookupRenamedName(string, enumerableMap, !bl, booleanFlag);
    }

    public static boolean isDottedIdentifier(String string, String string1) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, string1);

        while (stringTokenizer.hasMoreTokens()) {
            if (!Character.isJavaIdentifierStart(stringTokenizer.nextToken().charAt(0))) {
                return false;
            }
        }

        return true;
    }

    public final boolean isEndOfStream() {
        return this.endOfStream;
    }

    public static String translateResourcePath(String string, EnumerableMap enumerableMap, ResourcePathTranslator resourcePathTranslator1) throws IOException {
        if (string.endsWith(".class")) {
            return resourcePathTranslator1.renameClassFilePath(string, enumerableMap);
        } else {
            return resourcePathTranslator1.hasDirectoryMapping(string)
                    ? resourcePathTranslator1.renameDirectory(string)
                    : resourcePathTranslator1.translateResourcePath(string);
        }
    }

    public final String getUnwrappedHeaderValue(String key) {
        List list1 = this.headers.getValues(key);
        if (list1 != null) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                String string1 = (String) list1.get(elementIndex);
                if (elementIndex > 0) {
                    string1 = string1.substring(1);
                }

                stringBuilder.append(string1);
            }

            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    public static String lookupRenamedName(String string, EnumerableMap enumerableMap, boolean bl, BooleanFlag booleanFlag) {
        String key = string.trim().replace('.', '/');
        String keyValue = (String) enumerableMap.get(key);
        if (keyValue == null) {
            keyValue = key;
            booleanFlag.setValue(false);
        } else {
            booleanFlag.setValue(true);
        }

        return bl ? keyValue.replace('/', '.') : keyValue;
    }

    public final boolean hasNoHeaders() {
        return this.headers.getKeyCount() == 0;
    }

    public final String getRawHeaderValue(String key) {
        List list1 = this.headers.getValues(key);
        if (list1 != null) {
            StringBuilder stringBuilder = new StringBuilder();

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                String string1 = (String) list1.get(elementIndex);
                stringBuilder.append(string1);
                if (elementIndex < list1.size() - 1) {
                    stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
                }
            }

            return stringBuilder.toString();
        } else {
            return null;
        }
    }

    public abstract void writeSection(
            PrintWriter printWriter,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            Object object,
            String string,
            boolean bl,
            MessageReporter messageReporter1
    ) throws IOException;

    public AbstractManifestProcessor(BufferedReader bufferedReader) throws ZkmException, IOException {
        boolean bl = false;
        String key = null;

        String string1;
        while ((string1 = bufferedReader.readLine()) != null) {
            if (string1.length() == 0) {
                if (bl) {
                    return;
                }
            } else if (string1.equals(sectionTerminator)) {
                if (bl) {
                    return;
                }
            } else {
                bl = true;
                if (string1.charAt(0) == ' ') {
                    this.headers.addValue(key, string1);
                } else {
                    key = this.parseHeaderLine(string1);
                }
            }
        }

        this.endOfStream = true;
    }

    public abstract String normalizeHeaderName(String string);

    public static String translateName(String string, EnumerableMap enumerableMap) {
        return translateClassOrPathName(string, enumerableMap, new BooleanFlag());
    }

    public final String wrapManifestLine(String string) {
        int ba = string.length();
        if (ba <= 72) {
            return string;
        }

        int beginIndex = 0;
        int beginIndexValue = 72;
        StringBuilder stringBuilder = new StringBuilder(string.substring(beginIndex, 72));

        while (ba - beginIndexValue > 71) {
            beginIndex = beginIndexValue;
            beginIndexValue = beginIndex + 72 - 1;
            stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
            stringBuilder.append(' ');
            stringBuilder.append(string.substring(beginIndex, beginIndexValue));
        }

        if (beginIndexValue < ba) {
            stringBuilder.append(HiddenOptionFlags.LINE_SEPARATOR);
            stringBuilder.append(' ');
            stringBuilder.append(string.substring(beginIndexValue));
        }

        return stringBuilder.toString();
    }
}
