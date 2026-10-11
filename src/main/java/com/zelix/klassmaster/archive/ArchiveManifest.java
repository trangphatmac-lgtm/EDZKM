package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.xml.ResourcePathTranslator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ArchiveManifest {
    public List entrySections = new ArrayList();
    public String archiveName;
    public boolean compressed;
    public ManifestHandler mainSection;

    public boolean isCldc10() throws ZkmException {
        return this.mainSection.isCldc10();
    }

    public boolean isCompressed() {
        return this.compressed;
    }

    public void writeManifest(
            ZipOutputStream zipOutputStream1,
            Long long1,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            ResourcePathTranslator resourcePathTranslator1,
            boolean compressedArgument,
            MessageReporter messageReporter1
    ) throws IOException {
        ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, "META-INF/MANIFEST.MF", compressedArgument);
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(zipEntryWriter.getOutputStream(), "UTF-8"));
        boolean duplicateValues = hasDuplicateValues(enumerableMap);
        char bc = '\uda1b';
        MessageReporter messageReporter2 = messageReporter1;
        boolean duplicateValuesValue = duplicateValues;
        String archiveNameValue = this.archiveName;
        ResourcePathTranslator resourcePathTranslator2 = resourcePathTranslator1;
        EnumerableMap enumerableMap2 = enumerableMap1;
        EnumerableMap enumerableMap3 = enumerableMap;
        PrintWriter printWriter1 = printWriter;
        this.mainSection.writeSection(printWriter1, enumerableMap3, enumerableMap2, resourcePathTranslator2, archiveNameValue, duplicateValuesValue, messageReporter2);
        int elementIndex = 0;
        int bb = 0;

        for (List list1 = this.entrySections; bb < list1.size(); list1 = this.entrySections) {
            ManifestEntrySection manifestEntrySection = (ManifestEntrySection) this.entrySections.get(elementIndex);
            bc = '\uda1b';
            MessageReporter messageReporter3 = messageReporter1;
            boolean duplicateValuesValue2 = duplicateValues;
            String archiveNameValue2 = this.archiveName;
            ResourcePathTranslator resourcePathTranslator3 = resourcePathTranslator1;
            EnumerableMap enumerableMap4 = enumerableMap1;
            EnumerableMap enumerableMap5 = enumerableMap;
            PrintWriter printWriter2 = printWriter;
            manifestEntrySection.writeSection(printWriter2, enumerableMap5, enumerableMap4, resourcePathTranslator3, archiveNameValue2, duplicateValuesValue2, messageReporter3);
            bb = ++elementIndex;
        }

        printWriter.flush();
        zipEntryWriter.writeEntry(long1);
    }

    public void collectEntryPointClasses(String string, Map map1, Map map2, Map map3) {
        this.mainSection.collectEntryPointClasses(string, map1, map2, map3);
        int elementIndex = 0;
        int bb = 0;

        for (List list1 = this.entrySections; bb < list1.size(); list1 = this.entrySections) {
            ((ManifestEntrySection) this.entrySections.get(elementIndex)).collectEntryPointClasses();
            bb = ++elementIndex;
        }
    }

    public boolean hasMicroEditionConfiguration() {
        return this.mainSection.hasMicroEditionConfiguration();
    }

    public ArchiveManifest(ZipFile zipFile1, ZipEntry zipEntry1) throws ZkmException, IOException {
        InputStream inputStream1 = null;
        BufferedReader bufferedReader = null;
        this.archiveName = zipFile1.getName();
        if (zipEntry1.getMethod() == 8) {
            this.compressed = true;
        } else {
            this.compressed = false;
        }

        try {
            inputStream1 = zipFile1.getInputStream(zipEntry1);
            bufferedReader = ZkmFileUtils.createBomAwareReader(inputStream1, "UTF-8", (ObservableHolder) null);
            this.mainSection = new ManifestHandler(bufferedReader);
            boolean endOfStream = this.mainSection.isEndOfStream();

            while (!endOfStream) {
                ManifestEntrySection manifestEntrySection = new ManifestEntrySection(bufferedReader);
                if (!manifestEntrySection.hasNoHeaders()) {
                    this.entrySections.add(manifestEntrySection);
                }

                endOfStream = manifestEntrySection.isEndOfStream();
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException iOException1) {
                }
            } else if (inputStream1 != null) {
                try {
                    inputStream1.close();
                } catch (IOException iOException) {
                }
            }
        }
    }

    public static boolean isVersionedEntryPath(String string, MutableInt mutableInt) {
        mutableInt.setValue(-1);
        if (string.startsWith("META-INF/versions/") && string.length() > "META-INF/versions/".length()) {
            String string1 = string.substring("META-INF/versions/".length());
            int endIndex = string1.indexOf(47);
            if (endIndex > 0) {
                String string2 = string1.substring(0, endIndex);

                try {
                    int value = Integer.parseInt(string2);
                    mutableInt.setValue(value);
                    return true;
                } catch (NumberFormatException numberFormatException) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean hasDuplicateValues(EnumerableMap enumerableMap) {
        boolean duplicateValues = false;
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(enumerableMap.size()));
        Iterator iterator = enumerableMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (!hashSet.add(entry.getValue())) {
                duplicateValues = true;
                break;
            }
        }

        return duplicateValues;
    }

    public String getMainAttribute(String key) {
        return this.mainSection.getUnwrappedHeaderValue(key);
    }
}
