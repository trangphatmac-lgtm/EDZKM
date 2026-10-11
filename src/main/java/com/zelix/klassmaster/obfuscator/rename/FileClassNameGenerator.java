package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.MixedCaseNamesMode;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.Triple;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileClassNameGenerator extends ClassNameGenerator {
    public Map generatorsByPackage = ZkmUtils.createHashMap();
    public Map generatorsByOuterPrefix = ZkmUtils.createHashMap();
    public char[] firstChars;
    public char[] subsequentChars;
    public char[] archiveFirstChars;
    public char[] archiveSubsequentChars;
    public String namePrefix;
    public List nameFileNames;

    public FileClassNameGenerator(
            ClassRenameClashChecker classRenameClashChecker,
            boolean bl,
            boolean bl1,
            String namePrefixArgument,
            int keepInnerClassInfoMode,
            int keepGenericsInfoMode,
            boolean randomizeNamesArgument,
            MixedCaseNamesMode mixedCaseNamesMode1,
            HashMap hashMap,
            ListMultimap listMultimap,
            Map map1,
            List list1
    ) {
        super(classRenameClashChecker, hashMap, listMultimap, map1, keepInnerClassInfoMode, keepGenericsInfoMode, randomizeNamesArgument);
        switch (mixedCaseNamesMode1.getValue()) {
            case 0:
                if (!bl) {
                    this.firstChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                } else if (bl1) {
                    this.firstChars = SequentialNameGenerator.LETTER_UNDERSCORE_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.LETTER_UNDERSCORE_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                } else {
                    this.firstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                }
                break;
            case 1:
                if (!bl) {
                    this.firstChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                } else if (bl1) {
                    this.firstChars = SequentialNameGenerator.LOWER_UNDERSCORE_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.LOWER_UNDERSCORE_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                } else {
                    this.firstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                }
                break;
            case 2:
                if (!bl) {
                    this.firstChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.UNICODE_LOWER_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.UNICODE_MIXED_CASE_CHARS.clone();
                } else if (bl1) {
                    this.firstChars = SequentialNameGenerator.LOWER_UNDERSCORE_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.LETTER_UNDERSCORE_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                } else {
                    this.firstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.subsequentChars = SequentialNameGenerator.LOWER_DIGIT_UNDERSCORE_CHARS.clone();
                    this.archiveFirstChars = SequentialNameGenerator.DIGIT_CHARS.clone();
                    this.archiveSubsequentChars = SequentialNameGenerator.IDENTIFIER_CHARS.clone();
                }
        }

        this.namePrefix = namePrefixArgument;
        this.nameFileNames = list1;
        if (classRenameClashChecker.isRandomizeOrder()) {
            ZkmUtils.shuffleChars(this.subsequentChars);
            ZkmUtils.shuffleChars(this.archiveSubsequentChars);
        }
    }

    public UniqueNameGenerator createNameGenerator(char[] destinationArray, char[] subsequentCharsArgument, char[] destinationArrayArgument, char[] subsequentCharsArgument2, List list1) {
        return new UniqueNameGenerator(destinationArray, subsequentCharsArgument, destinationArrayArgument, subsequentCharsArgument2, list1, super.randomizeNames);
    }

    @Override
    public String generateNewName(ClassHierarchyNode classHierarchyNode, String string, HashMap hashMap, String string1, Triple triple, boolean bl) throws IOException {
        String second;
        String third;
        if (triple != null) {
            second = (String) triple.getSecond();
            third = (String) triple.getThird();
            if (triple.getFirst() == null) {
                second = second + "zzzz";
            }
        } else {
            second = "";
            third = "";
        }

        boolean referencedFromXml = classHierarchyNode.getProgramClass().isReferencedFromXml();
        boolean bl2 = bl;
        HashMap hashMap1 = hashMap;
        String thirdValue = third;
        String secondValue = second;
        String string6 = string1;
        String key = string;
        return this.buildUniqueClassName(key, string6, secondValue, thirdValue, hashMap1, bl2, referencedFromXml);
    }

    @Override
    public String generateFallbackName(ClassHierarchyNode classHierarchyNode, boolean bl) throws IOException {
        ProgramClass programClass1 = classHierarchyNode.getProgramClass();
        Triple triple = super.exclusionSet.getClassLink(programClass1);
        String second;
        String third;
        FileClassNameGenerator fileClassNameGenerator1;
        String string6;
        String className;
        if (triple != null) {
            second = (String) triple.getSecond();
            third = (String) triple.getThird();
            fileClassNameGenerator1 = this;
            string6 = "";
            className = classHierarchyNode.getClassName();
        } else {
            second = "";
            third = "";
            fileClassNameGenerator1 = this;
            string6 = "";
            className = classHierarchyNode.getClassName();
        }

        Boolean boolean2 = programClass1.isReferencedFromXml();
        Boolean boolean1 = bl;
        HashMap hashMap1 = (HashMap) null;
        boolean boolean1Value = boolean1;
        boolean boolean2Value = boolean2;
        boolean boolean1Value2 = boolean1Value;
        HashMap hashMap = hashMap1;
        String thirdValue = third;
        String secondValue = second;
        String classNameValue = className;
        String key = string6;
        return fileClassNameGenerator1.buildUniqueClassName(key, classNameValue, secondValue, thirdValue, hashMap, boolean1Value2, boolean2Value);
    }

    public String nextNameInScope(String key, String keyArgument, HashMap hashMap, boolean bl) {
        if (keyArgument.equals("")) {
            UniqueNameGenerator uniqueNameGenerator1 = (UniqueNameGenerator) this.generatorsByPackage.get(key);
            if (uniqueNameGenerator1 == null) {
                uniqueNameGenerator1 = this.createNameGenerator(
                        this.firstChars, this.subsequentChars, this.archiveFirstChars, this.archiveSubsequentChars, this.nameFileNames
                );
                this.generatorsByPackage.put(key, uniqueNameGenerator1);
            }

            return uniqueNameGenerator1.nextUniqueName(this.namePrefix, bl);
        } else {
            UniqueNameGenerator uniqueNameGenerator = (UniqueNameGenerator) this.generatorsByOuterPrefix.get(keyArgument);
            if (uniqueNameGenerator == null) {
                uniqueNameGenerator = this.createNameGenerator(
                        this.firstChars, this.subsequentChars, this.archiveFirstChars, this.archiveSubsequentChars, this.nameFileNames
                );
                this.generatorsByOuterPrefix.put(keyArgument, uniqueNameGenerator);
            }

            UniqueNameGenerator uniqueNameGenerator2 = uniqueNameGenerator;
            String namePrefixValue = this.namePrefix;

            while (true) {
                String string2 = uniqueNameGenerator2.nextUniqueName(namePrefixValue, bl);
                if (string2 != null && HiddenOptionFlags.MARK_GENERATED_CLASS_NAMES) {
                    string2 = string2 + "__";
                }

                if (string2 != null) {
                    if (hashMap == null || !hashMap.containsValue(string2)) {
                        return string2;
                    }

                    uniqueNameGenerator2 = uniqueNameGenerator;
                    namePrefixValue = this.namePrefix;
                } else {
                    uniqueNameGenerator2 = uniqueNameGenerator;
                    namePrefixValue = this.namePrefix;
                }
            }
        }
    }

    public String buildUniqueClassName(String key, String string1, String string2, String string3, HashMap hashMap, boolean bl, boolean bl1) throws IOException {
        String renamedPackagePrefix = "";
        if (key.equals("")) {
            renamedPackagePrefix = this.getRenamedPackagePrefix(string1);
        }

        boolean bl2 = bl1
                && key.length() == 0
                && string2.length() == 0
                && (
                this.namePrefix == null
                        || this.namePrefix.length() == 0
                        || !Character.isUpperCase(this.namePrefix.charAt(0)) && Character.isLetter(this.namePrefix.charAt(0))
        );

        String keyValue;
        do {
            String string6 = this.nextNameInScope(renamedPackagePrefix, key, hashMap, ((bl2) || (bl)));
            if (bl2 && !Character.isUpperCase(string6.charAt(0)) && Character.isLetter(string6.charAt(0))) {
                StringBuilder stringBuilder = new StringBuilder(string6);
                char ba = Character.toUpperCase(string6.charAt(0));
                stringBuilder.setCharAt(0, ba);
                string6 = stringBuilder.toString();
            }

            keyValue = renamedPackagePrefix + key + string2 + string6 + string3;
        } while (!this.isNameAvailable(string1, keyValue, bl, bl2));

        return keyValue;
    }
}
