package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassPathResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.MixedCaseNamesMode;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SystemEnvironmentConstants;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

public class RootPackageNode extends PackageTreeNodeBase {
    public void buildFromClasses(ClassFileBase[] classFileBases) {
        super.children.clear();

        for (int classFileBaseIndex = 0; classFileBaseIndex < classFileBases.length; classFileBaseIndex++) {
            this.addClassPackage(classFileBases[classFileBaseIndex].getClassName());
        }

        this.markArchiveOnlyPackages(classFileBases);
    }

    public void addClassPackageWithFlags(String string, Map map1) {
        int endIndex = string.lastIndexOf(47);
        if (endIndex != -1) {
            String[] strings = splitPackagePath(string.substring(0, endIndex));
            if (strings.length > 0) {
                this.addPackagePath(0, strings, map1);
            }
        }
    }

    public HashMap assignPackageNames(
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            PackageNameGeneratorBase packageNameGeneratorBase,
            ListMultimap listMultimap,
            boolean bl,
            Map map1,
            boolean bl1
    ) throws IOException {
        java.lang.Object[] packageTreeNodes = null;
        List list1 = this.getAllPackagePaths();
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(list1.size()));
        ListMultimap listMultimap1 = new ListMultimap(list1.size());

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            String string = (String) list1.get(elementIndex);
            if (nameExclusionSet.isPackageExcluded(string)) {
                hashMap.put(string, string);
                listMultimap1.addValue(string, string);
            } else if (changeLogMapping1 != null && changeLogMapping1.hasPackageMapping(string)) {
                String newPackageName = changeLogMapping1.getNewPackageName(string);
                if (newPackageName != null) {
                    hashMap.put(string, newPackageName);
                    listMultimap1.addValue(newPackageName, string);
                } else {
                    hashMap.put(string, string);
                    listMultimap1.addValue(string, string);
                }
            } else if (map1 != null && map1.containsKey(string)) {
                NamedFlag namedFlag = (NamedFlag) map1.get(string);
                if (!namedFlag.isBlocked()) {
                    String nameValue = namedFlag.getName();
                    hashMap.put(string, nameValue);
                    listMultimap1.addValue(nameValue, string);
                }
            }
        }

        TwoKeyMap twoKeyMap = new TwoKeyMap();
        Enumeration enumeration = listMultimap1.keys();

        while (enumeration.hasMoreElements()) {
            String keyValue = (String) enumeration.nextElement();
            List list3 = listMultimap1.getValues(keyValue);

            for (int elementIndexValue = 0; elementIndexValue < list3.size(); elementIndexValue++) {
                String keyValue2 = (String) list3.get(elementIndexValue);
                List list2 = listMultimap.getValues(keyValue2);
                if (list2 != null) {
                    for (int elementIndexValue2 = 0; elementIndexValue2 < list2.size(); elementIndexValue2++) {
                        ProgramClass programClass1 = (ProgramClass) list2.get(elementIndexValue2);
                        if (nameExclusionSet.isClassExcluded(programClass1)) {
                            twoKeyMap.putValue(keyValue, programClass1.getSimpleName(), programClass1);
                        }
                    }
                }
            }
        }

        if (!bl) {
            packageTreeNodes = super.children.toArray(new PackageTreeNode[super.children.size()]);
            if (!bl1 && !HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
                Arrays.sort(packageTreeNodes);
            } else {
                ZkmUtils.shuffleList(Arrays.asList(packageTreeNodes), ZkmUtils.createRandom(8));
            }

            for (int packageTreeNodeIndex = 0; packageTreeNodeIndex < packageTreeNodes.length; packageTreeNodeIndex++) {
                PackageTreeNode packageTreeNode = ((com.zelix.klassmaster.obfuscator.rename.PackageTreeNode) (packageTreeNodes[packageTreeNodeIndex]));
                packageTreeNode.assignNewNamesRecursively(
                        nameExclusionSet, changeLogMapping1, packageNameGeneratorBase, hashMap, listMultimap1, twoKeyMap, listMultimap, bl1
                );
            }
        } else {
            Map map2 = this.getNodesByPath();
            Iterator iterator = map2.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String key = (String) entry.getKey();
                String value = (String) hashMap.get(key);
                if (value == null) {
                    NamedFlag namedFlag1 = (NamedFlag) map1.get(key);
                    String nameValue2 = namedFlag1.getName();
                    PackageTreeNodeBase packageTreeNodeBase = (PackageTreeNodeBase) map2.get(nameValue2);
                    if (packageTreeNodeBase == null) {
                    }

                    boolean archiveOnly = ((PackageTreeNode) entry.getValue()).isArchiveOnly();
                    ChangeLogMapping changeLogMapping2 = changeLogMapping1;
                    value = packageNameGeneratorBase.generatePackageName(
                            nameValue2, archiveOnly, false, listMultimap1, twoKeyMap, (Set) null, changeLogMapping2, nameExclusionSet
                    );
                    if (nameValue2.length() > 0) {
                        value = nameValue2 + "/" + value;
                    }

                    hashMap.put(key, value);
                    listMultimap1.addValue(value, key);
                }
            }
        }

        this.rebuildFromRenamedPackages(listMultimap1);
        return hashMap;
    }

    public void checkChangeLogPackageClashes(TwoKeyMap twoKeyMap, NameExclusionSet nameExclusionSet, ChangeLogMapping changeLogMapping1) throws IOException {
        if (changeLogMapping1 != null) {
            ArrayList arrayList = changeLogMapping1.getSortedPackageNames();
            int arrayListSize = arrayList.size();

            for (int elementIndex = 0; elementIndex < arrayListSize; elementIndex++) {
                String newPackageNameValue2 = (String) arrayList.get(elementIndex);
                String newPackageName = changeLogMapping1.getNewPackageName(newPackageNameValue2);
                if (newPackageName == null) {
                    newPackageName = newPackageNameValue2;
                }

                if (!nameExclusionSet.isPackageExcluded(newPackageNameValue2) && nameExclusionSet.isClassNameExcluded(newPackageName)) {
                    changeLogMapping1.reportFatalError(
                            "Package \""
                                    + ZkmUtils.slashesToDots(newPackageNameValue2)
                                    + "\" is mapped to \""
                                    + ZkmUtils.slashesToDots(newPackageName)
                                    + "\" but this would result in a clash with class \""
                                    + ZkmUtils.slashesToDots(newPackageName)
                                    + "\" that will not be renamed."
                    );
                }
            }

            HashMap hashMap = ZkmUtils.createHashMap();
            Enumeration enumeration = twoKeyMap.keys();

            while (enumeration.hasMoreElements()) {
                String keyValue2 = (String) enumeration.nextElement();
                if (nameExclusionSet.isPackageExcluded(keyValue2) || changeLogMapping1.hasPackageMapping(keyValue2) && !changeLogMapping1.hasNewPackageName(keyValue2)) {
                    Map map1 = twoKeyMap.getInnerMap(keyValue2);
                    if (map1 != null) {
                        Iterator iterator = map1.entrySet().iterator();

                        while (iterator.hasNext()) {
                            Entry entry = (Entry) iterator.next();
                            String value = (String) entry.getValue();
                            hashMap.put(value, value);
                        }
                    }
                }
            }

            enumeration = twoKeyMap.keys();

            while (enumeration.hasMoreElements()) {
                String keyValue3 = (String) enumeration.nextElement();
                if (!nameExclusionSet.isPackageExcluded(keyValue3) && changeLogMapping1.hasPackageMapping(keyValue3) && changeLogMapping1.hasNewPackageName(keyValue3)) {
                    String newPackageNameValue = changeLogMapping1.getNewPackageName(keyValue3);
                    Map map2 = twoKeyMap.getInnerMap(keyValue3);
                    if (map2 != null) {
                        Iterator iterator1 = map2.entrySet().iterator();

                        while (iterator1.hasNext()) {
                            Entry entry1 = (Entry) iterator1.next();
                            String key = (String) entry1.getKey();
                            String valueValue = (String) entry1.getValue();
                            String keyValue = newPackageNameValue + key;
                            String valueValue2 = ((java.lang.String) (hashMap.put(keyValue, valueValue)));
                            if (valueValue2 != null) {
                                hashMap.put(keyValue, valueValue2);
                                changeLogMapping1.reportFatalError(
                                        "Package \""
                                                + ZkmUtils.slashesToDots(keyValue3)
                                                + "\" is mapped to \""
                                                + ZkmUtils.slashesToDots(newPackageNameValue)
                                                + "\" but this would result in \""
                                                + valueValue2
                                                + "\" clashing with \""
                                                + valueValue
                                                + "\""
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public Map buildCollapseMappings(
            ListMultimap listMultimap,
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            String value,
            TwoKeyMap twoKeyMap,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        HashMap hashMap = this.buildCollapseTargetMap(nameExclusionSet, changeLogMapping1, value);
        HashMap hashMap1 = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(hashMap.size()));
        ListMultimap listMultimap1 = new ListMultimap();
        Iterator iterator = hashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            listMultimap1.addValue(entry.getValue(), entry.getKey());
        }

        Enumeration enumeration = listMultimap1.keys();

        while (enumeration.hasMoreElements()) {
            String string2 = (String) enumeration.nextElement();
            Set set1 = null;
            if (changeLogMapping1 != null) {
                set1 = changeLogMapping1.getPackagesMappedTo(string2);
            }

            if (set1 == null) {
                set1 = ZkmUtils.createHashSet();
            }

            set1.add(string2);
            ObservableHolder observableHolder = new ObservableHolder();
            HashMap hashMap2 = ZkmUtils.createHashMap();
            HashMap hashMap3 = ZkmUtils.createHashMap();
            Iterator iterator1 = set1.iterator();

            while (iterator1.hasNext()) {
                String keyValue = (String) iterator1.next();
                List list1 = listMultimap.getValues(keyValue);
                if (list1 != null && list1.size() > 0) {
                    boolean bl = this.checkSimpleNameClashes(hashMap2, list1, nameExclusionSet, changeLogMapping1, observableHolder);
                    if (bl) {
                        changeLogMapping1.reportFatalError("Package mapping conflicts with class name exclusions : " + (String) observableHolder.getValue());
                    }
                }

                Map map1 = twoKeyMap.getInnerMap(keyValue);
                if (map1 != null && map1.size() > 0) {
                    boolean bl1 = this.checkNonClassFileClashes(hashMap3, map1, observableHolder);
                    if (bl1) {
                        changeLogMapping1.reportFatalError("Package mapping conflict : " + (String) observableHolder.getValue());
                    }
                }
            }

            List list2 = listMultimap1.getValues(string2);
            Collections.sort(list2);
            if (list2.size() > 0) {
                for (int elementIndex = 0; elementIndex < list2.size(); elementIndex++) {
                    String key = (String) list2.get(elementIndex);
                    List list3 = listMultimap.getValues(key);
                    NamedFlag namedFlag;
                    if (list3 != null) {
                        boolean bl2 = this.checkSimpleNameClashes(hashMap2, list3, nameExclusionSet, changeLogMapping1, observableHolder);
                        if (bl2) {
                            namedFlag = new NamedFlag(string2, true);
                            hashMap1.put(key, namedFlag);
                            scriptEnvironment1.logWarning(
                                    "Cannot collapse package '"
                                            + ZkmUtils.slashesToDots(key)
                                            + "' into '"
                                            + ZkmUtils.slashesToDots(string2)
                                            + "' because : \""
                                            + (String) observableHolder.getValue()
                                            + "\""
                            );
                        } else {
                            namedFlag = new NamedFlag(string2);
                            hashMap1.put(key, namedFlag);
                        }
                    } else {
                        namedFlag = new NamedFlag(string2);
                        hashMap1.put(key, namedFlag);
                    }

                    Map map2 = twoKeyMap.getInnerMap(key);
                    if (map2 != null && map2.size() > 0) {
                        boolean bl3 = this.checkNonClassFileClashes(hashMap3, map2, observableHolder);
                        if (bl3) {
                            if (!namedFlag.isBlocked()) {
                                namedFlag.setBlocked();
                            }

                            scriptEnvironment1.logWarning(
                                    "Cannot collapse package '"
                                            + ZkmUtils.slashesToDots(key)
                                            + "' into '"
                                            + ZkmUtils.slashesToDots(string2)
                                            + "' because : \""
                                            + (String) observableHolder.getValue()
                                            + "\""
                            );
                        }
                    }
                }
            } else {
                hashMap1.put(list2.get(0), new NamedFlag(string2));
            }
        }

        return hashMap1;
    }

    @Override
    public void indexInto(Object object) {
    }

    public Map getNodesByPath() {
        HashMap hashMap = ZkmUtils.createHashMap();
        this.indexAllNodes(hashMap);
        return hashMap;
    }

    @Override
    public String getPackagePath() {
        return "";
    }

    public void reportUnknownChangeLogPackages(ChangeLogMapping changeLogMapping1, Set set1) throws IOException {
        if (changeLogMapping1 != null) {
            ListIterator listIterator = new ArrayList(set1).listIterator();

            while (listIterator.hasNext()) {
                String string = (String) listIterator.next();
                String[] parentPackagePaths = getParentPackagePaths(string);

                for (int parentPackagePathIndex = 0; parentPackagePathIndex < parentPackagePaths.length; parentPackagePathIndex++) {
                    set1.add(parentPackagePaths[parentPackagePathIndex]);
                }
            }

            Map map1 = this.getNodesByPath();
            ArrayList arrayList = changeLogMapping1.getSortedPackageNames();

            for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                String key = (String) arrayList.get(elementIndex);
                if (!map1.containsKey(key) && !set1.contains(key)) {
                    changeLogMapping1.removePackageMapping(key);
                }
            }
        }
    }

    public static String[] getParentPackagePaths(String string) {
        String[] strings = splitPackagePath(string);
        int ba = Math.max(0, strings.length - 1);
        String[] parentPackagePaths = new String[ba];
        StringBuffer stringBuffer = new StringBuffer();

        for (int parentPackagePathIndex = 0; parentPackagePathIndex < ba; parentPackagePathIndex++) {
            if (parentPackagePathIndex > 0) {
                stringBuffer.append('/');
            }

            stringBuffer.append(strings[parentPackagePathIndex]);
            parentPackagePaths[parentPackagePathIndex] = stringBuffer.toString();
        }

        return parentPackagePaths;
    }

    public void markArchiveOnlyPackages(ClassFileBase[] classFileBases) {
        Map map1 = this.getNodesByPath();
        ListMultimap listMultimap = new ListMultimap(map1.size());

        for (ClassFileBase classFileBase : classFileBases) {
            PackageTreeNode packageTreeNode = (PackageTreeNode) map1.get(classFileBase.getPackagePath());
            if (packageTreeNode != null) {
                listMultimap.addValue(packageTreeNode, classFileBase);

                for (PackageTreeNodeBase packageTreeNodeBase = packageTreeNode.parent;
                     !packageTreeNodeBase.isRoot();
                     packageTreeNodeBase = ((PackageTreeNode) packageTreeNodeBase).parent
                ) {
                    listMultimap.addValue((PackageTreeNode) packageTreeNodeBase, classFileBase);
                }
            }
        }

        Iterator iterator = listMultimap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            PackageTreeNode packageTreeNode1 = (PackageTreeNode) entry.getKey();
            boolean archiveOnlyValue = true;
            Iterator iterator1 = ((List) entry.getValue()).iterator();

            while (iterator1.hasNext()) {
                ClassFileBase classFileBase1 = (ClassFileBase) iterator1.next();
                if (!classFileBase1.isFromArchive()) {
                    archiveOnlyValue = false;
                    break;
                }
            }

            packageTreeNode1.setArchiveOnly(archiveOnlyValue);
        }
    }

    public HashMap renamePackages(
            Enumeration enumeration,
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            boolean randomizeNames,
            Boolean boolean1,
            Boolean boolean2,
            String namePrefix,
            MixedCaseNamesMode mixedCaseNamesMode1,
            ClassPathResolver classPathResolver1,
            boolean bl1,
            String value,
            Iterator iterator,
            List list1,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        TwoKeyMap twoKeyMap = this.groupNonClassFilesByPackage(iterator);
        this.checkChangeLogPackageClashes(twoKeyMap, nameExclusionSet, changeLogMapping1);
        Map map1 = null;
        Map map2 = this.getNodesByPath();
        ListMultimap listMultimap = new ListMultimap();

        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String packagePath = programClass1.getPackagePath();
            listMultimap.addValue(packagePath, programClass1);
            if (programClass1.isPackageReferencedFromXml()) {
                PackageTreeNode packageTreeNode = (PackageTreeNode) map2.get(packagePath);
                if (packageTreeNode != null) {
                    packageTreeNode.getTopLevelAncestor().setRequireLowerCaseStart();
                }
            }
        }

        if (bl1) {
            map1 = this.buildCollapseMappings(listMultimap, nameExclusionSet, changeLogMapping1, value, twoKeyMap, scriptEnvironment1);
        }

        PackageNameGenerator packageNameGenerator = new PackageNameGenerator(classPathResolver1, boolean1, boolean2, randomizeNames, namePrefix, mixedCaseNamesMode1, list1);
        HashMap hashMap = this.assignPackageNames(nameExclusionSet, changeLogMapping1, packageNameGenerator, listMultimap, bl1, map1, randomizeNames);
        if (changeLogMapping1 != null) {
            ArrayList arrayList = changeLogMapping1.getSortedPackageNames();

            for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                String key = (String) arrayList.get(elementIndex);
                if (!hashMap.containsKey(key)) {
                    String newPackageName = changeLogMapping1.getNewPackageName(key);
                    if (newPackageName == null) {
                        newPackageName = key;
                    }

                    hashMap.put(key, newPackageName);
                }
            }
        }

        return hashMap;
    }

    public void addClassPackage(String string) {
        this.addClassPackageWithFlags(string, (Map) null);
    }

    public boolean checkSimpleNameClashes(
            Map map1, List list1, NameExclusionSet nameExclusionSet, ChangeLogMapping changeLogMapping1, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        boolean bl = false;
        ArrayList arrayList = new ArrayList();

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            ProgramClass programClass1 = (ProgramClass) list1.get(elementIndex);
            String className = programClass1.getClassName();
            if (changeLogMapping1 != null && changeLogMapping1.hasClassMapping(className)) {
                String newClassName = changeLogMapping1.getNewClassName(className);
                if (newClassName == null) {
                    newClassName = className;
                }

                String key = ClassFileBase.stripPackage(newClassName);
                ProgramClass programClass3 = ((com.zelix.klassmaster.classfile.ProgramClass) (map1.put(key, programClass1)));
                if (programClass3 != null && programClass3 != programClass1) {
                    map1.put(key, programClass3);
                    observableHolder.setValue(
                            "Unqualified class names for classes '"
                                    + programClass3.getDottedClassName()
                                    + "' and '"
                                    + programClass1.getDottedClassName()
                                    + "' would clash (A)"
                    );
                    bl = true;
                    break;
                }

                arrayList.add(key);
            } else if (nameExclusionSet.isClassExcluded(programClass1) || programClass1.getSimpleName().equals("package-info")) {
                String simpleName = programClass1.getSimpleName();
                ProgramClass programClass2 = ((com.zelix.klassmaster.classfile.ProgramClass) (map1.put(programClass1.getSimpleName(), programClass1)));
                if (programClass2 != null && programClass2 != programClass1) {
                    map1.put(simpleName, programClass2);
                    observableHolder.setValue(
                            "Unqualified class names for classes '"
                                    + programClass2.getDottedClassName()
                                    + "' and '"
                                    + programClass1.getDottedClassName()
                                    + "' would clash (B)"
                    );
                    bl = true;
                    break;
                }

                arrayList.add(simpleName);
            }
        }

        if (bl) {
            for (int elementIndexValue = 0; elementIndexValue < arrayList.size(); elementIndexValue++) {
                map1.remove(arrayList.get(elementIndexValue));
            }
        }

        return bl;
    }

    public static String normalizePackageName(String string, ObservableHolder observableHolder) throws ZkmException, IOException {
        String string1 = string.trim();
        String value = null;
        if (string1.length() > 0) {
            string1 = string1.replace('.', '/');
            if (string1.startsWith("/")) {
                value = "Leading package separator character";
            } else if (string1.indexOf("//") > -1) {
                value = "Duplicate package separator character";
            } else {
                if (string1.endsWith("/")) {
                    string1 = string1.substring(0, string1.length() - 1);
                }

                StringTokenizer stringTokenizer = new StringTokenizer(string1, "/");

                while (stringTokenizer.hasMoreTokens() && value == null) {
                    String string3 = stringTokenizer.nextToken();

                    for (int characterIndex = 0; characterIndex < string3.length(); characterIndex++) {
                        if (!Character.isJavaIdentifierPart(string3.charAt(characterIndex))) {
                            value = "Invalid character '" + string3.charAt(characterIndex) + "'";
                            break;
                        }
                    }
                }
            }
        }

        observableHolder.setValue(value);
        return string1;
    }

    public boolean hasNoChildren() {
        return super.children == null || super.children.size() == 0;
    }

    public static String[] splitPackagePath(String string) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, "/");
        int ba = stringTokenizer.countTokens();
        String[] strings = new String[ba];

        for (int i = 0; i < ba; i++) {
            strings[i] = stringTokenizer.nextToken();
        }

        return strings;
    }

    public void rebuildFromRenamedPackages(ListMultimap listMultimap) {
        Map map1 = this.getNodesByPath();
        super.children.clear();
        TreeMap treeMap = new TreeMap();
        Iterator iterator = listMultimap.keySet().iterator();

        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            boolean value = true;
            Iterator iterator1 = listMultimap.getValues(key).iterator();

            while (iterator1.hasNext()) {
                String keyValue = (String) iterator1.next();
                if (!((PackageTreeNode) map1.get(keyValue)).isArchiveOnly()) {
                    value = false;
                }
            }

            treeMap.put(key, value);
        }

        iterator = treeMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            this.addClassPackageWithFlags((String) entry.getKey() + '/', treeMap);
        }
    }

    public HashMap buildRetainedPackageMap(ChangeLogMapping changeLogMapping1) throws IOException {
        HashMap hashMap = ZkmUtils.createHashMap();
        if (changeLogMapping1 != null) {
            ArrayList arrayList = changeLogMapping1.getSortedPackageNames();
            Iterator iterator = arrayList.iterator();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String newPackageName = changeLogMapping1.getNewPackageName(key);
                if (newPackageName == null) {
                    newPackageName = key;
                }

                hashMap.put(key, newPackageName);
            }
        }

        List list1 = this.getAllPackagePaths();
        Iterator iterator1 = list1.iterator();

        while (iterator1.hasNext()) {
            String string2 = (String) iterator1.next();
            if (!hashMap.containsKey(string2)) {
                hashMap.put(string2, string2);
            }
        }

        return hashMap;
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    public List getAllPackagePaths() {
        ArrayList arrayList = new ArrayList();
        this.collectAllPaths(arrayList);
        return arrayList;
    }

    public TwoKeyMap groupNonClassFilesByPackage(Iterator iterator) {
        TwoKeyMap twoKeyMap = new TwoKeyMap();
        List list1 = this.getAllPackagePaths();
        Collections.sort(list1, Collections.reverseOrder());

        while (iterator.hasNext()) {
            String value = (String) iterator.next();
            value = value.replace(SystemEnvironmentConstants.PATH_SEPARATOR_CHAR, '/');
            boolean bl = false;

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                String prefix = (String) list1.get(elementIndex);
                if (value.startsWith(prefix) && value.length() > prefix.length() && value.charAt(prefix.length()) == '/') {
                    twoKeyMap.putValue(prefix, value.substring(prefix.length()), value);
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                String key;
                if (value.charAt(0) == '/') {
                    key = value;
                } else {
                    key = '/' + value;
                }

                twoKeyMap.putValue("", key, value);
            }
        }

        return twoKeyMap;
    }

    public HashMap buildCollapseTargetMap(NameExclusionSet nameExclusionSet, ChangeLogMapping changeLogMapping1, String valueArgument) throws IOException {
        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator = super.children.iterator();

        while (iterator.hasNext()) {
            PackageTreeNode packageTreeNode = (PackageTreeNode) iterator.next();
            String packagePath = packageTreeNode.getPackagePath();
            String value;
            if (changeLogMapping1 != null && changeLogMapping1.hasPackageMapping(packagePath)) {
                String newPackageName = changeLogMapping1.getNewPackageName(packagePath);
                if (newPackageName != null) {
                    value = newPackageName;
                } else {
                    value = packagePath;
                }
            } else if (nameExclusionSet.isPackageExcluded(packagePath)) {
                value = packagePath;
            } else {
                value = valueArgument;
            }

            hashMap.put(packagePath, value);
            packageTreeNode.collectRetainedPackageNames(hashMap, nameExclusionSet, changeLogMapping1, value);
        }

        return hashMap;
    }

    public boolean checkNonClassFileClashes(Map map1, Map map2, ObservableHolder observableHolder) throws ZkmException, IOException {
        boolean bl = false;
        ArrayList arrayList = new ArrayList();
        Iterator iterator = map2.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String valueValue = ((java.lang.String) (map1.put(key, value)));
            if (valueValue != null && !valueValue.equals(value)) {
                bl = true;
                observableHolder.setValue("Non-class files '" + valueValue + "' and '" + value + "' would clash");
                map1.put(key, valueValue);

                for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                    map1.remove(arrayList.get(elementIndex));
                }
            } else {
                arrayList.add(key);
            }
        }

        return bl;
    }
}
