package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.FieldSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.NumericStringUtil;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultFieldNameGenerator extends FieldNameAssigner {
    public SequentialNameGenerator nameGenerator;
    public String consumedMarker = "";
    public boolean letterStartNames;
    public boolean asciiNames;
    public String namePrefix;
    public boolean reuseNames;
    public List nameFileNames;

    @Override
    public String assignFieldName(
            ClassHierarchyNode classHierarchyNode,
            FieldInfo fieldInfo,
            String key,
            String keyArgument,
            Map map1,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            Map map2,
            boolean bl,
            Map map3,
            HashMap hashMap,
            SetMultiMap setMultiMap,
            TwoKeyMap twoKeyMap
    ) {
        if (super.keepInnerClassInfoMode == 1
                || ((!key.startsWith("this$") || !bl) && !key.startsWith("this+") || !NumericStringUtil.isInteger(key.substring("this$".length())))
                && (!key.startsWith("val$") || !bl)
                && !key.startsWith("val+")) {
            ProgramClass programClass1 = classHierarchyNode.getProgramClass();
            if (this.reuseNames) {
                List list1 = listMultimap.getValues(keyArgument);
                if (list1 != null) {
                    for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                        String string3 = (String) list1.get(elementIndex);
                        if (string3 != this.consumedMarker) {
                            String keyValue = string3;
                            List list2;
                            int elementIndexValue;
                            String consumedMarkerValue;
                            if (!keyValue.equals(key)) {
                                if (this.isNameAvailable(key, keyValue, fieldInfo, map2, map1, setMultiMap, twoKeyMap, programClass1, map3, hashMap)) {
                                    map2.put(keyValue, fieldInfo);
                                    list1.set(elementIndex, this.consumedMarker);
                                    return keyValue;
                                }

                                list2 = list1;
                                elementIndexValue = elementIndex;
                                consumedMarkerValue = this.consumedMarker;
                            } else {
                                list2 = list1;
                                elementIndexValue = elementIndex;
                                consumedMarkerValue = this.consumedMarker;
                            }

                            list2.set(elementIndexValue, consumedMarkerValue);
                        }
                    }
                }
            }

            boolean markRenamedLocals = HiddenOptionFlags.MARK_RENAMED_LOCALS;

            String keyValue2;
            do {
                keyValue2 = this.nextNameForClass(classHierarchyNode);
                if (markRenamedLocals) {
                    keyValue2 = key + '_' + keyValue2;
                }
            } while (!this.isNameAvailable(key, keyValue2, fieldInfo, map2, map1, setMultiMap, twoKeyMap, programClass1, map3, hashMap));

            map1.put(keyValue2, fieldInfo);
            map2.put(keyValue2, fieldInfo);
            listMultimap1.addValue(keyArgument, keyValue2);
            return keyValue2;
        } else {
            map1.put(key, fieldInfo);
            map2.put(key, fieldInfo);
            return null;
        }
    }

    public DefaultFieldNameGenerator(int keepInnerClassInfoModeArgument, boolean asciiNames, boolean letterStartNames, boolean randomizeNamesArgument, String namePrefixArgument, boolean reuseNames, List list1) {
        super(keepInnerClassInfoModeArgument, randomizeNamesArgument);
        this.letterStartNames = letterStartNames;
        this.asciiNames = asciiNames;
        this.namePrefix = namePrefixArgument;
        this.reuseNames = reuseNames;
        this.nameFileNames = list1;
    }

    public boolean isNameAvailable(
            String string,
            String key,
            FieldInfo fieldInfo,
            Map map1,
            Map map2,
            SetMultiMap setMultiMap,
            TwoKeyMap twoKeyMap,
            ProgramClass programClass1,
            Map map3,
            HashMap hashMap
    ) {
        if (string.equals(key)) {
            return false;
        }

        if (map1.containsKey(key)) {
            return false;
        }

        if (setMultiMap.containsValue(programClass1, new FieldSignature(key, fieldInfo.getDescriptor()))) {
            return false;
        }

        Map map4 = twoKeyMap.getInnerMap(programClass1);
        if (map4 != null) {
            Iterator iterator = map4.keySet().iterator();

            while (iterator.hasNext()) {
                FieldInfo fieldInfo1 = (FieldInfo) iterator.next();
                if (this.hasAssignedName(key, fieldInfo1, map3, hashMap)) {
                    return false;
                }
            }
        }

        Enumeration enumeration = programClass1.enumerateInheritedReferencedFields();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) enumeration.nextElement();
                if (this.hasAssignedName(key, abstractFieldInfo, map3, hashMap)) {
                    return false;
                }
            }
        }

        return !map2.containsKey(key) || this.reuseNames;
    }

    public boolean hasAssignedName(String string, AbstractFieldInfo abstractFieldInfo, Map map1, HashMap hashMap) {
        return map1 != null && string.equals(map1.get(abstractFieldInfo)) || string.equals(hashMap.get(abstractFieldInfo));
    }

    public String nextNameForClass(ClassHierarchyNode classHierarchyNode) {
        if (classHierarchyNode != super.currentClassNode) {
            super.currentClassNode = classHierarchyNode;
            if (!this.asciiNames) {
                this.nameGenerator = new SequentialNameGenerator(
                        SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS, SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS, this.nameFileNames, super.randomizeNames, "Field"
                );
            } else if (this.letterStartNames) {
                if (HiddenOptionFlags.ALT_FIELD_NAME_CHARS) {
                    this.nameGenerator = new SequentialNameGenerator(
                            SequentialNameGenerator.LETTER_CHARS, SequentialNameGenerator.ALPHANUMERIC_CHARS, this.nameFileNames, super.randomizeNames, "Field"
                    );
                } else {
                    this.nameGenerator = new SequentialNameGenerator(
                            SequentialNameGenerator.LETTER_UNDERSCORE_CHARS, SequentialNameGenerator.IDENTIFIER_CHARS, this.nameFileNames, super.randomizeNames, "Field"
                    );
                }
            } else {
                this.nameGenerator = new SequentialNameGenerator(
                        SequentialNameGenerator.DIGIT_CHARS, SequentialNameGenerator.IDENTIFIER_CHARS, this.nameFileNames, super.randomizeNames, "Field"
                );
            }
        }

        return this.nameGenerator.nextPrefixedName(this.namePrefix);
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
