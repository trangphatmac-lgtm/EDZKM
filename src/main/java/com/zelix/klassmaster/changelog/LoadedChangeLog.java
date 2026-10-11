package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ThreeKeyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class LoadedChangeLog extends AbstractChangeLog {
    public int packageCount;
    public Map sourceNameMappings;
    public Map classMappings;
    public ThreeKeyMultiMap methodMappingsByNewSignature;
    public int classCount;
    public TwoKeyMap lineNumberMappings;
    public NestedMultiMap originalMethodsByNewName;
    public boolean methodsHaveReturnTypes = true;
    public String changeLogName;

    public Integer lookupLineNumber(Object key, int integerArgument) {
        return (Integer) this.lineNumberMappings.getValue(key, this.getCachedInteger(integerArgument));
    }

    @Override
    public void addMemberFlowObfuscationData(Object object, Object object1) {
    }

    @Override
    public void addMethodMappings(String key, ListMultimap listMultimap) {
        ChangeLogSimpleNode.getOpaqueStrings();
        if (this.originalMethodsByNewName == null) {
            this.originalMethodsByNewName = new NestedMultiMap(this.classCount);
            this.methodMappingsByNewSignature = new ThreeKeyMultiMap(this.classCount);
        }

        Enumeration enumeration = listMultimap.keys();

        while (enumeration.hasMoreElements()) {
            ChangeLogMemberKey changeLogMemberKey = (ChangeLogMemberKey) enumeration.nextElement();
            String nameValue = changeLogMemberKey.getName();
            String typeValue = changeLogMemberKey.getType();
            String parameterTypes = changeLogMemberKey.getParameterTypes();
            Iterator iterator = listMultimap.getValues(changeLogMemberKey).iterator();

            while (iterator.hasNext()) {
                NewNameMapping newNameMapping = (NewNameMapping) iterator.next();
                String newName = newNameMapping.getNewName();
                NestedMultiMap nestedMultiMap;
                if (newNameMapping.hasParameterChangeData()) {
                    this.setParameterChangeDataPresent();
                    this.methodMappingsByNewSignature.addValue(key, newName, newNameMapping.getEffectiveParameterTypes(), new ObjectPair(nameValue, typeValue));
                    nestedMultiMap = this.originalMethodsByNewName;
                } else if (newNameMapping.isParametersObfuscated()) {
                    this.methodMappingsByNewSignature.addValue(key, newName, newNameMapping.getEffectiveParameterTypes(), new ObjectPair(nameValue, typeValue));
                    nestedMultiMap = this.originalMethodsByNewName;
                } else {
                    this.methodMappingsByNewSignature.addValue(key, newName, parameterTypes, new ObjectPair(nameValue, typeValue));
                    nestedMultiMap = this.originalMethodsByNewName;
                }

                nestedMultiMap.addValue(key, newName, new ObjectPair(nameValue, parameterTypes));
            }

            if (changeLogMemberKey.getType() != null) {
                this.methodsHaveReturnTypes = true;
            } else {
                this.methodsHaveReturnTypes = false;
            }
        }
    }

    @Override
    public void addModuleMethodParameterChangeClasses(Object object, Object object1) {
    }

    public String getSourceName(Object key) {
        return this.sourceNameMappings != null ? (String) this.sourceNameMappings.get(key) : null;
    }

    @Override
    public void addFieldMappings(Object object, Object object1) {
    }

    @Override
    public void incrementClassCount() {
        this.classCount++;
    }

    @Override
    public boolean acceptsLineNumberChanges() {
        return true;
    }

    @Override
    public void addPackageFlowObfuscationData(Object object, Object object1, Object object2) throws ZkmException, IOException {
    }

    @Override
    public void setReferenceObfuscationClass(Object referenceObfuscationClass) {
    }

    public LoadedChangeLog(String changeLogNameArgument) {
        this.changeLogName = changeLogNameArgument;
    }

    public void mapMethodParameterTypes() {
        if (this.hasParameterChangeData()) {
            ThreeKeyMultiMap threeKeyMultiMap = new ThreeKeyMultiMap(this.methodMappingsByNewSignature.getKeyCount());
            Iterator iterator = this.methodMappingsByNewSignature.getEntrySnapshot().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String key = (String) entry.getKey();
                Iterator iterator1 = ((NestedMultiMap) entry.getValue()).entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry1 = (Entry) iterator1.next();
                    String keyValue = (String) entry1.getKey();
                    Iterator iterator2 = ((ListMultimap) entry1.getValue()).entrySet().iterator();

                    while (iterator2.hasNext()) {
                        Entry entry2 = (Entry) iterator2.next();
                        String keyValue2 = (String) entry2.getKey();
                        String keyValue3 = this.mapTypeList(keyValue2, this.classMappings);
                        Iterator iterator3 = ((List) entry2.getValue()).iterator();

                        while (iterator3.hasNext()) {
                            ObjectPair objectPair = (ObjectPair) iterator3.next();
                            threeKeyMultiMap.addValue(key, keyValue, keyValue3, objectPair);
                        }
                    }
                }
            }

            this.methodMappingsByNewSignature = threeKeyMultiMap;
        }
    }

    public boolean hasClassMapping(Object key) {
        return this.classMappings.containsKey(key);
    }

    public String getNewClassName(Object key) {
        return (String) this.classMappings.get(key);
    }

    @Override
    public void addMainFlowObfuscationData(Object object, Object object1) throws ZkmException, IOException {
    }

    @Override
    public void addClassMapping(Object key, Object value) {
        if (this.classMappings == null) {
            this.classMappings = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.classCount));
        }

        this.classMappings.put(key, value);
    }

    public boolean hasLineNumberMappings(Object key) {
        return this.lineNumberMappings != null && this.lineNumberMappings.containsKey(key);
    }

    @Override
    public boolean acceptsFieldChanges() {
        return false;
    }

    @Override
    public void addPackageMapping(Object object, Object object1) {
    }

    public List findOriginalMethods(Object key, Object keyArgument) {
        return this.originalMethodsByNewName.getValues(key, keyArgument);
    }

    public String toNewTypeName(String key) {
        return AbstractChangeLog.mapTypeName(key, this.classMappings);
    }

    public String getOriginalMethodName(String key, String keyArgument, String[] strings, String keyArgument2) {
        if (key == null) {
            throw new IllegalArgumentException("New class name cannot be null");
        }

        if (keyArgument == null) {
            throw new IllegalArgumentException("New method name cannot be null");
        }

        if (strings == null) {
            throw new IllegalArgumentException("New argument types array cannot be null");
        }

        if (keyArgument2 == null) {
            throw new IllegalArgumentException("New return type cannot be null");
        }

        String keyValue = (String) ZkmUtils.mapOrSelf(key, this.classMappings);
        String string4 = this.joinTypeNames(strings);
        String keyValue2 = this.mapTypeList(string4, this.classMappings);
        String string6 = AbstractChangeLog.mapTypeName(keyArgument2, this.classMappings);
        List list1 = this.methodMappingsByNewSignature.getValues(keyValue, keyArgument, keyValue2);
        if (list1.size() == 1) {
            ObjectPair objectPair = (ObjectPair) list1.get(0);
            if (objectPair.getSecond() == null || string6.equals(objectPair.getSecond())) {
                return (String) objectPair.getFirst();
            }
        } else {
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                ObjectPair objectPair1 = (ObjectPair) iterator.next();
                if (string6.equals(objectPair1.getSecond())) {
                    return (String) objectPair1.getFirst();
                }
            }
        }

        return keyArgument;
    }

    @Override
    public void incrementPackageCount() {
        this.packageCount++;
    }

    public boolean isNewClassName(Object object) {
        return this.classMappings.containsValue(object);
    }

    @Override
    public void addModuleReferenceObfuscationClass(Object object, Object object1) {
    }

    @Override
    public void addSourceNameMapping(Object key, Object value) {
        if (this.sourceNameMappings == null) {
            this.sourceNameMappings = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.classCount));
        }

        this.sourceNameMappings.put(key, value);
    }

    public LoadedChangeLog mergeWith(LoadedChangeLog loadedChangeLog1, PrintWriter printWriter) throws ChangeLogMergeException {
        if (loadedChangeLog1.classMappings != null) {
            ArrayList arrayList = new ArrayList(loadedChangeLog1.classMappings.keySet());
            Collections.sort(arrayList);
            Iterator iterator = arrayList.iterator();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String valueValue = (String) loadedChangeLog1.classMappings.get(key);
                if (this.classMappings != null && this.classMappings.containsKey(key)) {
                    String string2 = (String) this.classMappings.get(key);
                    if (!string2.equals(valueValue)) {
                        printWriter.println(
                                "ERROR: Class name '"
                                        + key
                                        + "' mapping clash while merging with '"
                                        + loadedChangeLog1.changeLogName
                                        + "'. '"
                                        + string2
                                        + "' and '"
                                        + valueValue
                                        + "' both map to '"
                                        + key
                                        + "'."
                        );
                    }
                } else {
                    this.addClassMapping(key, valueValue);
                }
            }
        }

        if (loadedChangeLog1.sourceNameMappings != null) {
            Iterator iterator4 = loadedChangeLog1.sourceNameMappings.entrySet().iterator();

            while (iterator4.hasNext()) {
                Entry entry = (Entry) iterator4.next();
                String keyValue = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (this.sourceNameMappings != null && this.sourceNameMappings.containsKey(keyValue)) {
                    String string3 = (String) this.sourceNameMappings.get(keyValue);
                    if (!string3.equals(value)) {
                        printWriter.println(
                                "ERROR: Class name '"
                                        + keyValue
                                        + "' source name mapping clash while merging with '"
                                        + loadedChangeLog1.changeLogName
                                        + "'. '"
                                        + keyValue
                                        + "' maps to '"
                                        + value
                                        + "' and '"
                                        + value
                                        + "'."
                        );
                    }
                } else {
                    this.addSourceNameMapping(keyValue, value);
                }
            }
        }

        if (loadedChangeLog1.methodMappingsByNewSignature != null) {
            if (this.methodMappingsByNewSignature != null && this.methodsHaveReturnTypes != loadedChangeLog1.methodsHaveReturnTypes) {
                String string10 = "Change log methods "
                        + (this.methodsHaveReturnTypes ? "have return types" : "have no return types")
                        + " which is inconsistent with change log \""
                        + loadedChangeLog1.changeLogName
                        + "\" whose methods "
                        + (loadedChangeLog1.methodsHaveReturnTypes ? "have return types" : "have no return types")
                        + ". All method entries in change logs being merged must have return types or no method entries can have return types.";
                throw new ChangeLogMergeException(string10);
            }

            if (this.methodMappingsByNewSignature != null && this.hasParameterChangeData() != loadedChangeLog1.hasParameterChangeData()) {
                String string11 = "Change log methods "
                        + (this.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
                        + " which is inconsistent with change log \""
                        + loadedChangeLog1.changeLogName
                        + "\" whose methods "
                        + (loadedChangeLog1.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
                        + ". All method entries in change logs being merged must have new method parameter data or no method entries can have new method parameter data.";
                throw new ChangeLogMergeException(string11);
            }

            Enumeration enumeration1 = loadedChangeLog1.methodMappingsByNewSignature.keys();

            while (enumeration1.hasMoreElements()) {
                String keyValue2 = (String) enumeration1.nextElement();
                NestedMultiMap nestedMultiMap = loadedChangeLog1.methodMappingsByNewSignature.getNestedMultiMap(keyValue2);
                if (this.methodMappingsByNewSignature != null && this.methodMappingsByNewSignature.containsKey(keyValue2)) {
                    NestedMultiMap nestedMultiMap1 = this.methodMappingsByNewSignature.getNestedMultiMap(keyValue2);
                    Enumeration enumeration4 = nestedMultiMap.keys();

                    while (enumeration4.hasMoreElements()) {
                        String keyValue3 = (String) enumeration4.nextElement();
                        ListMultimap listMultimap2 = nestedMultiMap.getMultimap(keyValue3);
                        if (nestedMultiMap1.containsKey(keyValue3)) {
                            ListMultimap listMultimap3 = nestedMultiMap1.getMultimap(keyValue3);
                            Enumeration enumeration5 = listMultimap2.keys();

                            while (enumeration5.hasMoreElements()) {
                                String keyValue4 = (String) enumeration5.nextElement();
                                List list2 = listMultimap2.getValues(keyValue4);
                                if (listMultimap3.containsKey(keyValue4)) {
                                    List list3 = listMultimap3.getValues(keyValue4);
                                    Iterator iterator2 = list2.iterator();

                                    while (iterator2.hasNext()) {
                                        ObjectPair objectPair1 = (ObjectPair) iterator2.next();
                                        String first = (String) objectPair1.getFirst();
                                        String second = (String) objectPair1.getSecond();
                                        Iterator iterator3 = list3.iterator();

                                        while (iterator3.hasNext()) {
                                            ObjectPair objectPair2 = (ObjectPair) iterator3.next();
                                            String firstValue = (String) objectPair2.getFirst();
                                            String secondValue = (String) objectPair2.getSecond();
                                            if (second != null) {
                                                if (second.equals(secondValue) && !first.equals(firstValue)) {
                                                    printWriter.println(
                                                            "ERROR: Method name clash name while merging with '"
                                                                    + loadedChangeLog1.changeLogName
                                                                    + "'. Both '"
                                                                    + firstValue
                                                                    + "' and '"
                                                                    + first
                                                                    + "' map to new method name '"
                                                                    + second
                                                                    + " "
                                                                    + keyValue3
                                                                    + "("
                                                                    + keyValue4
                                                                    + ")' in class '"
                                                                    + keyValue2
                                                                    + "'."
                                                    );
                                                }
                                            } else if (!first.equals(firstValue)) {
                                                printWriter.println(
                                                        "ERROR: Method name clash name while merging with '"
                                                                + loadedChangeLog1.changeLogName
                                                                + "'. Both '"
                                                                + firstValue
                                                                + "' and '"
                                                                + first
                                                                + "' map to new method name '"
                                                                + keyValue3
                                                                + "("
                                                                + keyValue4
                                                                + ")' in class '"
                                                                + keyValue2
                                                                + "'."
                                                );
                                            }
                                        }
                                    }
                                } else {
                                    printWriter.println(
                                            "WARNING: Different versions of class '"
                                                    + keyValue2
                                                    + "' while merging with '"
                                                    + loadedChangeLog1.changeLogName
                                                    + "'.  Class '"
                                                    + keyValue2
                                                    + "' in '"
                                                    + this.changeLogName
                                                    + "' does not have a new method name '"
                                                    + keyValue3
                                                    + "("
                                                    + keyValue4
                                                    + ")'."
                                    );
                                }
                            }
                        } else {
                            printWriter.println(
                                    "WARNING: Different versions of class '"
                                            + keyValue2
                                            + "' while merging with '"
                                            + loadedChangeLog1.changeLogName
                                            + "'.  Class '"
                                            + keyValue2
                                            + "' in '"
                                            + this.changeLogName
                                            + "' does not have a new method name '"
                                            + keyValue3
                                            + "'."
                            );
                        }
                    }
                } else {
                    ListMultimap listMultimap1 = new ListMultimap();
                    Enumeration enumeration3 = nestedMultiMap.keys();

                    while (enumeration3.hasMoreElements()) {
                        String keyValue5 = (String) enumeration3.nextElement();
                        ListMultimap listMultimap = nestedMultiMap.getMultimap(keyValue5);
                        Enumeration enumeration = listMultimap.keys();

                        while (enumeration.hasMoreElements()) {
                            String string5 = (String) enumeration.nextElement();
                            List list1 = listMultimap.getValues(string5);
                            Iterator iterator1 = list1.iterator();

                            while (iterator1.hasNext()) {
                                ObjectPair objectPair = (ObjectPair) iterator1.next();
                                listMultimap1.addValue(
                                        new ChangeLogMemberKey((String) objectPair.getFirst(), string5, (String) objectPair.getSecond(), 0), new NewNameMapping(keyValue5)
                                );
                            }
                        }
                    }

                    this.addMethodMappings(keyValue2, listMultimap1);
                }
            }
        }

        if (loadedChangeLog1.lineNumberMappings != null) {
            Enumeration enumeration2 = loadedChangeLog1.lineNumberMappings.keys();

            while (enumeration2.hasMoreElements()) {
                String keyValue6 = (String) enumeration2.nextElement();
                Map map1 = loadedChangeLog1.lineNumberMappings.getInnerMap(keyValue6);
                if (this.lineNumberMappings != null && this.lineNumberMappings.containsKey(keyValue6)) {
                    Map map2 = this.lineNumberMappings.getInnerMap(keyValue6);
                    Iterator iterator5 = map1.entrySet().iterator();

                    while (iterator5.hasNext()) {
                        Entry entry1 = (Entry) iterator5.next();
                        Integer integer = (Integer) entry1.getKey();
                        Integer integer1 = (Integer) entry1.getValue();
                        if (map2.containsKey(integer)) {
                            if (!((Integer) map2.get(integer)).equals(integer1)) {
                                printWriter.println(
                                        "WARNING: Different line number mapping in class '"
                                                + keyValue6
                                                + "' while merging with '"
                                                + loadedChangeLog1.changeLogName
                                                + "'. Line number "
                                                + integer
                                                + " is mapped to by "
                                                + integer1
                                                + " and "
                                                + map2.get(integer)
                                );
                            }
                        } else {
                            printWriter.println(
                                    "WARNING: Different line number mapping in class '"
                                            + keyValue6
                                            + "' while merging with '"
                                            + loadedChangeLog1.changeLogName
                                            + "'. Line number "
                                            + integer
                                            + " does not appear in '"
                                            + this.changeLogName
                                            + "'."
                            );
                        }
                    }
                } else {
                    this.addLineNumberMappings(keyValue6, this.lineNumberMappings.copyOf(map1));
                }
            }
        }

        return this;
    }

    @Override
    public void setMethodParameterChangeClasses(Object methodParameterChangeClasses) {
    }

    @Override
    public void addModuleAutoReflectionClass(Object object, Object object1) {
    }

    public String[] findOriginalMethodNames(String key, String keyArgument, String string2, String keyArgument2) {
        String[] strings = null;
        if (this.methodMappingsByNewSignature != null) {
            List list1 = this.methodMappingsByNewSignature.getValues(key, keyArgument, keyArgument2);
            if (list1 != null) {
                if (string2 == null) {
                    strings = new String[list1.size()];

                    for (int elementIndex = 0; elementIndex < strings.length; elementIndex++) {
                        strings[elementIndex] = (String) ((ObjectPair) list1.get(elementIndex)).getFirst();
                    }
                } else {
                    ArrayList arrayList = new ArrayList();

                    for (int elementIndexValue = 0; elementIndexValue < list1.size(); elementIndexValue++) {
                        ObjectPair objectPair = (ObjectPair) list1.get(elementIndexValue);
                        String second = (String) objectPair.getSecond();
                        if (second == null || second.equals(string2)) {
                            arrayList.add(objectPair.getFirst());
                        }
                    }

                    if (arrayList.size() > 0) {
                        strings = new String[arrayList.size()];
                        strings = ((java.lang.String[]) (arrayList.toArray(strings)));
                    }
                }
            }
        }

        return strings;
    }

    @Override
    public void addLineNumberMappings(String key, Map map1) {
        if (this.lineNumberMappings == null) {
            this.lineNumberMappings = new TwoKeyMap(this.classCount);
        }

        this.lineNumberMappings.putInnerMap(key, map1);
    }

    @Override
    public void setAutoReflectionClass(Object autoReflectionClass) {
    }
}
