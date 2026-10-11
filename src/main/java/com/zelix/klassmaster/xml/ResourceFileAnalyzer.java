package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.archive.ArchiveManifest;
import com.zelix.klassmaster.archive.ArchiveZipFile;
import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.archive.JadFile;
import com.zelix.klassmaster.archive.ServiceProviderFile;
import com.zelix.klassmaster.archive.SourceArchive;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceFileAnalyzer {
    public Map openZipFiles = ZkmUtils.createHashMap();
    public SourceArchive[] sourceArchives;
    public Set excludedArchives;
    public final ResourceFileIndex resourceFileIndex;
    public final ClassMemberLookup classMemberLookup;
    public final ClassResolver classResolver;
    public MessageReporter messageReporter;

    public ResourceFileAnalyzer(
            SourceArchive[] sourceArchives1,
            Set set1,
            TwoKeyMap twoKeyMap,
            TwoKeyMap twoKeyMap1,
            TwoKeyMap twoKeyMap2,
            Enumeration enumeration,
            Enumeration enumeration1,
            ResourceFileIndex resourceFileIndex1,
            Enumeration enumeration2,
            Enumeration enumeration3,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            MessageReporter messageReporter1,
            Map map1,
            Map map2,
            Map map3,
            TwoKeyMap twoKeyMap3,
            Map map4
    ) throws ZkmException, IOException {
        this.sourceArchives = sourceArchives1;
        this.excludedArchives = set1;
        this.resourceFileIndex = resourceFileIndex1;
        this.classMemberLookup = classMemberLookup1;
        this.classResolver = classResolver1;
        this.messageReporter = messageReporter1;

        try {
            this.openArchives();
            this.analyzeJadFiles(enumeration, map1);
            this.analyzeXmlFiles(enumeration1, map1, map2, map3, twoKeyMap3);
            this.analyzeYamlFiles(enumeration2, map1);
            this.analyzePropertiesFiles(enumeration3, map1);
            this.analyzeArchiveEntries(map1, map2, map3, twoKeyMap3, map4, twoKeyMap, twoKeyMap1, twoKeyMap2);
        } finally {
            this.closeZipFiles();
        }
    }

    public void analyzeYamlFiles(Enumeration enumeration, Map map1) throws ZkmException, IOException {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                String qualifiedName = inputFileLocation.getQualifiedName();

                try {
                    FileInputStream fileInputStream = new FileInputStream(inputFileLocation.getFile());
                    BufferedReader bufferedReader = ZkmFileUtils.createBomAwareReader(fileInputStream);
                    new ResourceClassRefScanner(bufferedReader, qualifiedName, map1);
                } catch (IOException iOException) {
                    this.messageReporter.reportError("FILE ERROR:", "Unexpected error analyzing YAML file '" + qualifiedName + "' (B) : " + iOException.getMessage());
                }
            }
        }
    }

    public void closeZipFiles() {
        Iterator iterator = this.openZipFiles.values().iterator();

        while (iterator.hasNext()) {
            ZipFile zipFile1 = (ZipFile) iterator.next();
            String name = zipFile1.getName();

            try {
                zipFile1.close();
            } catch (IOException iOException) {
                this.messageReporter.reportWarning("FILE ERROR (4)", "FILE ERROR (4) '" + name + "' : " + iOException.getMessage());
            }
        }
    }

    public void analyzePropertiesFiles(Enumeration enumeration, Map map1) throws ZkmException, IOException {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                String qualifiedName = inputFileLocation.getQualifiedName();

                try {
                    FileInputStream fileInputStream = new FileInputStream(inputFileLocation.getFile());
                    BufferedReader bufferedReader = ZkmFileUtils.createBomAwareReader(fileInputStream);
                    new PropertiesClassRefScanner(bufferedReader, qualifiedName, map1);
                } catch (IOException iOException) {
                    this.messageReporter.reportError("FILE ERROR:", "Unexpected error analyzing PROPERTIES file '" + qualifiedName + "' (C) : " + iOException.getMessage());
                }
            }
        }
    }

    public void analyzeJadFiles(Enumeration enumeration, Map map1) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                JadFile jadFile1 = (JadFile) enumeration.nextElement();
                jadFile1.collectMidletClasses(jadFile1.getJadFilePath(), map1);
            }
        }
    }

    public void analyzeXmlFiles(Enumeration enumeration, Map map1, Map map2, Map map3, TwoKeyMap twoKeyMap) throws ZkmException, IOException {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                String qualifiedName = inputFileLocation.getQualifiedName();

                try {
                    ObservableHolder observableHolder = new ObservableHolder();
                    MutableInt mutableInt = new MutableInt(0);
                    MutableInt mutableInt1 = new MutableInt(0);
                    MutableInt mutableInt2 = new MutableInt(Integer.MAX_VALUE);
                    String string1 = XmlResourceProcessor.readFileContent(inputFileLocation.getFile(), observableHolder, mutableInt, mutableInt1, mutableInt2);
                    XmlContentHandler xmlContentHandler = XmlConfigFileHandler.createAnalysisHandler(
                            qualifiedName, string1, this.resourceFileIndex, this.classMemberLookup, this.classResolver, this.messageReporter
                    );
                    File file1 = inputFileLocation.getFile();
                    String value = (String) observableHolder.getValue();
                    XmlContentHandler xmlContentHandler1 = xmlContentHandler;
                    new XmlResourceReader(file1, mutableInt, mutableInt1, mutableInt2, value, xmlContentHandler1, map1, map2, map3, twoKeyMap);
                } catch (ZkmException zkmException) {
                    this.messageReporter.reportError("FILE ERROR:", "Invalid XML file '" + qualifiedName + "' (A) : " + zkmException.getMessage());
                } catch (IOException iOException) {
                    this.messageReporter.reportError("FILE ERROR:", "Error reading or writing XML file '" + qualifiedName + "' (A) : " + iOException.getMessage());
                } catch (Exception exception) {
                    this.messageReporter.reportError("FILE ERROR:", "Unexpected error analyzing XML file '" + qualifiedName + "' (A) : " + exception.getMessage());
                }
            }
        }
    }

    public void openArchives() {
        int sourceArchiveIndex = 0;
        int bb = 0;

        for (SourceArchive[] sourceArchives1 = this.sourceArchives; bb < sourceArchives1.length; sourceArchives1 = this.sourceArchives) {
            String filePath = this.sourceArchives[sourceArchiveIndex].getFilePath();

            try {
                ArchiveZipFile archiveZipFile = new ArchiveZipFile(filePath);
                this.openZipFiles.put(this.sourceArchives[sourceArchiveIndex], archiveZipFile);
            } catch (IOException iOException) {
                this.messageReporter.reportWarning("FILE ERROR (3)", "Couldn't open '" + filePath + "'. : " + iOException.getMessage());
            }

            bb = ++sourceArchiveIndex;
        }
    }

    public boolean isNestedArchiveEntry(ZipFile zipFile1, ZipEntry zipEntry1, MessageReporter messageReporter1) throws ZkmException, IOException {
        try {
            return ZkmFileUtils.isNestedArchiveEntry(zipFile1, zipEntry1);
        } catch (IOException iOException) {
            messageReporter1.reportError("FILE ERROR:", "File read error : " + iOException.getMessage());
            return false;
        }
    }

    public void analyzeArchiveEntries(
            Map map1, Map map2, Map map3, TwoKeyMap twoKeyMap, Map map4, TwoKeyMap twoKeyMap1, TwoKeyMap twoKeyMap2, TwoKeyMap twoKeyMap3
    ) throws ZkmException, IOException {
        Iterator iterator = this.openZipFiles.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            String name = sourceArchive1.getName();
            ZipFile zipFile1 = (ZipFile) entry.getValue();
            Map map5 = null;
            if (twoKeyMap1 != null) {
                map5 = twoKeyMap1.getInnerMap(sourceArchive1);
            }

            Map map6 = null;
            if (twoKeyMap2 != null) {
                map6 = twoKeyMap2.getInnerMap(sourceArchive1);
            }

            Map map7 = null;
            Enumeration enumeration1;
            if (twoKeyMap3 != null) {
                map7 = twoKeyMap3.getInnerMap(sourceArchive1);
                enumeration1 = zipFile1.entries();
            } else {
                enumeration1 = zipFile1.entries();
            }

            Enumeration enumeration = enumeration1;

            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry1 = (ZipEntry) enumeration.nextElement();
                String nameValue = zipEntry1.getName();
                if ((map7 == null || !map7.containsKey(nameValue))
                        && (this.excludedArchives == null || !this.excludedArchives.contains(sourceArchive1))
                        && (map5 == null || !map5.containsKey(nameValue))) {
                    String nameValue2;
                    String suffix;
                    if (map6 != null) {
                        if (map6.containsKey(nameValue)) {
                            continue;
                        }

                        nameValue2 = nameValue;
                        suffix = ".class";
                    } else {
                        nameValue2 = nameValue;
                        suffix = ".class";
                    }

                    if (!nameValue2.endsWith(suffix) && !this.isNestedArchiveEntry(zipFile1, zipEntry1, this.messageReporter)) {
                        String string2 = nameValue.toUpperCase();
                        if (!zipEntry1.isDirectory() && string2.equals("META-INF/MANIFEST.MF")) {
                            ArchiveManifest archiveManifest = sourceArchive1.getManifest();
                            if (archiveManifest != null) {
                                archiveManifest.collectEntryPointClasses(name, map1, map3, map4);
                            }
                        } else if (!zipEntry1.isDirectory()
                                && string2.startsWith("META-INF/services/".toUpperCase())
                                && nameValue.length() > ServiceProviderFile.SERVICES_PREFIX_LENGTH) {
                            try {
                                ServiceProviderFile serviceProviderFile = new ServiceProviderFile(zipFile1, zipEntry1);
                                serviceProviderFile.collectReferencedClasses(name, map1);
                            } catch (IOException iOException3) {
                                this.messageReporter.reportError("FILE ERROR:", "Error reading or writing service provider in '" + name + "' : " + iOException3);
                            }
                        } else if (!zipEntry1.isDirectory() && ZkmFileUtils.isXmlFileName(nameValue)) {
                            try {
                                ObservableHolder observableHolder = new ObservableHolder();
                                MutableInt mutableInt1 = new MutableInt(0);
                                MutableInt mutableInt2 = new MutableInt(0);
                                MutableInt mutableInt = new MutableInt(Integer.MAX_VALUE);
                                String string4 = XmlResourceProcessor.readZipEntryContent(zipFile1, zipEntry1, observableHolder, mutableInt1, mutableInt2, mutableInt);
                                XmlContentHandler xmlContentHandler = XmlConfigFileHandler.createAnalysisHandler(
                                        ZkmFileUtils.describeEntryWithOriginal(zipFile1, zipEntry1),
                                        string4,
                                        this.resourceFileIndex,
                                        this.classMemberLookup,
                                        this.classResolver,
                                        this.messageReporter
                                );
                                String value = (String) observableHolder.getValue();
                                XmlContentHandler xmlContentHandler1 = xmlContentHandler;
                                new XmlResourceReader(
                                        zipFile1, zipEntry1, mutableInt1, mutableInt2, mutableInt, value, xmlContentHandler1, map1, map2, map3, twoKeyMap
                                );
                            } catch (ZkmException zkmException) {
                                this.messageReporter
                                        .reportError("FILE ERROR:", "Invalid XML file '" + nameValue + "' in '" + name + "' : " + zkmException.getMessage());
                            } catch (IOException iOException2) {
                                this.messageReporter.reportError("FILE ERROR:", "Error reading or writing '" + nameValue + "' in '" + name + "' : " + iOException2);
                            }
                        } else if (!zipEntry1.isDirectory() && ZkmFileUtils.isYamlFileName(nameValue)) {
                            String entryPath = ZkmFileUtils.getEntryPath(zipFile1, zipEntry1);

                            try {
                                InputStream inputStream2 = zipFile1.getInputStream(zipEntry1);
                                BufferedReader bufferedReader1 = ZkmFileUtils.createBomAwareReader(inputStream2);
                                new ResourceClassRefScanner(bufferedReader1, entryPath, map1);
                            } catch (IOException iOException1) {
                                this.messageReporter
                                        .reportError("FILE ERROR:", "Unexpected error analyzing YAML file '" + entryPath + "' (A) : " + iOException1.getMessage());
                            }
                        } else if (!zipEntry1.isDirectory() && ZkmFileUtils.isPropertiesFileName(nameValue)) {
                            String entryPathValue = ZkmFileUtils.getEntryPath(zipFile1, zipEntry1);

                            try {
                                InputStream inputStream1 = zipFile1.getInputStream(zipEntry1);
                                BufferedReader bufferedReader = ZkmFileUtils.createBomAwareReader(inputStream1);
                                new PropertiesClassRefScanner(bufferedReader, entryPathValue, map1);
                            } catch (IOException iOException) {
                                this.messageReporter
                                        .reportError("FILE ERROR:", "Unexpected error analyzing PROPERTIES file '" + entryPathValue + "' (A) : " + iOException.getMessage());
                            }
                        } else if (!zipEntry1.isDirectory()) {
                        }
                    }
                }
            }
        }
    }
}
