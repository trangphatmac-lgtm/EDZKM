package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.archive.TempFileManager;
import com.zelix.klassmaster.classfile.attribute.AnnotationEntry;
import com.zelix.klassmaster.classfile.attribute.AnnotationsAttribute;
import com.zelix.klassmaster.classfile.attribute.Attribute;
import com.zelix.klassmaster.classfile.attribute.EnclosingMethodAttribute;
import com.zelix.klassmaster.classfile.attribute.InnerClassEntry;
import com.zelix.klassmaster.classfile.attribute.InnerClassesAttribute;
import com.zelix.klassmaster.classfile.attribute.NestHostAttribute;
import com.zelix.klassmaster.classfile.attribute.SourceFileAttribute;
import com.zelix.klassmaster.classfile.constpool.ClassConstantBase;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.classfile.hierarchy.MethodOverrideAnalyzer;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.engine.ProcessingStatistics;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmRuntimeException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.obfuscator.parameters.MethodParameterChangeSet;
import com.zelix.klassmaster.obfuscator.rename.MethodRenamer;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.EmptyEnumeration;
import com.zelix.klassmaster.util.JavaRuntimeVersion;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.NumericStringUtil;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SyncIndexedSet;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;

public abstract class ClassFileBase extends ClassFileComponent implements Comparable, NamedTypeRef {
    private static String marker;
    public InnerClassEntry innerClassEntry;
    public boolean packageReferencedFromXml;
    public int fieldCount;
    public int methodCount;
    public boolean referencedFromXml;
    public String className;
    public String originalClassName;
    public AccessFlags accessFlags;
    public ClassConstantBase[] interfaces;
    public Attribute[] attributes;
    public String lowerCaseClassName;
    public int constantPoolCount;
    public Integer releaseVersion;
    public int majorVersion;
    public int minorVersion;
    public List versionedVariants;
    public ClassConstantBase thisClassConstant;
    public int attributeCount;
    public ClassFileBase baseVersionClass;
    public ClassConstantBase superClassConstant;
    public int interfaceCount;
    private List inputLocations = HiddenOptionFlags.USE_PARALLEL ? new Vector(1) : new ArrayList(1);
    public boolean innerClassesTrimPending = false;
    public boolean innerClassesRemovalPending = false;
    public boolean permittedSubclassesPresent = false;
    public boolean enclosingMethodPresent = false;
    public boolean fromArchive;
    public final byte[] classDigest;
    public final int creationKind;

    public String getPackageName() {
        String classNameValue = this.getClassName();
        int endIndex = classNameValue.lastIndexOf(47);
        return endIndex == -1 ? "" : classNameValue.substring(0, endIndex).replace('/', '.');
    }

    @Override
    public String getOriginalClassName() {
        return this.originalClassName;
    }

    public final boolean hasSuperFlag() {
        return this.accessFlags.isSuper();
    }

    public static String extractClassName(String string) {
        return extractClassName(string, null);
    }

