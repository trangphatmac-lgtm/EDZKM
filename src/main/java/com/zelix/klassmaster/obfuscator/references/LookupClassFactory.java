package com.zelix.klassmaster.obfuscator.references;

import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.archive.SourceArchive;
import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.classfile.hierarchy.ClasspathClassLoader;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.parameters.LookupClassOption;
import com.zelix.klassmaster.obfuscator.rename.SequentialNameGenerator;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.CountingBag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.JavaRuntimeVersion;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObjectTriple;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMapView;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class LookupClassFactory {
    public static String describeLookupClasses(String string, Set set1, ListMultimap listMultimap) {
        String string1 = "\t" + string + " : Created special lookup class" + (set1.size() == 1 ? "" : "es") + " as ";
        String string2 = ZkmStringUtils.pad("\t", 76, string1.length(), 32);
        StringBuilder stringBuilder = new StringBuilder();
        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            ProgramClass programClass1 = (ProgramClass) iterator.next();
            StringBuilder stringBuilder1;
            char bb;
            if (stringBuilder.length() == 0) {
                stringBuilder.append(string1);
                stringBuilder1 = stringBuilder;
                bb = '\'';
            } else {
                stringBuilder.append(string2);
                stringBuilder1 = stringBuilder;
                bb = '\'';
            }

            stringBuilder1.append(bb);
            stringBuilder.append(programClass1.getDottedClassName());
            stringBuilder.append('\'');
            List list1 = listMultimap.getValues(programClass1);
            if (list1 != null) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    SourceArchive sourceArchive1 = (SourceArchive) list1.get(elementIndex);
                    if (elementIndex == 0) {
                        stringBuilder.append(" in module" + (list1.size() > 1 ? "s '" : " '"));
                    }

                    stringBuilder.append(sourceArchive1.getModuleName());
                    if (elementIndex < list1.size() - 1) {
                        stringBuilder.append("', ");
                    } else {
                        stringBuilder.append('\'');
                    }
                }

                stringBuilder.append(ZkmUtils.LINE_SEPARATOR);
            } else {
                stringBuilder.append(ZkmUtils.LINE_SEPARATOR);
            }
        }

        return stringBuilder.toString();
    }

    public static String describeLookupClassesByArchive(String string, Set set1, Map map1) {
        ListMultimap listMultimap = new ListMultimap();
        Iterator iterator = map1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            listMultimap.addValue(entry.getValue(), entry.getKey());
        }

        return describeLookupClasses(string, set1, listMultimap);
    }

    public static ProgramClass[] createLookupClasses(
            Set set1,
            Map map1,
            ObservableHolder observableHolder,
            ObservableHolder observableHolder1,
            Map map2,
            Map map3,
            Set set2,
            Map map4,
            int ba,
            int bb,
            SyntheticClassFactory syntheticClassFactory,
            ClassRepository classRepository1,
            LookupClassOption lookupClassOption
    ) throws ZkmException, IOException {
        ProgramClass[] programClass1 = null;
        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            LookupClassOption lookupClassOption1 = lookupClassOption;
            Integer integer1 = bb;
            Integer integer = 30276;
            ProgramClass programClass2 = syntheticClassFactory.createSyntheticClass(key, ba, integer, integer1, lookupClassOption1);
            programClass1 = classRepository1.addClass(programClass2);
            hashMap.put(key, programClass2);
            set2.add(programClass2);
            if (programClass2.supportsJava6()) {
                programClass2.markAllMethodsModified();
            }
        }

        if (lookupClassOption == null || lookupClassOption.shouldMapClassesToLookupClass()) {
            iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                ProgramClass programClass5 = (ProgramClass) entry.getKey();
                String value = (String) entry.getValue();
                ProgramClass programClass3 = (ProgramClass) hashMap.get(value);
                map3.put(programClass5, programClass3);
            }

            iterator = map4.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry1 = (Entry) iterator.next();
                SourceArchive sourceArchive1 = (SourceArchive) entry1.getKey();
                String valueValue = (String) entry1.getValue();
                ProgramClass programClass6 = (ProgramClass) hashMap.get(valueValue);
                map2.put(sourceArchive1, programClass6);
            }

            if (!observableHolder.isValueNull()) {
                ProgramClass programClass4 = (ProgramClass) hashMap.get(observableHolder.getValue());
                observableHolder1.setValue(programClass4);
            }
        }

        return programClass1;
    }

    public static void assignClassesToLookupNames(ObservableHolder observableHolder, Map map1, Set set1, Map map2, ReadOnlyMultiMapView readOnlyMultiMapView) {
        Iterator iterator = readOnlyMultiMapView.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            String value = (String) map1.get(sourceArchive1);
            Iterator iterator1 = ((List) entry.getValue()).iterator();

            while (iterator1.hasNext()) {
                ProgramClass programClass1 = (ProgramClass) iterator1.next();
                map2.put(programClass1, value);
            }
        }

        if (!set1.isEmpty()) {
            String valueValue = (String) observableHolder.getValue();
            Iterator iterator2 = set1.iterator();

            while (iterator2.hasNext()) {
                ProgramClass programClass2 = (ProgramClass) iterator2.next();
                map2.put(programClass2, valueValue);
            }
        }
    }

    public static void checkCommonPackage(SourceArchive sourceArchive1, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) throws ZkmProcessingException {
        if (setMultiMap1.getValues(sourceArchive1).isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("There are modules that have packages in common but there is no one package that is common to them all : '");
            Iterator iterator = setMultiMap.getValues(sourceArchive1).iterator();

            while (iterator.hasNext()) {
                SourceArchive sourceArchive2 = (SourceArchive) iterator.next();
                stringBuilder.append(sourceArchive2.getModuleName());
                stringBuilder.append(", ");
            }

            stringBuilder.append(sourceArchive1.getModuleName());
            stringBuilder.append('\'');
            throw new ZkmProcessingException(stringBuilder.toString());
        }
    }

    public static ReadOnlyMultiMapView filterModuleArchives(ReadOnlyMultiMapView readOnlyMultiMapView, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) {
        ListMultimap listMultimap = new ListMultimap(readOnlyMultiMapView.getKeyCount());
        SetMultiMap setMultiMap2 = new SetMultiMap(readOnlyMultiMapView.getKeyCount());
        Iterator iterator = readOnlyMultiMapView.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            if (sourceArchive1.hasModuleName()) {
                List list1 = (List) entry.getValue();
                listMultimap.appendValues(sourceArchive1, list1);
                Iterator iterator1 = list1.iterator();

                while (iterator1.hasNext()) {
                    ProgramClass programClass1 = (ProgramClass) iterator1.next();
                    setMultiMap2.addValue(sourceArchive1, programClass1.getPackagePath());
                }
            }
        }

        computeCommonPackages(setMultiMap1, setMultiMap, setMultiMap2);
        return new ReadOnlyMultiMapView(listMultimap);
    }

    public static Set assignLookupClassNames(
            Set set1,
            String string,
            ObservableHolder observableHolder,
            Map map1,
            Map map2,
            String string1,
            String string2,
            Set set2,
            Set set3,
            ReadOnlyMultiMapView readOnlyMultiMapView,
            SetMultiMap setMultiMap,
            SetMultiMap setMultiMap1,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            EnumerableMap enumerableMap2,
            Set set4,
            String string3,
            boolean bl,
            boolean bl1,
            List list1,
            ClasspathClassLoader classpathClassLoader1,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        String keyValue = null;
        if (string2 != null && string2.length() > 0) {
            String key = ZkmUtils.dotsToSlashes(string2);
            String keyValue2 = (String) enumerableMap.get(key);
            if (keyValue2 != null) {
                keyValue = keyValue2;
            } else {
                scriptEnvironment1.logWarning(
                        "Package '" + string2 + "' specified in '" + string1 + "' parameter but the package does not exist.  Another package will be used."
                );
            }
        }

        boolean bl2 = set2.size() > 0;
        HashSet hashSet = ZkmUtils.createHashSet((bl2 ? 1 : 0) + ZkmUtils.getPrimeCapacity(readOnlyMultiMapView.getKeyCount()));
        if (readOnlyMultiMapView.getKeyCount() == 0) {
            Boolean boolean2 = bl1;
            List list2 = list1;
            ClasspathClassLoader classpathClassLoader2 = classpathClassLoader1;
            Boolean boolean1 = bl;
            String string8 = string3;
            Set set7 = set4;
            EnumerableMap enumerableMap5 = enumerableMap2;
            EnumerableMap enumerableMap4 = enumerableMap1;
            EnumerableMap enumerableMap3 = enumerableMap;
            Set set6 = set1;
            String value = generateLookupClassName(
                    string,
                    keyValue,
                    (Collection) null,
                    set6,
                    enumerableMap3,
                    enumerableMap4,
                    enumerableMap5,
                    set7,
                    string8,
                    boolean1,
                    classpathClassLoader2,
                    list2,
                    boolean2
            );
            hashSet.add(value);
            observableHolder.setValue(value);
            Iterator iterator2 = set1.iterator();

            while (iterator2.hasNext()) {
                ProgramClass programClass1 = (ProgramClass) iterator2.next();
                if (!programClass1.isGenerated()) {
                    map2.put(programClass1, value);
                }
            }

            return hashSet;
        } else {
            Iterator iterator = readOnlyMultiMapView.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
                if (!map1.containsKey(sourceArchive1)) {
                    checkCommonPackage(sourceArchive1, setMultiMap, setMultiMap1);
                    if (!map1.containsKey(sourceArchive1)) {
                        String valueValue = null;
                        if (setMultiMap.containsKey(sourceArchive1)) {
                            Set set5 = setMultiMap.getValues(sourceArchive1);
                            Iterator iterator1 = set5.iterator();

                            while (iterator1.hasNext()) {
                                SourceArchive sourceArchive2 = (SourceArchive) iterator1.next();
                                if (map1.containsKey(sourceArchive2)) {
                                    valueValue = (String) map1.get(sourceArchive2);
                                    ZkmAssert.assertTrue(
                                            setMultiMap1.getValues(sourceArchive1).contains(ClassFileBase.getPackagePath(valueValue)),
                                            new String[]{"No common package names : '" + sourceArchive1.getModuleName() + "' and '" + sourceArchive2.getModuleName() + "'"}
                                    );
                                    break;
                                }
                            }
                        }

                        if (valueValue == null) {
                            Set set10 = setMultiMap1.getValues(sourceArchive1);
                            Collection collection1 = (Collection) entry.getValue();
                            Boolean boolean3 = bl1;
                            valueValue = generateLookupClassName(
                                    string,
                                    keyValue,
                                    set10,
                                    collection1,
                                    enumerableMap,
                                    enumerableMap1,
                                    enumerableMap2,
                                    set4,
                                    string3,
                                    bl,
                                    classpathClassLoader1,
                                    list1,
                                    boolean3
                            );
                        }

                        map1.put(sourceArchive1, valueValue);
                    }
                }
            }

            if (!set3.isEmpty() && observableHolder.isValueNull()) {
                HashSet hashSet1 = ZkmUtils.createHashSetFrom(set2);
                hashSet1.removeAll(set3);
                String string11 = null;
                Iterator iterator3 = hashSet1.iterator();

                while (iterator3.hasNext()) {
                    ProgramClass programClass2 = (ProgramClass) iterator3.next();
                    Enumeration enumeration = programClass2.enumerateInputLocations();

                    while (enumeration.hasMoreElements()) {
                        InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                        if (inputFileLocation.isArchiveEntry()) {
                            SourceArchive sourceArchive3 = inputFileLocation.getSourceArchive();
                            if (sourceArchive3.hasModuleName()) {
                                SourceArchive sourceArchive5 = sourceArchive3;
                                string11 = (String) map1.get(sourceArchive5);
                            }
                        }
                    }
                }

                String valueValue2 = string11;
                if (valueValue2 == null) {
                    Boolean boolean5 = bl1;
                    List list3 = list1;
                    ClasspathClassLoader classpathClassLoader3 = classpathClassLoader1;
                    Boolean boolean4 = bl;
                    String string9 = string3;
                    Set set9 = set4;
                    EnumerableMap enumerableMap8 = enumerableMap2;
                    EnumerableMap enumerableMap7 = enumerableMap1;
                    EnumerableMap enumerableMap6 = enumerableMap;
                    Set set8 = set3;
                    valueValue2 = generateLookupClassName(
                            string,
                            keyValue,
                            (Collection) null,
                            set8,
                            enumerableMap6,
                            enumerableMap7,
                            enumerableMap8,
                            set9,
                            string9,
                            boolean4,
                            classpathClassLoader3,
                            list3,
                            boolean5
                    );
                    hashSet.add(valueValue2);
                }

                observableHolder.setValue(valueValue2);
                Iterator iterator4 = set3.iterator();

                while (iterator4.hasNext()) {
                    ProgramClass programClass3 = (ProgramClass) iterator4.next();
                    map2.put(programClass3, valueValue2);
                }
            }

            iterator = readOnlyMultiMapView.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry1 = (Entry) iterator.next();
                SourceArchive sourceArchive4 = (SourceArchive) entry1.getKey();
                String valueValue3 = (String) map1.get(sourceArchive4);
                hashSet.add(valueValue3);
                Iterator iterator5 = ((List) entry1.getValue()).iterator();

                while (iterator5.hasNext()) {
                    ProgramClass programClass4 = (ProgramClass) iterator5.next();
                    if (!programClass4.isGenerated()) {
                        map2.put(programClass4, valueValue3);
                    }
                }
            }

            return hashSet;
        }
    }

    public static String validateChangeLogLookupName(
            String string, String string1, String string2, String string3, String string4, Set set1, ChangeLogMapping changeLogMapping1, EnumerableMap enumerableMap
    ) throws IOException {
        String key = ZkmUtils.dotsToSlashes(string1);
        if (enumerableMap.containsKey(key)) {
            changeLogMapping1.logError(
                    "The class '"
                            + string1
                            + "' appears in the change log as a "
                            + string4
                            + " lookup class but that name is already used. Another name will be used. You must distribute this application as a whole. (1)"
            );
            return null;
        }

        if (ClassHierarchyNode.findNode(key) != null) {
            ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(key);
            changeLogMapping1.logError(
                    "The class '"
                            + string1
                            + "' appears in the change log as a "
                            + string4
                            + " lookup class but that name is already used. Another name will be used. You must distribute this application as a whole."
                            + (programClass1 == null ? "" : " : " + programClass1.getCreationKind())
                            + " (2)"
                            + ""
            );
            return null;
        }

        String packagePath = ClassFileBase.getPackagePath(key);
        if (string3 != null) {
            String newPackageNameValue = ZkmUtils.dotsToSlashes(string3);
            String newPackageName = changeLogMapping1.getNewPackageName(newPackageNameValue);
            if (newPackageName == null && changeLogMapping1.hasPackageMapping(newPackageNameValue)) {
                newPackageName = newPackageNameValue;
            }

            if (newPackageName == null || !newPackageName.equals(packagePath)) {
                String string9 = "'"
                        + string2
                        + "' parameter in 'obfuscate' statement specifies package name '"
                        + string3
                        + "' but input change log '"
                        + changeLogMapping1.getChangeLogName()
                        + "' contains a '"
                        + string
                        + "' clause specifying package name '"
                        + ZkmUtils.slashesToDots(packagePath)
                        + "' which will be used.";
                changeLogMapping1.logWarning(string9);
            }
        }

        if (!set1.contains(packagePath)) {
            String string10 = ZkmUtils.toQuotedListString(set1);
            changeLogMapping1.logError(
                    "The class '"
                            + string1
                            + "' appears in the change log '"
                            + string
                            + "' clause as a "
                            + string4
                            + " lookup class but it is not in a suitable package. Another name will be used. You must distribute this application as a whole. : {"
                            + string10
                            + "}"
            );
            return null;
        } else {
            return key;
        }
    }

    public static void findMinClassVersion(MutableInt mutableInt, MutableInt mutableInt1, ClassFileBase[] classFileBases, Integer integer) {
        int maxValue = Integer.MAX_VALUE;
        int maxValueValue = Integer.MAX_VALUE;

        for (ClassFileBase classFileBase : classFileBases) {
            int majorVersion = classFileBase.getMajorVersion();
            if (majorVersion < maxValue) {
                maxValue = majorVersion;
                maxValueValue = classFileBase.getMinorVersion();
            } else if (majorVersion == maxValue && classFileBase.getMinorVersion() < maxValueValue) {
                maxValueValue = classFileBase.getMinorVersion();
            }
        }

        if (integer != null) {
            int maxValueValue2 = JavaRuntimeVersion.toClassMajorVersion(integer);
            if (maxValueValue2 > maxValue) {
                maxValue = maxValueValue2;
                maxValueValue = 0;
            }
        }

        mutableInt.setValue(maxValue);
        mutableInt1.setValue(maxValueValue);
    }

    public static Set partitionClassesByModule(Set set1, Set set2, Set set3, Set set4) throws ZkmClassNotFoundException {
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(set1.size()));
        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            ProgramClass programClass1 = (ProgramClass) iterator.next();
            if (!programClass1.isGenerated()) {
                ArrayList arrayList = new ArrayList();
                if (!programClass1.isMultiRelease()) {
                    hashSet.add(programClass1);
                } else {
                    Iterator iterator1 = programClass1.getAllVersions().iterator();

                    while (iterator1.hasNext()) {
                        ClassFileBase classFileBase = (ClassFileBase) iterator1.next();
                        arrayList.add((ProgramClass) classFileBase);
                    }

                    hashSet.addAll(arrayList);
                }

                boolean bl = false;
                boolean bl1 = false;
                Enumeration enumeration = programClass1.enumerateInputLocations();

                while (enumeration.hasMoreElements()) {
                    InputFileLocation inputFileLocation = (InputFileLocation) enumeration.nextElement();
                    if (inputFileLocation.isArchiveEntry()) {
                        if (inputFileLocation.getSourceArchive().hasModuleName()) {
                            bl = true;
                        } else {
                            bl1 = true;
                        }
                    } else {
                        bl1 = true;
                    }
                }

                if (bl1) {
                    if (programClass1.isMultiRelease()) {
                        set2.addAll(arrayList);
                        if (!bl) {
                            set3.addAll(arrayList);
                        }
                    } else {
                        set2.add(programClass1);
                        if (!bl) {
                            set3.add(programClass1);
                        }
                    }
                }
            }
        }

        iterator = set2.iterator();

        while (iterator.hasNext()) {
            ProgramClass programClass2 = (ProgramClass) iterator.next();
            set4.add(programClass2.getPackagePath());
        }

        return hashSet;
    }

    public static String describeLookupClassTriples(String string, ListMultimap listMultimap, Set set1) {
        String string1 = "\t" + string + " : Created special lookup classes as ";
        String string2 = ZkmStringUtils.pad("\t", 76, string1.length(), 32);
        StringBuilder stringBuilder = new StringBuilder();
        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            ObjectTriple objectTriple = (ObjectTriple) iterator.next();
            StringBuilder stringBuilder1;
            char bb;
            if (stringBuilder.length() == 0) {
                stringBuilder.append(string1);
                stringBuilder1 = stringBuilder;
                bb = '\'';
            } else {
                stringBuilder.append(string2);
                stringBuilder1 = stringBuilder;
                bb = '\'';
            }

            stringBuilder1.append(bb);
            stringBuilder.append(((ProgramClass) objectTriple.getFirst()).getDottedClassName());
            stringBuilder.append("', '");
            stringBuilder.append(((ProgramClass) objectTriple.getSecond()).getDottedClassName());
            stringBuilder.append("' and '");
            stringBuilder.append(((ProgramClass) objectTriple.getThird()).getDottedClassName());
            stringBuilder.append('\'');
            List list1 = listMultimap.getValues(objectTriple);
            if (list1 != null) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    SourceArchive sourceArchive1 = (SourceArchive) list1.get(elementIndex);
                    if (elementIndex == 0) {
                        stringBuilder.append(" in module" + (list1.size() > 1 ? "s '" : " '"));
                    }

                    stringBuilder.append(sourceArchive1.getModuleName());
                    if (elementIndex < list1.size() - 1) {
                        stringBuilder.append("', ");
                    } else {
                        stringBuilder.append('\'');
                    }
                }

                stringBuilder.append(ZkmUtils.LINE_SEPARATOR);
            } else {
                stringBuilder.append(ZkmUtils.LINE_SEPARATOR);
            }
        }

        return stringBuilder.toString();
    }

    public static String generateLookupClassName(
            String string,
            String keyArgument,
            Collection collection1,
            Collection collection2,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            EnumerableMap enumerableMap2,
            Set set1,
            String string2,
            boolean bl,
            ClasspathClassLoader classpathClassLoader1,
            List list1,
            Boolean boolean1
    ) throws ZkmException, IOException {
        HashSet hashSet = ZkmUtils.createHashSet();
        Iterator iterator = collection2.iterator();

        while (iterator.hasNext()) {
            ProgramClass programClass1 = (ProgramClass) iterator.next();
            hashSet.add(programClass1.getPackagePath());
        }

        if (string != null && hashSet.contains(ClassFileBase.getPackagePath(string))) {
            return string;
        }

        String keyValue = null;
        if (keyArgument != null && hashSet.contains(keyArgument)) {
            keyValue = keyArgument;
        }

        int collection2Size = collection2.size();
        if (keyValue == null) {
            HashSet hashSet1 = null;
            if (collection1 != null) {
                hashSet1 = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(collection1.size()));
                Iterator iterator1 = collection1.iterator();

                while (iterator1.hasNext()) {
                    String keyValue2 = (String) iterator1.next();
                    String string5 = (String) ZkmUtils.mapOrSelf(keyValue2, enumerableMap);
                    hashSet1.add(string5);
                }
            }

            CountingBag countingBag = new CountingBag(collection2Size);
            Iterator iterator3 = collection2.iterator();

            while (iterator3.hasNext()) {
                ProgramClass programClass3 = (ProgramClass) iterator3.next();
                if (hashSet1 == null || hashSet1.contains(programClass3.getPackagePath())) {
                    programClass3.countReferencedProgramClasses(countingBag);
                    countingBag.add(programClass3);
                }
            }

            iterator3 = countingBag.iterator();

            while (iterator3.hasNext()) {
                ProgramClass programClass4 = (ProgramClass) iterator3.next();
                if (!collection2.contains(programClass4)) {
                    iterator3.remove();
                }
            }

            CountingBag countingBag1 = new CountingBag();
            CountingBag countingBag2 = new CountingBag();
            Iterator iterator2 = countingBag.iterator();

            while (iterator2.hasNext()) {
                ProgramClass programClass2 = (ProgramClass) iterator2.next();
                String packagePath = programClass2.getPackagePath();
                countingBag1.add(packagePath);
                int value = countingBag1.getCount(packagePath) + countingBag.getCount(programClass2) - 1;
                String packagePathValue = packagePath;
                countingBag1.addCount(packagePathValue, value);
                if (programClass2.isPublic()) {
                    countingBag2.add(packagePath);
                    value = countingBag2.getCount(packagePath) + countingBag.getCount(programClass2) - 1;
                    packagePathValue = packagePath;
                    countingBag2.addCount(packagePathValue, value);
                }
            }

            List list2 = countingBag2.getSortedByCount();
            if (list2.size() > 0) {
                keyValue = (String) list2.get(0);
            } else {
                List list3 = countingBag1.getSortedByCount();
                if (list3.size() > 0) {
                    keyValue = (String) list3.get(0);
                }
            }

            if (keyValue == null) {
                keyValue = "";
            }
        }

        TwoKeyMap twoKeyMap = new TwoKeyMap();
        HashSet hashSet2 = ZkmUtils.createHashSet();
        HashSet hashSet3 = ZkmUtils.createHashSet();
        Iterator iterator4 = collection2.iterator();

        while (iterator4.hasNext()) {
            ProgramClass programClass5 = (ProgramClass) iterator4.next();
            twoKeyMap.putValue(programClass5.getPackagePath(), programClass5.getSimpleName(), programClass5);
            String lowerCaseName = programClass5.getLowerCaseName();
            hashSet2.add(lowerCaseName);
            hashSet3.add(ClassFileBase.stripPackage(lowerCaseName));
        }

        Map map1 = twoKeyMap.getInnerMap(keyValue);
        SequentialNameGenerator sequentialNameGenerator = new SequentialNameGenerator(
                SequentialNameGenerator.LOWER_UNDERSCORE_CHARS, SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS, list1, boolean1
        );
        String keyValue4 = null;

        do {
            String key = sequentialNameGenerator.nextPrefixedName(string2);
            String keyValue3 = (keyValue.length() > 0 ? keyValue + '/' : "") + key;
            if ((!bl || !hashSet3.contains(key))
                    && !set1.contains(keyValue3)
                    && (enumerableMap1 == null || !enumerableMap1.containsKey(keyValue3))
                    && (enumerableMap2 == null || !enumerableMap2.containsKey(keyValue3))
                    && (map1 == null || !map1.containsKey(key))
                    && !hashSet2.contains(keyValue3.toLowerCase())
                    && ClassHierarchyNode.findClassFile(keyValue3) == null
                    && classpathClassLoader1.findClassFileQuietly(keyValue3) == null) {
                keyValue4 = keyValue3;
            }
        } while (keyValue4 == null);

        set1.add(keyValue4);
        return keyValue4;
    }

    public static void computeCommonPackages(SetMultiMap setMultiMap, SetMultiMap setMultiMap1, SetMultiMap setMultiMap2) {
        SetMultiMap setMultiMap3 = new SetMultiMap();
        Iterator iterator = setMultiMap2.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            SourceArchive sourceArchive1 = (SourceArchive) entry.getKey();
            Set set1 = (Set) entry.getValue();
            setMultiMap.addValues(sourceArchive1, set1);
            Iterator iterator1 = set1.iterator();

            while (iterator1.hasNext()) {
                String key = (String) iterator1.next();
                setMultiMap3.addValue(key, sourceArchive1);
            }
        }

        iterator = setMultiMap3.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            Set set3 = (Set) entry1.getValue();
            if (set3.size() > 1) {
                Iterator iterator3 = set3.iterator();
                HashSet hashSet = ZkmUtils.createHashSetFrom(setMultiMap.getValues(iterator3.next()));

                while (iterator3.hasNext()) {
                    SourceArchive sourceArchive3 = (SourceArchive) iterator3.next();
                    Set set2 = setMultiMap.getValues(sourceArchive3);
                    hashSet.retainAll(set2);
                }

                Iterator iterator4 = set3.iterator();

                while (iterator4.hasNext()) {
                    SourceArchive sourceArchive4 = (SourceArchive) iterator4.next();
                    setMultiMap.putValueSet(sourceArchive4, ZkmUtils.createHashSetFrom(hashSet));
                }

                iterator4 = set3.iterator();

                while (iterator4.hasNext()) {
                    SourceArchive sourceArchive5 = (SourceArchive) iterator4.next();
                    Iterator iterator2 = set3.iterator();

                    while (iterator2.hasNext()) {
                        SourceArchive sourceArchive2 = (SourceArchive) iterator2.next();
                        if (sourceArchive5 != sourceArchive2) {
                            setMultiMap1.addValue(sourceArchive5, sourceArchive2);
                        }
                    }
                }
            }
        }
    }

    private LookupClassFactory() {
    }
}
