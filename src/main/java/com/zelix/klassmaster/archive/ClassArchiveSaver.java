package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ModuleInfoClass;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.CommonSuperTypeResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.MethodAnalysisException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.obfuscator.exclude.FixedClassesExclusionSet;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.SetValuedMap;
import com.zelix.klassmaster.util.Sha256Digester;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.TwoKeySetMultiMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.xml.PropertiesFileRewriter;
import com.zelix.klassmaster.xml.ResourceFileIndex;
import com.zelix.klassmaster.xml.ResourcePathTranslator;
import com.zelix.klassmaster.xml.TextResourceRewriter;
import com.zelix.klassmaster.xml.XmlConfigFileHandler;
import com.zelix.klassmaster.xml.XmlContentHandler;
import com.zelix.klassmaster.xml.XmlResourceProcessor;
import com.zelix.klassmaster.xml.XmlZipEntryRewriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ClassArchiveSaver {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public final List topLevelArchives = new ArrayList();
    public final Map openZipFiles = new TreeMap();
    public final Map outputArchiveFiles = ZkmUtils.createHashMap();
    public final Map outputStreamNames = ZkmUtils.createHashMap();
    public final Map archiveOutputStreams = ZkmUtils.createHashMap();
    public final SetMultiMap writtenEntries = new SetMultiMap();
    public final ProgramClass[] classes;
    public final SourceArchive[] sourceArchives;
    public final Set skippedNestedArchives;
    public final TwoKeyMap skippedClassFiles;
    public final TwoKeyMap skippedXmlFiles;
    public final TwoKeyMap skippedYamlFiles;
    public final TwoKeyMap skippedPropertiesFiles;
    public final TwoKeyMap filteredOutEntries;
    public final HashMap jadFiles;
    public final InputFileLocation[] xmlFileLocations;
    private final InputFileLocation[] yamlFileLocations;
    private final InputFileLocation[] propertiesFileLocations;
    public final HashMap archiveSaveTargets;
    public final HashMap jadSaveTargets;
    public final HashMap xmlSaveTargets;
    private final HashMap yamlSaveTargets;
    public final EnumerableMap packageRenameMap;
    public final EnumerableMap classRenameMap;
    public final EnumerableMap originalClassNames;
    public final ReadOnlyMultiMap fieldRenameMap;
    public final ReadOnlyMultiMap methodRenameMap;
    public final ClassMemberLookup classMemberLookup;
    public final CommonSuperTypeResolver commonSuperTypeResolver;
    public final int compressionMode;
    public final boolean deleteEmptyDirectories;
    public final boolean removeXmlComments;
    public final MessageReporter messageReporter;
    public final ScriptEnvironment scriptEnvironment;
    public final FixedClassesExclusionSet fixedClassesExclusions;
    public final ResourceFileIndex resourceFileIndex;
    public final ObservableHolder statusHolder;
    public final File saveLocation;
    public final Long entryTimestamp;
    public final boolean saveToSingleArchive;
    public ZipOutputStream singleArchiveOutput;
    public final ResourcePathTranslator resourcePathTranslator;

    public void writeServiceProviderFiles() throws ZkmException, IOException {
        ListMultimap listMultimap = new ListMultimap();
        Iterator iterator = this.archiveOutputStreams.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            listMultimap.addValue(entry.getValue(), entry.getKey());
        }

        iterator = listMultimap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            ZipOutputStream zipOutputStream1 = (ZipOutputStream) entry1.getKey();
            List list1 = (List) entry1.getValue();
            ListMultimap listMultimap1 = new ListMultimap();
            Iterator iterator1 = list1.iterator();

            while (iterator1.hasNext()) {
                SourceArchive sourceArchive1 = (SourceArchive) iterator1.next();
                ZipFile zipFile1 = (ZipFile) this.openZipFiles.get(sourceArchive1);
                Enumeration<? extends ZipEntry> enumeration = zipFile1.entries();

                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry1 = (ZipEntry) enumeration.nextElement();
                    String name = zipEntry1.getName();
                    String string1 = name.toUpperCase();
                    if (string1.startsWith("META-INF/services/".toUpperCase()) && name.length() > ServiceProviderFile.SERVICES_PREFIX_LENGTH) {
                        try {
                            ServiceProviderFile serviceProviderFile = new ServiceProviderFile(zipFile1, zipEntry1);
                            String serviceName = serviceProviderFile.getServiceName();
                            listMultimap1.addValue(serviceName, serviceProviderFile);
                        } catch (IOException iOException) {
                            this.messageReporter.reportError("FILE ERROR:", "Error reading service provider in '" + zipFile1.getName() + "' : " + iOException);
                        }
                    }
                }
            }

            iterator1 = listMultimap1.entrySet().iterator();

            while (iterator1.hasNext()) {
                Entry entry2 = (Entry) iterator1.next();
                String key = (String) entry2.getKey();
                List list2 = (List) entry2.getValue();
                Iterator iterator2 = list2.iterator();
                ServiceProviderFile serviceProviderFile1 = (ServiceProviderFile) iterator2.next();

                while (iterator2.hasNext()) {
                    ServiceProviderFile serviceProviderFile2 = (ServiceProviderFile) iterator2.next();
                    serviceProviderFile1.mergeFrom(serviceProviderFile2);
                }

                String translatedEntryName = serviceProviderFile1.getTranslatedEntryName(this.classRenameMap);
                if (!this.isAlreadyWritten(zipOutputStream1, translatedEntryName)) {
                    boolean compressed = this.compressionMode == 1 || this.compressionMode == 3 && serviceProviderFile1.isCompressed();

                    try {
                        serviceProviderFile1.writeTranslated(zipOutputStream1, this.entryTimestamp, this.classRenameMap, compressed);
                    } catch (IOException exception) {
                        this.messageReporter.reportError("FILE ERROR:", "Error writing service provider 'META-INF/services/" + key + "' : " + exception);
                    }
                    this.writtenEntries.addValue(zipOutputStream1, translatedEntryName);
                }
            }
        }
    }

    public void writeGeneratedClasses(SetValuedMap setValuedMap, Map map1, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) throws ZkmException, IOException {
        if (!setValuedMap.isEmpty()) {
            int lengthValue = this.sourceArchives.length;
            TwoKeySetMultiMap twoKeySetMultiMap = new TwoKeySetMultiMap(lengthValue, setValuedMap.getKeyCount(), 7);
            HashSet hashSet = ZkmUtils.createHashSet();
            Iterator iterator = setValuedMap.keySet().iterator();

            while (iterator.hasNext()) {
                ProgramClass programClass1 = (ProgramClass) iterator.next();
                List list1 = setValuedMap.getValueList(programClass1);
                Iterator iterator1 = list1.iterator();

                while (iterator1.hasNext()) {
                    ProgramClass programClass2 = (ProgramClass) iterator1.next();
                    Enumeration enumeration = programClass2.enumerateInputLocations();

                    while (enumeration.hasMoreElements()) {
                        InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                        SourceArchive sourceArchive1 = inputFileLocation.getSourceArchive();
                        if (sourceArchive1 != null) {
                            String string = (String) map1.get(inputFileLocation);
                            twoKeySetMultiMap.addValue(sourceArchive1, programClass1, string);
                        } else {
                            hashSet.add(programClass1);
                        }
                    }
                }
            }

            iterator = hashSet.iterator();

            while (iterator.hasNext()) {
                ProgramClass programClass4 = (ProgramClass) iterator.next();
                if (!this.saveToSingleArchive) {
                    this.writeClassFile(programClass4, (byte[]) null);
                } else {
                    String classEntryName = this.getClassEntryName(programClass4);
                    boolean bl1 = this.compressionMode == 1;
                    if (!this.isAlreadyWritten(this.singleArchiveOutput, classEntryName)) {
                        ZipOutputStream zipOutputStream4 = this.singleArchiveOutput;
                        File file4 = this.saveLocation;
                        String absolutePath = this.saveLocation.getAbsolutePath();
                        Boolean boolean1 = bl1;
                        String absolutePathValue = absolutePath;
                        File file1 = file4;
                        ZipOutputStream zipOutputStream1 = zipOutputStream4;
                        String classEntryNameValue = classEntryName;
                        this.writeClassEntry(programClass4, (byte[]) null, classEntryNameValue, zipOutputStream1, file1, absolutePathValue, boolean1);
                    }
                }
            }

            boolean bl = this.compressionMode == 1 || this.compressionMode == 3;
            Iterator iterator3 = twoKeySetMultiMap.entrySet().iterator();

            while (iterator3.hasNext()) {
                Entry entry = (Entry) iterator3.next();
                SourceArchive sourceArchive2 = (SourceArchive) entry.getKey();
                ZipFile zipFile1 = (ZipFile) this.openZipFiles.get(sourceArchive2);
                ZipOutputStream zipOutputStream3 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive2);
                SetMultiMap setMultiMap2 = (SetMultiMap) entry.getValue();
                Iterator iterator4 = setMultiMap2.entrySet().iterator();

                while (iterator4.hasNext()) {
                    Entry entry1 = (Entry) iterator4.next();
                    ProgramClass programClass3 = (ProgramClass) entry1.getKey();
                    Iterator iterator2 = ((Set) entry1.getValue()).iterator();

                    while (iterator2.hasNext()) {
                        String string1 = (String) iterator2.next();
                        String string2 = string1 + programClass3.getClassName() + ".class";
                        if (!this.isAlreadyWritten(zipOutputStream3, string2)) {
                            File file3 = (File) this.outputArchiveFiles.get(sourceArchive2);
                            String name = zipFile1.getName();
                            Boolean boolean2 = bl;
                            String nameValue = name;
                            File file2 = file3;
                            ZipOutputStream zipOutputStream2 = zipOutputStream3;
                            String entryName = string2;
                            this.writeClassEntry(programClass3, (byte[]) null, entryName, zipOutputStream2, file2, nameValue, boolean2);
                            this.recordEntryPath(string2, sourceArchive2, setMultiMap, setMultiMap1);
                        }
                    }
                }
            }
        }
    }

    public void writeClasses(SetValuedMap setValuedMap, Map map1, SetMultiMap setMultiMap, Map map2, SetMultiMap setMultiMap1, SetMultiMap setMultiMap2) throws ZkmException, IOException {
        Map map3 = ZkmUtils.createHashMap(this.classes.length);
        if (HiddenOptionFlags.USE_PARALLEL && HiddenOptionFlags.PROCESSOR_COUNT >= 2) {
            List<ObjectPair> list1 = Arrays.stream(this.classes).map(programClass4 -> new ObjectPair(programClass4, null)).collect(Collectors.toList());
            Vector vector = new Vector();
            map3 = list1.parallelStream()
                    .map(
                            objectPair -> {
                                try {
                                    try {
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                                        ((ProgramClass) objectPair.getFirst())
                                                .writeClassFile(dataOutputStream, this.commonSuperTypeResolver, this.classMemberLookup, this.scriptEnvironment);
                                        dataOutputStream.flush();
                                        objectPair.setSecond(byteArrayOutputStream.toByteArray());
                                        dataOutputStream.close();
                                    } catch (IOException iOException) {
                                        vector.add(iOException);
                                    } catch (ZkmProcessingException zkmProcessingException) {
                                        vector.add(zkmProcessingException);
                                    }

                                    return objectPair;
                                } catch (Throwable throwable) {
                                    throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
                                }
                            }
                    )
                    .collect(Collectors.toMap(ObjectPair::getFirst, ObjectPair::getSecond));
            if (!vector.isEmpty()) {
                Iterator iterator = vector.iterator();

                while (iterator.hasNext()) {
                    Exception exception = (Exception) iterator.next();
                    if (exception instanceof MethodAnalysisException) {
                        this.messageReporter
                                .reportFatalError(
                                        "ERROR:",
                                        "Method '"
                                                + ((MethodAnalysisException) exception).getMethodName()
                                                + "' could not be analyzed (I). Consider excluding it from Flow and Exception Obfuscation."
                                );
                    } else {
                        this.messageReporter.reportFatalError("ERROR:", exception.getMessage());
                    }
                }
            }
        }

        int classeIndex = 0;
        int classeIndexValue = classeIndex;

        for (ProgramClass[] programClass3 = this.classes; classeIndexValue < programClass3.length; programClass3 = this.classes) {
            ProgramClass programClass1 = this.classes[classeIndex];
            String string4 = (String) ZkmUtils.mapOrSelf(programClass1.getClassName(), this.originalClassNames);
            string4 = ZkmUtils.slashesToDots(string4);
            if (!programClass1.isGenerated()) {
                Enumeration enumeration = programClass1.enumerateInputLocations();

                while (enumeration.hasMoreElements()) {
                    InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                    ZipFile zipFile1 = null;
                    ZipEntry zipEntry1 = null;
                    ZipOutputStream zipOutputStream1 = null;
                    File file1 = null;
                    boolean boolean1Value = false;
                    if (!inputFileLocation.isArchiveEntry()) {
                        if (inputFileLocation.isPlainFile() && this.saveToSingleArchive) {
                            zipOutputStream1 = this.singleArchiveOutput;
                            boolean1Value = this.compressionMode == 1;
                        }
                    } else {
                        SourceArchive sourceArchive1 = inputFileLocation.getSourceArchive();
                        zipFile1 = (ZipFile) this.openZipFiles.get(sourceArchive1);
                        zipEntry1 = zipFile1.getEntry(inputFileLocation.getName());
                        zipOutputStream1 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive1);
                        boolean1Value = this.compressionMode == 1 || this.compressionMode == 3 && zipEntry1.getMethod() == 8;
                        file1 = (File) this.outputArchiveFiles.get(sourceArchive1);
                    }

                    boolean bl1 = this.fixedClassesExclusions != null
                            && this.fixedClassesExclusions.isMatchedClass(programClass1)
                            && HiddenOptionFlags.FIXED_TOTALLY_UNCHANGED;
                    if (bl1 && !this.fixedClassesExclusions.isFixedClassBroken(programClass1)) {
                        if (this.scriptEnvironment.isVerbose()) {
                            this.scriptEnvironment
                                    .getLogWriter()
                                    .println("\tClass '" + string4 + "' saved unchanged because '" + "ZKM_FIXED_TOTALLY_UNCHANGED" + "' set.");
                        }

                        if (!inputFileLocation.isPlainFile()) {
                            if (zipOutputStream1 == null) {
                                this.copyUnchangedClassFile(programClass1, inputFileLocation.getName());
                            } else if (!this.isAlreadyWritten(zipOutputStream1, zipEntry1.getName())) {
                                this.copyZipEntry(zipFile1, zipEntry1, zipOutputStream1, boolean1Value);
                                setMultiMap.addValue(zipOutputStream1, zipEntry1.getName());
                                if (programClass1.hasVersionedVariants()) {
                                    List list3 = programClass1.getVersionedVariants();
                                    Iterator iterator2 = list3.iterator();

                                    while (iterator2.hasNext()) {
                                        ClassFileBase classFileBase1 = (ClassFileBase) iterator2.next();
                                        InputFileLocation inputFileLocation2 = classFileBase1.getInputLocation();
                                        ZipEntry zipEntry2 = zipFile1.getEntry(inputFileLocation2.getName());
                                        this.copyZipEntry(zipFile1, zipEntry2, zipOutputStream1, boolean1Value);
                                        setMultiMap.addValue(zipOutputStream1, zipEntry2.getName());
                                    }
                                }
                            }
                        } else if (this.saveToSingleArchive) {
                            String classEntryName = this.getClassEntryName(programClass1);
                            if (!this.isAlreadyWritten(zipOutputStream1, classEntryName)) {
                                this.writeFileEntry(inputFileLocation.getName(), classEntryName, zipOutputStream1, boolean1Value);
                            }
                        } else {
                            this.copyUnchangedClassFile(programClass1, inputFileLocation.getName());
                        }
                    } else {
                        if (bl1) {
                            String brokenReason = this.fixedClassesExclusions.getBrokenReason(programClass1);
                            this.scriptEnvironment
                                    .logError(
                                            "'Fixed' class '"
                                                    + string4
                                                    + "' NOT saved unchanged in spite of '"
                                                    + "ZKM_FIXED_TOTALLY_UNCHANGED"
                                                    + "' being set because : '"
                                                    + brokenReason
                                                    + "'."
                                    );
                        }

                        byte[] classBytesValue = (byte[]) map3.get(programClass1);
                        if (!inputFileLocation.isPlainFile()) {
                            if (zipOutputStream1 != null) {
                                String name = inputFileLocation.getName();
                                String entryName = (String) map1.get(inputFileLocation);
                                if (!this.isAlreadyWritten(zipOutputStream1, entryName)) {
                                    String nameValue = zipFile1.getName();
                                    Boolean boolean1 = boolean1Value;
                                    this.writeClassEntry(programClass1, classBytesValue, entryName, zipOutputStream1, file1, nameValue, boolean1);
                                    setMultiMap.addValue(zipOutputStream1, name);
                                    if (programClass1.hasVersionedVariants()) {
                                        List list2 = programClass1.getVersionedVariants();
                                        Iterator iterator1 = list2.iterator();

                                        while (iterator1.hasNext()) {
                                            ClassFileBase classFileBase = (ClassFileBase) iterator1.next();
                                            InputFileLocation inputFileLocation1 = classFileBase.getInputLocation();
                                            String entryNameValue = (String) map1.get(inputFileLocation1);
                                            this.writeClassEntry(
                                                    (ProgramClass) classFileBase, (byte[]) map3.get(classFileBase), entryNameValue, zipOutputStream1, file1, zipFile1.getName(), boolean1Value
                                            );
                                            setMultiMap.addValue(zipOutputStream1, entryNameValue);
                                        }
                                    }
                                }
                            } else {
                                this.writeClassFile(this.classes[classeIndex], classBytesValue);
                            }
                        } else if (this.saveToSingleArchive) {
                            String classEntryNameValue = this.getClassEntryName(this.classes[classeIndex]);
                            if (!this.isAlreadyWritten(zipOutputStream1, classEntryNameValue)) {
                                ProgramClass programClass2 = this.classes[classeIndex];
                                File file2 = this.saveLocation;
                                String absolutePath = this.saveLocation.getAbsolutePath();
                                Boolean boolean2 = boolean1Value;
                                this.writeClassEntry(programClass2, classBytesValue, classEntryNameValue, zipOutputStream1, file2, absolutePath, boolean2);
                            }
                        } else {
                            this.writeClassFile(this.classes[classeIndex], classBytesValue);
                        }
                    }
                }
            }

            classeIndexValue = ++classeIndex;
        }

        this.writeGeneratedClasses(setValuedMap, map2, setMultiMap1, setMultiMap2);
    }

    
    
    public void rewriteTextResourceFiles(InputFileLocation[] inputFileLocations, HashMap hashMap, boolean bl) throws ZkmException, IOException {
        if (inputFileLocations != null) {
            for (InputFileLocation inputFileLocation : inputFileLocations) {
                String qualifiedName = inputFileLocation.getQualifiedName();
                File file1 = inputFileLocation.getFile();
                String expectedFinalSha256 = inputFileLocation.getExpectedFinalSha256();
                MutableInt mutableInt = new MutableInt(0);
                MutableInt mutableInt1 = new MutableInt(0);
                MutableInt mutableInt2 = new MutableInt(Integer.MAX_VALUE);
                FileInputStream fileInputStream = null;
                PushbackInputStream pushbackInputStream = null;
                boolean bl4 = false ;

                try {
                    bl4 = true;
                    fileInputStream = new FileInputStream(inputFileLocation.getFile());
                    pushbackInputStream = new PushbackInputStream(fileInputStream, 4);
                    String string2 = ZkmFileUtils.detectBomEncoding(ZkmFileUtils.peekBytes(pushbackInputStream), mutableInt, mutableInt1, mutableInt2);
                    pushbackInputStream.close();
                    fileInputStream = null;
                    pushbackInputStream = null;
                    if (string2 == null && bl) {
                        if (HiddenOptionFlags.PROPERTIES_ENCODING != null) {
                            string2 = HiddenOptionFlags.PROPERTIES_ENCODING.trim();
                        } else {
                            string2 = "UTF-8";
                        }
                    }

                    if (this.saveToSingleArchive) {
                        ZipOutputStream zipOutputStream3 = this.singleArchiveOutput;
                        if (this.isAlreadyWritten(zipOutputStream3, inputFileLocation.getName())) {
                            bl4 = false;
                        } else {
                            ObservableHolder observableHolder1 = new ObservableHolder();
                            if (ZkmFileUtils.isYamlFileName(inputFileLocation.getName())) {
                                boolean bl6;
                                EnumerableMap enumerableMap4;
                                if (this.compressionMode == 1) {
                                    bl6 = true;
                                    enumerableMap4 = this.classRenameMap;
                                } else {
                                    bl6 = false;
                                    enumerableMap4 = this.classRenameMap;
                                }

                                ObservableHolder observableHolder = observableHolder1;
                                EnumerableMap enumerableMap = enumerableMap4;
                                boolean bl1 = bl6;
                                ObservableHolder observableHolder2 = observableHolder;
                                EnumerableMap enumerableMap1 = enumerableMap;
                                boolean compressed = bl1;
                                ZipOutputStream zipOutputStream1 = zipOutputStream3;
                                String string3 = string2;
                                MutableInt mutableInt3 = mutableInt2;
                                MutableInt mutableInt4 = mutableInt1;
                                MutableInt mutableInt5 = mutableInt;
                                File file3 = file1;
                                new TextResourceRewriter(
                                        file3, mutableInt5, mutableInt4, mutableInt3, string3, zipOutputStream1, compressed, enumerableMap1, observableHolder2
                                );
                            } else {
                                boolean bl5;
                                EnumerableMap enumerableMap3;
                                if (this.compressionMode == 1) {
                                    bl5 = true;
                                    enumerableMap3 = this.classRenameMap;
                                } else {
                                    bl5 = false;
                                    enumerableMap3 = this.classRenameMap;
                                }

                                ObservableHolder observableHolder3 = observableHolder1;
                                EnumerableMap enumerableMap2 = enumerableMap3;
                                boolean compressedValue = bl5;
                                ZipOutputStream zipOutputStream2 = zipOutputStream3;
                                String string4 = string2;
                                MutableInt mutableInt6 = mutableInt2;
                                MutableInt mutableInt7 = mutableInt1;
                                MutableInt mutableInt8 = mutableInt;
                                File file4 = file1;
                                new PropertiesFileRewriter(
                                        file4, mutableInt8, mutableInt7, mutableInt6, string4, zipOutputStream2, compressedValue, enumerableMap2, observableHolder3
                                );
                            }

                            if (expectedFinalSha256 != null) {
                                BufferedInputStream bufferedInputStream1 = new BufferedInputStream(new ByteArrayInputStream((byte[]) observableHolder1.getValue()));
                                ScriptEnvironment scriptEnvironment1 = this.scriptEnvironment;
                                String expectedFinalSha256Value = expectedFinalSha256;
                                BufferedInputStream bufferedInputStream = bufferedInputStream1;
                                this.verifyStreamSha256(file1, bufferedInputStream, expectedFinalSha256Value, scriptEnvironment1);
                            }

                            this.writtenEntries.addValue(zipOutputStream3, inputFileLocation.getName());
                            bl4 = false;
                        }
                        continue;
                    }

                    File file2;
                    if (hashMap != null && hashMap.containsKey(inputFileLocation)) {
                        file2 = (File) hashMap.get(inputFileLocation);
                    } else {
                        file2 = new File(this.saveLocation, file1.getName());
                    }

                    if (ZkmFileUtils.isYamlFileName(file1.getName())) {
                        new TextResourceRewriter(file1, mutableInt, mutableInt1, mutableInt2, string2, file2, this.classRenameMap);
                    } else {
                        new PropertiesFileRewriter(file1, mutableInt, mutableInt1, mutableInt2, string2, file2, this.classRenameMap);
                    }

                    if (this.scriptEnvironment.isVerbose()) {
                        this.scriptEnvironment
                                .getLogWriter()
                                .println(
                                        "\t"
                                                + (ZkmFileUtils.isYamlFileName(inputFileLocation.getName()) ? "YAML" : "PROPERTIES")
                                                + " file '"
                                                + file1.getAbsolutePath()
                                                + "' saved to '"
                                                + file2.getAbsolutePath()
                                                + "'"
                                                + (
                                                !HiddenOptionFlags.USE_PARALLEL && HiddenOptionFlags.DETERMINISTIC_OUTPUT
                                                        ? " with SHA256 hash '" + computeSha256(file2, this.scriptEnvironment) + "'"
                                                        : ""
                                        )
                                                + "."
                                );
                    }

                    if (expectedFinalSha256 != null) {
                        this.verifyFileSha256(file1, file2, expectedFinalSha256, this.scriptEnvironment);
                        bl4 = false;
                    } else {
                        bl4 = false;
                    }
                    continue;
                } catch (IOException iOException4) {
                    this.messageReporter
                            .reportError(
                                    "FILE ERROR:",
                                    "Error reading or writing "
                                            + (ZkmFileUtils.isYamlFileName(inputFileLocation.getName()) ? "YAML" : "PROPERTIES")
                                            + " file '"
                                            + qualifiedName
                                            + "' : "
                                            + iOException4
                            );
                    bl4 = false;
                } finally {
                    if (bl4) {
                        if (pushbackInputStream != null) {
                            try {
                                pushbackInputStream.close();
                            } catch (IOException iOException1) {
                            }
                        } else if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException iOException) {
                            }
                        }
                    }
                }

                if (pushbackInputStream != null) {
                    try {
                        pushbackInputStream.close();
                    } catch (IOException iOException3) {
                    }
                } else if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException iOException2) {
                    }
                }
            }
        }
    }

    public void verifyStreamSha256(File file1, BufferedInputStream bufferedInputStream, String string, ScriptEnvironment scriptEnvironment1) {
        try {
            Sha256Digester sha256Digester = new Sha256Digester(bufferedInputStream);
            String string1 = sha256Digester.getHexDigest().toLowerCase();
            if (!string.equals(string1)) {
                scriptEnvironment1.logFatalError(
                        "File '"
                                + file1.getAbsolutePath()
                                + "' was expected to be saved with the SHA256 hash '"
                                + string
                                + "' but the actual hash was '"
                                + string1
                                + "'"
                );
            }
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            scriptEnvironment1.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (9)");
        } catch (IOException iOException) {
            scriptEnvironment1.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + iOException + "' (10)");
        }
    }

    public String prepareClassFilePath(ProgramClass programClass1) {
        String string = programClass1.isModule() ? "module-info" : programClass1.getClassName();
        String string1 = string.substring(0, string.lastIndexOf(47) + 1);
        StringTokenizer stringTokenizer = new StringTokenizer(string1, "/");
        StringBuilder stringBuilder = new StringBuilder();

        for (boolean moreElements = stringTokenizer.hasMoreElements(); moreElements; moreElements = stringTokenizer.hasMoreElements()) {
            stringBuilder.append(stringTokenizer.nextToken());
            String string2 = this.saveLocation.getAbsolutePath() + FILE_SEPARATOR + stringBuilder.toString();
            File file1 = new File(string2);
            if (!file1.exists() && !file1.mkdirs()) {
                this.scriptEnvironment
                        .logWarning(
                                "Creation of directories '"
                                        + file1.getAbsolutePath()
                                        + "' failed while preparing to savie class '"
                                        + programClass1.getDottedClassName()
                                        + "'."
                        );
            }

            stringBuilder.append(stringTokenizer.hasMoreElements() ? FILE_SEPARATOR : "");
        }

        String string3 = string.substring(string.lastIndexOf(47) + 1);
        String string4 = this.saveLocation.getAbsolutePath() + (string1.length() > 0 ? FILE_SEPARATOR : "") + stringBuilder.toString();
        return string4 + FILE_SEPARATOR + string3 + ".class";
    }

    public void copyZipEntry(ZipFile zipFile1, ZipEntry zipEntry1, ZipOutputStream zipOutputStream1, Boolean boolean1) throws ZkmException, IOException {
        try {
            String name = zipEntry1.getName();
            InputStream inputStream1 = zipFile1.getInputStream(zipEntry1);
            new ZipEntryWriter(zipOutputStream1, name, boolean1, inputStream1, (int) zipEntry1.getSize()).writeEntry(this.entryTimestamp);
            this.writtenEntries.addValue(zipOutputStream1, name);
        } catch (IOException iOException) {
            this.messageReporter
                    .reportError(
                            "FILE ERROR:", "Couldn't save '" + zipEntry1.getName() + "' from '" + zipFile1.getName() + "' to new archive (D) : " + iOException.getMessage()
                    );
        }
    }

    public boolean isAlreadyWritten(ZipOutputStream zipOutputStream1, String string) {
        return this.checkDuplicateEntry(zipOutputStream1, string, true);
    }

    public void writeJadFiles() throws ZkmException, IOException {
        if (this.jadFiles != null) {
            Iterator iterator = this.jadFiles.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                JadFile jadFile1 = (JadFile) entry.getKey();
                SourceArchive sourceArchive1 = (SourceArchive) entry.getValue();
                if (sourceArchive1 != null) {
                    File file1 = (File) this.outputArchiveFiles.get(sourceArchive1);
                    if (file1 != null) {
                        jadFile1.setJarSize(file1.length());
                    }
                }

                File file2;
                if (this.jadSaveTargets != null && this.jadSaveTargets.containsKey(jadFile1)) {
                    file2 = (File) this.jadSaveTargets.get(jadFile1);
                    if (this.scriptEnvironment.isVerbose()) {
                        this.scriptEnvironment.getLogWriter().println("\tJAD file '" + jadFile1.getJadFilePath() + "' saved to '" + file2.getAbsolutePath() + "'.");
                    }
                } else {
                    file2 = new File(this.saveLocation, jadFile1.getJadFileName());
                }

                try {
                    EnumerableMap enumerableMap = this.packageRenameMap;
                    EnumerableMap enumerableMap1 = this.classRenameMap;
                    String filePath = sourceArchive1.getFilePath();
                    MessageReporter messageReporter1 = this.messageReporter;
                    String filePathValue = filePath;
                    jadFile1.writeTranslatedJad(file2, enumerableMap, enumerableMap1, filePathValue, messageReporter1);
                } catch (ZkmException zkmException) {
                    this.messageReporter.reportError("JAD FILE ERROR:", "JAD file write error : " + zkmException.getMessage());
                }
            }
        }
    }

    public final void copyUnchangedClassFile(ProgramClass programClass1, String string) throws ZkmException, IOException {
        String string1 = this.prepareClassFilePath(programClass1);

        try {
            if (this.scriptEnvironment.isVerbose()) {
                this.scriptEnvironment.getLogWriter().println("\tClass file '" + string + "' saved as '" + string1 + "'.");
            }

            ZkmFileUtils.copyFile(string, string1);
        } catch (IOException iOException) {
            this.messageReporter.reportError("FILE ERROR:", "Copying " + iOException.getMessage());
        }
    }

    
    
    public final void writeClassFile(ProgramClass programClass, byte[] classBytes) throws ZkmException, IOException {
        DataOutputStream output = null;
        String outputPath = this.prepareClassFilePath(programClass);
        try {
            if (this.scriptEnvironment.isVerbose()) {
                this.scriptEnvironment.getLogWriter().println("\tClass file '" + programClass.getLocationName() + "' saved as '" + outputPath + "'.");
            }
            FileOutputStream fileOutput = new FileOutputStream(outputPath);
            output = new DataOutputStream(new BufferedOutputStream(fileOutput, 2048));
            if (classBytes != null) {
                output.write(classBytes);
            } else {
                programClass.writeClassFile(output, this.commonSuperTypeResolver, this.classMemberLookup, this.scriptEnvironment);
            }
        } catch (IOException exception) {
            this.messageReporter.reportError("FILE ERROR:", "Saving " + exception.getMessage());
        } catch (MethodAnalysisException exception) {
            this.messageReporter.reportError(
                    "ERROR:", "Method '" + exception.getMethodName()
                            + "' could not be analyzed (H). Consider excluding it from Flow and Exception Obfuscation."
            );
        } catch (ZkmProcessingException exception) {
            this.messageReporter.reportError("ERROR:", exception.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException exception) {
                }
            }
        }
    }

    public void verifyFileSha256(File file1, File file2, String string, ScriptEnvironment scriptEnvironment1) {
        try {
            Sha256Digester sha256Digester = new Sha256Digester(file2.getAbsolutePath());
            String string1 = sha256Digester.getHexDigest().toLowerCase();
            if (!string.equals(string1)) {
                scriptEnvironment1.logFatalError(
                        "File '"
                                + file1.getAbsolutePath()
                                + "' was expected to be saved as "
                                + file2.getAbsolutePath()
                                + " with the SHA256 hash '"
                                + string
                                + "' but the actual hash was '"
                                + string1
                                + "'"
                );
            }
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            scriptEnvironment1.logFatalError("'" + file2.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (7)");
        } catch (IOException iOException) {
            scriptEnvironment1.logFatalError("'" + file2.getAbsolutePath() + "' couldn't be analyzed : '" + iOException + "' (8)");
        }
    }

    public static Set findEmptyDirectories(Enumeration enumeration, Set set1) {
        HashSet hashSet = ZkmUtils.createHashSet();
        HashSet hashSet1 = ZkmUtils.createHashSet();
        HashMap hashMap = ZkmUtils.createHashMap();

        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry1 = (ZipEntry) enumeration.nextElement();
            String name = zipEntry1.getName();
            if (zipEntry1.isDirectory()) {
                hashSet1.add(name);
            } else {
                int ba = name.lastIndexOf("/");
                if (ba > -1) {
                    String key = name.substring(0, ba + 1);
                    if (name.toLowerCase().endsWith(".class")) {
                        if (set1 != null && set1.contains(name)) {
                            hashMap.put(key, name);
                        }
                    } else {
                        hashMap.put(key, name);
                    }
                }
            }
        }

        HashSet hashSet2 = ZkmUtils.createHashSet();
        Iterator iterator = hashMap.keySet().iterator();

        while (iterator.hasNext()) {
            String string3 = (String) iterator.next();
            StringTokenizer stringTokenizer = new StringTokenizer(string3, "/");
            StringBuilder stringBuilder = new StringBuilder();

            while (stringTokenizer.hasMoreTokens()) {
                String string2 = stringTokenizer.nextToken();
                stringBuilder.append(string2);
                stringBuilder.append("/");
                hashSet2.add(stringBuilder.toString());
            }
        }

        iterator = hashSet1.iterator();

        while (iterator.hasNext()) {
            String string4 = (String) iterator.next();
            if (!hashSet2.contains(string4)) {
                hashSet.add(string4);
            }
        }

        return hashSet;
    }

    public void openArchives() throws ZkmException, IOException {
        int sourceArchiveIndex = 0;
        int bc = 0;

        for (SourceArchive[] sourceArchives1 = this.sourceArchives; bc < sourceArchives1.length; sourceArchives1 = this.sourceArchives) {
            if (this.sourceArchives[sourceArchiveIndex].isTopLevel()) {
                this.topLevelArchives.add(this.sourceArchives[sourceArchiveIndex]);
            }

            if (this.skippedNestedArchives == null || !this.skippedNestedArchives.contains(this.sourceArchives[sourceArchiveIndex])) {
                String filePath = this.sourceArchives[sourceArchiveIndex].getFilePath();

                try {
                    ArchiveZipFile archiveZipFile = new ArchiveZipFile(filePath);
                    this.openZipFiles.put(this.sourceArchives[sourceArchiveIndex], archiveZipFile);
                } catch (IOException iOException2) {
                    if (filePath.equals(this.sourceArchives[sourceArchiveIndex].getQualifiedPath())) {
                        this.scriptEnvironment
                                .logSeriousError_v(
                                        "Couldn't open '" + filePath + "' as an archive file. : " + iOException2.getMessage() + " : Please check if it is corrupt."
                                );
                    } else {
                        this.scriptEnvironment
                                .logSeriousError_v(
                                        "Couldn't open '"
                                                + filePath
                                                + "' as an archive file. : "
                                                + iOException2.getMessage()
                                                + " : Temporary copy of "
                                                + this.sourceArchives[sourceArchiveIndex].getQualifiedPath()
                                                + " : Please check if the original is corrupt."
                                );
                    }
                }
            }

            bc = ++sourceArchiveIndex;
        }

        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator = this.openZipFiles.keySet().iterator();

        while (iterator.hasNext()) {
            SourceArchive sourceArchive1 = (SourceArchive) iterator.next();
            File file1;
            String string1;
            if (sourceArchive1.isTopLevel()) {
                String name = sourceArchive1.getName();
                if (this.hasExplicitSaveTarget(sourceArchive1)) {
                    file1 = (File) this.archiveSaveTargets.get(sourceArchive1);
                    String absolutePath = file1.getAbsolutePath();
                    string1 = file1.getName();
                    if (this.scriptEnvironment.isVerbose()) {
                        this.scriptEnvironment
                                .getLogWriter()
                                .println("\tArchive file '" + sourceArchive1.getName() + "' saved to '" + file1.getAbsolutePath() + "'.");
                    }

                    this.registerOutputArchivePath(absolutePath, sourceArchive1, hashMap, "A");
                } else if (this.saveToSingleArchive) {
                    file1 = this.saveLocation;
                    this.saveLocation.getAbsolutePath();
                    string1 = this.saveLocation.getName();
                } else {
                    String key = this.saveLocation.getAbsolutePath() + FILE_SEPARATOR + name.substring(name.lastIndexOf(FILE_SEPARATOR) + 1);
                    file1 = new File(key);
                    string1 = file1.getName();
                    this.registerOutputArchivePath(key, sourceArchive1, hashMap, "B");
                }

                this.outputArchiveFiles.put(sourceArchive1, file1);
                if (file1.exists() && (!this.saveToSingleArchive || !file1.getAbsolutePath().equals(this.saveLocation.getAbsolutePath()))) {
                    this.backupExistingFile(file1);
                }
            } else {
                try {
                    file1 = TempFileManager.createTempFile(sourceArchive1.getDisplayPath());
                    this.outputArchiveFiles.put(sourceArchive1, file1);
                    SourceArchive sourceArchive2 = sourceArchive1.getRootArchive();
                    if (this.hasExplicitSaveTarget(sourceArchive2)) {
                        string1 = sourceArchive1.getDisplayPath();
                    } else if (this.saveToSingleArchive) {
                        String displayPath = sourceArchive1.getDisplayPath();
                        int beginIndex = displayPath.indexOf("!");
                        string1 = this.saveLocation.getName() + displayPath.substring(beginIndex);
                    } else {
                        string1 = sourceArchive1.getDisplayPath();
                    }
                } catch (IOException iOException1) {
                    this.messageReporter
                            .reportFatalError("FILE ERROR:", "Could not create temporary file in '" + TempFileManager.tempDirPath + "' : " + iOException1 + " (3)");
                    return;
                }
            }

            ZipOutputStream zipOutputStream1 = null;
            if (!sourceArchive1.isTopLevel()
                    || !this.saveToSingleArchive
                    || this.archiveSaveTargets != null && this.archiveSaveTargets.containsKey(sourceArchive1)) {
                try {
                    zipOutputStream1 = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file1), 2048));
                    this.outputStreamNames.put(zipOutputStream1, string1);
                } catch (IOException iOException) {
                    this.messageReporter
                            .reportFatalError("FILE ERROR:", "Couldn't create '" + file1.getAbsolutePath() + "' : " + iOException.getMessage() + " : (2)");
                }
            } else {
                zipOutputStream1 = this.singleArchiveOutput;
            }

            ZkmAssert.assertNotNull(zipOutputStream1, sourceArchive1.getName());
            this.archiveOutputStreams.put(sourceArchive1, zipOutputStream1);
        }
    }

    public void copyArchiveResources(SetMultiMap setMultiMap, SetMultiMap setMultiMap1, SetMultiMap setMultiMap2) throws ZkmException, IOException {
        Iterator iterator = this.openZipFiles.entrySet().iterator();
        if (this.openZipFiles.size() > 0) {
            if (!HiddenOptionFlags.COPY_ARCHIVE_DIRECTORIES) {
                this.messageReporter
                        .reportInfo("INFO:", "'ZKM_COPY_ARCHIVE_DIRECTORIES' command line option set to 'false'. No archive directories will not be copied.");
            } else if (HiddenOptionFlags.DELETE_EMPTY_DIRECTORIES) {
                this.messageReporter
                        .reportInfo("INFO:", "'ZKM_DELETE_EMPTY_DIRECTORIES' command line option set to 'true'. Empty archive directories will not be copied.");
            }
        }

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            String name = sourceArchive1.getName();
            ZipFile zipFile1 = (ZipFile) entry.getValue();
            ZipOutputStream zipOutputStream1 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive1);
            if (zipOutputStream1 != null) {
                boolean bl = false;
                Set set1 = findEmptyDirectories(zipFile1.entries(), setMultiMap.getValues(zipOutputStream1));
                Enumeration enumeration1;
                if (!HiddenOptionFlags.DELETE_EMPTY_DIRECTORIES && !this.deleteEmptyDirectories) {
                    enumeration1 = zipFile1.entries();
                } else {
                    bl = true;
                    enumeration1 = zipFile1.entries();
                }

                Enumeration enumeration = enumeration1;
                HashSet hashSet = ZkmUtils.createHashSet();

                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry1 = (ZipEntry) enumeration.nextElement();
                    String nameValue = zipEntry1.getName();
                    boolean bl1 = this.compressionMode == 1 || this.compressionMode == 3 && zipEntry1.getMethod() == 8;
                    if (this.isNestedArchive(zipFile1, zipEntry1, this.messageReporter)) {
                        if (this.filteredOutEntries == null || !this.filteredOutEntries.containsKeys(sourceArchive1, nameValue)) {
                            SourceArchive sourceArchive2 = sourceArchive1.findNestedArchive(nameValue);
                            if (this.skippedNestedArchives != null && this.skippedNestedArchives.contains(sourceArchive2)) {
                                try {
                                    InputStream inputStream1 = null;

                                    try {
                                        inputStream1 = zipFile1.getInputStream(zipEntry1);
                                        if (inputStream1 != null) {
                                            if (!this.checkDuplicateEntry(zipOutputStream1, nameValue, false)) {
                                                ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, nameValue, bl1, inputStream1, (int) zipEntry1.getSize());
                                                if (HiddenOptionFlags.PRESERVE_ZIP_ENTRY_TIMES) {
                                                    zipEntryWriter.writeEntry(zipEntry1.getTime());
                                                } else {
                                                    zipEntryWriter.writeEntry(this.entryTimestamp);
                                                }

                                                this.writtenEntries.addValue(zipOutputStream1, nameValue);
                                            }
                                        } else {
                                            this.scriptEnvironment
                                                    .logWarning(
                                                            "File Error: Couldn't copy '"
                                                                    + nameValue
                                                                    + "' from '"
                                                                    + name
                                                                    + "' to new archive because the entry appears to be corrupt (A)"
                                                    );
                                        }
                                    } finally {
                                        if (inputStream1 != null) {
                                            try {
                                                inputStream1.close();
                                            } catch (IOException iOException) {
                                            }
                                        }
                                    }
                                } catch (IOException iOException5) {
                                    this.messageReporter
                                            .reportError(
                                                    "FILE ERROR:", "Couldn't copy '" + nameValue + "' from '" + name + "' to new archive (A) : " + iOException5.getMessage()
                                            );
                                }
                            }
                        }
                    } else if ((!nameValue.endsWith(".class") || this.skippedClassFiles != null && this.skippedClassFiles.containsKeys(sourceArchive1, nameValue))
                            && !this.isNestedArchive(zipFile1, zipEntry1, this.messageReporter)) {
                        String string2 = nameValue.toUpperCase();
                        String string3 = nameValue.toLowerCase();
                        this.statusHolder.setValue("Saving " + nameValue);
                        if (zipEntry1.isDirectory()) {
                            if (HiddenOptionFlags.COPY_ARCHIVE_DIRECTORIES) {
                                String entryName = this.resourcePathTranslator.translateResourcePath(nameValue);
                                if (entryName.length() > 0 && hashSet.add(entryName) && (!bl || set1 == null || !set1.contains(nameValue))) {
                                    if (this.isAlreadyWritten(zipOutputStream1, entryName)) {
                                        continue;
                                    }

                                    this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, nameValue, entryName, bl1);
                                }
                            }
                        } else if ((this.filteredOutEntries == null || !this.filteredOutEntries.containsKeys(sourceArchive1, nameValue))
                                && !string2.equals("META-INF/MANIFEST.MF")
                                && (!string2.startsWith("META-INF/services/".toUpperCase()) || nameValue.length() <= ServiceProviderFile.SERVICES_PREFIX_LENGTH)) {
                            if (this.skippedXmlFiles != null && this.skippedXmlFiles.containsKeys(sourceArchive1, nameValue)) {
                                if (this.isAlreadyWritten(zipOutputStream1, nameValue)) {
                                    continue;
                                }

                                Boolean boolean1 = true;
                                this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, nameValue, nameValue, bl1, boolean1);
                            } else if (ZkmFileUtils.isXmlFileName(string3)) {
                                try {
                                    ObservableHolder observableHolder = new ObservableHolder();
                                    MutableInt mutableInt = new MutableInt(0);
                                    MutableInt mutableInt1 = new MutableInt(0);
                                    MutableInt mutableInt2 = new MutableInt(Integer.MAX_VALUE);

                                    String string5;
                                    try {
                                        string5 = XmlResourceProcessor.readZipEntryContent(zipFile1, zipEntry1, observableHolder, mutableInt, mutableInt1, mutableInt2);
                                    } catch (OutOfMemoryError outOfMemoryError) {
                                        this.messageReporter
                                                .reportError(
                                                        "FILE ERROR:",
                                                        "OutOfMemoryError reading or writing '"
                                                                + nameValue
                                                                + "' in '"
                                                                + name
                                                                + "' : "
                                                                + zipEntry1.getSize()
                                                                + " : "
                                                                + outOfMemoryError
                                                );
                                        throw outOfMemoryError;
                                    }

                                    if (this.isAlreadyWritten(zipOutputStream1, this.resourcePathTranslator.translateResourcePath(nameValue))) {
                                        continue;
                                    }

                                    XmlContentHandler xmlContentHandler = XmlConfigFileHandler.createUpdateHandler(
                                            ZkmFileUtils.describeEntryWithOriginal(zipFile1, zipEntry1),
                                            string5,
                                            this.classRenameMap,
                                            this.originalClassNames,
                                            this.fieldRenameMap,
                                            this.methodRenameMap,
                                            this.resourcePathTranslator,
                                            this.resourceFileIndex,
                                            this.classMemberLookup,
                                            this.commonSuperTypeResolver.getClassResolver(),
                                            this.messageReporter
                                    );
                                    new XmlZipEntryRewriter(
                                            zipFile1,
                                            zipEntry1,
                                            this.entryTimestamp,
                                            mutableInt,
                                            mutableInt1,
                                            mutableInt2,
                                            (String) observableHolder.getValue(),
                                            zipOutputStream1,
                                            this.resourcePathTranslator,
                                            xmlContentHandler,
                                            bl1,
                                            this.removeXmlComments
                                    );
                                    this.writtenEntries.addValue(zipOutputStream1, nameValue);
                                } catch (ZkmException zkmException1) {
                                    this.messageReporter.reportError("FILE ERROR:", "Invalid '" + nameValue + "' in '" + name + "' : " + zkmException1.getMessage());
                                } catch (IOException iOException4) {
                                    this.messageReporter.reportError("FILE ERROR:", "Error reading or writing '" + nameValue + "' in '" + name + "' : " + iOException4);
                                }
                            } else if (this.skippedYamlFiles != null && this.skippedYamlFiles.containsKeys(sourceArchive1, nameValue)) {
                                if (this.isAlreadyWritten(zipOutputStream1, nameValue)) {
                                    continue;
                                }

                                Boolean boolean2 = true;
                                this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, nameValue, nameValue, bl1, boolean2);
                            } else if (ZkmFileUtils.isYamlFileName(string3)) {
                                try {
                                    if (this.isAlreadyWritten(zipOutputStream1, this.resourcePathTranslator.translateResourcePath(nameValue))) {
                                        continue;
                                    }

                                    MutableInt mutableInt3 = new MutableInt(0);
                                    MutableInt mutableInt5 = new MutableInt(0);
                                    MutableInt mutableInt7 = new MutableInt(Integer.MAX_VALUE);
                                    InputStream inputStream2 = zipFile1.getInputStream(zipEntry1);
                                    PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream2, 4);
                                    String string7 = ZkmFileUtils.detectBomEncoding(ZkmFileUtils.peekBytes(pushbackInputStream), mutableInt3, mutableInt5, mutableInt7);
                                    pushbackInputStream.close();
                                    if (string7 == null) {
                                        string7 = "UTF-8";
                                    }

                                    new TextResourceRewriter(
                                            zipFile1,
                                            zipEntry1,
                                            this.entryTimestamp,
                                            mutableInt3,
                                            mutableInt5,
                                            mutableInt7,
                                            string7,
                                            zipOutputStream1,
                                            this.resourcePathTranslator,
                                            bl1,
                                            this.classRenameMap
                                    );
                                    this.writtenEntries.addValue(zipOutputStream1, nameValue);
                                } catch (IOException iOException2) {
                                    this.messageReporter.reportError("FILE ERROR:", "Error reading or writing '" + nameValue + "' in '" + name + "' : " + iOException2);
                                }
                            } else if (this.skippedPropertiesFiles != null && this.skippedPropertiesFiles.containsKeys(sourceArchive1, nameValue)) {
                                if (this.isAlreadyWritten(zipOutputStream1, nameValue)) {
                                    continue;
                                }

                                Boolean boolean3 = true;
                                this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, nameValue, nameValue, bl1, boolean3);
                            } else if (ZkmFileUtils.isPropertiesFileName(string3)) {
                                try {
                                    if (this.isAlreadyWritten(zipOutputStream1, this.resourcePathTranslator.translateResourcePath(nameValue))) {
                                        continue;
                                    }

                                    MutableInt mutableInt4 = new MutableInt(0);
                                    MutableInt mutableInt6 = new MutableInt(0);
                                    MutableInt mutableInt8 = new MutableInt(Integer.MAX_VALUE);
                                    InputStream inputStream3 = zipFile1.getInputStream(zipEntry1);
                                    PushbackInputStream pushbackInputStream1 = new PushbackInputStream(inputStream3, 4);
                                    String string8 = ZkmFileUtils.detectBomEncoding(ZkmFileUtils.peekBytes(pushbackInputStream1), mutableInt4, mutableInt6, mutableInt8);
                                    pushbackInputStream1.close();
                                    if (string8 == null) {
                                        if (HiddenOptionFlags.PROPERTIES_ENCODING != null) {
                                            string8 = HiddenOptionFlags.PROPERTIES_ENCODING.trim();
                                        } else {
                                            string8 = "UTF-8";
                                        }
                                    }

                                    new PropertiesFileRewriter(
                                            zipFile1,
                                            zipEntry1,
                                            this.entryTimestamp,
                                            mutableInt4,
                                            mutableInt6,
                                            mutableInt8,
                                            string8,
                                            zipOutputStream1,
                                            this.resourcePathTranslator,
                                            bl1,
                                            this.classRenameMap,
                                            new ObservableHolder()
                                    );
                                    this.writtenEntries.addValue(zipOutputStream1, nameValue);
                                } catch (IOException iOException1) {
                                    this.messageReporter.reportError("FILE ERROR:", "Error reading or writing '" + nameValue + "' in '" + name + "' : " + iOException1);
                                }
                            } else if (nameValue.equalsIgnoreCase("META-INF/INDEX.LIST")) {
                                try {
                                    JarIndexListRewriter jarIndexListRewriter = new JarIndexListRewriter(sourceArchive1, zipFile1, zipEntry1);
                                    if (this.isAlreadyWritten(zipOutputStream1, "META-INF/INDEX.LIST")) {
                                        continue;
                                    }

                                    jarIndexListRewriter.writeIndexList(zipOutputStream1, this.entryTimestamp, setMultiMap1, setMultiMap2, bl1);
                                    this.writtenEntries.addValue(zipOutputStream1, "META-INF/INDEX.LIST");
                                } catch (ZkmException zkmException) {
                                    this.messageReporter
                                            .reportError("FILE ERROR:", "Invalid INDEX.LIST file '" + nameValue + "' in '" + name + "' : " + zkmException.getMessage());
                                } catch (IOException iOException3) {
                                    this.messageReporter
                                            .reportError("FILE ERROR:", "Error reading or writing INDEX.LIST file '" + nameValue + "' in '" + name + "' : " + iOException3);
                                }
                            } else if (!string2.startsWith("META-INF/")
                                    || !string2.endsWith(".SF") && !string2.endsWith(".DSA") && !string2.endsWith(".RSA") && !string2.endsWith(".PGP")) {
                                String nameValue2;
                                if (this.skippedClassFiles != null && this.skippedClassFiles.containsKeys(sourceArchive1, nameValue)) {
                                    nameValue2 = nameValue;
                                } else {
                                    nameValue2 = this.resourcePathTranslator.translateResourcePath(nameValue);
                                }

                                if (this.isAlreadyWritten(zipOutputStream1, nameValue2)) {
                                    continue;
                                }

                                this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, nameValue, nameValue2, bl1);
                            } else if (string2.endsWith(".SF")) {
                                this.scriptEnvironment
                                        .logWarning("File '" + name + "' appears to have been signed. Signature data removed. You must re-sign AFTER obfuscation.");
                            }
                        }

                        this.statusHolder.setValue(" ");
                    }
                }
            }
        }
    }

    public void copyRenamedEntry(
            ZipFile zipFile1, ZipEntry zipEntry1, ZipOutputStream zipOutputStream1, String string, String entryName, Boolean boolean1, boolean bl
    ) throws ZkmException, IOException {
        try {
            InputStream inputStream1 = null;

            try {
                inputStream1 = zipFile1.getInputStream(zipEntry1);
                if (inputStream1 != null) {
                    ZipEntryWriter entryWriter = new ZipEntryWriter(zipOutputStream1, entryName, boolean1, inputStream1, (int) zipEntry1.getSize());
                    if (HiddenOptionFlags.PRESERVE_ZIP_ENTRY_TIMES && bl) {
                        entryWriter.writeEntry(zipEntry1.getTime());
                    } else {
                        entryWriter.writeEntry(this.entryTimestamp);
                    }

                    this.writtenEntries.addValue(zipOutputStream1, entryName);
                } else {
                    this.scriptEnvironment
                            .logWarning(
                                    "File Error: Couldn't copy '" + string + "' from '" + zipFile1.getName() + "' to new archive because the entry appears to be corrupt (B)"
                            );
                }
            } finally {
                if (inputStream1 != null) {
                    try {
                        inputStream1.close();
                    } catch (IOException iOException) {
                    }
                }
            }
        } catch (IOException iOException1) {
            this.messageReporter
                    .reportError("FILE ERROR:", "Couldn't copy '" + string + "' from '" + zipFile1.getName() + "' to new archive (B) : " + iOException1.getMessage());
        }
    }

    public void appendClassSaveDescription(StringBuilder stringBuilder, List list1, String string) {
        if (list1.size() > 1) {
            stringBuilder.append("Classes ");
        } else {
            stringBuilder.append("Class ");
        }

        String[] strings = new String[list1.size()];

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            strings[elementIndex] = ((ProgramClass) list1.get(elementIndex)).getDisplayLocationName();
        }

        stringBuilder.append(ZkmUtils.toQuotedArrayString(strings));
        String[] strings1 = new String[list1.size()];

        for (int elementIndexValue = 0; elementIndexValue < list1.size(); elementIndexValue++) {
            strings1[elementIndexValue] = ((ProgramClass) list1.get(elementIndexValue)).getDottedClassName();
        }

        stringBuilder.append(" will be saved to ");
        stringBuilder.append(string);
        stringBuilder.append(" as ");
        stringBuilder.append(ZkmUtils.toQuotedArrayString(strings1));
        stringBuilder.append(". ");
    }

    public void collectArchiveEntryPaths(Map map1, ResourcePathTranslator resourcePathTranslator1, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) throws IOException {
        int classeIndex = 0;
        int bc = 0;

        for (ProgramClass[] programClass2 = this.classes; bc < programClass2.length; programClass2 = this.classes) {
            ProgramClass programClass1 = this.classes[classeIndex];
            Enumeration enumeration = programClass1.enumerateInputLocations();

            while (enumeration.hasMoreElements()) {
                InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                if (inputFileLocation.isArchiveEntry()) {
                    SourceArchive sourceArchive1 = inputFileLocation.getSourceArchive();
                    String string = (String) map1.get(inputFileLocation);
                    this.recordEntryPath(string, sourceArchive1, setMultiMap, setMultiMap1);
                }
            }

            bc = ++classeIndex;
        }

        Iterator iterator = this.openZipFiles.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive2 = (SourceArchive) entry.getKey();
            ZipFile zipFile1 = (ZipFile) entry.getValue();
            Enumeration<? extends ZipEntry> enumeration1 = zipFile1.entries();

            while (enumeration1.hasMoreElements()) {
                ZipEntry zipEntry1 = (ZipEntry) enumeration1.nextElement();
                if (!zipEntry1.isDirectory()) {
                    String name = zipEntry1.getName();
                    String string2 = name.toUpperCase();
                    if (!name.toLowerCase().endsWith(".class") && !string2.startsWith("META-INF/")) {
                        String string3 = resourcePathTranslator1.translateResourcePath(name);
                        int endIndex = string3.lastIndexOf("/");
                        if (endIndex == -1) {
                            setMultiMap.addValue(sourceArchive2, string3);
                        } else {
                            String string4 = string3.substring(0, endIndex);
                            setMultiMap1.addValue(sourceArchive2, string4);
                        }
                    }
                }
            }
        }
    }

    public void backupExistingFile(File file1) throws ZkmException, IOException {
        String absolutePath = file1.getAbsolutePath();

        try {
            String string1 = absolutePath + ".BACKUP";
            File file2 = new File(string1);
            boolean renameTempOutputFile;
            if (file2.exists()) {
                boolean bl = file2.delete();
                if (!bl) {
                    throw new Exception("Could not delete '" + file2.getAbsolutePath() + "'");
                }

                renameTempOutputFile = HiddenOptionFlags.RENAME_TEMP_OUTPUT_FILE;
            } else {
                renameTempOutputFile = HiddenOptionFlags.RENAME_TEMP_OUTPUT_FILE;
            }

            if (renameTempOutputFile) {
                boolean bl1 = file1.renameTo(file2);
                if (!bl1) {
                    throw new Exception("Could not rename '" + file1.getAbsolutePath() + "' to '" + file2.getAbsolutePath() + "'");
                }

                this.scriptEnvironment
                        .logWarning(
                                "File '"
                                        + absolutePath
                                        + "' already exists"
                                        + (this.saveToSingleArchive ? "" : " in '" + this.saveLocation.getAbsolutePath())
                                        + "'. Renaming to '"
                                        + string1
                                        + "'."
                        );
            }
        } catch (AssertionFailedException assertionFailedException) {
            throw assertionFailedException;
        } catch (Exception exception) {
            this.messageReporter.reportError("FILE ERROR:", "Couldn't back up and recreate '" + absolutePath + "' : " + exception.getMessage());
        }
    }

    public boolean isNestedArchive(ZipFile zipFile1, ZipEntry zipEntry1, MessageReporter messageReporter1) throws ZkmException, IOException {
        try {
            return ZkmFileUtils.isNestedArchiveEntry(zipFile1, zipEntry1);
        } catch (IOException iOException) {
            messageReporter1.reportError("FILE ERROR:", "File read error : " + iOException.getMessage());
            return false;
        }
    }

    public void rewriteXmlFiles() throws ZkmException, IOException {
        if (this.xmlFileLocations != null) {
            for (InputFileLocation inputFileLocation : this.xmlFileLocations) {
                String qualifiedName = inputFileLocation.getQualifiedName();
                File file1 = inputFileLocation.getFile();
                ObservableHolder observableHolder = new ObservableHolder();
                MutableInt mutableInt = new MutableInt(0);
                MutableInt mutableInt1 = new MutableInt(0);
                MutableInt mutableInt2 = new MutableInt(Integer.MAX_VALUE);

                try {
                    String string1 = XmlResourceProcessor.readFileContent(file1, observableHolder, mutableInt, mutableInt1, mutableInt2);
                    XmlContentHandler xmlContentHandler = XmlConfigFileHandler.createUpdateHandler(
                            qualifiedName,
                            string1,
                            this.classRenameMap,
                            this.originalClassNames,
                            this.fieldRenameMap,
                            this.methodRenameMap,
                            this.resourcePathTranslator,
                            this.resourceFileIndex,
                            this.classMemberLookup,
                            this.commonSuperTypeResolver.getClassResolver(),
                            this.messageReporter
                    );
                    if (this.saveToSingleArchive) {
                        ZipOutputStream zipOutputStream2 = this.singleArchiveOutput;
                        if (!this.isAlreadyWritten(zipOutputStream2, inputFileLocation.getName())) {
                            String value = (String) observableHolder.getValue();
                            boolean bl2;
                            boolean removeXmlCommentsValue;
                            if (this.compressionMode == 1) {
                                bl2 = true;
                                removeXmlCommentsValue = this.removeXmlComments;
                            } else {
                                bl2 = false;
                                removeXmlCommentsValue = this.removeXmlComments;
                            }

                            boolean removeXmlCommentsValue2 = removeXmlCommentsValue;
                            boolean compressed = bl2;
                            XmlContentHandler xmlContentHandler1 = xmlContentHandler;
                            ZipOutputStream zipOutputStream1 = zipOutputStream2;
                            String valueValue = value;
                            MutableInt mutableInt3 = mutableInt2;
                            MutableInt mutableInt4 = mutableInt1;
                            MutableInt mutableInt5 = mutableInt;
                            File file3 = file1;
                            new XmlZipEntryRewriter(file3, mutableInt5, mutableInt4, mutableInt3, valueValue, zipOutputStream1, xmlContentHandler1, compressed, removeXmlCommentsValue2);
                            this.writtenEntries.addValue(zipOutputStream2, inputFileLocation.getName());
                        }
                    } else {
                        File file2;
                        if (this.xmlSaveTargets != null && this.xmlSaveTargets.containsKey(inputFileLocation)) {
                            file2 = (File) this.xmlSaveTargets.get(inputFileLocation);
                        } else {
                            file2 = new File(this.saveLocation, file1.getName());
                        }

                        new XmlZipEntryRewriter(
                                file1, mutableInt, mutableInt1, mutableInt2, (String) observableHolder.getValue(), file2, xmlContentHandler, this.removeXmlComments
                        );
                        if (inputFileLocation.getExpectedFinalSha256() != null) {
                            String expectedFinalSha256 = inputFileLocation.getExpectedFinalSha256();
                            this.verifyFileSha256(file1, file2, expectedFinalSha256, this.scriptEnvironment);
                        }

                        if (this.scriptEnvironment.isVerbose()) {
                            this.scriptEnvironment
                                    .getLogWriter()
                                    .println(
                                            "\tXML file '"
                                                    + file1.getAbsolutePath()
                                                    + "' saved to '"
                                                    + file2.getAbsolutePath()
                                                    + "'"
                                                    + (
                                                    !HiddenOptionFlags.USE_PARALLEL && HiddenOptionFlags.DETERMINISTIC_OUTPUT
                                                            ? " with SHA256 hash '" + computeSha256(file2, this.scriptEnvironment) + "'"
                                                            : ""
                                            )
                                                    + "."
                                    );
                        }
                    }
                } catch (ZkmException zkmException) {
                    this.messageReporter.reportError("FILE ERROR:", "Invalid XML file '" + qualifiedName + "' : " + zkmException.getMessage());
                } catch (IOException iOException) {
                    this.messageReporter.reportError("FILE ERROR:", "Error reading or writing XML file '" + qualifiedName + "' : " + iOException);
                }
            }
        }
    }

    public void closeOutputStream(ZipOutputStream zipOutputStream1, String string) {
        try {
            zipOutputStream1.close();
        } catch (IOException iOException) {
            this.scriptEnvironment.logWarning("FILE ERROR (2) '" + string + "' : " + iOException.getMessage());
        }
    }

    public void writeNestedArchive(SourceArchive sourceArchive1, SourceArchive sourceArchive2) throws ZkmException, IOException {
        Enumeration enumeration = sourceArchive1.getNestedArchives();

        while (enumeration.hasMoreElements()) {
            SourceArchive sourceArchive3 = (SourceArchive) enumeration.nextElement();
            if (this.skippedNestedArchives == null || !this.skippedNestedArchives.contains(sourceArchive3)) {
                this.writeNestedArchive(sourceArchive3, sourceArchive1);
            }
        }

        ZipOutputStream zipOutputStream2 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive2);
        ZipOutputStream zipOutputStream1 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive1);
        ZkmAssert.assertNotNull(zipOutputStream1, "Problem with " + sourceArchive1.getQualifiedPath() + " : " + sourceArchive1.getFilePath());

        try {
            try {
                zipOutputStream1.close();
                File file1 = (File) this.outputArchiveFiles.get(sourceArchive1);
                ZipEntry zipEntry1 = ((ZipFile) this.openZipFiles.get(sourceArchive2)).getEntry(sourceArchive1.getName());
                boolean compressed = this.compressionMode == 1 || this.compressionMode == 3 && zipEntry1.getMethod() == 8;
                new ZipEntryWriter(zipOutputStream2, sourceArchive1.getName(), compressed, file1).writeEntry(this.entryTimestamp);
                this.writtenEntries.addValue(zipOutputStream1, sourceArchive1.getName());
            } catch (IOException iOException) {
                this.messageReporter
                        .reportError("FILE ERROR:", "Couldn't write " + sourceArchive1.getName() + " to '" + sourceArchive2.getName() + "' : " + iOException);
            }
        } finally {
            ;
        }
    }

    public void closeAll() {
        Iterator iterator = this.openZipFiles.entrySet().iterator();

        while (iterator.hasNext()) {
            ZipFile zipFile1 = (ZipFile) ((Entry) iterator.next()).getValue();
            String name = zipFile1.getName();

            try {
                zipFile1.close();
            } catch (IOException iOException1) {
                this.scriptEnvironment.logWarning("FILE ERROR (1) '" + name + "' : " + iOException1.getMessage());
            }
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        if (this.singleArchiveOutput != null) {
            this.closeOutputStream(this.singleArchiveOutput, this.saveLocation.getAbsolutePath());
            hashSet.add(this.singleArchiveOutput);
        }

        Iterator iterator1 = this.archiveOutputStreams.entrySet().iterator();

        while (iterator1.hasNext()) {
            Entry entry = (Entry) iterator1.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            if (sourceArchive1.isTopLevel()) {
                ZipOutputStream zipOutputStream1 = (ZipOutputStream) entry.getValue();
                if (!hashSet.contains(zipOutputStream1)) {
                    ZkmAssert.assertNotNull(
                            zipOutputStream1, "Null zip outputstream '" + sourceArchive1.getOriginalPath() + "' : '" + sourceArchive1.getFilePath() + "'"
                    );
                    this.closeOutputStream(zipOutputStream1, sourceArchive1.getName());
                    File file1 = (File) this.outputArchiveFiles.get(sourceArchive1);
                    if (file1 != null && file1.exists()) {
                        if (sourceArchive1.getExpectedFinalSha256() != null) {
                            this.verifyFileSha256(new File(sourceArchive1.getOriginalPath()), file1, sourceArchive1.getExpectedFinalSha256(), this.scriptEnvironment);
                        }

                        if (!HiddenOptionFlags.USE_PARALLEL && HiddenOptionFlags.DETERMINISTIC_OUTPUT && HiddenOptionFlags.RANDOM_SEED != null) {
                            try {
                                Sha256Digester sha256Digester = new Sha256Digester(file1.getAbsolutePath());
                                String string1 = sha256Digester.getHexDigest().toLowerCase();
                                this.scriptEnvironment.getLogWriter().println("\tFile '" + file1.getAbsolutePath() + "' saved with SHA256 hash '" + string1 + "'");
                            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                                this.scriptEnvironment.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (5)");
                            } catch (IOException iOException) {
                                this.scriptEnvironment.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + iOException + "' (6)");
                            }
                        }
                    }
                }
            }
        }
    }

    public String getClassEntryName(ProgramClass programClass1) {
        String string = programClass1.isModule() ? "module-info" : programClass1.getClassName();
        String packagePath = ClassFileBase.getPackagePath(string);
        packagePath = ZkmStringUtils.replaceAll(packagePath, FILE_SEPARATOR, "/");
        String string2 = ClassFileBase.stripPackage(string);
        return (packagePath.length() > 0 ? packagePath + "/" : "") + string2 + ".class";
    }

    public void writeClassEntry(
            ProgramClass programClass1, byte[] byteArray, String entryName, ZipOutputStream zipOutputStream1, File file1, String string1, Boolean boolean1
    ) throws ZkmException, IOException {
        try {
            ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, entryName, boolean1);
            DataOutputStream dataOutputStream = new DataOutputStream(zipEntryWriter.getOutputStream());

            try {
                if (this.scriptEnvironment.isVerbose()) {
                    String originalName = TempFileManager.getOriginalName(file1.getAbsolutePath());
                    this.scriptEnvironment
                            .getLogWriter()
                            .println(
                                    "\tClass file '"
                                            + programClass1.getDisplayLocationName()
                                            + "' saved to '"
                                            + (originalName == null ? file1.getAbsolutePath() : originalName)
                                            + "' as '"
                                            + entryName
                                            + "'."
                            );
                }

                ZipEntryWriter zipEntryWriter1;
                Long long1;
                if (byteArray != null) {
                    dataOutputStream.write(byteArray);
                    zipEntryWriter1 = zipEntryWriter;
                    long1 = this.entryTimestamp;
                } else {
                    programClass1.writeClassFile(dataOutputStream, this.commonSuperTypeResolver, this.classMemberLookup, this.scriptEnvironment);
                    dataOutputStream.flush();
                    zipEntryWriter1 = zipEntryWriter;
                    long1 = this.entryTimestamp;
                }

                zipEntryWriter1.writeEntry(long1);
                this.writtenEntries.addValue(zipOutputStream1, entryName);
            } catch (MethodAnalysisException methodAnalysisException) {
                this.messageReporter
                        .reportFatalError(
                                "ERROR:",
                                "Method '"
                                        + methodAnalysisException.getMethodName()
                                        + "' could not be analyzed (G). Consider excluding it from Flow and Exception Obfuscation."
                        );
            } catch (ZkmProcessingException zkmProcessingException) {
                this.messageReporter.reportFatalError("ERROR:", zkmProcessingException.getMessage());
            }
        } catch (IOException iOException) {
            this.messageReporter
                    .reportFatalError(
                            "FILE ERROR:",
                            "Couldn't save '"
                                    + programClass1.getDottedClassName()
                                    + "' from '"
                                    + string1
                                    + "' to new archive (C) : '"
                                    + entryName
                                    + "' : "
                                    + iOException.getMessage()
                    );
        }
    }

    public boolean hasExplicitSaveTarget(Object key) {
        return this.archiveSaveTargets != null && this.archiveSaveTargets.containsKey(key);
    }

    public void recordEntryPath(String string, SourceArchive sourceArchive1, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) {
        int endIndex = string.lastIndexOf("/");
        if (endIndex == -1) {
            setMultiMap.addValue(sourceArchive1, string);
        } else {
            String string1 = string.substring(0, endIndex);
            setMultiMap1.addValue(sourceArchive1, string1);
        }
    }

    public Long parseTimestamp(String string) {
        try {
            long ba = Long.parseLong(string);
            FileTime fileTime1 = FileTime.fromMillis(ba);
            this.scriptEnvironment.getLogWriter().println("All archive entries will have their last modified dates set to '" + fileTime1 + "' : " + string);
            return ba;
        } catch (NumberFormatException numberFormatException) {
            try {
                Instant instant = Instant.parse(string);
                FileTime fileTime = FileTime.from(instant);
                this.scriptEnvironment
                        .getLogWriter()
                        .println("All archive entries will have their last modified dates set to '" + fileTime + "' : '" + string + "'");
                return fileTime.toMillis();
            } catch (DateTimeParseException dateTimeParseException) {
                this.scriptEnvironment.logFatalError("Could not successfully parse '" + string + "' : '" + dateTimeParseException + "'");
                return null;
            }
        }
    }

    public void writeManifests() throws ZkmException, IOException {
        Iterator iterator = this.openZipFiles.keySet().iterator();

        while (iterator.hasNext()) {
            SourceArchive sourceArchive1 = (SourceArchive) iterator.next();
            if (this.skippedNestedArchives == null || !this.skippedNestedArchives.contains(sourceArchive1)) {
                ZipOutputStream zipOutputStream1 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive1);
                ArchiveManifest archiveManifest = sourceArchive1.getManifest();
                if (zipOutputStream1 != null
                        && archiveManifest != null
                        && (this.filteredOutEntries == null || !this.filteredOutEntries.containsKeys(sourceArchive1, "META-INF/MANIFEST.MF"))
                        && !this.isAlreadyWritten(zipOutputStream1, "META-INF/MANIFEST.MF")) {
                    boolean compressed = this.compressionMode == 1 || this.compressionMode == 3 && archiveManifest.isCompressed();

                    try {
                        archiveManifest.writeManifest(
                                zipOutputStream1, this.entryTimestamp, this.packageRenameMap, this.classRenameMap, this.resourcePathTranslator, compressed, this.messageReporter
                        );
                        this.writtenEntries.addValue(zipOutputStream1, "META-INF/MANIFEST.MF");
                    } catch (IOException iOException) {
                        this.messageReporter
                                .reportError(
                                        "FILE ERROR:", "Couldn't write MANIFEST.MF to '" + (String) this.outputStreamNames.get(zipOutputStream1) + "' : " + iOException
                                );
                    }
                }
            }
        }
    }

    public void writeFileEntry(String string, String entryName, ZipOutputStream zipOutputStream1, boolean compressed) throws ZkmException, IOException {
        ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, entryName, compressed, new File(string));

        try {
            zipEntryWriter.writeEntry(this.entryTimestamp);
            this.writtenEntries.addValue(zipOutputStream1, entryName);
        } catch (IOException iOException) {
            this.messageReporter.reportError("FILE ERROR:", "Couldn't save '" + string + "' to new archive (A) : " + iOException.getMessage());
        }
    }

    public void checkCaseInsensitiveClashes() throws ZkmException, IOException {
        if (!this.saveToSingleArchive) {
            SetMultiMap setMultiMap = new SetMultiMap();
            ListMultimap listMultimap = new ListMultimap();

            for (ProgramClass programClass1 : this.classes) {
                String packagePath = programClass1.getPackagePath();
                if (!programClass1.isFromArchive()) {
                    setMultiMap.addValue(packagePath.toLowerCase(), packagePath);
                }

                listMultimap.addValue(programClass1.getClassName().toLowerCase(), programClass1);
            }

            boolean bl = false;
            Iterator iterator1 = listMultimap.entrySet().iterator();

            while (iterator1.hasNext()) {
                Entry entry = (Entry) iterator1.next();
                List list1 = (List) entry.getValue();
                if (list1.size() > 1) {
                    ArrayList arrayList1 = new ArrayList(list1.size());
                    ArrayList arrayList = new ArrayList(list1.size());
                    Iterator iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        ProgramClass programClass2 = (ProgramClass) iterator.next();
                        if (!programClass2.isFromArchive()) {
                            arrayList1.add(programClass2);
                        } else {
                            arrayList.add(programClass2);
                        }
                    }

                    if (arrayList1.size() > 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        this.appendClassSaveDescription(stringBuilder, arrayList1, "the file system");
                        if (arrayList.size() > 0) {
                            this.appendClassSaveDescription(stringBuilder, arrayList, "archive files");
                        }

                        if (arrayList1.size() + arrayList.size() > 2) {
                            stringBuilder.append("Some of these class names");
                        } else {
                            stringBuilder.append("These class names");
                        }

                        if (ZkmFileUtils.caseSensitiveFileSystem) {
                            stringBuilder.append(" would clash in a case insensitive environment.");
                            this.messageReporter.reportWarning("WARNING:", stringBuilder.toString());
                        } else {
                            stringBuilder.append(" will clash in the current case insensitive environment. : ");
                            stringBuilder.append(ZkmFileUtils.OS_NAME);
                            this.messageReporter.reportWarning("WARNING:", stringBuilder.toString());
                            bl = true;
                        }
                    }
                }
            }

            if (bl) {
                this.messageReporter
                        .reportFatalError("FATAL ERROR:", "Classes would be saved to the file system with clashing mixed case names. See warnings for details.");
            }

            boolean bl1 = false;
            Iterator iterator2 = setMultiMap.entrySet().iterator();

            while (iterator2.hasNext()) {
                Entry entry1 = (Entry) iterator2.next();
                Set set1 = (Set) entry1.getValue();
                int set1Size = set1.size();
                if (set1Size > 1) {
                    String string1 = ZkmUtils.toQuotedArrayString(((java.lang.String[]) (set1.toArray(new String[set1.size()]))));
                    if (ZkmFileUtils.caseSensitiveFileSystem) {
                        this.messageReporter
                                .reportWarning(
                                        "WARNING:", "Packages will be saved to the file system with names that would clash in a case insensitive environment : " + string1
                                );
                    } else {
                        bl1 = true;
                        this.messageReporter.reportWarning("WARNING:", "Packages would be saved to the file system with clashing names : " + string1);
                    }
                }
            }

            if (bl1) {
                this.messageReporter.reportFatalError("FATAL ERROR:", "Packages would be saved to the file system with clashing mixed case package names.");
            }
        }
    }

    public Set computeClassEntryNames(Map map1, Map map2) {
        HashSet hashSet = ZkmUtils.createHashSet();

        for (ProgramClass programClass1 : this.classes) {
            Enumeration enumeration = programClass1.enumerateInputLocations();

            while (enumeration.hasMoreElements()) {
                InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                if (inputFileLocation.isArchiveEntry()) {
                    this.computeClassEntryName(inputFileLocation, programClass1, hashSet, map1, map2);
                }
            }

            if (programClass1.hasVersionedVariants()) {
                List list1 = programClass1.getVersionedVariants();
                Iterator iterator = list1.iterator();

                while (iterator.hasNext()) {
                    ClassFileBase classFileBase = (ClassFileBase) iterator.next();
                    InputFileLocation inputFileLocation1 = classFileBase.getInputLocation();
                    this.computeClassEntryName(inputFileLocation1, (ProgramClass) classFileBase, hashSet, map1, map2);
                }
            }
        }

        return hashSet;
    }

    public ClassArchiveSaver(
            ProgramClass[] programClass1,
            ModuleInfoClass[] moduleInfoClass,
            SourceArchive[] sourceArchives1,
            Set set1,
            TwoKeyMap twoKeyMap,
            TwoKeyMap twoKeyMap1,
            TwoKeyMap twoKeyMap2,
            TwoKeyMap twoKeyMap3,
            TwoKeyMap twoKeyMap4,
            HashMap hashMap,
            InputFileLocation[] inputFileLocations,
            InputFileLocation[] inputFileLocations1,
            InputFileLocation[] inputFileLocations2,
            HashMap hashMap1,
            HashMap hashMap2,
            HashMap hashMap3,
            HashMap hashMap4,
            HashMap hashMap5,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            EnumerableMap enumerableMap2,
            ReadOnlyMultiMap readOnlyMultiMap,
            ReadOnlyMultiMap readOnlyMultiMap1,
            ClassMemberLookup classMemberLookup1,
            File file1,
            int compressionMode,
            boolean deleteEmptyDirectories,
            boolean removeXmlComments,
            String string,
            MessageReporter messageReporter1,
            ScriptEnvironment scriptEnvironment1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            FixedClassesExclusionSet fixedClassesExclusionSet1,
            SetValuedMap setValuedMap,
            ResourceFileIndex resourceFileIndex1,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        if (moduleInfoClass.length > 0) {
            this.classes = new ProgramClass[programClass1.length + moduleInfoClass.length];
            System.arraycopy(programClass1, 0, this.classes, 0, programClass1.length);
            System.arraycopy(moduleInfoClass, 0, this.classes, programClass1.length, moduleInfoClass.length);
        } else {
            this.classes = new ProgramClass[programClass1.length];
            System.arraycopy(programClass1, 0, this.classes, 0, programClass1.length);
        }

        this.sourceArchives = sourceArchives1;
        this.skippedNestedArchives = set1;
        this.skippedClassFiles = twoKeyMap;
        this.skippedXmlFiles = twoKeyMap1;
        this.skippedYamlFiles = twoKeyMap2;
        this.skippedPropertiesFiles = twoKeyMap3;
        this.filteredOutEntries = twoKeyMap4;
        this.jadFiles = hashMap;
        this.xmlFileLocations = inputFileLocations;
        this.yamlFileLocations = inputFileLocations1;
        this.propertiesFileLocations = inputFileLocations2;
        this.archiveSaveTargets = hashMap1;
        this.jadSaveTargets = hashMap2;
        this.xmlSaveTargets = hashMap3;
        this.yamlSaveTargets = hashMap4;
        this.packageRenameMap = enumerableMap;
        this.classRenameMap = enumerableMap1;
        this.originalClassNames = enumerableMap2;
        this.fieldRenameMap = readOnlyMultiMap;
        this.methodRenameMap = readOnlyMultiMap1;
        this.classMemberLookup = classMemberLookup1;
        this.commonSuperTypeResolver = commonSuperTypeResolver1;
        this.compressionMode = compressionMode;
        this.deleteEmptyDirectories = deleteEmptyDirectories;
        this.removeXmlComments = removeXmlComments;
        this.messageReporter = messageReporter1;
        this.scriptEnvironment = scriptEnvironment1;
        this.fixedClassesExclusions = fixedClassesExclusionSet1;
        this.resourceFileIndex = resourceFileIndex1;
        this.statusHolder = observableHolder;
        this.saveLocation = file1;
        if (string != null) {
            this.entryTimestamp = this.parseTimestamp(string);
        } else {
            this.entryTimestamp = null;
        }

        if (ZkmFileUtils.looksLikeArchiveName(file1.getAbsolutePath())) {
            if (hashMap != null && hashMap.size() > 0) {
                this.saveToSingleArchive = false;
                messageReporter1.reportError(
                        "ERROR:", "Must specify a base save directory when processing JAD files. Cannot save to a base archive : '" + file1.getAbsolutePath() + "'"
                );
            } else {
                this.saveToSingleArchive = true;
                if (file1.exists()) {
                    this.backupExistingFile(file1);
                }

                try {
                    this.singleArchiveOutput = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file1), 2048));
                    this.outputStreamNames.put(this.singleArchiveOutput, file1.getAbsolutePath());
                } catch (IOException iOException) {
                    messageReporter1.reportError("FILE ERROR:", "Couldn't create '" + file1.getAbsolutePath() + "' : " + iOException.getMessage() + " : (1)");
                }
            }
        } else {
            this.saveToSingleArchive = false;
        }

        try {
            this.checkCaseInsensitiveClashes();
            this.openArchives();
            HashMap hashMap6 = ZkmUtils.createHashMap();
            HashMap hashMap7 = ZkmUtils.createHashMap();
            Set set2 = this.computeClassEntryNames(hashMap6, hashMap7);
            this.resourcePathTranslator = new ResourcePathTranslator(enumerableMap, ZkmUtils.createHashSetFrom(set2));
            SetMultiMap setMultiMap = new SetMultiMap(sourceArchives1.length);
            SetMultiMap setMultiMap1 = new SetMultiMap(sourceArchives1.length);
            this.collectArchiveEntryPaths(hashMap6, this.resourcePathTranslator, setMultiMap, setMultiMap1);
            SetMultiMap setMultiMap2 = new SetMultiMap();
            this.writeManifests();
            this.writeServiceProviderFiles();
            this.writeClasses(setValuedMap, hashMap6, setMultiMap2, hashMap7, setMultiMap, setMultiMap1);
            this.copyArchiveResources(setMultiMap2, setMultiMap, setMultiMap1);
            this.rewriteXmlFiles();
            this.rewriteTextResourceFiles(inputFileLocations1, hashMap4, false);
            this.rewriteTextResourceFiles(inputFileLocations2, hashMap5, true);
            this.writeNestedArchives();
        } finally {
            this.closeAll();
        }

        if (!this.saveToSingleArchive) {
            this.writeJadFiles();
        }
    }

    public void writeNestedArchives() throws ZkmException, IOException {
        this.statusHolder.setValue("Writing nested archive files");
        int elementIndex = 0;
        int bb = 0;

        for (List list1 = this.topLevelArchives; bb < list1.size(); list1 = this.topLevelArchives) {
            SourceArchive sourceArchive1 = (SourceArchive) this.topLevelArchives.get(elementIndex);
            Enumeration enumeration = sourceArchive1.getNestedArchives();

            while (enumeration.hasMoreElements()) {
                SourceArchive sourceArchive2 = (SourceArchive) enumeration.nextElement();
                if (this.skippedNestedArchives == null || !this.skippedNestedArchives.contains(sourceArchive2)) {
                    this.writeNestedArchive(sourceArchive2, sourceArchive1);
                }
            }

            bb = ++elementIndex;
        }

        this.statusHolder.setValue(" ");
    }

    public void computeClassEntryName(InputFileLocation inputFileLocation, ProgramClass programClass1, Set set1, Map map1, Map map2) {
        String name = inputFileLocation.getName();
        name = name.substring(0, name.lastIndexOf(".class"));
        String key = programClass1.isModule() ? "module-info" : programClass1.getClassName();
        String searchText = (String) ZkmUtils.mapOrSelf(key, this.originalClassNames);
        int endIndex = name.lastIndexOf(searchText);
        String value;
        if (endIndex > 0) {
            String valueValue = name.substring(0, endIndex);
            value = valueValue + key + ".class";
            if (valueValue.charAt(0) == '/') {
                valueValue = valueValue.substring(1);
            }

            set1.add(valueValue);
            map2.put(inputFileLocation, valueValue);
        } else {
            value = key + ".class";
            map2.put(inputFileLocation, "");
        }

        map1.put(inputFileLocation, value);
    }

    public static String computeSha256(File file1, ScriptEnvironment scriptEnvironment1) {
        String string = null;

        try {
            Sha256Digester sha256Digester = new Sha256Digester(file1.getAbsolutePath());
            string = sha256Digester.getHexDigest().toLowerCase();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            scriptEnvironment1.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (5)");
        } catch (IOException iOException) {
            scriptEnvironment1.logFatalError("'" + file1.getAbsolutePath() + "' couldn't be analyzed : '" + iOException + "' (6)");
        }

        return string;
    }

    public boolean checkDuplicateEntry(ZipOutputStream zipOutputStream1, String string, boolean bl) {
        if (this.writtenEntries.containsValue(zipOutputStream1, string)) {
            if (bl) {
                this.messageReporter
                        .reportWarning(
                                "WARNING:",
                                "Duplicate entry '"
                                        + string
                                        + "' while writing to '"
                                        + (String) this.outputStreamNames.get(zipOutputStream1)
                                        + "'. Using only the first entry encountered."
                        );
            }

            return true;
        } else {
            return false;
        }
    }

    public void registerOutputArchivePath(String key, SourceArchive sourceArchive1, Map map1, String string1) throws ZkmException, IOException {
        if (!map1.containsKey(key)) {
            map1.put(key, sourceArchive1);
        } else {
            SourceArchive sourceArchive2 = (SourceArchive) map1.get(key);
            ZipOutputStream zipOutputStream1 = (ZipOutputStream) this.archiveOutputStreams.get(sourceArchive2);
            File file1 = (File) this.outputArchiveFiles.get(sourceArchive2);

            try {
                zipOutputStream1.close();
                Files.deleteIfExists(file1.toPath());
            } catch (Throwable throwable) {
                ZkmAssert.assertTrue(false, new String[]{throwable.toString()});
            }

            this.messageReporter.reportFatalError(
                    "ERROR:",
                    "Duplicate unqualified archive names while attempting to save '"
                            + sourceArchive1.getOriginalPath()
                            + "' to '"
                            + key
                            + "'. Clash with '"
                            + sourceArchive2.getOriginalPath()
                            + "' ("
                            + string1
                            + ")"
            );
        }
    }

    public void copyRenamedEntry(ZipFile zipFile1, ZipEntry zipEntry1, ZipOutputStream zipOutputStream1, String string, String entryName, boolean boolean1Argument) throws ZkmException, IOException {
        Boolean boolean1 = false;
        this.copyRenamedEntry(zipFile1, zipEntry1, zipOutputStream1, string, entryName, boolean1Argument, boolean1);
    }
}