    public final void renameMethods(ClassHierarchyNode classHierarchyNode, MethodRenamer methodRenamer1) throws ZkmException, IOException {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < this.methodCount; abstractMethodInfoIndex++) {
            AbstractMethodInfo abstractMethodInfo = abstractMethodInfos[abstractMethodInfoIndex];
            if (!abstractMethodInfo.isStaticInitializer() && !abstractMethodInfo.isConstructor()) {
                methodRenamer1.applyChangeLogMethodMapping(classHierarchyNode, this, abstractMethodInfo);
            }
        }
    }

    public static String substringBeforeLast(String string, char searchCharacter) {
        int endIndex = string.lastIndexOf(searchCharacter);
        return endIndex == -1 ? "" : string.substring(0, endIndex);
    }

    public String getOuterClassName() {
        if (this.innerClassEntry != null) {
            String commonOuterPrefix = this.innerClassEntry.getCommonOuterPrefix();
            if (commonOuterPrefix != null) {
                if (commonOuterPrefix.equals(this.getClassName())) {
                    return null;
                }

                return commonOuterPrefix;
            }

            String enclosingClassName = this.getEnclosingClassName();
            if (enclosingClassName != null) {
                String classNameValue = this.getClassName();
                if (classNameValue.startsWith(enclosingClassName)) {
                    commonOuterPrefix = enclosingClassName;
                } else {
                    commonOuterPrefix = classNameValue;

                    while (commonOuterPrefix != null && !enclosingClassName.startsWith(commonOuterPrefix)) {
                        int endIndex = commonOuterPrefix.lastIndexOf("$");
                        if (endIndex > 0) {
                            commonOuterPrefix = commonOuterPrefix.substring(0, endIndex);
                        } else {
                            commonOuterPrefix = null;
                        }
                    }
                }

                return commonOuterPrefix;
            }
        }

        return null;
    }

    public void makePublic() {
        this.accessFlags.makePublic();
    }

    public final Set getAnnotationTypes() {
        HashSet hashSet = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof AnnotationsAttribute) {
                if (hashSet == null) {
                    hashSet = ZkmUtils.createHashSet(13);
                }

                String[] annotationTypeNames = ((AnnotationsAttribute) this.attributes[attributeIndex]).getAnnotationTypeNames();

                for (int annotationTypeNameIndex = 0; annotationTypeNameIndex < annotationTypeNames.length; annotationTypeNameIndex++) {
                    hashSet.add(annotationTypeNames[annotationTypeNameIndex]);
                }
            }
        }

        return hashSet;
    }

    @Override
    public final String getClassName() {
        return this.className;
    }

    public int getInnerClassAccessFlags() {
        return this.innerClassEntry != null ? this.innerClassEntry.getAccessFlags() : -1;
    }

    public final Map computeInheritedMethods(
            MethodOverrideAnalyzer methodOverrideAnalyzer, ObservableHolder observableHolder, ScriptEnvironment scriptEnvironment1, Random random1
    ) throws ZkmException, IOException {
        String classNameValue = this.getClassName();
        boolean bl = true;
        String classNameValue3;
        String string11;
        if (random1.nextInt(23) == 0) {
            if (observableHolder.isValueNull()) {
                String string1 = TempFileManager.findTempFilePath(scriptEnvironment1.getLogFileName());
                BufferedReader bufferedReader = null;

                try {
                    if (string1 != null) {
                        File file1 = new File(string1);
                        if (file1.exists()) {
                            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file1), "UTF-8"));
                            ArrayList arrayList = new ArrayList();
                            boolean bl1 = false;

                            String string2;
                            while ((string2 = bufferedReader.readLine()) != null && !bl1) {
                                arrayList.add(string2);
                                if (string2.indexOf("47 078 740 093") > -1) {
                                    bl1 = true;
                                    String string4 = ((String) arrayList.get(arrayList.size() - 3)).trim();
                                    long ba = ZkmUtils.computeStringHash(string4);
                                    if (ba != 2088943589338982045L) {
                                        observableHolder.setValue(Boolean.FALSE);
                                    } else {
                                        observableHolder.setValue(Boolean.TRUE);
                                    }

                                    String string5 = ((String) arrayList.get(arrayList.size() - 5)).trim().substring(3, 10);
                                    StringTokenizer stringTokenizer = new StringTokenizer(string5, ".");
                                    double bb = Integer.parseInt(stringTokenizer.nextToken()) * 365.25;
                                    bb += Integer.parseInt(stringTokenizer.nextToken()) * 30.4;
                                    bb += Integer.parseInt(stringTokenizer.nextToken());
                                    bb *= 1440.0;
                                    if ((long) bb > 14117040L) {
                                        observableHolder.setValue(Boolean.FALSE);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException iOException) {
                        }
                    }
                }
            }

            bl = (!(!observableHolder.isValueNull()) || ((Boolean) observableHolder.getValue()));
            if (!bl) {
                if (scriptEnvironment1.getPendingMarkerCount() == 0) {
                    scriptEnvironment1.incrementPendingMarkerCount();
                    classNameValue3 = classNameValue;
                    string11 = "java/lang/Object";
                } else {
                    classNameValue3 = classNameValue;
                    string11 = "java/lang/Object";
                }
            } else {
                classNameValue3 = classNameValue;
                string11 = "java/lang/Object";
            }
        } else {
            classNameValue3 = classNameValue;
            string11 = "java/lang/Object";
        }

        HashMap hashMap;
        if (!classNameValue3.equals(string11) && !this.isModule() && bl) {
            String superclassName = this.getSuperclassName();
            String string7 = "looking for superclass of '" + this.getLocationName() + "'";
            Map map1 = methodOverrideAnalyzer.getOrBuildVisibleMethodOwners(superclassName, observableHolder, scriptEnvironment1, random1, string7);
            hashMap = ZkmUtils.copyToHashMap(map1);
        } else {
            hashMap = ZkmUtils.createHashMap();
        }

        Vector vector = new Vector();
        int interfaceIndex = 0;
        int bf = 0;

        for (int interfaceCountValue = this.interfaceCount; bf < interfaceCountValue; interfaceCountValue = this.interfaceCount) {
            String interfaceName = this.getInterfaceName(interfaceIndex);
            String string9 = "looking implemented interface of '" + this.getLocationName() + "'";
            Map map3 = methodOverrideAnalyzer.getOrBuildVisibleMethodOwners(interfaceName, observableHolder, scriptEnvironment1, random1, string9);
            vector.add(map3);
            bf = ++interfaceIndex;
        }

        for (int elementIndex = 0; elementIndex < vector.size(); elementIndex++) {
            Map map2 = (Map) vector.elementAt(elementIndex);
            Iterator iterator = map2.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                MethodSignature methodSignature1 = (MethodSignature) entry.getKey();
                ClassFileBase classFileBase1 = (ClassFileBase) entry.getValue();
                String classNameValue2 = classFileBase1.getClassName();
                if (!classNameValue2.equals("java/lang/Object")) {
                    ClassFileBase classFileBase2 = methodOverrideAnalyzer.findTopOverriddenOwner(classFileBase1, methodSignature1);
                    if (classFileBase2 != null && classFileBase2 != classFileBase1) {
                        classFileBase1 = classFileBase2;
                        classNameValue2 = classFileBase2.getClassName();
                    }

                    if (classFileBase1 != this) {
                        ClassFileBase classFileBase3 = ((com.zelix.klassmaster.classfile.ClassFileBase) (hashMap.put(methodSignature1, classFileBase1)));
                        if (classFileBase3 != null && classFileBase1 != classFileBase3) {
                            ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(classNameValue2);
                            ClassHierarchyNode classHierarchyNode1 = ClassHierarchyNode.findNode(classFileBase3.getClassName());
                            if (classHierarchyNode1 != null && !classHierarchyNode1.hasNoClassFile()) {
                                if (classHierarchyNode != null && !classHierarchyNode.hasNoClassFile()) {
                                    this.propagateMethodImplementation(methodOverrideAnalyzer, classFileBase3, methodSignature1, classFileBase1);
                                } else {
                                    this.propagateMethodImplementation(methodOverrideAnalyzer, classFileBase3, methodSignature1, classFileBase1);
                                }
                            } else if (classHierarchyNode != null && !classHierarchyNode.hasNoClassFile()) {
                                this.propagateMethodImplementation(methodOverrideAnalyzer, classFileBase1, methodSignature1, classFileBase3);
                                hashMap.put(methodSignature1, classFileBase3);
                            }
                        }
                    }
                }
            }
        }

        HashMap hashMap1 = ZkmUtils.copyToHashMap(hashMap);
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < this.methodCount; abstractMethodInfoIndex++) {
            abstractMethodInfos[abstractMethodInfoIndex].addToInheritedMethods(methodOverrideAnalyzer, hashMap1, ClassHierarchyNode.isUnknownClass(classNameValue), this);
        }

        methodOverrideAnalyzer.putOverriddenMethodOwners(classNameValue, hashMap);
        methodOverrideAnalyzer.putVisibleMethodOwners(classNameValue, hashMap1);
        return hashMap1;
    }

    public void markHasEnclosingMethod() {
        this.enclosingMethodPresent = true;
    }

    public void resolveInnerClassEntry() {
        if (!this.isModule()) {
            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (this.attributes[attributeIndex] instanceof InnerClassesAttribute) {
                    InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute) this.attributes[attributeIndex];
                    if (innerClassesAttribute.valid) {
                        this.innerClassEntry = innerClassesAttribute.findEntryForClass(this.thisClassConstant);
                    }
                    break;
                }
            }
        }
    }

    public static boolean isSignaturePolymorphicOwner(String string) {
        return string.equals("java/lang/invoke/MethodHandle") || string.equals("java/lang/invoke/VarHandle");
    }

    public boolean hasPermittedSubclasses() {
        return this.permittedSubclassesPresent;
    }

    public final String getModifierString() {
        return this.accessFlags.getModifierString();
    }

    public boolean isGenerated() throws ZkmClassNotFoundException {
        return this.creationKind != 0;
    }

    @Override
    public String getClassSimpleName() {
        String classNameValue = this.getClassName();
        int ba = classNameValue.lastIndexOf(47);
        return ba == -1 ? classNameValue : classNameValue.substring(ba + 1);
    }

    public boolean isInnerClassesTrimPending() {
        return this.innerClassesTrimPending;
    }

    public byte[] getClassDigest() {
        return this.classDigest.clone();
    }

    public boolean isStaticInnerClass() {
        return this.innerClassEntry != null && this.innerClassEntry.isStatic();
    }

    public boolean isInnerClass() {
        return this.innerClassEntry != null;
    }

    public void clearInnerClassInfo() {
        this.innerClassEntry = null;
        this.enclosingMethodPresent = false;
    }

    public final String[] getInterfaceNames() {
        String[] interfaceNames;
        if (this.interfaces == null) {
            interfaceNames = new String[0];
        } else {
            interfaceNames = new String[this.interfaces.length];
            int ba = 0;
            int bb = 0;

            for (ClassConstantBase[] classConstantBases = this.interfaces; bb < classConstantBases.length; classConstantBases = this.interfaces) {
                interfaceNames[ba] = this.interfaces[ba].getClassName();
                bb = ++ba;
            }
        }

        return interfaceNames;
    }

    public final boolean isAbstract() {
        return this.accessFlags.isAbstract();
    }

    public final boolean isFinal() {
        return this.accessFlags.isFinal();
    }

    public List getAllVersions() {
        ArrayList arrayList;
        if (this.versionedVariants != null) {
            arrayList = new ArrayList(this.versionedVariants.size() + 1);
            arrayList.add(this);
            arrayList.addAll(this.versionedVariants);
        } else if (this.baseVersionClass != null) {
            arrayList = new ArrayList(this.baseVersionClass.versionedVariants.size() + 1);
            arrayList.add(this.baseVersionClass);
            arrayList.addAll(this.baseVersionClass.versionedVariants);
        } else {
            arrayList = new ArrayList(1);
            arrayList.add(this);
        }

        return arrayList;
    }

    public InputFileLocation getInputLocation() {
        return this.inputLocations != null && this.inputLocations.size() != 0 ? (InputFileLocation) this.inputLocations.get(0) : null;
    }

    public void setHasPermittedSubclasses() {
        this.permittedSubclassesPresent = true;
    }

    public String getOriginalPackagePath() {
        return getPackagePath(this.originalClassName);
    }

    public int getInputLocationCount() {
        return this.inputLocations.size();
    }

    public abstract AbstractMethodInfo[] getDeclaredMethods();

    public Set collectAnnotationValues(String string, String string1, boolean bl) {
        HashSet hashSet = ZkmUtils.createHashSet(13);

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof AnnotationsAttribute) {
                AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) this.attributes[attributeIndex];
                AnnotationEntry annotationEntry = annotationsAttribute.findAnnotation(string, bl);
                if (annotationEntry != null) {
                    String elementClassValue = annotationEntry.getElementClassValue(string1, bl);
                    if (elementClassValue != null && !elementClassValue.startsWith("[")) {
                        hashSet.add(elementClassValue);
                    }
                }
            }
        }

        AbstractFieldInfo[] abstractFieldInfos = this.getFields();

        for (int abstractFieldInfoIndex = 0; abstractFieldInfoIndex < abstractFieldInfos.length; abstractFieldInfoIndex++) {
            Set set1 = abstractFieldInfos[abstractFieldInfoIndex].collectAnnotationValues(string, string1, bl);
            hashSet.addAll(set1);
        }

        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < abstractMethodInfos.length; abstractMethodInfoIndex++) {
            Set set2 = abstractMethodInfos[abstractMethodInfoIndex].collectAnnotationValues(string, string1, bl);
            hashSet.addAll(set2);
        }

        return hashSet;
    }

    public static String getPackagePath(String string) {
        return substringBeforeLast(string, '/');
    }

    public AbstractMethodInfo findMethod(MethodSignature methodSignature1) {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < abstractMethodInfos.length; abstractMethodInfoIndex++) {
            if (abstractMethodInfos[abstractMethodInfoIndex].getSignature().equals(methodSignature1)) {
                return abstractMethodInfos[abstractMethodInfoIndex];
            }
        }

        return null;
    }

    public boolean isPackageReferencedFromXml() {
        return this.packageReferencedFromXml;
    }

    public void setInnerClassesRemovalPending() {
        this.innerClassesTrimPending = false;
        this.innerClassesRemovalPending = true;
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }

    public static String stripOuterClassPrefix(String string) {
        int ba = string.lastIndexOf("$");
        String string1;
        if (ba == string.length() - 1) {
            string1 = string;
        } else if (ba > -1) {
            string1 = string.substring(ba + 1);
        } else {
            string1 = string;
        }

        return string1;
    }

    public boolean hasEnclosingMethod() {
        return this.enclosingMethodPresent;
    }

    public boolean supportsJava7() {
        return JavaRuntimeVersion.isAtLeastJava7(this.majorVersion);
    }

    public SyncIndexedSet collectAllInterfaces(ClassResolver classResolver1, Integer integer) throws ZkmException, IOException {
        HashSet hashSet = ZkmUtils.createHashSet();
        hashSet.add(this);
        return this.collectAllInterfaces(hashSet, classResolver1, integer);
    }

    public void trimInnerClassesAttribute() {
        if (this.innerClassesTrimPending) {
            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (this.attributes[attributeIndex] instanceof InnerClassesAttribute) {
                    ((InnerClassesAttribute) this.attributes[attributeIndex]).syncInnerNames();
                }
            }
        }
    }

    public String getEnclosingClassName() {
        if (this.innerClassEntry != null) {
            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
                    EnclosingMethodAttribute enclosingMethodAttribute = (EnclosingMethodAttribute) this.attributes[attributeIndex];
                    if (enclosingMethodAttribute.isValid()) {
                        return enclosingMethodAttribute.getEnclosingClassName();
                    }
                    break;
                }
            }

            String outerClassName = this.innerClassEntry.getOuterClassName();
            if (outerClassName != null) {
                return outerClassName;
            }

            String classNameValue = this.getClassName();
            String innerSimpleName = this.innerClassEntry.getInnerSimpleName();
            if (innerSimpleName != null && innerSimpleName.trim().length() > 0 && classNameValue.endsWith(innerSimpleName)) {
                String enclosingClassName = classNameValue.substring(0, classNameValue.length() - innerSimpleName.length());
                if (enclosingClassName.endsWith("$")) {
                    enclosingClassName = enclosingClassName.substring(0, enclosingClassName.length() - 1);
                }

                if (ClassHierarchyNode.findNode(enclosingClassName) != null) {
                    return enclosingClassName;
                }

                String enclosingClassNameValue = this.resolveAnonymousOuterClass(enclosingClassName);
                if (enclosingClassNameValue != null) {
                    return enclosingClassNameValue;
                }
            }

            int endIndex = classNameValue.lastIndexOf("$");
            if (endIndex > -1 && endIndex < classNameValue.length() - 1) {
                outerClassName = classNameValue.substring(0, endIndex);
                if (ClassHierarchyNode.findNode(outerClassName) != null) {
                    return outerClassName;
                }

                String enclosingClassNameValue2 = this.resolveAnonymousOuterClass(outerClassName);
                if (enclosingClassNameValue2 != null) {
                    return enclosingClassNameValue2;
                }
            }
        }

        return null;
    }

    @Override
    public final boolean isPrimitive() {
        return false;
    }

    public abstract AbstractFieldInfo[] getFields();

    public final void setAnnotationFlag() {
        this.accessFlags.setAnnotation();
    }

    public final boolean isModule() {
        return this.accessFlags.isModule();
    }

    public ClassFileBase(InputFileLocation inputFileLocation, byte[] classDigest, int creationKind) {
        super(null);
        this.inputLocations.add(inputFileLocation);
        if (!inputFileLocation.isPlainFile()) {
            this.fromArchive = true;
        } else {
            this.fromArchive = false;
        }

        this.classDigest = classDigest;
        this.creationKind = creationKind;
    }

    public int getInterfaceCount() {
        return this.interfaceCount;
    }

    public static String descriptorToClassName(String string) {
        int beginIndex = string.lastIndexOf("[") + 1;
        String string1 = string.substring(beginIndex);
        if (beginIndex > 0 && string1.length() == 1) {
            return null;
        }

        String string2;
        if (string1.startsWith("L") && string1.endsWith(";")) {
            string2 = string1.substring(1, string1.length() - 1);
        } else {
            string2 = string1;
        }

        return toInternalName(string2);
    }

    public boolean isFromArchive() {
        return this.fromArchive;
    }

    public ClassFileBase getBaseVersionClass() {
        return this.baseVersionClass;
    }

    public void addInputLocation(InputFileLocation inputFileLocation) {
        this.inputLocations.add(inputFileLocation);
        if (inputFileLocation.isPlainFile()) {
            this.fromArchive = false;
        }
    }

    public String resolveAnonymousOuterClass(String string) {
        String key = string;
        int endIndex = key.lastIndexOf("$");
        if (endIndex > -1 && endIndex < key.length() - 1 && NumericStringUtil.isInteger(key.substring(endIndex + 1))) {
            key = key.substring(0, endIndex);
            if (ClassHierarchyNode.findNode(key) != null) {
                return key;
            }
        }

        return null;
    }

    public void propagateMethodImplementation(
            MethodOverrideAnalyzer methodOverrideAnalyzer, ClassFileBase classFileBase, MethodSignature methodSignature1, ClassFileBase classFileBase1
    ) {
        if (classFileBase != classFileBase1) {
            String classNameValue = classFileBase.getClassName();
            methodOverrideAnalyzer.putVisibleMethodOwner(classNameValue, methodSignature1, classFileBase1);
            ClassFileBase classFileBase2 = methodOverrideAnalyzer.putOverriddenMethodOwner(classNameValue, methodSignature1, classFileBase1);
            if (!ClassHierarchyNode.isUnknownClass(classFileBase1.getClassName()) && classFileBase1.isInterface()) {
                while (
                        classFileBase2 != null
                                && classFileBase2 != classFileBase1
                                && !ClassHierarchyNode.isUnknownClass(classFileBase2.getClassName())
                                && classFileBase2.isInterface()
                ) {
                    String classNameValue2 = classFileBase2.getClassName();
                    methodOverrideAnalyzer.putVisibleMethodOwner(classNameValue2, methodSignature1, classFileBase1);
                    ClassFileBase classFileBase3 = classFileBase2;
                    classFileBase2 = methodOverrideAnalyzer.putOverriddenMethodOwner(classNameValue2, methodSignature1, classFileBase1);
                    ZkmAssert.assertTrue(
                            classFileBase2 == null || classFileBase2 != classFileBase3,
                            new String[]{"Loop: " + classFileBase.getDottedClassName() + " " + (classFileBase2 != null ? classFileBase2.getDottedClassName() : "null")}
                    );
                }
            }
        }
    }

    public int getMethodCount() {
        return this.methodCount;
    }

    public String getInnerSimpleName() {
        return this.innerClassEntry != null ? this.innerClassEntry.getInnerSimpleName() : null;
    }

    public String getNestHostName() {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof NestHostAttribute) {
                NestHostAttribute nestHostAttribute = (NestHostAttribute) this.attributes[attributeIndex];
                if (nestHostAttribute.valid) {
                    return nestHostAttribute.getHostClassName();
                }
            }
        }

        return null;
    }

    public void applyFieldRenames(TwoKeyMap twoKeyMap) throws ZkmException, IOException {
        Map map1 = twoKeyMap.getInnerMap(this.getClassName());
        if (map1 != null) {
            Iterator iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                FieldSignature fieldSignature = (FieldSignature) entry.getKey();
                FieldSignature fieldSignature1 = (FieldSignature) entry.getValue();
                if (!fieldSignature.equals(fieldSignature1)) {
                    Iterator iterator1 = this.getVersionedVariants().iterator();

                    while (iterator1.hasNext()) {
                        AbstractFieldInfo abstractFieldInfo = ((ClassFileBase) iterator1.next()).findField(fieldSignature);
                        if (abstractFieldInfo != null) {
                            abstractFieldInfo.setName(fieldSignature1.getName());
                        }
                    }
                }
            }
        }
    }

    public final String getOriginalSimpleName() {
        int ba = this.originalClassName.lastIndexOf("/");
        return ba > 0 ? this.originalClassName.substring(ba + 1) : this.originalClassName;
    }

    public final boolean isAnnotation() {
        return this.accessFlags.isAnnotation();
    }

    public void markReferencedFromXml() {
        this.referencedFromXml = true;
    }

    public final int getAccessFlags() {
        return this.accessFlags.getFlags();
    }

    @Override
    public String getPackagePath() {
        return substringBeforeLast(this.getClassName(), '/');
    }

    public static String extractClassName(String string, MutableInt mutableInt) {
        int value = string.lastIndexOf("[") + 1;
        if (mutableInt != null) {
            mutableInt.setValue(value);
        }

        String string1 = string.substring(value);
        if (value > 0 && string1.length() == 1) {
            return null;
        } else if (string1.startsWith("L") && string1.endsWith(";")) {
            return toInternalName(string1.substring(1, string1.length() - 1));
        } else {
            return string1.length() > 1 ? toInternalName(string1) : null;
        }
    }

    public final AbstractFieldInfo findField(Object object, Object object1) {
        AbstractFieldInfo[] abstractFieldInfos = this.getFields();

        for (int abstractFieldInfoIndex = 0; abstractFieldInfoIndex < abstractFieldInfos.length; abstractFieldInfoIndex++) {
            if (abstractFieldInfos[abstractFieldInfoIndex].getSourceName().equals(object) && abstractFieldInfos[abstractFieldInfoIndex].getDescriptor().equals(object1)) {
                return abstractFieldInfos[abstractFieldInfoIndex];
            }
        }

        return null;
    }

    @Override
    public String getOriginalDottedName() {
        return toDottedName(this.originalClassName);
    }

    public final int compareByName(ClassFileBase classFileBase1) {
        return this.getClassName().compareTo(classFileBase1.getClassName());
    }

    public AbstractFieldInfo findField(Object other) {
        AbstractFieldInfo[] abstractFieldInfos = this.getFields();

        for (int abstractFieldInfoIndex = 0; abstractFieldInfoIndex < abstractFieldInfos.length; abstractFieldInfoIndex++) {
            if (abstractFieldInfos[abstractFieldInfoIndex].getSignature().equals(other)) {
                return abstractFieldInfos[abstractFieldInfoIndex];
            }
        }

        return null;
    }

    public static String getMarker() {
        return marker;
    }

    public final void renameClass(String classNameArgument, HashMap hashMap) throws ZkmException, IOException {
        this.getConstantPool().renameThisClass(classNameArgument);
        this.className = classNameArgument;
        this.lowerCaseClassName = this.className.toLowerCase();
        if (!HiddenOptionFlags.KEEP_SOURCE_FILE_NAMES) {
            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (this.attributes[attributeIndex] instanceof SourceFileAttribute) {
                    hashMap.put(this.className, ((SourceFileAttribute) this.attributes[attributeIndex]).sourceFileName.getValue());
                    ((SourceFileAttribute) this.attributes[attributeIndex]).sourceFileName.setValue(this.getSimpleName() + ".java");
                    break;
                }
            }
        }

        this.setChanged();
        this.notifyObservers(new MutableInt(0), this, null);
        if (this.hasVersionedVariants()) {
            Iterator iterator = this.getVersionedVariants().iterator();

            while (iterator.hasNext()) {
                ((ClassFileBase) iterator.next()).renameClass(classNameArgument, hashMap);
            }
        }
    }

    public final String getSimpleName() {
        int ba = this.className.lastIndexOf("/");
        return ba > 0 ? this.className.substring(ba + 1) : this.className;
    }

    public ArrayList getSuperclassChain(ClassResolver classResolver1, Integer integer) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = null;
        String superclassName = this.getSuperclassName();
        int ba = 0;
        ClassFileBase classFileBase1 = this;
        ClassFileBase classFileBase2 = null;
        Integer integer1 = integer;

        while (superclassName != null) {
            arrayList.add(superclassName);
            String string1 = "looking for the superclass of '" + classFileBase1.getLocationName() + "'";
            classFileBase1 = classResolver1.getVersionedClass(superclassName, integer1, string1);
            String hierarchyGapMessage;
            if (HiddenOptionFlags.TEST_HIERARCHY
                    && classFileBase2 != null
                    && (hierarchyGapMessage = ClassHierarchyNode.getHierarchyGapMessage(classFileBase2, classFileBase1, 1)) != null) {
                throw new ZkmRuntimeException(hierarchyGapMessage);
            }

            classFileBase2 = classFileBase1;
            superclassName = classFileBase1.getSuperclassName();
            integer1 = classFileBase1.hasReleaseVersion() ? classFileBase1.getReleaseVersion() : integer;
            if (ba++ > 1000) {
                if (hashSet == null) {
                    hashSet = ZkmUtils.createHashSet();
                }

                if (!hashSet.add(superclassName)) {
                    throw new ZkmRuntimeException(
                            "Class '" + classFileBase1.getLocationName() + "' appears more than once in inheritance hierarchy of class '" + this.getLocationName() + "'"
                    );
                }
            }
        }

        return arrayList;
    }

    public String getLowerCaseName() {
        return this.lowerCaseClassName;
    }

    public void readClassConstants(int ba, Integer integer, int[] integerArray, ListMultimap listMultimap) throws ClassFileFormatException {
        ConstantPoolEntry constantPoolEntry = this.getConstantPoolEntry(ba);
        if (!(constantPoolEntry instanceof ClassConstantBase)) {
            throw new ClassFileFormatException(this.getLocationName() + " : " + "Invalid Constant Pool index" + " (A)");
        }

        this.thisClassConstant = (ClassConstantBase) constantPoolEntry;
        this.className = this.thisClassConstant.getClassName();
        this.lowerCaseClassName = this.className.toLowerCase();
        this.originalClassName = this.className;
        if (listMultimap != null) {
            listMultimap.addValue((ResolvedClassConstant) this.thisClassConstant, (ProgramClass) this);
        }

        if (!this.accessFlags.isModule()) {
            ConstantPoolEntry constantPoolEntry1 = this.getConstantPoolEntry(integer);
            if (!(constantPoolEntry1 instanceof ClassConstantBase)) {
                if (!this.getClassName().equals("java/lang/Object")) {
                    throw new ClassFileFormatException(this.getLocationName() + " : " + "Invalid Constant Pool index" + " (B)");
                }
            } else {
                this.superClassConstant = (ClassConstantBase) constantPoolEntry1;
                if (listMultimap != null && this.superClassConstant instanceof ResolvedClassConstant) {
                    listMultimap.addValue((ResolvedClassConstant) this.superClassConstant, (ProgramClass) this);
                }
            }
        }

        this.interfaces = new ClassConstantBase[integerArray.length];
        int bc = 0;
        int bd = 0;

        for (int interfaceCountValue = this.interfaceCount; bd < interfaceCountValue; interfaceCountValue = this.interfaceCount) {
            ConstantPoolEntry constantPoolEntry2 = this.getConstantPoolEntry(integerArray[bc]);
            if (!(constantPoolEntry2 instanceof ClassConstantBase)) {
                throw new ClassFileFormatException(this.getLocationName() + " : " + "Invalid Constant Pool index" + " (C)");
            }

            this.interfaces[bc] = (ClassConstantBase) constantPoolEntry2;
            if (listMultimap != null && this.interfaces[bc] instanceof ResolvedClassConstant) {
                listMultimap.addValue((ResolvedClassConstant) this.interfaces[bc], (ProgramClass) this);
            }

            bd = ++bc;
        }
    }

    public Integer getReleaseVersion() {
        return this.releaseVersion;
    }

    public abstract boolean isProgramClass();

    public static String toDottedName(String string) {
        return string.replace('/', '.');
    }

    public void readMagicAndVersion(ClassFileInputStream classFileInputStream) throws ClassFileFormatException, IOException {
        int ba = classFileInputStream.readInt();
        if (ba != -889275714) {
            throw new ClassFileFormatException("'" + this.getLocationName() + "' is not a valid class file : '" + Integer.toHexString(ba) + "'");
        } else {
            this.minorVersion = classFileInputStream.readUnsignedShort();
            this.majorVersion = classFileInputStream.readUnsignedShort();
            if (this.majorVersion < 45 || this.majorVersion == 45 && this.minorVersion < 3) {
                throw new ClassFileFormatException("'" + this.getLocationName() + "' is an incompatible version. : " + this.majorVersion + "." + this.minorVersion);
            }
        }
    }

    public void markPackageReferencedFromXml() {
        this.packageReferencedFromXml = true;
    }

    public boolean isPublic() {
        return this.accessFlags.isPublic();
    }

    public boolean hasReleaseVersion() {
        return this.releaseVersion != null;
    }

    public boolean isRenamed() {
        return !this.originalClassName.equals(this.className);
    }

    public String getSuperclassName() {
        return this.superClassConstant != null ? this.superClassConstant.getClassName() : null;
    }

    public void setInnerClassesTrimPending() {
        this.innerClassesTrimPending = true;
        this.innerClassesRemovalPending = false;
    }

    public final boolean isInterface() {
        return this.accessFlags.isInterface();
    }

    public boolean supportsJava19() {
        return JavaRuntimeVersion.isAtLeastJava19(this.majorVersion);
    }

    public final void collectStatistics(ProcessingStatistics processingStatistics1) {
        processingStatistics1.incrementClassCount();
        processingStatistics1.addPackageName(this.getPackagePath());
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();
        int lengthValue = abstractMethodInfos.length;

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < lengthValue; abstractMethodInfoIndex++) {
            abstractMethodInfos[abstractMethodInfoIndex].collectStatistics(processingStatistics1);
        }
    }

    public final AbstractFieldInfo[] findFieldsByName(Object object) {
        ArrayList arrayList = new ArrayList();
        AbstractFieldInfo[] abstractFieldInfos = this.getFields();

        for (int abstractFieldInfoIndex = 0; abstractFieldInfoIndex < abstractFieldInfos.length; abstractFieldInfoIndex++) {
            if (abstractFieldInfos[abstractFieldInfoIndex].getSourceName().equals(object)) {
                arrayList.add(abstractFieldInfos[abstractFieldInfoIndex]);
            }
        }

        AbstractFieldInfo[] abstractFieldInfos1 = new AbstractFieldInfo[arrayList.size()];
        return ((com.zelix.klassmaster.classfile.AbstractFieldInfo[]) (arrayList.toArray(abstractFieldInfos1)));
    }

    public ClassConstantBase getThisClassConstant() {
        return this.thisClassConstant;
    }

    public boolean supportsJava9() {
        return JavaRuntimeVersion.isAtLeastJava9(this.majorVersion);
    }

    public final String getTypeDescriptor() {
        return 'L' + this.className + ';';
    }

    @Override
    public String getInputPath() {
        return ((InputFileLocation) this.inputLocations.get(0)).getName();
    }

    public static void setMarker() {
        marker = "T4iqpc";
    }

    public String getInterfaceName(int interfaceIndex) {
        return this.interfaces[interfaceIndex].getClassName();
    }

    public boolean supportsJava4() {
        return JavaRuntimeVersion.isAtLeastJava4(this.majorVersion);
    }

    public boolean supportsJava6() {
        return JavaRuntimeVersion.isAtLeastJava6(this.majorVersion);
    }

    public void setReleaseVersion(Integer integer) {
        this.releaseVersion = integer;
    }

    @Override
    public String getDisplayLocationName() {
        String qualifiedName = ((InputFileLocation) this.inputLocations.get(0)).getQualifiedName();
        String string1 = TempFileManager.replaceTempPaths(qualifiedName);
        if (!qualifiedName.equals(string1)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(string1);
            stringBuilder.append(" (");
            stringBuilder.append(qualifiedName);
            stringBuilder.append(")");
            return stringBuilder.toString();
        } else {
            return qualifiedName;
        }
    }

    public boolean supportsJava8() {
        return JavaRuntimeVersion.isAtLeastJava8(this.majorVersion);
    }

    @Override
    public final String getDottedClassName() {
        return this.className.replace('/', '.');
    }

    public boolean supportsJava5() {
        return JavaRuntimeVersion.isAtLeastJava5(this.majorVersion);
    }

    public static String toTypeDescriptor(String string, int ba) {
        String string1 = toInternalName(string);
        string1 = "L" + string1 + ";";
        if (ba > 0) {
            string1 = ZkmStringUtils.pad(string1, 82, string1.length() + ba, 91);
        }

        return string1;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public final void applyParameterChanges(ClassHierarchyNode classHierarchyNode, MethodParameterChangeSet methodParameterChangeSet) throws ZkmException, IOException {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < this.methodCount; abstractMethodInfoIndex++) {
            AbstractMethodInfo abstractMethodInfo = abstractMethodInfos[abstractMethodInfoIndex];
            if (!abstractMethodInfo.isStaticInitializer()) {
                methodParameterChangeSet.validateMappedSignature(classHierarchyNode, this, abstractMethodInfo);
            }
        }
    }

    public SyncIndexedSet collectAllInterfaces(HashSet hashSet, ClassResolver classResolver1, Integer integer) throws ZkmException, IOException {
        SyncIndexedSet syncIndexedSet = new SyncIndexedSet();
        int syncIndexedSetSizeValue = 0;
        int bg = 0;

        for (int interfaceCountValue = this.interfaceCount; bg < interfaceCountValue; interfaceCountValue = this.interfaceCount) {
            String classNameValue = this.interfaces[syncIndexedSetSizeValue].getClassName();
            syncIndexedSet.add(classNameValue);
            bg = ++syncIndexedSetSizeValue;
        }

        syncIndexedSetSizeValue = 0;
        int syncIndexedSetSize = syncIndexedSet.size();
        Integer integer1 = integer;

        while (syncIndexedSetSizeValue < syncIndexedSetSize) {
            for (int elementIndex = syncIndexedSetSizeValue; elementIndex < syncIndexedSetSize; elementIndex++) {
                String elementAtValue = (String) syncIndexedSet.getElementAt(elementIndex);
                String string2 = "looking for an interface implemented by '" + this.getLocationName() + "'";
                ClassFileBase classFileBase1 = classResolver1.getVersionedClass(elementAtValue, integer1, string2);
                integer1 = classFileBase1.hasReleaseVersion() ? classFileBase1.getReleaseVersion() : integer;
                String hierarchyGapMessage;
                if (HiddenOptionFlags.TEST_HIERARCHY && (hierarchyGapMessage = ClassHierarchyNode.getHierarchyGapMessage(this, classFileBase1, 2)) != null) {
                    throw new ZkmRuntimeException(hierarchyGapMessage);
                }

                if (hashSet.add(classFileBase1)) {
                    String[] interfaceNames = classFileBase1.getInterfaceNames();

                    for (int interfaceNameIndex = 0; interfaceNameIndex < interfaceNames.length; interfaceNameIndex++) {
                        syncIndexedSet.add(interfaceNames[interfaceNameIndex]);
                    }
                }
            }

            syncIndexedSetSizeValue = syncIndexedSetSize;
            syncIndexedSetSize = syncIndexedSet.size();
        }

        String superclassName = this.getSuperclassName();
        if (superclassName != null) {
            String string5 = "looking for the superclass of '" + this.getLocationName() + "'";
            ClassFileBase classFileBase2 = classResolver1.getVersionedClass(superclassName, integer1, string5);
            if (classFileBase2.hasReleaseVersion()) {
                classFileBase2.getReleaseVersion();
            }

            String hierarchyGapMessageValue;
            if (HiddenOptionFlags.TEST_HIERARCHY && (hierarchyGapMessageValue = ClassHierarchyNode.getHierarchyGapMessage(this, classFileBase2, 1)) != null) {
                throw new ZkmRuntimeException(hierarchyGapMessageValue);
            }

            if (hashSet.add(classFileBase2)) {
                SyncIndexedSet syncIndexedSet1 = classFileBase2.collectAllInterfaces(hashSet, classResolver1, integer);
                int elementIndexValue = 0;
                bg = 0;

                for (int syncIndexedSet1Size = syncIndexedSet1.size(); bg < syncIndexedSet1Size; syncIndexedSet1Size = syncIndexedSet1.size()) {
                    String elementAtValue2 = (String) syncIndexedSet1.getElementAt(elementIndexValue);
                    syncIndexedSet.add(elementAtValue2);
                    bg = ++elementIndexValue;
                }
            }
        }

        return syncIndexedSet;
    }

    @Override
    public abstract ConstantPoolEntry getConstantPoolEntry(int ba);

    public final Enumeration enumerateAnnotationTypes() {
        Set set1 = this.getAnnotationTypes();
        return set1 == null ? new EmptyEnumeration() : Collections.enumeration(set1);
    }

    public final AbstractMethodInfo[] findMethodsByNameAndType(FieldNameTypeSignature fieldNameTypeSignature) {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();
        ArrayList arrayList = new ArrayList();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < abstractMethodInfos.length; abstractMethodInfoIndex++) {
            if (abstractMethodInfos[abstractMethodInfoIndex].getJvmName().equals(fieldNameTypeSignature.getName())
                    && abstractMethodInfos[abstractMethodInfoIndex].getParameterDescriptor().equals(fieldNameTypeSignature.getParameterDescriptor())) {
                arrayList.add(abstractMethodInfos[abstractMethodInfoIndex]);
            }
        }

        if (arrayList.size() == 0) {
            return null;
        }

        AbstractMethodInfo[] abstractMethodInfos1 = new AbstractMethodInfo[arrayList.size()];
        return ((com.zelix.klassmaster.classfile.AbstractMethodInfo[]) (arrayList.toArray(abstractMethodInfos1)));
    }

    @Override
    public int compareTo(Object other) {
        return this.compareByName((ClassFileBase) other);
    }

    public String formatInheritedAnnotations(ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        StringBuilder stringBuilder = new StringBuilder();

        Iterator iterator;
        try {
            iterator = classHierarchyQuery.getClassAnnotations(this.getClassName(), this.hasReleaseVersion() ? this.getReleaseVersion() : null, true).iterator();
        } catch (ClassFileLoadException classFileLoadException) {
            return this.formatAnnotations();
        }

        while (iterator.hasNext()) {
            stringBuilder.append('@');
            String string = (String) iterator.next();
            stringBuilder.append(toDottedName(string));
            stringBuilder.append(" ");
        }

        return stringBuilder.toString();
    }

    public ClassFileBase selectVersionForRelease(Integer integer) {
        ClassFileBase classFileBase1 = this;
        if (this.versionedVariants != null) {
            Iterator iterator = this.versionedVariants.iterator();

            while (iterator.hasNext()) {
                ClassFileBase classFileBase2 = (ClassFileBase) iterator.next();
                if (classFileBase2.releaseVersion <= integer) {
                    classFileBase1 = classFileBase2;
                }
            }
        }

        return classFileBase1;
    }

    public String formatAnnotations() {
        StringBuffer stringBuffer = new StringBuffer();
        Enumeration enumeration = this.enumerateAnnotationTypes();

        while (enumeration.hasMoreElements()) {
            stringBuffer.append('@');
            String string = (String) enumeration.nextElement();
            stringBuffer.append(toDottedName(string));
            stringBuffer.append(" ");
        }

        return stringBuffer.toString();
    }

    public boolean isMultiRelease() {
        return this.hasVersionedVariants() || this.hasReleaseVersion();
    }

    public int getFieldCount() {
        return this.fieldCount;
    }

    public static String toInternalName(String string) {
        return string.replace('.', '/');
    }

    public abstract void updateFieldReferences(ScriptEnvironment scriptEnvironment1, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException;

    public static String stripPackage(String string) {
        int ba = string.lastIndexOf("/");
        String string1;
        if (ba > -1) {
            string1 = string.substring(ba + 1);
        } else {
            string1 = string;
        }

        return string1;
    }

    public boolean isReferencedFromXml() {
        return this.referencedFromXml;
    }

    @Override
    public String getLocationName() {
        return ((InputFileLocation) this.inputLocations.get(0)).getQualifiedName();
    }

    public abstract void remapClassNames(int ba, int bb, HashMap hashMap, Object object, Object object1, HashMap hashMap1, MessageReporter messageReporter1) throws ZkmException, IOException;

    public boolean hasAnnotation(Object object) {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof AnnotationsAttribute) {
                String[] annotationTypeNames = ((AnnotationsAttribute) this.attributes[attributeIndex]).getAnnotationTypeNames();

                for (int annotationTypeNameIndex = 0; annotationTypeNameIndex < annotationTypeNames.length; annotationTypeNameIndex++) {
                    if (annotationTypeNames[annotationTypeNameIndex].equals(object)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isClassFile() {
        return true;
    }

    public boolean hasVersionedVariants() {
        return this.versionedVariants != null && this.versionedVariants.size() > 0;
    }

    public void applyMethodRenames(TwoKeyMap twoKeyMap) throws ZkmException, IOException {
        Map map1 = twoKeyMap.getInnerMap(this.getClassName());
        if (map1 != null) {
            Iterator iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                MethodSignature methodSignature1 = (MethodSignature) entry.getKey();
                MethodSignature methodSignature2 = (MethodSignature) entry.getValue();
                if (!methodSignature1.equals(methodSignature2)) {
                    Iterator iterator1 = this.getVersionedVariants().iterator();

                    while (iterator1.hasNext()) {
                        AbstractMethodInfo abstractMethodInfo = ((ClassFileBase) iterator1.next()).findMethod(methodSignature1);
                        if (abstractMethodInfo != null) {
                            abstractMethodInfo.setName(methodSignature2.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public ClassFileBase getOwningClass() {
        return this;
    }

    public int getCreationKind() {
        return this.creationKind;
    }

    public abstract void updateMethodReferences(ScriptEnvironment scriptEnvironment1, ClassMemberLookup classMemberLookup1, Object object) throws ZkmException, IOException;

    public List findMethodsByName(Object object) {
        ArrayList arrayList = new ArrayList();
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < abstractMethodInfos.length; abstractMethodInfoIndex++) {
            if (abstractMethodInfos[abstractMethodInfoIndex].getJvmName().equals(object)) {
                arrayList.add(abstractMethodInfos[abstractMethodInfoIndex]);
            }
        }

        return arrayList;
    }

    public boolean isInnerClassesRemovalPending() {
        return this.innerClassesRemovalPending;
    }

    public final void setFinal() {
        this.accessFlags.setFinal(false);
    }

    public List getVersionedVariants() {
        return this.versionedVariants != null ? new ArrayList(this.versionedVariants) : new ArrayList(0);
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public final void propagateParameterChanges(MethodParameterChangeSet methodParameterChangeSet) {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < this.methodCount; abstractMethodInfoIndex++) {
            methodParameterChangeSet.checkOverriddenMapping(abstractMethodInfos[abstractMethodInfoIndex]);
        }
    }

    public void addVersionedVariant(ClassFileBase classFileBase1, Integer integer) {
        if (this.versionedVariants == null) {
            this.versionedVariants = new ArrayList();
        }

        classFileBase1.baseVersionClass = this;
        classFileBase1.setReleaseVersion(integer);
        this.versionedVariants.add(classFileBase1);
        if (this.versionedVariants.size() > 1) {
            Collections.sort(this.versionedVariants, (classFileBase, classFileBase1x) -> ((ClassFileBase) classFileBase).releaseVersion - ((ClassFileBase) classFileBase1x).releaseVersion);
        }
    }

    public final void propagateMethodRenames(MethodRenamer methodRenamer1) throws IOException {
        AbstractMethodInfo[] abstractMethodInfos = this.getDeclaredMethods();

        for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < this.methodCount; abstractMethodInfoIndex++) {
            methodRenamer1.checkChangeLogOverrideMapping(this, abstractMethodInfos[abstractMethodInfoIndex]);
        }
    }

    public boolean isVersionedVariant() {
        return this.baseVersionClass != null;
    }

    public Enumeration enumerateInputLocations() {
        return Collections.enumeration(this.inputLocations);
    }

    static {
        if (getMarker() != null) {
            setMarker();
        }
    }
}
