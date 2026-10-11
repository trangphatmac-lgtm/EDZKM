package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.ModuleInfoClass;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.config.GroupingsSpec;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.constants.IntegerEncryptionExclusions;
import com.zelix.klassmaster.obfuscator.constants.LongEncryptionExclusionHandler;
import com.zelix.klassmaster.obfuscator.exceptions.ExceptionObfuscationExclusions;
import com.zelix.klassmaster.obfuscator.exclude.AbstractExclusionSpec;
import com.zelix.klassmaster.obfuscator.exclude.AccessFlagsSpec;
import com.zelix.klassmaster.obfuscator.exclude.ClassNamePattern;
import com.zelix.klassmaster.obfuscator.exclude.DescribableSpec;
import com.zelix.klassmaster.obfuscator.exclude.ExclusionHandlerBase;
import com.zelix.klassmaster.obfuscator.exclude.ExclusionSetBase;
import com.zelix.klassmaster.obfuscator.exclude.ExistingSerializedClassesHandler;
import com.zelix.klassmaster.obfuscator.exclude.FixedClassesExclusionSet;
import com.zelix.klassmaster.obfuscator.exclude.MemberSpecCollector;
import com.zelix.klassmaster.obfuscator.exclude.MemberSpecifierHandler;
import com.zelix.klassmaster.obfuscator.exclude.MethodArgsPattern;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.obfuscator.exclude.PackagePatternSpec;
import com.zelix.klassmaster.obfuscator.flow.FlowObfuscationExclusions;
import com.zelix.klassmaster.obfuscator.parameters.MethodParameterExclusions;
import com.zelix.klassmaster.obfuscator.references.ReferenceObfuscationExclusions;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionAccessMatcher;
import com.zelix.klassmaster.obfuscator.string.StringEncryptionExclusionSpec;
import com.zelix.klassmaster.obfuscator.trim.RemoveMethodCallsHandler;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.ScriptStatementInfo;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ArrayEnumeration;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ASTRenameFilterParameter extends ZkmScriptSimpleNode implements MemberSpecifierHandler, MemberSpecCollector, Comparable {
    public static final char CARET_TAG = "^".charAt(0);
    public boolean standaloneAnnotation;
    public ASTComplexFieldSpecifier fieldSpecifier;
    public ScriptStatementInfo statementInfo;
    public ASTComplexAnnotationSpecifier memberAnnotation;
    public int specificityScore;
    public ASTContainingClause containingClause;
    public String linkClassSuffix;
    public String archivePathPattern;
    public String referencingAnnotationComponent;
    public String linkMethodSignature;
    public String accessModifier;
    public ASTComplexMethodSpecifier methodSpecifier;
    public ClassNamePattern classNamePattern;
    public String fieldType;
    public ASTContainedInClause containedInClause;
    public int specifierKind;
    public AccessFlagsSpec classAccessFlags;
    public String referencingAnnotation;
    public ASTComplexAnnotationSpecifier extendsAnnotation;
    public String extendsClassName;
    public ScriptEnvironment scriptEnvironment;
    public AccessFlagsSpec memberAccessFlags;
    public boolean plusSignatureClasses;
    public PackagePatternSpec packagePattern;
    public ASTComplexAnnotationSpecifier[] parameterAnnotations;
    public MethodArgsPattern argsPattern;
    public String linkClassPrefix;
    public String parameterText;
    public ASTComplexAnnotationSpecifier classAnnotation;
    public ASTComplexModuleSpecifier moduleSpecifier;
    public String linkClassName;
    public String linkMethodPrefix;
    public Map classModifiers = ZkmUtils.createHashMap(19);
    public Map memberModifiers = ZkmUtils.createHashMap(19);
    public ArrayList linkPackageNames = new ArrayList();
    public List implementsAnnotations = new ArrayList();
    public List implementsNames = new ArrayList();
    public List throwsTypes = new ArrayList();
    public boolean enabled = true;

    public double computeFieldSpecificity() {
        ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier = this.memberAnnotation;
        return this.fieldSpecifier.estimateMatchFraction(this.memberAccessFlags, aSTComplexAnnotationSpecifier);
    }

    public void applyFlowExclusion(FlowObfuscationExclusions flowObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        flowObfuscationExclusions.hasOriginalNameCaches()
                                || flowObfuscationExclusions.isHierarchySealed()
                                || flowObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(flowObfuscationExclusions.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    this.excludeFlowClasses(flowObfuscationExclusions, flowObfuscationExclusions.getIncludedClasses(), set1);
                    break;
                case 3:
                    this.excludeFlowFieldHosts(flowObfuscationExclusions, flowObfuscationExclusions.getIncludedClasses(), set1);
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && flowObfuscationExclusions.canUseMethodNameIndex()) {
                        Enumeration enumeration = flowObfuscationExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(flowObfuscationExclusions, methodInfo1)) {
                                            flowObfuscationExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.excludeFlowMethods(flowObfuscationExclusions, flowObfuscationExclusions.getIncludedMethods(), set1);
                    }
            }
        }
    }

    public void includeReferenceFields(ReferenceObfuscationExclusions referenceObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) enumeration.nextElement();
            if (this.matchesField(abstractFieldInfo, referenceObfuscationExclusions)) {
                ClassFileBase classFileBase = abstractFieldInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, referenceObfuscationExclusions)) {
                    if (this.containedInClause == null) {
                        String string2 = "inclusion parameter '" + this.parameterText + "'";
                        referenceObfuscationExclusions.includeFieldReference(abstractFieldInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = referenceObfuscationExclusions.getReferencingMethods(abstractFieldInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(referenceObfuscationExclusions, methodBytecode1)) {
                                referenceObfuscationExclusions.includeFieldReference(
                                        abstractFieldInfo, methodBytecode1, "inclusion parameter '" + this.parameterText + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public void excludeParameterMethods(MethodParameterExclusions methodParameterExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(methodParameterExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, methodParameterExclusions)) {
                    methodParameterExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void setLinkMethodSignature(String linkMethodSignatureArgument) {
        this.linkMethodSignature = linkMethodSignatureArgument;
        int endIndex = this.linkMethodSignature.indexOf("<link>");
        this.linkMethodPrefix = this.linkMethodSignature.substring(0, endIndex);
    }

    public boolean hasExtendsClause() {
        return this.extendsClassName != null;
    }

    public void unexcludeFlowFieldHosts(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                flowObfuscationExclusions.unexcludeFromPredicateFields(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public double computeClassSpecificity() {
        return this.hasLinkClassName() ? 0.05 : this.classNamePattern.computeSpecificity(this.classAccessFlags, this.classAnnotation);
    }

    public void applyExceptionExclusion(ExceptionObfuscationExclusions exceptionObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        exceptionObfuscationExclusions.hasOriginalNameCaches()
                                || exceptionObfuscationExclusions.isHierarchySealed()
                                || exceptionObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(exceptionObfuscationExclusions.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    this.excludeExceptionClasses(exceptionObfuscationExclusions, exceptionObfuscationExclusions.getIncludedClasses(), set1);
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && exceptionObfuscationExclusions.canUseMethodNameIndex()) {
                        Enumeration enumeration = exceptionObfuscationExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(exceptionObfuscationExclusions, methodInfo1)) {
                                            exceptionObfuscationExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.excludeExceptionMethods(exceptionObfuscationExclusions, exceptionObfuscationExclusions.getIncludedMethods(), set1);
                    }
            }
        }
    }

    public boolean hasContainingClause() {
        return this.containingClause != null;
    }

    public void addClassModifier(String accessModifierArgument) {
        this.addModifier(accessModifierArgument, this.classModifiers);
    }

    public boolean isInterfaceRequired() {
        return this.classAccessFlags.isInterfaceRequired();
    }

    public final boolean hasReferencingAnnotation() {
        return this.referencingAnnotation != null;
    }

    public void setModuleSpecifier(ASTComplexModuleSpecifier aSTComplexModuleSpecifier) {
        this.moduleSpecifier = aSTComplexModuleSpecifier;
    }

    public void unexcludeClassIfCaret(ExclusionHandlerBase exclusionHandlerBase, ProgramClass programClass1) throws ZkmException, IOException {
        if (this.classNamePattern.hasCaretTag()) {
            exclusionHandlerBase.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
        }
    }

    public void unexcludeReflectionMembers(ReflectionAccessMatcher reflectionAccessMatcher, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            reflectionAccessMatcher.markFieldNotAccessed(fieldInfos[fieldInfoIndex], "unexclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            if (!methodInfo1.isConstructor() && !methodInfo1.isStaticInitializer()) {
                reflectionAccessMatcher.markMethodNotAccessed(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void unexcludeNameFields(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, nameExclusionSet)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                    nameExclusionSet.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                    this.unexcludePackageIfCaret(nameExclusionSet, classMatchName);
                    this.unexcludeClassIfCaret(nameExclusionSet, programClass1);
                }
            }
        }
    }

    @Override
    public void setFieldType(String fieldTypeArgument) {
        this.fieldType = fieldTypeArgument;
    }

    public void setContainedInClause(ASTContainedInClause aSTContainedInClause) {
        this.containedInClause = aSTContainedInClause;
    }

    public void applyLongEncryptionUnexclusion(LongEncryptionExclusionHandler longEncryptionExclusionHandler) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    longEncryptionExclusionHandler.hasOriginalNameCaches()
                            || longEncryptionExclusionHandler.isHierarchySealed()
                            || longEncryptionExclusionHandler.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(longEncryptionExclusionHandler.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.unexcludeLongEncryptionClasses(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getCandidateClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && longEncryptionExclusionHandler.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = longEncryptionExclusionHandler.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, longEncryptionExclusionHandler)) {
                                        longEncryptionExclusionHandler.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeLongEncryptionFields(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getCandidateFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && longEncryptionExclusionHandler.canUseMethodNameIndex()) {
                    Enumeration enumeration = longEncryptionExclusionHandler.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, longEncryptionExclusionHandler)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(longEncryptionExclusionHandler, methodInfo1)) {
                                        longEncryptionExclusionHandler.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeLongEncryptionMethods(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getCandidateMethods(), set1);
                }
        }
    }

    @Override
    public void addParameterType(Object object) {
        this.throwsTypes.add(object);
    }

    public static boolean matchesParameterAnnotations(
            AbstractMethodInfo abstractMethodInfo,
            ASTComplexAnnotationSpecifier[] aSTComplexAnnotationSpecifiers,
            MethodArgsPattern methodArgsPattern1,
            ClassHierarchyQuery classHierarchyQuery
    ) {
        if (aSTComplexAnnotationSpecifiers != null) {
            boolean bl = false;
            int ba;
            if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                String originalDescriptor = abstractMethodInfo.getOriginalDescriptor();
                ba = ConstantPoolEntry.getParameterTypes(originalDescriptor).size();
                bl = true;
            } else {
                ba = abstractMethodInfo.getParameterCount();
            }

            if (methodArgsPattern1.isAnyArgs()) {
                if (aSTComplexAnnotationSpecifiers[0] == null) {
                    return true;
                }

                HashSet hashSet1 = ZkmUtils.createHashSet(23);

                for (int i = 0; i < ba; i++) {
                    Enumeration enumeration1;
                    if (bl) {
                        enumeration1 = abstractMethodInfo.enumerateParameterAnnotationTypes(i);
                    } else {
                        enumeration1 = abstractMethodInfo.enumerateDeclaredParameterAnnotationTypes(i);
                    }

                    while (enumeration1.hasMoreElements()) {
                        String string2 = (String) enumeration1.nextElement();
                        if (bl) {
                            hashSet1.add(classHierarchyQuery.getOriginalClassName(string2));
                        } else {
                            hashSet1.add(string2);
                        }
                    }
                }

                return aSTComplexAnnotationSpecifiers[0].matchesAnyAnnotation(hashSet1);
            }

            if (aSTComplexAnnotationSpecifiers.length == ba) {
                for (int aSTComplexAnnotationSpecifierIndex = 0; aSTComplexAnnotationSpecifierIndex < aSTComplexAnnotationSpecifiers.length; aSTComplexAnnotationSpecifierIndex++) {
                    if (aSTComplexAnnotationSpecifiers[aSTComplexAnnotationSpecifierIndex] != null) {
                        HashSet hashSet = ZkmUtils.createHashSet(13);
                        Enumeration enumeration;
                        if (bl) {
                            enumeration = abstractMethodInfo.enumerateParameterAnnotationTypes(aSTComplexAnnotationSpecifierIndex);
                        } else {
                            enumeration = abstractMethodInfo.enumerateDeclaredParameterAnnotationTypes(aSTComplexAnnotationSpecifierIndex);
                        }

                        while (enumeration.hasMoreElements()) {
                            String string1 = (String) enumeration.nextElement();
                            if (bl) {
                                hashSet.add(classHierarchyQuery.getOriginalClassName(string1));
                            } else {
                                hashSet.add(string1);
                            }
                        }

                        if (!aSTComplexAnnotationSpecifiers[aSTComplexAnnotationSpecifierIndex].matchesAnyAnnotation(hashSet)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void checkPackagePattern() {
        if (this.packagePattern != null) {
        }
    }

    public void applyExceptionUnexclusion(ExceptionObfuscationExclusions exceptionObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        exceptionObfuscationExclusions.hasOriginalNameCaches()
                                || exceptionObfuscationExclusions.isHierarchySealed()
                                || exceptionObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(exceptionObfuscationExclusions.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    this.unexcludeExceptionClasses(exceptionObfuscationExclusions, exceptionObfuscationExclusions.getCandidateClasses(), set1);
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && exceptionObfuscationExclusions.canUseMethodNameIndex()) {
                        Enumeration enumeration = exceptionObfuscationExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(exceptionObfuscationExclusions, methodInfo1)) {
                                            exceptionObfuscationExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.unexcludeExceptionMethods(exceptionObfuscationExclusions, exceptionObfuscationExclusions.getCandidateMethods(), set1);
                    }
            }
        }
    }

    public void excludeSerializedClasses(ExistingSerializedClassesHandler existingSerializedClassesHandler, Enumeration enumeration) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    existingSerializedClassesHandler.hasOriginalNameCaches()
                            || existingSerializedClassesHandler.isHierarchySealed()
                            || existingSerializedClassesHandler.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(existingSerializedClassesHandler.getCandidateClasses(), bl);
        }

        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String className = programClass1.getClassName();
            String originalClassName = programClass1.getOriginalClassName();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            boolean bl1 = HiddenOptionFlags.MATCH_ORIGINAL_NAMES && existingSerializedClassesHandler.hasOriginalNameCaches();

            try {
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, existingSerializedClassesHandler) && !programClass1.isInterface()) {
                    if (!this.implementsNames.contains("java/io/Serializable") && !this.implementsNames.contains("java/io/Externalizable")) {
                        String keyValueValue;
                        String keyValue2;
                        if (bl1) {
                            keyValueValue = originalClassName;
                            keyValue2 = "java/io/Serializable";
                        } else {
                            keyValueValue = className;
                            keyValue2 = "java/io/Serializable";
                        }

                        String key = keyValue2;
                        String keyValue = keyValueValue;
                        if (!existingSerializedClassesHandler.implementsInterface(keyValue, key)) {
                            if (bl1) {
                                keyValueValue = originalClassName;
                                keyValue2 = "java/io/Externalizable";
                            } else {
                                keyValueValue = className;
                                keyValue2 = "java/io/Externalizable";
                            }

                            key = keyValue2;
                            keyValue = keyValueValue;
                            if (!existingSerializedClassesHandler.implementsInterface(keyValue, key)) {
                                continue;
                            }
                        }
                    }

                    existingSerializedClassesHandler.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
                }
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : class '"
                                        + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName())
                                        + "' not found. (E)"
                        );
            } catch (ClassFileLoadException classFileLoadException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : '"
                                        + classFileLoadException.getMessage()
                                        + "' (E)"
                        );
            }
        }
    }

    public void applyMethodParameterExclusion(MethodParameterExclusions methodParameterExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        methodParameterExclusions.hasOriginalNameCaches()
                                || methodParameterExclusions.isHierarchySealed()
                                || methodParameterExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(methodParameterExclusions.getCandidateClasses(), bl);
            }

            if (this.methodSpecifier.isLiteralName() && methodParameterExclusions.canUseMethodNameIndex()) {
                Enumeration enumeration = methodParameterExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, methodParameterExclusions)) {
                            ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                            while (arrayEnumeration.hasMoreElements()) {
                                MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                if (this.matchesMethod(methodParameterExclusions, methodInfo1)) {
                                    methodParameterExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                    this.unexcludeClassIfCaret(methodParameterExclusions, programClass1);
                                }
                            }
                        }
                    }
                }
            } else {
                this.excludeParameterMethods(methodParameterExclusions, methodParameterExclusions.getExcludedMethods(), set1);
            }
        }
    }

    public void addLinkPackageName(Object object) {
        this.linkPackageNames.add(object);
    }

    public void addImplementsAnnotation(Object object) {
        this.implementsAnnotations.add(object);
    }

    public void unexcludeReflectionMethods(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(reflectionAccessMatcher, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                    reflectionAccessMatcher.markMethodNotAccessed(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                    this.unexcludeClassIfCaret(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    @Override
    public final boolean equals(Object other) {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        boolean bl = other instanceof ASTRenameFilterParameter;
        if (flowControlKey != 0) {
            if (bl) {
                ASTRenameFilterParameter aSTRenameFilterParameter1 = (ASTRenameFilterParameter) other;
                return this.parameterText.equals(aSTRenameFilterParameter1.parameterText);
            }

            bl = false;
        }

        return bl;
    }

    public static boolean containsWildcard(String string) {
        return string.indexOf("*") != -1;
    }

    public void excludeNameClass(NameExclusionSet nameExclusionSet, ProgramClass programClass1, Set set1) throws ZkmException, IOException {
        String packagePath = programClass1.getPackagePath();
        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
            if (this.hasLinkClassName()) {
                nameExclusionSet.addClassLink(
                        programClass1, this.linkClassPrefix, this.linkClassSuffix, this.linkPackageNames, "exclusion parameter '" + this.parameterText + "'"
                );
            } else {
                nameExclusionSet.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }

            this.excludePackageIfCaret(nameExclusionSet, packagePath);
            if (this.classNamePattern != null && this.classNamePattern.hasPlusTag()) {
                this.excludeNameMembers(nameExclusionSet, programClass1);
            }
        }
    }

    public void excludeFlowClasses(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                flowObfuscationExclusions.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void unexcludeTrimClass(TrimProcessor trimProcessor1, ProgramClass programClass1, Set set1) throws ZkmException, IOException {
        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
            trimProcessor1.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            if (this.classNamePattern.hasPlusTag()) {
                this.unexcludeTrimMembers(trimProcessor1, programClass1);
            }
        }
    }

    public ASTRenameFilterParameter() {
        super(86);
    }

    public void excludeClassIfCaret(ExclusionSetBase exclusionSetBase, ProgramClass programClass1) throws ZkmException, IOException {
        if (this.classNamePattern.hasCaretTag()) {
            exclusionSetBase.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
        }
    }

    public void addImplementsName(Object object) {
        this.implementsNames.add(object);
    }

    public void unexcludeFlowClasses(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                flowObfuscationExclusions.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public boolean isFieldSpecifier() {
        return this.specifierKind == 3;
    }

    public boolean matchesField(AbstractFieldInfo abstractFieldInfo, ClassHierarchyQuery classHierarchyQuery) {
        if (!matchesAccessFlags(abstractFieldInfo.getAccessFlags(), this.memberAccessFlags)) {
            return false;
        } else if (!this.matchesFieldName(abstractFieldInfo)) {
            return false;
        } else {
            return (!(!this.matchesFieldType(abstractFieldInfo)) && (matchesFieldAnnotations(abstractFieldInfo, this.memberAnnotation, classHierarchyQuery)));
        }
    }

    public double computePackageSpecificity() {
        return this.packagePattern != null ? this.packagePattern.computeSpecificity() : 1.0;
    }

    public int computeSpecifierKind() {
        if (this.moduleSpecifier != null) {
            return 0;
        } else if (this.linkClassName == null && this.classNamePattern == null) {
            return 1;
        } else if (this.fieldSpecifier != null) {
            return 3;
        } else if (this.linkMethodSignature != null || this.methodSpecifier != null) {
            return 4;
        } else {
            return this.linkClassName == null && this.classNamePattern == null ? 99 : 2;
        }
    }

    public void excludeTrimFields(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, trimProcessor1)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                    trimProcessor1.matchField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                    this.warnPackageExclusionIgnored();
                    this.excludeClassIfCaret(trimProcessor1, programClass1);
                }
            }
        }
    }

    public static String toDottedName(String string) {
        return string.replace('/', '.');
    }

    public void excludeTrimMembers(TrimProcessor trimProcessor1, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            trimProcessor1.matchField(fieldInfos[fieldInfoIndex], "exclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            trimProcessor1.matchMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
        }
    }

    public final boolean matchesExtendsAsInterface(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        boolean bl = false;

        try {
            if (this.extendsAnnotation != null) {
                bl = classHierarchyQuery.implementsAnnotatedInterface(classFileBase.getClassName(), this.extendsClassName, this.extendsAnnotation);
            } else if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                bl = classHierarchyQuery.implementsInterfaceByOriginalName(classFileBase.getOriginalClassName(), this.extendsClassName);
            } else {
                bl = classHierarchyQuery.implementsInterface(classFileBase.getClassName(), this.extendsClassName);
            }
        } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
            this.scriptEnvironment
                    .logWarning(
                            "Error while executing \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getLineNumber()
                                    + " while analysing '"
                                    + this.parameterText
                                    + "' : class '"
                                    + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName())
                                    + "' not found. (G)"
                    );
        } catch (ClassFileLoadException classFileLoadException) {
            this.scriptEnvironment
                    .logWarning(
                            "Error while executing \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getLineNumber()
                                    + " while analysing '"
                                    + this.parameterText
                                    + "' : '"
                                    + classFileLoadException.getMessage()
                                    + "' (G)"
                    );
        }

        return bl;
    }

    public void excludeStringEncryptionFields(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, stringEncryptionExclusionSpec)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                    stringEncryptionExclusionSpec.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public static String formatArgsWithAnnotations(MethodArgsPattern arguments, ASTComplexAnnotationSpecifier[] annotations) {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        ZkmAssert.formatArray(annotations);
        StringBuilder result = new StringBuilder();
        List parameterTypes = ConstantPoolEntry.getParameterTypes(arguments.argsText);
        int parameterCount = parameterTypes.size();
        for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
            String parameterType = (String) parameterTypes.get(parameterIndex);
            if (flowControlKey == 0) {
                return parameterType;
            }
            if (annotations != null && annotations[parameterIndex] != null) {
                result.append(annotations[parameterIndex].getSpecText());
                result.append(' ');
            }
            result.append(toJavaTypeName(parameterType));
            if (parameterIndex < parameterTypes.size() - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    public void excludeReferenceMethods(ReferenceObfuscationExclusions referenceObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) enumeration.nextElement();
            if (this.matchesMethod(referenceObfuscationExclusions, abstractMethodInfo)) {
                ClassFileBase classFileBase = abstractMethodInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, referenceObfuscationExclusions)) {
                    if (this.containedInClause == null) {
                        String string2 = "exclusion parameter '" + this.parameterText + "'";
                        referenceObfuscationExclusions.excludeMethodReference(abstractMethodInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = referenceObfuscationExclusions.getReferencingMethods(abstractMethodInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(referenceObfuscationExclusions, methodBytecode1)) {
                                referenceObfuscationExclusions.excludeMethodReference(
                                        abstractMethodInfo, methodBytecode1, "exclusion parameter '" + this.parameterText + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public final boolean containedInHasLinkMethod() {
        return this.containedInClause != null
                && this.containedInClause.getFilterParameter() != null
                && this.containedInClause.getFilterParameter().linkMethodSignature != null;
    }

    public final int getLineNumber() {
        return this.statementInfo.getStatementLine();
    }

    public boolean isAnyFieldWithoutType() {
        return (!(this.fieldSpecifier == null) && (this.fieldType == null && isAnyWildcard(this.fieldSpecifier.getChildDescription())));
    }

    public void excludeTrimLinkedClasses(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        Enumeration enumeration1 = enumeration;
        HashMap hashMap = ZkmUtils.createHashMap();

        while (enumeration1.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration1.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                trimProcessor1.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
                String className = programClass1.getClassName();
                if (classSimpleMatchName.length() > this.linkClassSuffix.length()) {
                    String key = className.substring(0, className.length() - this.linkClassSuffix.length());
                    hashMap.put(key, programClass1);
                }
            }
        }

        enumeration1 = trimProcessor1.getCandidateClasses();

        while (enumeration1.hasMoreElements()) {
            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
            String classNameValue = programClass2.getClassName();
            if (hashMap.containsKey(classNameValue)) {
                ProgramClass programClass3 = (ProgramClass) hashMap.get(classNameValue);
                trimProcessor1.excludeClass(
                        programClass2, "exclusion parameter '" + this.parameterText + "' matching class '" + programClass3.getDottedClassName() + "'"
                );
            }
        }
    }

    public void warnPlusTagIgnored(String string) {
        this.scriptEnvironment
                .logError(
                        "Class exclude parameter \""
                                + this.parameterText
                                + "\" in \""
                                + this.getStatementName()
                                + "\" statement at line "
                                + this.getLineNumber()
                                + "\" has a \""
                                + "+"
                                + "\" followed by a "
                                + string
                                + " specification. \""
                                + "+"
                                + "\" ignored.",
                        true
                );
    }

    public boolean isPrivateRequired() {
        return this.memberAccessFlags.isPrivateRequired();
    }

    public void setReferencingAnnotation(String referencingAnnotationArgument) {
        this.referencingAnnotation = referencingAnnotationArgument;
    }

    public void setReferencingAnnotationComponent(String referencingAnnotationComponentArgument) {
        this.referencingAnnotationComponent = referencingAnnotationComponentArgument;
    }

    public static AccessFlagsSpec buildMemberAccessFlags(Map map1, int ba) {
        AccessFlagsSpec accessFlagsSpec1 = null;
        Iterator iterator = map1.keySet().iterator();

        while (iterator.hasNext()) {
            if (accessFlagsSpec1 == null) {
                byte memberKind;
                if (ba == 3) {
                    memberKind = 2;
                } else if (ba == 4) {
                    memberKind = 3;
                } else {
                    memberKind = 0;
                }

                accessFlagsSpec1 = new AccessFlagsSpec(memberKind);
            }

            boolean bl = false;
            String string = (String) iterator.next();
            if (string.startsWith("!")) {
                string = string.substring(1);
                bl = true;
            }

            if (bl) {
                if (string.equals("public")) {
                    accessFlagsSpec1.forbidPublic();
                } else if (string.equals("protected")) {
                    accessFlagsSpec1.forbidProtected();
                } else if (string.equals("private")) {
                    accessFlagsSpec1.forbidPrivate();
                } else if (string.equals("package")) {
                    accessFlagsSpec1.forbidPackage();
                } else if (string.equals("native")) {
                    accessFlagsSpec1.forbidNative();
                } else if (string.equals("static")) {
                    accessFlagsSpec1.forbidStatic();
                } else if (string.equals("final")) {
                    accessFlagsSpec1.forbidFinal();
                } else if (string.equals("synchronized")) {
                    accessFlagsSpec1.forbidSynchronized();
                } else if (string.equals("volatile")) {
                    accessFlagsSpec1.forbidBridge();
                } else if (string.equals("transient")) {
                    accessFlagsSpec1.forbidTransient();
                } else if (string.equals("abstract")) {
                    accessFlagsSpec1.forbidAbstract();
                } else if (string.equals("synthetic")) {
                    accessFlagsSpec1.forbidSynthetic();
                } else if (string.equals("enum")) {
                    accessFlagsSpec1.forbidEnum();
                } else if (string.equals("bridge")) {
                    accessFlagsSpec1.forbidVolatile();
                }
            } else if (string.equals("public")) {
                accessFlagsSpec1.requirePublic();
            } else if (string.equals("protected")) {
                accessFlagsSpec1.requireProtected();
            } else if (string.equals("private")) {
                accessFlagsSpec1.requirePrivate();
            } else if (string.equals("package")) {
                accessFlagsSpec1.requirePackage();
            } else if (string.equals("native")) {
                accessFlagsSpec1.requireNative();
            } else if (string.equals("static")) {
                accessFlagsSpec1.requireStatic();
            } else if (string.equals("final")) {
                accessFlagsSpec1.requireFinal();
            } else if (string.equals("synchronized")) {
                accessFlagsSpec1.requireSynchronized();
            } else if (string.equals("volatile")) {
                accessFlagsSpec1.requireVolatile();
            } else if (string.equals("transient")) {
                accessFlagsSpec1.requireTransient();
            } else if (string.equals("abstract")) {
                accessFlagsSpec1.requireAbstract();
            } else if (string.equals("synthetic")) {
                accessFlagsSpec1.requireSynthetic();
            } else if (string.equals("enum")) {
                accessFlagsSpec1.requireEnum();
            } else if (string.equals("bridge")) {
                accessFlagsSpec1.requireBridge();
            }
        }

        return accessFlagsSpec1;
    }

    public double computeMethodSpecificity() {
        return this.hasLinkMethodSignature() ? 0.05 : this.methodSpecifier.estimateMatchFraction(this.memberAccessFlags, this.argsPattern, this.memberAnnotation);
    }

    public String joinWithAnd(Vector vector) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int elementIndex = 0; elementIndex < vector.size(); elementIndex++) {
            if (elementIndex == vector.size() - 1) {
                stringBuffer.append((String) vector.elementAt(elementIndex));
            } else if (elementIndex == vector.size() - 2) {
                stringBuffer.append((String) vector.elementAt(elementIndex) + " and ");
            } else {
                stringBuffer.append((String) vector.elementAt(elementIndex) + ", ");
            }
        }

        return stringBuffer.toString();
    }

    public void excludeTrimAnnotationTypes(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ClassFileBase classFileBase = (ClassFileBase) enumeration.nextElement();
            String packagePath = classFileBase.getPackagePath();
            String simpleName = classFileBase.getSimpleName();
            if (this.matchesClass(classFileBase, set1, packagePath, simpleName, trimProcessor1)) {
                trimProcessor1.addAnnotationRetainedClass(classFileBase, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public final String getStatementName() {
        return this.statementInfo.getStatementName();
    }

    public void unexcludeNameMethods(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(nameExclusionSet, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                    nameExclusionSet.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                    this.unexcludePackageIfCaret(nameExclusionSet, classMatchName);
                    this.unexcludeClassIfCaret(nameExclusionSet, programClass1);
                    if (this.plusSignatureClasses) {
                        this.unexcludeSignatureClasses(nameExclusionSet, methodInfo1);
                    }
                }
            }
        }
    }

    public final boolean hasLinkMethodSignature() {
        return this.linkMethodSignature != null;
    }

    public void addMemberModifier(String accessModifierArgument) {
        this.addModifier(accessModifierArgument, this.memberModifiers);
    }

    public void collectGroupingClasses(GroupingsSpec groupingsSpec1, HashSet hashSet) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (groupingsSpec1.hasOriginalNameCaches() || groupingsSpec1.isHierarchySealed() || groupingsSpec1.isHierarchyMarked());
            set1 = this.findAnnotationReferencingClasses(groupingsSpec1.enumerateClasses(), bl);
        }

        Enumeration enumeration = groupingsSpec1.enumerateClasses();

        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, groupingsSpec1)) {
                hashSet.add(programClass1);
            }
        }
    }

    public boolean matchesMethodName(AbstractMethodInfo abstractMethodInfo) {
        if (this.hasLinkMethodSignature()) {
            String methodMatchName = AbstractExclusionSpec.getMethodMatchName(abstractMethodInfo);
            return methodMatchName.startsWith(this.linkMethodPrefix) && methodMatchName.length() > this.linkMethodPrefix.length();
        } else {
            return this.methodSpecifier.matchesName(AbstractExclusionSpec.getMethodMatchName(abstractMethodInfo));
        }
    }

    public void excludeExceptionMethods(ExceptionObfuscationExclusions exceptionObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(exceptionObfuscationExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                    exceptionObfuscationExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public static boolean matchesAnnotations(
            Enumeration enumeration, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier, ClassHierarchyQuery classHierarchyQuery
    ) {
        if (aSTComplexAnnotationSpecifier == null) {
            return true;
        }

        HashSet hashSet = ZkmUtils.createHashSet(13);

        while (enumeration.hasMoreElements()) {
            String string = (String) enumeration.nextElement();
            if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                hashSet.add(classHierarchyQuery.getOriginalClassName(string));
            } else {
                hashSet.add(string);
            }
        }

        return aSTComplexAnnotationSpecifier.matchesAnyAnnotation(hashSet);
    }

    public void applyTrimExclusion(TrimProcessor trimProcessor1) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (trimProcessor1.hasOriginalNameCaches() || trimProcessor1.isHierarchySealed() || trimProcessor1.isHierarchyMarked());
                set1 = this.findAnnotationReferencingClasses(trimProcessor1.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    if (this.classNamePattern != null
                            && this.classNamePattern.isLiteralName()
                            && (this.packagePattern == null || this.packagePattern.isLiteralName())) {
                        ProgramClass programClass2 = this.findLiteralClass();
                        if (programClass2 != null) {
                            this.excludeTrimClass(trimProcessor1, programClass2, set1);
                        }
                    } else if (this.isClassPlusSpec()) {
                        this.excludeTrimClasses(trimProcessor1, trimProcessor1.getCandidateClasses(), set1);
                    } else if (this.hasLinkClassName()) {
                        this.excludeTrimLinkedClasses(trimProcessor1, trimProcessor1.getCandidateClasses(), set1);
                    } else {
                        this.excludeTrimClasses(trimProcessor1, trimProcessor1.getIncludedClasses(), set1);
                    }

                    if (trimProcessor1.isDeleteAnnotationAttributes() && this.standaloneAnnotation) {
                        this.excludeTrimAnnotationTypes(trimProcessor1, trimProcessor1.enumerateAnnotationTypes(), set1);
                    }
                    break;
                case 3:
                    if (this.fieldSpecifier.isLiteralName() && trimProcessor1.canUseFieldNameIndex()) {
                        Enumeration enumeration1 = trimProcessor1.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                        if (enumeration1 != null) {
                            while (enumeration1.hasMoreElements()) {
                                ProgramClass programClass3 = (ProgramClass) enumeration1.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass3);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass3);
                                if (this.matchesClass(programClass3, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                                    ArrayEnumeration arrayEnumeration1 = programClass3.enumerateFields();

                                    while (arrayEnumeration1.hasMoreElements()) {
                                        FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                        if (this.matchesField(fieldInfo, trimProcessor1)) {
                                            trimProcessor1.matchField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                            this.warnPackageExclusionIgnored();
                                            this.excludeClassIfCaret(trimProcessor1, programClass3);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.excludeTrimFields(trimProcessor1, trimProcessor1.getIncludedFields(), set1);
                    }
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && trimProcessor1.canUseMethodNameIndex()) {
                        Enumeration enumeration = trimProcessor1.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, trimProcessor1)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(trimProcessor1, methodInfo1)) {
                                            trimProcessor1.matchMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                            this.warnPackageExclusionIgnored();
                                            this.excludeClassIfCaret(trimProcessor1, programClass1);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.excludeTrimMethods(trimProcessor1, trimProcessor1.getIncludedMethods(), set1);
                    }
            }
        }
    }

    public void addModifier(String accessModifierArgument, Map map1) {
        if (accessModifierArgument.equals("public") || accessModifierArgument.equals("protected") || accessModifierArgument.equals("private") || accessModifierArgument.equals("package")) {
            if (this.accessModifier == null) {
                this.accessModifier = accessModifierArgument;
            } else if (!this.accessModifier.equals(accessModifierArgument)) {
                this.scriptEnvironment
                        .logWarning(
                                "\""
                                        + accessModifierArgument
                                        + "\" appears after \""
                                        + this.accessModifier
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + ". \""
                                        + accessModifierArgument
                                        + "\" ignored.",
                                true
                        );
                return;
            }
        }

        if (map1.put(accessModifierArgument, accessModifierArgument) != null) {
            this.scriptEnvironment
                    .logWarning(
                            "\"" + accessModifierArgument + "\" appears more than once in \"" + this.getStatementName() + "\" statement at line " + this.getLineNumber() + ".", true
                    );
        }
    }

    public void unexcludeFlowMethods(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(flowObfuscationExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                    flowObfuscationExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void applyTrimUnexclusion(TrimProcessor trimProcessor1) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (trimProcessor1.hasOriginalNameCaches() || trimProcessor1.isHierarchySealed() || trimProcessor1.isHierarchyMarked());
                set1 = this.findAnnotationReferencingClasses(trimProcessor1.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    if (this.classNamePattern != null
                            && this.classNamePattern.isLiteralName()
                            && (this.packagePattern == null || this.packagePattern.isLiteralName())) {
                        ProgramClass programClass2 = this.findLiteralClass();
                        if (programClass2 != null) {
                            this.unexcludeTrimClass(trimProcessor1, programClass2, set1);
                        }
                    } else if (this.isClassPlusSpec()) {
                        this.unexcludeTrimClasses(trimProcessor1, trimProcessor1.getCandidateClasses(), set1);
                    } else {
                        this.unexcludeTrimClasses(trimProcessor1, trimProcessor1.getExcludedClasses(), set1);
                    }

                    if (trimProcessor1.isDeleteAnnotationAttributes() && this.standaloneAnnotation) {
                        this.unexcludeTrimAnnotationTypes(trimProcessor1, trimProcessor1.enumerateAnnotationTypes(), set1);
                    }
                    break;
                case 3:
                    if (this.fieldSpecifier.isLiteralName() && trimProcessor1.canUseFieldNameIndex()) {
                        Enumeration enumeration1 = trimProcessor1.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                        if (enumeration1 != null) {
                            while (enumeration1.hasMoreElements()) {
                                ProgramClass programClass3 = (ProgramClass) enumeration1.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass3);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass3);
                                if (this.matchesClass(programClass3, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                                    ArrayEnumeration arrayEnumeration1 = programClass3.enumerateFields();

                                    while (arrayEnumeration1.hasMoreElements()) {
                                        FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                        if (this.matchesField(fieldInfo, trimProcessor1)) {
                                            trimProcessor1.unmatchField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                            this.warnTrimPackageExclusion();
                                            this.unexcludeClassIfCaret(trimProcessor1, programClass3);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.unexcludeTrimFields(trimProcessor1, trimProcessor1.getExcludedFields(), set1);
                    }
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && trimProcessor1.canUseMethodNameIndex()) {
                        Enumeration enumeration = trimProcessor1.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, trimProcessor1)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(trimProcessor1, methodInfo1)) {
                                            trimProcessor1.unmatchMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                            this.warnTrimPackageExclusion();
                                            this.unexcludeClassIfCaret(trimProcessor1, programClass1);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.unexcludeTrimMethods(trimProcessor1, trimProcessor1.getExcludedMethods(), set1);
                    }
            }
        }
    }

    public boolean matchesMethod(ClassHierarchyQuery classHierarchyQuery, AbstractMethodInfo abstractMethodInfo) throws ZkmException, IOException {
        if (!matchesAccessFlags(abstractMethodInfo.getAccessFlags(), this.memberAccessFlags)) {
            return false;
        }

        if (!this.matchesMethodName(abstractMethodInfo)) {
            return false;
        }

        if (!matchesArgs(AbstractExclusionSpec.getMemberMatchDescriptor(abstractMethodInfo), this.argsPattern)) {
            return false;
        }

        if (!matchesMethodAnnotations(abstractMethodInfo, this.memberAnnotation, classHierarchyQuery)) {
            return false;
        }

        if (!matchesParameterAnnotations(abstractMethodInfo, this.parameterAnnotations, this.argsPattern, classHierarchyQuery)) {
            return false;
        }

        ObservableHolder observableHolder = new ObservableHolder();
        boolean bl = matchesThrowsClause(classHierarchyQuery, abstractMethodInfo, this.throwsTypes, observableHolder);
        if (!observableHolder.isValueNull()) {
            String value = (String) observableHolder.getValue();
            value = ZkmStringUtils.replaceAll(value, "<0>", this.parameterText);
            value = ZkmStringUtils.replaceAll(value, "<1>", this.getStatementName());
            value = ZkmStringUtils.replaceAll(value, "<2>", String.valueOf(this.getLineNumber()));
            this.scriptEnvironment.logWarning(value);
        }

        return bl;
    }

    public void unexcludeTrimMethods(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(trimProcessor1, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                    trimProcessor1.unmatchMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                    this.warnPackageExclusionIgnored();
                    this.unexcludeClassIfCaret(trimProcessor1, programClass1);
                }
            }
        }
    }

    public void applyFlowUnexclusion(FlowObfuscationExclusions flowObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        flowObfuscationExclusions.hasOriginalNameCaches()
                                || flowObfuscationExclusions.isHierarchySealed()
                                || flowObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(flowObfuscationExclusions.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    this.unexcludeFlowClasses(flowObfuscationExclusions, flowObfuscationExclusions.getCandidateClasses(), set1);
                    break;
                case 3:
                    this.unexcludeFlowFieldHosts(flowObfuscationExclusions, flowObfuscationExclusions.getCandidateClasses(), set1);
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && flowObfuscationExclusions.canUseMethodNameIndex()) {
                        Enumeration enumeration = flowObfuscationExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(flowObfuscationExclusions, methodInfo1)) {
                                            flowObfuscationExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        this.unexcludeFlowMethods(flowObfuscationExclusions, flowObfuscationExclusions.getCandidateMethods(), set1);
                    }
            }
        }
    }

    public void applyReferenceExclusion(ReferenceObfuscationExclusions referenceObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        referenceObfuscationExclusions.hasOriginalNameCaches()
                                || referenceObfuscationExclusions.isHierarchySealed()
                                || referenceObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(referenceObfuscationExclusions.getMemberOwnerClasses(), bl);
            }

            switch (this.specifierKind) {
                case 3:
                    this.excludeReferenceFields(referenceObfuscationExclusions, referenceObfuscationExclusions.getMatchedFields(), set1);
                    break;
                case 4:
                    this.excludeReferenceMethods(referenceObfuscationExclusions, referenceObfuscationExclusions.getMatchedMethods(), set1);
            }
        }
    }

    public void applyStringEncryptionUnexclusion(StringEncryptionExclusionSpec stringEncryptionExclusionSpec) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    stringEncryptionExclusionSpec.hasOriginalNameCaches()
                            || stringEncryptionExclusionSpec.isHierarchySealed()
                            || stringEncryptionExclusionSpec.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(stringEncryptionExclusionSpec.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.unexcludeStringEncryptionClasses(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getCandidateClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && stringEncryptionExclusionSpec.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = stringEncryptionExclusionSpec.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, stringEncryptionExclusionSpec)) {
                                        stringEncryptionExclusionSpec.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeStringEncryptionFields(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getCandidateFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && stringEncryptionExclusionSpec.canUseMethodNameIndex()) {
                    Enumeration enumeration = stringEncryptionExclusionSpec.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, stringEncryptionExclusionSpec)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(stringEncryptionExclusionSpec, methodInfo1)) {
                                        stringEncryptionExclusionSpec.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeStringEncryptionMethods(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getCandidateMethods(), set1);
                }
        }
    }

    public void excludeReflectionFields(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, reflectionAccessMatcher)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                    reflectionAccessMatcher.markFieldAccessed(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                    this.excludeClassIfCaret(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    public boolean matchesClassName(String string) {
        return !this.hasLinkClassName()
                ? this.classNamePattern.matchesName(string)
                : string.endsWith(this.linkClassSuffix)
                && (this.linkClassPrefix.length() == 0 || string.startsWith(this.linkClassPrefix))
                && string.length() > this.linkClassPrefix.length() + this.linkClassSuffix.length();
    }

    public boolean isTransientRequired() {
        return this.memberAccessFlags.isTransientRequired();
    }

    public void excludeReferenceFields(ReferenceObfuscationExclusions referenceObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) enumeration.nextElement();
            if (this.matchesField(abstractFieldInfo, referenceObfuscationExclusions)) {
                ClassFileBase classFileBase = abstractFieldInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, referenceObfuscationExclusions)) {
                    if (this.containedInClause == null) {
                        String string2 = "exclusion parameter '" + this.parameterText + "'";
                        referenceObfuscationExclusions.excludeFieldReference(abstractFieldInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = referenceObfuscationExclusions.getReferencingMethods(abstractFieldInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(referenceObfuscationExclusions, methodBytecode1)) {
                                referenceObfuscationExclusions.excludeFieldReference(
                                        abstractFieldInfo, methodBytecode1, "exclusion parameter '" + this.parameterText + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public void excludeReflectionClasses(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                reflectionAccessMatcher.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
                if (this.classNamePattern != null && this.classNamePattern.hasPlusTag()) {
                    this.excludeReflectionMembers(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    public void includeParameterMethods(MethodParameterExclusions methodParameterExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (MethodParameterExclusions.isEligible(methodInfo1) && this.matchesMethod(methodParameterExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, methodParameterExclusions)) {
                    methodParameterExclusions.includeMatchedMethod(methodInfo1, "inclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public boolean containedInHasPlusSignature() {
        return this.containedInClause != null
                && this.containedInClause.getFilterParameter() != null
                && this.containedInClause.getFilterParameter().plusSignatureClasses;
    }

    public void unexcludeTrimClasses(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                trimProcessor1.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
                if (this.classNamePattern.hasPlusTag()) {
                    this.unexcludeTrimMembers(trimProcessor1, programClass1);
                }
            }
        }
    }

    public static String formatMethodSpec(
            ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier,
            AccessFlagsSpec accessFlagsSpec1,
            DescribableSpec describableSpec,
            MethodArgsPattern methodArgsPattern1,
            ASTComplexAnnotationSpecifier[] aSTComplexAnnotationSpecifiers,
            List list1
    ) {
        Boolean boolean1 = false;
        List list2 = list1;
        ASTComplexAnnotationSpecifier[] aSTComplexAnnotationSpecifiers1 = aSTComplexAnnotationSpecifiers;
        MethodArgsPattern methodArgsPattern2 = methodArgsPattern1;
        DescribableSpec describableSpec1 = describableSpec;
        Object object1 = null;
        Object object = null;
        return formatMemberSpec(
                aSTComplexAnnotationSpecifier,
                accessFlagsSpec1,
                (String) null,
                (DescribableSpec) object,
                (String) object1,
                describableSpec1,
                methodArgsPattern2,
                aSTComplexAnnotationSpecifiers1,
                list2,
                boolean1
        );
    }

    public static String formatMemberSpec(
            ASTComplexAnnotationSpecifier annotation,
            AccessFlagsSpec accessFlags,
            String fieldType,
            DescribableSpec fieldName,
            String methodName,
            DescribableSpec methodSpecifier,
            MethodArgsPattern arguments,
            ASTComplexAnnotationSpecifier[] parameterAnnotations,
            List throwsTypes,
            boolean plusSignatureClasses
    ) {
        int flowPredicate = ZkmScriptSimpleNode.getFlowPredicate();
        StringBuilder result = new StringBuilder();
        if (annotation != null) {
            result.append(annotation.getSpecText());
        }
        if (accessFlags != null) {
            boolean appendPackage;
            int prefixLength = result.length();
            if (flowPredicate == 0) {
                if (prefixLength > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                appendPackage = accessFlags.isPackageRequired();
            } else {
                appendPackage = prefixLength != 0;
            }
            if (appendPackage) {
                result.append("package ");
            }
            if (accessFlags.isPackageForbidden()) {
                result.append("!package ");
            }
            result.append(accessFlags.toSpecString());
        }
        if (fieldType != null && flowPredicate == 0) {
            if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                result.append(' ');
            }
            result.append(toJavaTypeName(fieldType));
        }
        if (fieldName != null && flowPredicate == 0) {
            if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                result.append(' ');
            }
            result.append(fieldName.getSpecText());
        }
        if (methodName != null || methodSpecifier != null) {
            if (flowPredicate == 0) {
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
            } else {
                result.append((char) result.length());
            }
            if (methodName != null) {
                result.append(methodName);
            }
            if (methodName == null || flowPredicate != 0) {
                result.append(methodSpecifier.getSpecText());
            }
            result.append('(');
            if (flowPredicate == 0) {
                if (arguments != null) {
                    result.append(formatArgsWithAnnotations(arguments, parameterAnnotations));
                }
                result.append(')');
            }
            int throwsCount = throwsTypes.size();
            if (flowPredicate == 0) {
                if (throwsCount > 0) {
                    result.append(" throws ");
                    for (int typeIndex = 0; typeIndex < throwsTypes.size(); typeIndex++) {
                        result.append(toDottedName((String) throwsTypes.get(typeIndex)));
                        if (typeIndex < throwsTypes.size() - 1) {
                            result.append(", ");
                        }
                    }
                }
                if (plusSignatureClasses) {
                    result.append(" +signatureClasses");
                }
            } else if (throwsCount != 0) {
                result.append(" +signatureClasses");
            }
        }
        return result.toString();
    }

    public void unexcludeStringEncryptionMethods(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(stringEncryptionExclusionSpec, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                    stringEncryptionExclusionSpec.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public boolean matchesFieldType(AbstractFieldInfo abstractFieldInfo) {
        if (this.fieldType == null) {
            return true;
        }

        String memberMatchDescriptor = AbstractExclusionSpec.getMemberMatchDescriptor(abstractFieldInfo);
        return this.fieldType.equals(memberMatchDescriptor);
    }

    public static boolean isAnyWildcard(String string) {
        return string.equals("*");
    }

    public void setLinkClassName(String linkClassNameArgument) {
        this.linkClassName = linkClassNameArgument;
        int endIndex = linkClassNameArgument.indexOf("<link>");
        this.linkClassSuffix = linkClassNameArgument.substring(endIndex + "<link>".length());
        this.linkClassPrefix = linkClassNameArgument.substring(0, endIndex);
        this.accessModifier = null;
    }

    public int computeSpecificityScore() {
        int ba = 1000;
        switch (this.specifierKind) {
            case 0:
                ba = (int) (ba * this.moduleSpecifier.estimateMatchFraction());
                break;
            case 1:
                ba = (int) (1000 * this.computePackageSpecificity());
                break;
            case 2:
                ba = (int) (ba * this.computePackageSpecificity());
                ba = (int) (ba * this.computeClassSpecificity());
                break;
            case 3:
                ba = (int) (ba * this.computePackageSpecificity());
                ba = (int) (ba * this.computeClassSpecificity());
                ba = (int) (ba * this.computeFieldSpecificity());
                break;
            case 4:
                ba = (int) (ba * this.computePackageSpecificity());
                ba = (int) (ba * this.computeClassSpecificity());
                ba = (int) (ba * this.computeMethodSpecificity());
        }

        return ba + this.countCaretTags() * 1001;
    }

    @Override
    public final int hashCode() {
        return this.parameterText.hashCode();
    }

    public void excludeFixedClasses(FixedClassesExclusionSet fixedClassesExclusionSet1, Enumeration enumeration) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    fixedClassesExclusionSet1.hasOriginalNameCaches()
                            || fixedClassesExclusionSet1.isHierarchySealed()
                            || fixedClassesExclusionSet1.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(fixedClassesExclusionSet1.getCandidateClasses(), bl);
        }

        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, fixedClassesExclusionSet1)) {
                fixedClassesExclusionSet1.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void includeReferenceMethods(ReferenceObfuscationExclusions referenceObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) enumeration.nextElement();
            if (this.matchesMethod(referenceObfuscationExclusions, abstractMethodInfo)) {
                ClassFileBase classFileBase = abstractMethodInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, referenceObfuscationExclusions)) {
                    if (this.containedInClause == null) {
                        String string2 = "inclusion parameter '" + this.parameterText + "'";
                        referenceObfuscationExclusions.includeMethodReference(abstractMethodInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = referenceObfuscationExclusions.getReferencingMethods(abstractMethodInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(referenceObfuscationExclusions, methodBytecode1)) {
                                referenceObfuscationExclusions.includeMethodReference(
                                        abstractMethodInfo, methodBytecode1, "inclusion parameter '" + this.parameterText + "'"
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean matchesMethodAnnotations(
            AbstractMethodInfo abstractMethodInfo, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier, ClassHierarchyQuery classHierarchyQuery
    ) {
        return matchesAnnotations(abstractMethodInfo.enumerateAnnotationTypes(), aSTComplexAnnotationSpecifier, classHierarchyQuery);
    }

    public void warnPackageExclusionIgnored() {
        if (this.packagePattern != null && this.packagePattern.hasCaretTag()) {
            this.scriptEnvironment
                    .logWarning(
                            "\""
                                    + this.getStatementName()
                                    + "\" parameters cannot specify a package exclusion. Package exclusion in \""
                                    + this.parameterText
                                    + "\" in \""
                                    + this.getStatementName()
                                    + "\" statement at line "
                                    + this.getLineNumber()
                                    + " will be ignored.",
                            true
                    );
        }
    }

    public static String toJavaTypeName(String descriptor) {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        if (flowControlKey == 0 || descriptor.equals("*") || descriptor.equals("?")) {
            return descriptor;
        }
        return ConstantPoolEntry.descriptorToJavaType(descriptor).replace('/', '.');
    }

    public boolean isStandaloneAnnotation() {
        return this.standaloneAnnotation;
    }

    public void excludeFlowFieldHosts(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                flowObfuscationExclusions.excludeFromPredicateFields(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public boolean isMemberFinalRequired() {
        return this.memberAccessFlags.isFinalRequired();
    }

    public void applyMethodParameterInclusion(MethodParameterExclusions methodParameterExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        methodParameterExclusions.hasOriginalNameCaches()
                                || methodParameterExclusions.isHierarchySealed()
                                || methodParameterExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(methodParameterExclusions.getCandidateClasses(), bl);
            }

            if (this.methodSpecifier.isLiteralName() && methodParameterExclusions.canUseMethodNameIndex()) {
                Enumeration enumeration = methodParameterExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, methodParameterExclusions)) {
                            ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                            while (arrayEnumeration.hasMoreElements()) {
                                MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                if (MethodParameterExclusions.isEligible(methodInfo1) && this.matchesMethod(methodParameterExclusions, methodInfo1)) {
                                    methodParameterExclusions.includeMatchedMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                    this.excludeClassIfCaret(methodParameterExclusions, programClass1);
                                }
                            }
                        }
                    }
                }
            } else {
                this.includeParameterMethods(methodParameterExclusions, methodParameterExclusions.getIncludedMethods(), set1);
            }
        }
    }

    public void applyFixedClassesExclusion(FixedClassesExclusionSet fixedClassesExclusionSet1) throws ZkmException, IOException {
        this.excludeFixedClasses(fixedClassesExclusionSet1, fixedClassesExclusionSet1.getIncludedClasses());
    }

    public void unexcludeLongEncryptionFields(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, longEncryptionExclusionHandler)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                    longEncryptionExclusionHandler.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void excludeStringEncryptionClasses(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                stringEncryptionExclusionSpec.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void applyIntegerEncryptionUnexclusion(IntegerEncryptionExclusions integerEncryptionExclusions) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    integerEncryptionExclusions.hasOriginalNameCaches()
                            || integerEncryptionExclusions.isHierarchySealed()
                            || integerEncryptionExclusions.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(integerEncryptionExclusions.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.unexcludeIntegerEncryptionClasses(integerEncryptionExclusions, integerEncryptionExclusions.getCandidateClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && integerEncryptionExclusions.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = integerEncryptionExclusions.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, integerEncryptionExclusions)) {
                                        integerEncryptionExclusions.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeIntegerEncryptionFields(integerEncryptionExclusions, integerEncryptionExclusions.getCandidateFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && integerEncryptionExclusions.canUseMethodNameIndex()) {
                    Enumeration enumeration = integerEncryptionExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, integerEncryptionExclusions)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(integerEncryptionExclusions, methodInfo1)) {
                                        integerEncryptionExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.unexcludeIntegerEncryptionMethods(integerEncryptionExclusions, integerEncryptionExclusions.getCandidateMethods(), set1);
                }
        }
    }

    public void validateModifiers() {
        if (this.classAccessFlags != null) {
            if (this.isInterfaceRequired() && this.isClassFinalRequired()) {
                this.setEnabled();
                this.scriptEnvironment
                        .logWarning(
                                "Class exclude parameter \""
                                        + this.parameterText
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + "\" has the invalid combination of modifiers : interface and final. The parameter will be ignored.",
                                true
                        );
            }

            if (this.isAbstractRequired() && this.isClassFinalRequired()) {
                this.setEnabled();
                this.scriptEnvironment
                        .logWarning(
                                "Class exclude parameter \""
                                        + this.parameterText
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + "\" has the invalid combination of modifiers : abstract and final. The parameter will be ignored.",
                                true
                        );
            }

            if (this.classAccessFlags.isContradictory()) {
                this.setEnabled();
                this.scriptEnvironment
                        .logWarning(
                                "Class exclude parameter \""
                                        + this.parameterText
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + "\" has a contradictory combination of modifiers. The parameter will be ignored.",
                                true
                        );
            }
        }

        if (this.memberAccessFlags != null) {
            Vector vector = new Vector();
            switch (this.specifierKind) {
                case 3:
                    if (this.isSynchronizedRequired()) {
                        vector.addElement("synchronized");
                    }

                    if (this.isNativeRequired()) {
                        vector.addElement("native");
                    }

                    if (this.isMemberAbstractRequired()) {
                        vector.addElement("abstract");
                    }

                    if (vector.size() > 0) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Field exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has modifiers that are invalid for fields : "
                                                + this.joinWithAnd(vector)
                                                + ". They will be ignored.",
                                        true
                                );
                    }

                    if (this.isVolatileRequired() && this.isMemberFinalRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Field exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : final and volatile. The parameter will be ignored.",
                                        true
                                );
                    }
                    break;
                case 4:
                    if (this.isTransientRequired()) {
                        vector.addElement("transient");
                    }

                    if (this.isTransientRequired()) {
                        vector.addElement("enum");
                    }

                    if (vector.size() > 0) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has modifiers that are invalid for methods : "
                                                + this.joinWithAnd(vector)
                                                + ". They will be ignored.",
                                        true
                                );
                    }

                    if (this.isMemberAbstractRequired() && this.isPrivateRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : abstract and private. The parameter will be ignored.",
                                        true
                                );
                    }

                    if (this.isMemberAbstractRequired() && this.isStaticRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : abstract and static. The parameter will be ignored.",
                                        true
                                );
                    }

                    if (this.isMemberAbstractRequired() && this.isMemberFinalRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : abstract and final. The parameter will be ignored.",
                                        true
                                );
                    }

                    if (this.isMemberAbstractRequired() && this.isNativeRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : abstract and native. The parameter will be ignored.",
                                        true
                                );
                    }

                    if (this.isMemberAbstractRequired() && this.isSynchronizedRequired()) {
                        this.setEnabled();
                        this.scriptEnvironment
                                .logWarning(
                                        "Method exclude parameter \""
                                                + this.parameterText
                                                + "\" in \""
                                                + this.getStatementName()
                                                + "\" statement at line "
                                                + this.getLineNumber()
                                                + "\" has the invalid combination of modifiers : abstract and synchronized. The parameter will be ignored.",
                                        true
                                );
                    }
            }

            if (this.memberAccessFlags.isContradictory()) {
                this.setEnabled();
                this.scriptEnvironment
                        .logWarning(
                                "Member exclude parameter \""
                                        + this.parameterText
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + "\" has a contradictory combination of modifiers. The parameter will be ignored.",
                                true
                        );
            }
        }
    }

    public void excludeReflectionMethods(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(reflectionAccessMatcher, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                    reflectionAccessMatcher.markMethodAccessed(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                    this.excludeClassIfCaret(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    public void unexcludePackageIfCaret(NameExclusionSet nameExclusionSet, String string) throws IOException {
        if (this.packagePattern != null && this.packagePattern.hasCaretTag()) {
            nameExclusionSet.unexcludePackage(string, "unexclusion parameter '" + this.parameterText + "'");
        }
    }

    @Override
    public void setFieldSpecifier(ASTComplexFieldSpecifier aSTComplexFieldSpecifier) {
        this.fieldSpecifier = aSTComplexFieldSpecifier;
    }

    public boolean hasPlusSignatureClasses() {
        return this.plusSignatureClasses;
    }

    public void checkClassNamePattern() {
        if (this.classNamePattern != null) {
        }
    }

    public boolean isClassFinalRequired() {
        return this.classAccessFlags.isFinalRequired();
    }

    public static String formatFieldSpec(
            ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier, AccessFlagsSpec accessFlagsSpec1, String fieldTypeArgument, DescribableSpec describableSpec
    ) {
        Boolean boolean1 = false;
        Object object3 = null;
        Object object2 = null;
        Object object1 = null;
        Object object = null;
        return formatMemberSpec(
                aSTComplexAnnotationSpecifier,
                accessFlagsSpec1,
                fieldTypeArgument,
                describableSpec,
                (String) null,
                (DescribableSpec) object,
                (MethodArgsPattern) object1,
                (ASTComplexAnnotationSpecifier[]) object2,
                (List) object3,
                boolean1
        );
    }

    @Override
    public int compareTo(Object other) {
        return this.compareSpecificity((ASTRenameFilterParameter) other);
    }

    public void excludeIntegerEncryptionClasses(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                integerEncryptionExclusions.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void excludeNameMembers(NameExclusionSet nameExclusionSet, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            nameExclusionSet.excludeField(fieldInfos[fieldInfoIndex], "exclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            if (!methodInfo1.isConstructor() && !methodInfo1.isStaticInitializer()) {
                nameExclusionSet.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void setMemberAnnotation(ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) {
        this.memberAnnotation = aSTComplexAnnotationSpecifier;
    }

    public void excludeLongEncryptionClasses(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                longEncryptionExclusionHandler.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void unexcludeIntegerEncryptionClasses(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                integerEncryptionExclusions.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void unexcludeStringEncryptionFields(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, stringEncryptionExclusionSpec)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                    stringEncryptionExclusionSpec.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void checkPlusTagUsage() {
        if (this.classNamePattern != null && this.classNamePattern.hasPlusTag()) {
            if (this.specifierKind == 3) {
                this.warnPlusTagIgnored("field");
            } else if (this.specifierKind == 4) {
                this.warnPlusTagIgnored("method");
            }
        }
    }

    public void applyRemoveMethodCallsExclusion(RemoveMethodCallsHandler removeMethodCallsHandler) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        removeMethodCallsHandler.hasOriginalNameCaches()
                                || removeMethodCallsHandler.isHierarchySealed()
                                || removeMethodCallsHandler.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(removeMethodCallsHandler.getMemberOwnerClasses(), bl);
            }

            switch (this.specifierKind) {
                case 4:
                    this.excludeRemoveMethodCalls(removeMethodCallsHandler, removeMethodCallsHandler.getMatchedMethods(), set1);
            }
        }
    }

    public final boolean matchesImplementsClause(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        for (int elementIndex = 0; elementIndex < this.implementsNames.size(); elementIndex++) {
            String string = (String) this.implementsNames.get(elementIndex);
            ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier = (ASTComplexAnnotationSpecifier) this.implementsAnnotations.get(elementIndex);

            try {
                if (classFileBase.isGenerated()) {
                    return false;
                }

                boolean bl;
                if (aSTComplexAnnotationSpecifier != null) {
                    if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                        bl = classHierarchyQuery.implementsAnnotatedInterfaceByOriginalName(
                                classFileBase.getOriginalClassName(), string, aSTComplexAnnotationSpecifier
                        );
                    } else {
                        bl = classHierarchyQuery.implementsAnnotatedInterface(classFileBase.getClassName(), string, aSTComplexAnnotationSpecifier);
                    }
                } else if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                    bl = classHierarchyQuery.implementsInterfaceByOriginalName(classFileBase.getOriginalClassName(), string);
                } else {
                    bl = classHierarchyQuery.implementsInterface(classFileBase.getClassName(), string);
                }

                if (!bl) {
                    return false;
                }
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : class '"
                                        + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName())
                                        + "' not found. (D)"
                        );
            } catch (ClassFileLoadException classFileLoadException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : "
                                        + classFileLoadException.getMessage()
                                        + " (D)"
                        );
            }
        }

        return true;
    }

    public void excludeRemoveMethodCalls(RemoveMethodCallsHandler removeMethodCallsHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) enumeration.nextElement();
            if (this.matchesMethod(removeMethodCallsHandler, abstractMethodInfo)) {
                ClassFileBase classFileBase = abstractMethodInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, removeMethodCallsHandler)) {
                    if (this.containedInClause == null) {
                        String string2 = "exclusion parameter '" + this.parameterText + "'";
                        removeMethodCallsHandler.excludeMethodCall(abstractMethodInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = removeMethodCallsHandler.getCallers(abstractMethodInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(removeMethodCallsHandler, methodBytecode1)) {
                                removeMethodCallsHandler.excludeMethodCall(abstractMethodInfo, methodBytecode1, "exclusion parameter '" + this.parameterText + "'");
                            }
                        }
                    }
                }
            }
        }
    }

    public void unexcludeIntegerEncryptionMethods(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(integerEncryptionExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                    integerEncryptionExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public static boolean matchesAccessFlags(int flags, AccessFlagsSpec accessFlagsSpec1) {
        return ((accessFlagsSpec1 == null) || (accessFlagsSpec1.matches(flags)));
    }

    public void excludeSignatureClasses(NameExclusionSet nameExclusionSet, MethodInfo methodInfo1) throws ZkmException, IOException {
        ArrayList arrayList = methodInfo1.getDescriptorProgramClasses();

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            ProgramClass programClass1 = (ProgramClass) arrayList.get(elementIndex);
            String packagePath = programClass1.getPackagePath();
            nameExclusionSet.excludePackage(packagePath, "exclusion parameter '" + this.parameterText + "'");
            nameExclusionSet.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
        }
    }

    public void excludeStringEncryptionMethods(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(stringEncryptionExclusionSpec, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                    stringEncryptionExclusionSpec.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void unexcludeNameClass(NameExclusionSet nameExclusionSet, ProgramClass programClass1, Set set1) throws ZkmException, IOException {
        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
            nameExclusionSet.unexcludeClassInternal(programClass1, "unexclusion parameter '" + this.parameterText + "'", false);
            this.unexcludePackageIfCaret(nameExclusionSet, classMatchName);
            if (this.classNamePattern != null && this.classNamePattern.hasPlusTag()) {
                this.unexcludeNameMembers(nameExclusionSet, programClass1);
            }
        }
    }

    public void includeRemoveMethodCalls(RemoveMethodCallsHandler removeMethodCallsHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) enumeration.nextElement();
            if (this.matchesMethod(removeMethodCallsHandler, abstractMethodInfo)) {
                ClassFileBase classFileBase = abstractMethodInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (this.matchesClass(classFileBase, set1, classMatchName, classSimpleMatchName, removeMethodCallsHandler)) {
                    if (this.containedInClause == null) {
                        String string2 = "inclusion parameter '" + this.parameterText + "'";
                        removeMethodCallsHandler.includeMethodCall(abstractMethodInfo, (MethodBytecode) null, string2);
                    } else {
                        Iterator iterator = removeMethodCallsHandler.getCallers(abstractMethodInfo).iterator();

                        while (iterator.hasNext()) {
                            MethodBytecode methodBytecode1 = (MethodBytecode) iterator.next();
                            if (this.matchesContainedIn(removeMethodCallsHandler, methodBytecode1)) {
                                removeMethodCallsHandler.includeMethodCall(abstractMethodInfo, methodBytecode1, "inclusion parameter '" + this.parameterText + "'");
                            }
                        }
                    }
                }
            }
        }
    }

    public void setMethodSpecifier(ASTComplexMethodSpecifier aSTComplexMethodSpecifier) {
        this.methodSpecifier = aSTComplexMethodSpecifier;
    }

    public int compareSpecificity(ASTRenameFilterParameter aSTRenameFilterParameter1) {
        if (this.specificityScore < aSTRenameFilterParameter1.specificityScore) {
            return -1;
        } else {
            return this.specificityScore == aSTRenameFilterParameter1.specificityScore ? 0 : 1;
        }
    }

    public void buildClassAccessFlags() {
        Iterator iterator = this.classModifiers.keySet().iterator();
        if (iterator.hasNext()) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        while (iterator.hasNext()) {
            boolean bl = false;
            String string = (String) iterator.next();
            if (string.startsWith("!")) {
                string = string.substring(1);
                bl = true;
            }

            if (bl) {
                if (string.equals("public")) {
                    this.classAccessFlags.forbidPublic();
                } else if (string.equals("package")) {
                    this.classAccessFlags.forbidPackage();
                } else if (string.equals("final")) {
                    this.classAccessFlags.forbidFinal();
                } else if (string.equals("interface")) {
                    this.classAccessFlags.forbidInterface();
                } else if (string.equals("abstract")) {
                    this.classAccessFlags.forbidAbstract();
                } else if (string.equals("synthetic")) {
                    this.classAccessFlags.forbidSynthetic();
                } else if (string.equals("enum")) {
                    this.classAccessFlags.forbidEnum();
                } else if (string.equals("annotation")) {
                    this.classAccessFlags.forbidAnnotation();
                }
            } else if (string.equals("public")) {
                this.classAccessFlags.requirePublic();
            } else if (string.equals("package")) {
                this.classAccessFlags.requirePackage();
            } else if (string.equals("final")) {
                this.classAccessFlags.requireFinal();
            } else if (string.equals("interface")) {
                this.classAccessFlags.requireInterface();
            } else if (string.equals("abstract")) {
                this.classAccessFlags.requireAbstract();
            } else if (string.equals("synthetic")) {
                this.classAccessFlags.requireSynthetic();
            } else if (string.equals("enum")) {
                this.classAccessFlags.requireEnum();
            } else if (string.equals("annotation")) {
                this.classAccessFlags.requireAnnotation();
            }
        }

        this.memberAccessFlags = buildMemberAccessFlags(this.memberModifiers, this.specifierKind);
    }

    public void excludeIntegerEncryptionMethods(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(integerEncryptionExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                    integerEncryptionExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void applyLongEncryptionExclusion(LongEncryptionExclusionHandler longEncryptionExclusionHandler) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    longEncryptionExclusionHandler.hasOriginalNameCaches()
                            || longEncryptionExclusionHandler.isHierarchySealed()
                            || longEncryptionExclusionHandler.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(longEncryptionExclusionHandler.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.excludeLongEncryptionClasses(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getIncludedClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && longEncryptionExclusionHandler.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = longEncryptionExclusionHandler.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, longEncryptionExclusionHandler)) {
                                        longEncryptionExclusionHandler.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeLongEncryptionFields(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getIncludedFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && longEncryptionExclusionHandler.canUseMethodNameIndex()) {
                    Enumeration enumeration = longEncryptionExclusionHandler.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, longEncryptionExclusionHandler)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(longEncryptionExclusionHandler, methodInfo1)) {
                                        longEncryptionExclusionHandler.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeLongEncryptionMethods(longEncryptionExclusionHandler, longEncryptionExclusionHandler.getIncludedMethods(), set1);
                }
        }
    }

    public void warnTrimPackageExclusion() {
        this.warnPackageExclusionIgnored();
    }

    public void applyNameExclusion(NameExclusionSet nameExclusionSet) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            int specifierKindValue;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (nameExclusionSet.hasOriginalNameCaches() || nameExclusionSet.isHierarchySealed() || nameExclusionSet.isHierarchyMarked());
                set1 = this.findAnnotationReferencingClasses(nameExclusionSet.getInputClassFiles(), bl);
                specifierKindValue = this.specifierKind;
            } else {
                specifierKindValue = this.specifierKind;
            }

            switch (specifierKindValue) {
                case 0:
                    Enumeration enumeration3 = nameExclusionSet.getIncludedModules();

                    while (enumeration3.hasMoreElements()) {
                        ModuleInfoClass moduleInfoClass = (ModuleInfoClass) enumeration3.nextElement();
                        if (this.matchesModule(moduleInfoClass)) {
                            nameExclusionSet.excludeModule(moduleInfoClass, "exclusion parameter '" + this.parameterText + "'");
                        }
                    }
                    break;
                case 1:
                    Enumeration enumeration = nameExclusionSet.getIncludedPackages();

                    while (enumeration.hasMoreElements()) {
                        String string3 = (String) enumeration.nextElement();
                        if (this.matchesPackageName(string3)) {
                            nameExclusionSet.excludePackage(string3, "exclusion parameter '" + this.parameterText + "'");
                        }
                    }
                    break;
                case 2:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (!this.hasCaretTag() && !this.isClassPlusSpec() && !this.hasLinkClassName()) {
                            this.excludeNameClasses(nameExclusionSet, nameExclusionSet.getIncludedClasses(), set1);
                        } else {
                            this.excludeNameClasses(nameExclusionSet, nameExclusionSet.getCandidateClasses(), set1);
                        }
                    } else {
                        ProgramClass programClass4 = this.findLiteralClass();
                        if (programClass4 != null) {
                            this.excludeNameClass(nameExclusionSet, programClass4, set1);
                        }
                    }
                    break;
                case 3:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (this.fieldSpecifier.isLiteralName() && nameExclusionSet.canUseFieldNameIndex()) {
                            Enumeration enumeration2 = nameExclusionSet.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                            if (enumeration2 != null) {
                                while (enumeration2.hasMoreElements()) {
                                    ProgramClass programClass5 = (ProgramClass) enumeration2.nextElement();
                                    String packagePath = programClass5.getPackagePath();
                                    String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass5);
                                    String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass5);
                                    if (this.matchesClass(programClass5, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                                        ArrayEnumeration arrayEnumeration1 = programClass5.enumerateFields();

                                        while (arrayEnumeration1.hasMoreElements()) {
                                            FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                            if (this.matchesField(fieldInfo, nameExclusionSet)) {
                                                nameExclusionSet.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                                this.excludePackageIfCaret(nameExclusionSet, packagePath);
                                                this.excludeClassIfCaret(nameExclusionSet, programClass5);
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (this.hasCaretTag()) {
                            this.excludeNameFields(nameExclusionSet, nameExclusionSet.getCandidateFields(), set1);
                        } else {
                            this.excludeNameFields(nameExclusionSet, nameExclusionSet.getIncludedFields(), set1);
                        }
                    } else {
                        ProgramClass programClass3 = this.findLiteralClass();
                        if (programClass3 != null) {
                            this.excludeNameFields(nameExclusionSet, programClass3.enumerateFields(), set1);
                        }
                    }
                    break;
                case 4:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (this.hasLinkMethodSignature()) {
                            this.excludeNameMethods(nameExclusionSet, nameExclusionSet.getCandidateMethods(), set1);
                        } else if (this.methodSpecifier.isLiteralName() && nameExclusionSet.canUseMethodNameIndex()) {
                            Enumeration enumeration1 = nameExclusionSet.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                            if (enumeration1 != null) {
                                while (enumeration1.hasMoreElements()) {
                                    ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                                    String packagePathValue = programClass2.getPackagePath();
                                    String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass2);
                                    String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                                    if (this.matchesClass(programClass2, set1, classMatchNameValue, classSimpleMatchNameValue, nameExclusionSet)) {
                                        ArrayEnumeration arrayEnumeration = programClass2.enumerateMethods();

                                        while (arrayEnumeration.hasMoreElements()) {
                                            MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                            if (this.matchesMethod(nameExclusionSet, methodInfo1)) {
                                                nameExclusionSet.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                                this.excludePackageIfCaret(nameExclusionSet, packagePathValue);
                                                this.excludeClassIfCaret(nameExclusionSet, programClass2);
                                                if (this.plusSignatureClasses) {
                                                    this.excludeSignatureClasses(nameExclusionSet, methodInfo1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!this.hasCaretTag() && !this.hasPlusSignatureClasses()) {
                            this.excludeNameMethods(nameExclusionSet, nameExclusionSet.getIncludedMethods(), set1);
                        } else {
                            this.excludeNameMethods(nameExclusionSet, nameExclusionSet.getCandidateMethods(), set1);
                        }
                    } else {
                        ProgramClass programClass1 = this.findLiteralClass();
                        if (programClass1 != null) {
                            this.excludeNameMethods(nameExclusionSet, programClass1.enumerateMethods(), set1);
                        }
                    }
            }
        }
    }

    public final boolean matchesExtendsClause(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        boolean bl;
        if (this.extendsClassName != null) {
            try {
                if (classFileBase.isGenerated()) {
                    return this.extendsClassName.equals("java/lang/Object") && this.extendsAnnotation == null;
                }

                if (this.extendsAnnotation != null) {
                    if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                        bl = classHierarchyQuery.extendsAnnotatedClassByOriginalName(
                                classFileBase.getOriginalClassName(), this.extendsClassName, this.extendsAnnotation
                        );
                    } else {
                        bl = classHierarchyQuery.extendsAnnotatedClass(classFileBase.getClassName(), this.extendsClassName, this.extendsAnnotation);
                    }
                } else if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                    bl = classHierarchyQuery.isSubclassByOriginalName(classFileBase.getOriginalClassName(), this.extendsClassName);
                } else {
                    bl = classHierarchyQuery.isSubclass(classFileBase.getClassName(), this.extendsClassName);
                }
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : class '"
                                        + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName())
                                        + "' not found. (C)"
                        );
                bl = false;
            } catch (ClassFileLoadException classFileLoadException) {
                this.scriptEnvironment
                        .logWarning(
                                "Error while executing \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " while analysing '"
                                        + this.parameterText
                                        + "' : "
                                        + classFileLoadException.getMessage()
                                        + " (C)"
                        );
                bl = false;
            }
        } else {
            bl = true;
        }

        return (bl || !this.scriptEnvironment.isProGuardMappingInputEnabled() || classFileBase.isGenerated() || HiddenOptionFlags.MATCH_UNRESOLVED_SUPERTYPES)
                && (bl || !HiddenOptionFlags.SKIP_SUPERTYPE_MATCHING)
                ? bl
                : this.matchesExtendsAsInterface(classFileBase, classHierarchyQuery);
    }

    public boolean matchesMethodSpec(ClassHierarchyQuery classHierarchyQuery, ClassFileBase classFileBase, String string, String string1) throws ZkmException, IOException {
        Object object = null;
        String packagePath = classFileBase.getPackagePath();
        String simpleName = classFileBase.getSimpleName();
        if (this.matchesClass(classFileBase, (Set) object, packagePath, simpleName, classHierarchyQuery)) {
            return ((this.isClassPlusSpec()) || (this.methodSpecifier.matchesName(string) && matchesArgs(string1, this.argsPattern)));
        } else {
            return false;
        }
    }

    public boolean isSynchronizedRequired() {
        return this.memberAccessFlags.isSynchronizedRequired();
    }

    public final boolean matchesArchivePath(ClassFileBase classFileBase) {
        if (this.archivePathPattern != null) {
            Enumeration enumeration = classFileBase.enumerateInputLocations();

            while (enumeration.hasMoreElements()) {
                String archivePath = ((InputFileLocation) enumeration.nextElement()).getArchivePath();
                if (archivePath != null) {
                    if (this.archivePathPattern.length() > 0) {
                        archivePath = archivePath.replace('\\', '/');
                        if (!ZkmFileUtils.caseSensitiveFileSystem) {
                            archivePath = archivePath.toLowerCase();
                        }

                        String string1 = "*" + this.archivePathPattern;
                        if (ZkmStringUtils.matchesWildcard(archivePath, string1)) {
                            return true;
                        }
                    }
                } else if (this.archivePathPattern.length() == 0) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public Set findAnnotationReferencingClasses(Enumeration enumeration, boolean bl) {
        HashSet hashSet = ZkmUtils.createHashSet();

        while (enumeration.hasMoreElements()) {
            Iterator iterator = ((ClassFileBase) enumeration.nextElement())
                    .collectAnnotationValues(this.referencingAnnotation, this.referencingAnnotationComponent, bl)
                    .iterator();

            while (iterator.hasNext()) {
                ProgramClass programClass1 = ClassHierarchyNode.findProgramClass((String) iterator.next());
                if (programClass1 != null) {
                    hashSet.add(programClass1);
                }
            }
        }

        return hashSet;
    }

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        this.scriptEnvironment = (ScriptEnvironment) object1;
        this.statementInfo = (ScriptStatementInfo) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, this.scriptEnvironment);
        }

        this.specifierKind = this.computeSpecifierKind();
        this.buildClassAccessFlags();
        this.checkPackagePattern();
        this.checkClassNamePattern();
        this.specificityScore = this.computeSpecificityScore();
        this.parameterText = this.toScriptText();
        this.checkPlusTagUsage();
        this.validateModifiers();
    }

    public void setArchivePathPattern(String archivePathPatternArgument) {
        if (archivePathPatternArgument != null) {
            this.archivePathPattern = archivePathPatternArgument.replace('\\', '/');
            if (!ZkmFileUtils.caseSensitiveFileSystem && !this.archivePathPattern.equals(this.archivePathPattern.toLowerCase())) {
                this.scriptEnvironment
                        .logMessage(
                                "\""
                                        + this.archivePathPattern
                                        + "\" converted to \""
                                        + this.archivePathPattern.toLowerCase()
                                        + "\" in \""
                                        + this.getStatementName()
                                        + "\" statement at line "
                                        + this.getLineNumber()
                                        + " because file system \""
                                        + ZkmFileUtils.OS_NAME
                                        + "\" is case insensitive.",
                                true
                        );
                this.archivePathPattern = this.archivePathPattern.toLowerCase();
            }
        }
    }

    public void setParameterAnnotations(ASTComplexAnnotationSpecifier[] aSTComplexAnnotationSpecifiers) {
        this.parameterAnnotations = aSTComplexAnnotationSpecifiers;
    }

    public void setArgsPattern(MethodArgsPattern methodArgsPattern1) {
        this.argsPattern = methodArgsPattern1;
    }

    public final String toScriptText() {
        int flowPredicate = ZkmScriptSimpleNode.getFlowPredicate();
        StringBuilder result = new StringBuilder();
        if (flowPredicate == 0) {
            if (this.containedInClause != null) {
                result.append(this.containedInClause.toScriptText());
                result.append(' ');
            }
            if (this.classAnnotation != null) {
                result.append(this.classAnnotation.getSpecText());
                result.append(' ');
            }
            if (this.classAccessFlags != null && this.classAccessFlags.isPackageRequired()) {
                result.append("package ");
            }
            if (this.classAccessFlags != null && this.classAccessFlags.isPackageForbidden()) {
                result.append("!package ");
            }
        }

        if (flowPredicate == 0 && !this.standaloneAnnotation) {
            result.append(this.classAccessFlags == null ? "" : this.classAccessFlags.toSpecString());
            if (this.referencingAnnotation != null) {
                result.append('@');
                result.append(toDottedName(this.referencingAnnotation));
                result.append('(');
                result.append(this.referencingAnnotationComponent);
                result.append('=');
            }
            if (this.archivePathPattern != null) {
                result.append("\"");
                result.append(this.archivePathPattern);
                result.append("\"");
                result.append("!");
            }
            if (this.packagePattern != null) {
                result.append(this.packagePattern.getSpecText());
            }
            if (this.linkClassName != null) {
                result.append(this.linkClassName);
            } else if (this.classNamePattern != null) {
                result.append(this.classNamePattern.getSpecText());
            }
            if (this.referencingAnnotation != null) {
                result.append(')');
            }
            if (this.containingClause != null) {
                result.append(' ' + this.containingClause.toScriptText());
            }
            if (this.extendsClassName != null) {
                result.append(" extends ");
                if (this.extendsAnnotation != null) {
                    result.append(this.extendsAnnotation.getSpecText());
                    result.append(" ");
                }
                result.append(toDottedName(this.extendsClassName));
            }
            if (this.implementsNames.size() > 0) {
                result.append(" implements ");
                for (int interfaceIndex = 0; interfaceIndex < this.implementsNames.size(); interfaceIndex++) {
                    if (this.implementsAnnotations.get(interfaceIndex) != null) {
                        result.append(((ASTComplexAnnotationSpecifier) this.implementsAnnotations.get(interfaceIndex)).getSpecText());
                        result.append(" ");
                    }
                    result.append(toDottedName((String) this.implementsNames.get(interfaceIndex)));
                    if (interfaceIndex < this.implementsNames.size() - 1) {
                        result.append(", ");
                    }
                }
            }
        } else {
            if (flowPredicate == 0) {
                result.append('@');
            }
            if (this.packagePattern != null) {
                result.append(this.packagePattern.getSpecText());
            }
            result.append(this.classNamePattern.getSpecText());
        }

        if (flowPredicate == 0 && this.linkPackageNames.size() > 0) {
            result.append(" search ");
            for (int packageIndex = 0; packageIndex < this.linkPackageNames.size(); packageIndex++) {
                result.append(((String) this.linkPackageNames.get(packageIndex)).replace('/', '.'));
                if (packageIndex < this.linkPackageNames.size() - 1) {
                    result.append(", ");
                }
            }
        }
        String memberText = formatMemberSpec(
                this.memberAnnotation, this.memberAccessFlags, this.fieldType, this.fieldSpecifier,
                this.linkMethodSignature, this.methodSpecifier, this.argsPattern, this.parameterAnnotations,
                this.throwsTypes, this.plusSignatureClasses
        );
        if (flowPredicate != 0 || memberText.length() > 0) {
            if (flowPredicate == 0) {
                result.append(' ');
            }
            result.append(memberText);
        }
        if (this.moduleSpecifier != null) {
            result.append(this.moduleSpecifier.getSpecText());
        }
        return result.toString();
    }

    public boolean isNativeRequired() {
        return this.memberAccessFlags.isNativeRequired();
    }

    public boolean matchesClass(ClassFileBase classFileBase, Set set1, String string, String string1, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        if (set1 != null && !set1.contains(classFileBase)) {
            return false;
        } else if (!this.matchesClassName(string1)) {
            return false;
        } else if (!this.matchesPackageName(string)) {
            return false;
        } else if (!matchesAccessFlags(classFileBase.getAccessFlags(), this.classAccessFlags)) {
            return false;
        } else if (!this.matchesArchivePath(classFileBase)) {
            return false;
        } else if (!this.matchesContainingClause(classFileBase, classHierarchyQuery)) {
            return false;
        } else if (!this.matchesExtendsClause(classFileBase, classHierarchyQuery)) {
            return false;
        } else {
            return (!(!this.matchesImplementsClause(classFileBase, classHierarchyQuery)) && (this.matchesClassAnnotation(classFileBase, classHierarchyQuery)));
        }
    }

    public boolean matchesCaretClassName(String string) {
        return ((this.classNamePattern != null && this.classNamePattern.hasCaretTag()) && (this.classNamePattern.matchesName(string)));
    }

    public void unexcludeReflectionFields(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, reflectionAccessMatcher)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                    reflectionAccessMatcher.markFieldNotAccessed(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                    this.unexcludeClassIfCaret(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    public boolean isVolatileRequired() {
        return this.memberAccessFlags.isVolatileRequired();
    }

    public void unexcludeStringEncryptionClasses(StringEncryptionExclusionSpec stringEncryptionExclusionSpec, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                stringEncryptionExclusionSpec.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void excludeTrimClasses(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            this.excludeTrimClass(trimProcessor1, programClass1, set1);
        }
    }

    public void unexcludeTrimMembers(TrimProcessor trimProcessor1, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            trimProcessor1.unmatchField(fieldInfos[fieldInfoIndex], "unexclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            trimProcessor1.unmatchMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
        }
    }

    public void applyStringEncryptionExclusion(StringEncryptionExclusionSpec stringEncryptionExclusionSpec) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    stringEncryptionExclusionSpec.hasOriginalNameCaches()
                            || stringEncryptionExclusionSpec.isHierarchySealed()
                            || stringEncryptionExclusionSpec.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(stringEncryptionExclusionSpec.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.excludeStringEncryptionClasses(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getIncludedClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && stringEncryptionExclusionSpec.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = stringEncryptionExclusionSpec.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, stringEncryptionExclusionSpec)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, stringEncryptionExclusionSpec)) {
                                        stringEncryptionExclusionSpec.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeStringEncryptionFields(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getIncludedFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && stringEncryptionExclusionSpec.canUseMethodNameIndex()) {
                    Enumeration enumeration = stringEncryptionExclusionSpec.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, stringEncryptionExclusionSpec)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(stringEncryptionExclusionSpec, methodInfo1)) {
                                        stringEncryptionExclusionSpec.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeStringEncryptionMethods(stringEncryptionExclusionSpec, stringEncryptionExclusionSpec.getIncludedMethods(), set1);
                }
        }
    }

    public void excludeLongEncryptionMethods(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(longEncryptionExclusionHandler, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                    longEncryptionExclusionHandler.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void excludeExceptionClasses(ExceptionObfuscationExclusions exceptionObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                exceptionObfuscationExclusions.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public static boolean matchesThrowsClause(
            ClassHierarchyQuery classHierarchyQuery, AbstractMethodInfo abstractMethodInfo, List list1, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        if (list1.size() == 0) {
            return true;
        }

        ArrayList arrayList = abstractMethodInfo.getExceptionClassNames();
        if (arrayList.size() == 0) {
            return false;
        }

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            boolean bl = false;
            String string = (String) list1.get(elementIndex);
            if (!containsWildcard(string)) {
                try {
                    if (!string.equals("java/lang/Throwable")) {
                        boolean throwableType;
                        if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                            throwableType = classHierarchyQuery.isSubclassByOriginalName(string, "java/lang/Throwable");
                        } else {
                            throwableType = classHierarchyQuery.isSubclass(string, "java/lang/Throwable");
                        }
                        if (!throwableType) {
                            observableHolder.setValue("Method exclude parameter '<0>' in \"<1>\" statement at line <2> has a non-Throwable class in it's throws clause");
                        }
                    }
                } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                    String className = zkmClassNotFoundException.getClassName();
                    if (!className.startsWith("java.rmi")) {
                        observableHolder.setValue(
                                "Error while executing \"<1>\" statement at line <2> while analysing '<0>' : class '"
                                        + ZkmUtils.slashesToDots(className)
                                        + "' not found. (A)"
                        );
                    }
                } catch (ClassFileLoadException classFileLoadException) {
                    observableHolder.setValue(
                            "Error while executing \"<1>\" statement at line <2> while analysing '<0>' : \""
                                    + ZkmUtils.slashesToDots(string)
                                    + "\" not found. : \""
                                    + classFileLoadException.getMessage()
                                    + "\" (A)"
                    );
                }
            }

            for (int elementIndexValue = 0; elementIndexValue < arrayList.size(); elementIndexValue++) {
                String string3 = (String) arrayList.get(elementIndexValue);

                try {
                    if (HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches()) {
                        String originalClassName = classHierarchyQuery.getOriginalClassName(string3);
                        if (string.equals(originalClassName)
                                || containsWildcard(string) && ZkmStringUtils.matchesWildcard(originalClassName, string)
                                || classHierarchyQuery.isSubclassByOriginalName(originalClassName, string)) {
                            bl = true;
                            break;
                        }
                    } else if (string.equals(string3)
                            || containsWildcard(string) && ZkmStringUtils.matchesWildcard(string3, string)
                            || classHierarchyQuery.isSubclass(string3, string)) {
                        bl = true;
                        break;
                    }
                } catch (ZkmClassNotFoundException zkmClassNotFoundException1) {
                    observableHolder.setValue(
                            "Error while executing \"<1>\" statement at line <2> while analysing '<1>' : class '"
                                    + ZkmUtils.slashesToDots(zkmClassNotFoundException1.getClassName())
                                    + "' not found. (B)"
                    );
                } catch (ClassFileLoadException classFileLoadException1) {
                    observableHolder.setValue(
                            "Error while executing \"<1>\" statement at line <2> while analysing '<0>' : \"" + classFileLoadException1.getMessage() + "\" (B)"
                    );
                }
            }

            if (!bl) {
                return false;
            }
        }

        return true;
    }

    public void excludeNameFields(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, nameExclusionSet)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String packagePath = programClass1.getPackagePath();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                    nameExclusionSet.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                    this.excludePackageIfCaret(nameExclusionSet, packagePath);
                    this.excludeClassIfCaret(nameExclusionSet, programClass1);
                }
            }
        }
    }

    public List getPackageSegments() {
        return this.packagePattern != null ? this.packagePattern.getNameSegments() : new ArrayList();
    }

    public void applyReflectionUnexclusion(ReflectionAccessMatcher reflectionAccessMatcher) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        reflectionAccessMatcher.hasOriginalNameCaches() || reflectionAccessMatcher.isHierarchySealed() || reflectionAccessMatcher.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(reflectionAccessMatcher.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    if (!this.hasCaretTag() && !this.isClassPlusSpec()) {
                        this.unexcludeReflectionClasses(reflectionAccessMatcher, reflectionAccessMatcher.getExcludedClasses(), set1);
                    } else {
                        this.unexcludeReflectionClasses(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateClasses(), set1);
                    }
                    break;
                case 3:
                    if (this.fieldSpecifier.isLiteralName() && reflectionAccessMatcher.canUseFieldNameIndex()) {
                        Enumeration enumeration1 = reflectionAccessMatcher.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                        if (enumeration1 != null) {
                            while (enumeration1.hasMoreElements()) {
                                ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                                if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                                    ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                    while (arrayEnumeration1.hasMoreElements()) {
                                        FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                        if (this.matchesField(fieldInfo, reflectionAccessMatcher)) {
                                            reflectionAccessMatcher.markFieldNotAccessed(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                            this.unexcludeClassIfCaret(reflectionAccessMatcher, programClass2);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (this.hasCaretTag()) {
                        this.unexcludeReflectionFields(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateFields(), set1);
                    } else {
                        this.unexcludeReflectionFields(reflectionAccessMatcher, reflectionAccessMatcher.getExcludedFields(), set1);
                    }
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && reflectionAccessMatcher.canUseMethodNameIndex()) {
                        Enumeration enumeration = reflectionAccessMatcher.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, reflectionAccessMatcher)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(reflectionAccessMatcher, methodInfo1)) {
                                            reflectionAccessMatcher.markMethodNotAccessed(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                            this.unexcludeClassIfCaret(reflectionAccessMatcher, programClass1);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (this.hasCaretTag()) {
                        this.unexcludeReflectionMethods(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateMethods(), set1);
                    } else {
                        this.unexcludeReflectionMethods(reflectionAccessMatcher, reflectionAccessMatcher.getExcludedMethods(), set1);
                    }
            }
        }
    }

    public void applyReflectionExclusion(ReflectionAccessMatcher reflectionAccessMatcher) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        reflectionAccessMatcher.hasOriginalNameCaches() || reflectionAccessMatcher.isHierarchySealed() || reflectionAccessMatcher.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(reflectionAccessMatcher.getCandidateClasses(), bl);
            }

            switch (this.specifierKind) {
                case 2:
                    if (!this.hasCaretTag() && !this.isClassPlusSpec()) {
                        this.excludeReflectionClasses(reflectionAccessMatcher, reflectionAccessMatcher.getIncludedClasses(), set1);
                    } else {
                        this.excludeReflectionClasses(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateClasses(), set1);
                    }
                    break;
                case 3:
                    if (this.fieldSpecifier.isLiteralName() && reflectionAccessMatcher.canUseFieldNameIndex()) {
                        Enumeration enumeration1 = reflectionAccessMatcher.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                        if (enumeration1 != null) {
                            while (enumeration1.hasMoreElements()) {
                                ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                                if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                                    ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                    while (arrayEnumeration1.hasMoreElements()) {
                                        FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                        if (this.matchesField(fieldInfo, reflectionAccessMatcher)) {
                                            reflectionAccessMatcher.markFieldAccessed(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                            this.excludeClassIfCaret(reflectionAccessMatcher, programClass2);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (this.hasCaretTag()) {
                        this.excludeReflectionFields(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateFields(), set1);
                    } else {
                        this.excludeReflectionFields(reflectionAccessMatcher, reflectionAccessMatcher.getIncludedFields(), set1);
                    }
                    break;
                case 4:
                    if (this.methodSpecifier.isLiteralName() && reflectionAccessMatcher.canUseMethodNameIndex()) {
                        Enumeration enumeration = reflectionAccessMatcher.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                        if (enumeration != null) {
                            while (enumeration.hasMoreElements()) {
                                ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                                String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                                String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                                if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, reflectionAccessMatcher)) {
                                    ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                    while (arrayEnumeration.hasMoreElements()) {
                                        MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                        if (this.matchesMethod(reflectionAccessMatcher, methodInfo1)) {
                                            reflectionAccessMatcher.markMethodAccessed(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                            this.excludeClassIfCaret(reflectionAccessMatcher, programClass1);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (this.hasCaretTag()) {
                        this.excludeReflectionMethods(reflectionAccessMatcher, reflectionAccessMatcher.getCandidateMethods(), set1);
                    } else {
                        this.excludeReflectionMethods(reflectionAccessMatcher, reflectionAccessMatcher.getIncludedMethods(), set1);
                    }
            }
        }
    }

    public void excludeNameMethods(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(nameExclusionSet, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String packagePath = programClass1.getPackagePath();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                    if (this.hasLinkMethodSignature()) {
                        nameExclusionSet.addMethodLink(methodInfo1, this.linkMethodPrefix, null, "exclusion parameter '" + this.parameterText + "'");
                    } else {
                        nameExclusionSet.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                        this.excludePackageIfCaret(nameExclusionSet, packagePath);
                        this.excludeClassIfCaret(nameExclusionSet, programClass1);
                        if (this.plusSignatureClasses) {
                            this.excludeSignatureClasses(nameExclusionSet, methodInfo1);
                        }
                    }
                }
            }
        }
    }

    public void unexcludeNameClasses(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            this.unexcludeNameClass(nameExclusionSet, programClass1, set1);
        }
    }

    public void excludeFlowMethods(FlowObfuscationExclusions flowObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(flowObfuscationExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, flowObfuscationExclusions)) {
                    flowObfuscationExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void excludeIntegerEncryptionFields(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, integerEncryptionExclusions)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                    integerEncryptionExclusions.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public boolean matchesFieldName(AbstractFieldInfo abstractFieldInfo) {
        return this.fieldSpecifier.matchesName(AbstractExclusionSpec.getFieldMatchName(abstractFieldInfo));
    }

    public void unexcludeReflectionClasses(ReflectionAccessMatcher reflectionAccessMatcher, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, reflectionAccessMatcher)) {
                reflectionAccessMatcher.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
                if (this.classNamePattern != null && this.classNamePattern.hasPlusTag()) {
                    this.unexcludeReflectionMembers(reflectionAccessMatcher, programClass1);
                }
            }
        }
    }

    public final int countCaretTags() {
        int ba = 0;
        if (this.packagePattern != null && this.packagePattern.hasCaretTag()) {
            ba++;
        }

        if (this.classNamePattern != null && this.classNamePattern.hasCaretTag()) {
            ba++;
        }

        return ba;
    }

    public void applyRemoveMethodCallsInclusion(RemoveMethodCallsHandler removeMethodCallsHandler) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        removeMethodCallsHandler.hasOriginalNameCaches()
                                || removeMethodCallsHandler.isHierarchySealed()
                                || removeMethodCallsHandler.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(removeMethodCallsHandler.getMemberOwnerClasses(), bl);
            }

            switch (this.specifierKind) {
                case 4:
                    this.includeRemoveMethodCalls(removeMethodCallsHandler, removeMethodCallsHandler.getUnmatchedMethods(), set1);
            }
        }
    }

    public void unexcludeLongEncryptionMethods(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(longEncryptionExclusionHandler, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                    longEncryptionExclusionHandler.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    @Override
    public String toString() {
        return this.toScriptText();
    }

    public final boolean matchesContainingClause(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        return (!(this.containingClause != null) || (this.containingClause.containsMatchingMember(classFileBase, classHierarchyQuery)));
    }

    public void excludeTrimClass(TrimProcessor trimProcessor1, ProgramClass programClass1, Set set1) throws ZkmException, IOException {
        String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
        String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
        if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
            trimProcessor1.excludeClass(programClass1, "exclusion parameter '" + this.parameterText + "'");
            if (this.classNamePattern.hasPlusTag()) {
                this.excludeTrimMembers(trimProcessor1, programClass1);
            }
        }
    }

    public boolean matchesQualifiedClassName(String string) {
        return (!(!this.matchesClassName(ClassFileBase.stripPackage(string))) && (this.matchesPackageName(ClassFileBase.getPackagePath(string))));
    }

    public void markStandaloneAnnotation() {
        this.standaloneAnnotation = true;
    }

    public void applySerializedClassesExclusion(ExistingSerializedClassesHandler existingSerializedClassesHandler) throws ZkmException, IOException {
        this.excludeSerializedClasses(existingSerializedClassesHandler, existingSerializedClassesHandler.getIncludedClasses());
    }

    public final boolean hasCaretTag() {
        return this.countCaretTags() > 0;
    }

    public void unexcludeSignatureClasses(NameExclusionSet nameExclusionSet, MethodInfo methodInfo1) throws ZkmException, IOException {
        ArrayList arrayList = methodInfo1.getDescriptorProgramClasses();

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            ProgramClass programClass1 = (ProgramClass) arrayList.get(elementIndex);
            String packagePath = programClass1.getPackagePath();
            nameExclusionSet.unexcludePackage(packagePath, "unexclusion parameter '" + this.parameterText + "'");
            nameExclusionSet.unexcludeClassInternal(programClass1, "unexclusion parameter '" + this.parameterText + "'", false);
        }
    }

    public void excludeLongEncryptionFields(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, longEncryptionExclusionHandler)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                    longEncryptionExclusionHandler.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void unexcludeExceptionClasses(ExceptionObfuscationExclusions exceptionObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                exceptionObfuscationExclusions.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void excludePackageIfCaret(NameExclusionSet nameExclusionSet, String string) throws IOException {
        if (this.packagePattern != null && this.packagePattern.hasCaretTag()) {
            nameExclusionSet.excludePackage(string, "exclusion parameter '" + this.parameterText + "'");
        }
    }

    public void unexcludeExceptionMethods(ExceptionObfuscationExclusions exceptionObfuscationExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(exceptionObfuscationExclusions, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, exceptionObfuscationExclusions)) {
                    exceptionObfuscationExclusions.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void excludeNameClasses(NameExclusionSet nameExclusionSet, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            this.excludeNameClass(nameExclusionSet, programClass1, set1);
        }
    }

    public final boolean containedInHasCaretTag() {
        return this.containedInClause != null
                && this.containedInClause.getFilterParameter() != null
                && this.containedInClause.getFilterParameter().countCaretTags() > 0;
    }

    public boolean isContainedInMethodSpec() {
        return this.containedInClause == null
                || this.containedInClause.getFilterParameter() == null
                || this.containedInClause.getFilterParameter().specifierKind == 4;
    }

    public void setPlusSignatureClasses() {
        this.plusSignatureClasses = true;
    }

    public void setClassAnnotation(ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) {
        this.classAnnotation = aSTComplexAnnotationSpecifier;
    }

    public boolean matchesContainedIn(ClassHierarchyQuery classHierarchyQuery, MethodBytecode methodBytecode1) throws ZkmException, IOException {
        ASTRenameFilterParameter aSTRenameFilterParameter1;
        if (this.containedInClause != null && (aSTRenameFilterParameter1 = this.containedInClause.getFilterParameter()) != null) {
            AbstractMethodInfo abstractMethodInfo = methodBytecode1.getMethod();
            if (aSTRenameFilterParameter1.matchesMethod(classHierarchyQuery, abstractMethodInfo)) {
                ClassFileBase classFileBase = abstractMethodInfo.getOwningClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(classFileBase);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(classFileBase);
                if (aSTRenameFilterParameter1.matchesClass(classFileBase, null, classMatchName, classSimpleMatchName, classHierarchyQuery)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public boolean isAbstractRequired() {
        return this.classAccessFlags.isAbstractRequired();
    }

    public boolean hasImplementsClause() {
        return !this.implementsNames.isEmpty();
    }

    public void applyIntegerEncryptionExclusion(IntegerEncryptionExclusions integerEncryptionExclusions) throws ZkmException, IOException {
        Set set1 = null;
        if (this.hasReferencingAnnotation()) {
            boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                    && (
                    integerEncryptionExclusions.hasOriginalNameCaches()
                            || integerEncryptionExclusions.isHierarchySealed()
                            || integerEncryptionExclusions.isHierarchyMarked()
            );
            set1 = this.findAnnotationReferencingClasses(integerEncryptionExclusions.getCandidateClasses(), bl);
        }

        switch (this.specifierKind) {
            case 2:
                this.excludeIntegerEncryptionClasses(integerEncryptionExclusions, integerEncryptionExclusions.getIncludedClasses(), set1);
                break;
            case 3:
                if (this.fieldSpecifier.isLiteralName() && integerEncryptionExclusions.canUseFieldNameIndex()) {
                    Enumeration enumeration1 = integerEncryptionExclusions.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                    if (enumeration1 != null) {
                        while (enumeration1.hasMoreElements()) {
                            ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass2);
                            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                            if (this.matchesClass(programClass2, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                                ArrayEnumeration arrayEnumeration1 = programClass2.enumerateFields();

                                while (arrayEnumeration1.hasMoreElements()) {
                                    FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                    if (this.matchesField(fieldInfo, integerEncryptionExclusions)) {
                                        integerEncryptionExclusions.excludeField(fieldInfo, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeIntegerEncryptionFields(integerEncryptionExclusions, integerEncryptionExclusions.getIncludedFields(), set1);
                }
                break;
            case 4:
                if (this.methodSpecifier.isLiteralName() && integerEncryptionExclusions.canUseMethodNameIndex()) {
                    Enumeration enumeration = integerEncryptionExclusions.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
                            String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass1);
                            String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                            if (this.matchesClass(programClass1, set1, classMatchNameValue, classSimpleMatchNameValue, integerEncryptionExclusions)) {
                                ArrayEnumeration arrayEnumeration = programClass1.enumerateMethods();

                                while (arrayEnumeration.hasMoreElements()) {
                                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                    if (this.matchesMethod(integerEncryptionExclusions, methodInfo1)) {
                                        integerEncryptionExclusions.excludeMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                                    }
                                }
                            }
                        }
                    }
                } else {
                    this.excludeIntegerEncryptionMethods(integerEncryptionExclusions, integerEncryptionExclusions.getIncludedMethods(), set1);
                }
        }
    }

    public boolean isMethodSpecifier() {
        return this.specifierKind == 4;
    }

    private boolean matchesClassAnnotation(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        if (this.classAnnotation != null) {
            boolean bl;
            if (this.classAnnotation.isLiteralName()) {
                bl = false;
                String specText = this.classAnnotation.getSpecText();
                String key = ZkmUtils.dotsToSlashes(specText.substring(1));

                try {
                    ClassFileBase classFileBase1 = classHierarchyQuery.getClassResolver().getClassFile(key);
                    if (classFileBase1.hasAnnotation("java/lang/annotation/Inherited")) {
                        bl = true;
                    }
                } catch (ClassFileLoadException classFileLoadException) {
                    String string2 = "Error while executing \""
                            + this.getStatementName()
                            + "\" statement at line "
                            + this.getLineNumber()
                            + " while analysing '"
                            + this.parameterText
                            + "' : '"
                            + classFileLoadException.getMessage()
                            + "' : While looking for class '"
                            + key
                            + "'. (F)";
                    this.scriptEnvironment.logError(string2);
                }
            } else {
                bl = true;
            }

            try {
                Integer integer1 = classFileBase.hasReleaseVersion() ? classFileBase.getReleaseVersion() : null;
                boolean bl1 = HiddenOptionFlags.MATCH_ORIGINAL_NAMES && classHierarchyQuery.hasOriginalNameCaches();
                String className = classFileBase.getClassName();
                Boolean boolean1 = bl1;
                Integer integer = integer1;
                String classNameValue = className;
                Set set1 = classHierarchyQuery.getClassAnnotations(bl, classNameValue, integer, boolean1);
                return this.classAnnotation.matchesAnyAnnotation(set1);
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                String string5 = "Error while executing \""
                        + this.getStatementName()
                        + "\" statement at line "
                        + this.getLineNumber()
                        + " while analysing '"
                        + this.parameterText
                        + "' : class '"
                        + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName())
                        + "' not found while examining class '"
                        + classFileBase.getDottedClassName()
                        + "'. (G)";
                if (!HiddenOptionFlags.RENAME_FILTER_ERRORS_NOT_FATAL) {
                    this.scriptEnvironment.logFatalError(string5);
                } else {
                    this.scriptEnvironment.logError(string5);
                }

                return false;
            } catch (ClassFileLoadException classFileLoadException1) {
                String string4 = "Error while executing \""
                        + this.getStatementName()
                        + "\" statement at line "
                        + this.getLineNumber()
                        + " while analysing '"
                        + this.parameterText
                        + "' : '"
                        + classFileLoadException1.getMessage()
                        + "' : While examining class '"
                        + classFileBase.getDottedClassName()
                        + "'. (H)";
                if (!HiddenOptionFlags.RENAME_FILTER_ERRORS_NOT_FATAL) {
                    this.scriptEnvironment.logFatalError(string4);
                } else {
                    this.scriptEnvironment.logError(string4);
                }

                return false;
            }
        } else {
            return true;
        }
    }

    public boolean isStaticRequired() {
        return this.memberAccessFlags.isStaticRequired();
    }

    public boolean hasClassCaretTag() {
        return this.classNamePattern != null && this.classNamePattern.hasCaretTag();
    }

    public void setExtendsClassName(String extendsClassNameArgument) {
        this.extendsClassName = extendsClassNameArgument;
    }

    public void setEnabled() {
        this.enabled = false;
    }

    public boolean matchesPackageName(String string) {
        return this.packagePattern != null ? this.packagePattern.matchesName(string) : string.length() == 0;
    }

    public boolean isMemberAbstractRequired() {
        return this.memberAccessFlags.isAbstractRequired();
    }

    public void unexcludeTrimAnnotationTypes(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ClassFileBase classFileBase = (ClassFileBase) enumeration.nextElement();
            String packagePath = classFileBase.getPackagePath();
            String simpleName = classFileBase.getSimpleName();
            if (this.matchesClass(classFileBase, set1, packagePath, simpleName, trimProcessor1)) {
                trimProcessor1.removeAnnotationRetainedClass(classFileBase, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void unexcludeIntegerEncryptionFields(IntegerEncryptionExclusions integerEncryptionExclusions, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, integerEncryptionExclusions)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, integerEncryptionExclusions)) {
                    integerEncryptionExclusions.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                }
            }
        }
    }

    public void unexcludeLongEncryptionClasses(LongEncryptionExclusionHandler longEncryptionExclusionHandler, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            ProgramClass programClass1 = (ProgramClass) enumeration.nextElement();
            String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
            String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
            if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, longEncryptionExclusionHandler)) {
                longEncryptionExclusionHandler.unexcludeClass(programClass1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public void setExtendsAnnotation(ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) {
        this.extendsAnnotation = aSTComplexAnnotationSpecifier;
    }

    public void unexcludeNameMembers(NameExclusionSet nameExclusionSet, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            nameExclusionSet.unexcludeField(fieldInfos[fieldInfoIndex], "unexclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            if (!methodInfo1.isConstructor() && !methodInfo1.isStaticInitializer()) {
                nameExclusionSet.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public static boolean matchesArgs(String string, MethodArgsPattern methodArgsPattern1) {
        String string1 = string.substring(0, string.indexOf(41) + 1);
        if (methodArgsPattern1 == null) {
            return string1.length() == 2;
        } else {
            return ((methodArgsPattern1.isAnyArgs()) || (methodArgsPattern1.argsRegex.matcher(string1).matches()));
        }
    }

    public boolean matchesModule(ModuleInfoClass moduleInfoClass) {
        return ((this.moduleSpecifier != null) && (this.moduleSpecifier.matchesName(moduleInfoClass.getModuleName())));
    }

    public void setClassNamePattern(ClassNamePattern classNamePattern1) {
        this.classNamePattern = classNamePattern1;
        this.accessModifier = null;
    }

    public void setPackagePattern(PackagePatternSpec packagePatternSpec) {
        this.packagePattern = packagePatternSpec;
    }

    public void excludeReflectionMembers(ReflectionAccessMatcher reflectionAccessMatcher, ProgramClass programClass1) throws ZkmException, IOException {
        FieldInfo[] fieldInfos = programClass1.getFieldInfos();

        for (int fieldInfoIndex = 0; fieldInfoIndex < fieldInfos.length; fieldInfoIndex++) {
            reflectionAccessMatcher.markFieldAccessed(fieldInfos[fieldInfoIndex], "exclusion parameter '" + this.parameterText + "'");
        }

        MethodInfo[] methodInfos = programClass1.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < methodInfos.length; methodInfoIndex++) {
            MethodInfo methodInfo1 = methodInfos[methodInfoIndex];
            if (!methodInfo1.isConstructor() && !methodInfo1.isStaticInitializer()) {
                reflectionAccessMatcher.markMethodAccessed(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
            }
        }
    }

    public boolean isClassPlusSpec() {
        return this.isClassSpecifier() && this.classNamePattern != null && this.classNamePattern.hasPlusTag();
    }

    public boolean isPackageSpecifier() {
        return this.specifierKind == 1;
    }

    public boolean isClassSpecifier() {
        return this.specifierKind == 2;
    }

    public boolean matchesFieldSpec(ClassHierarchyQuery classHierarchyQuery, ClassFileBase classFileBase, String string, Object object) throws ZkmException, IOException {
        Object object1 = null;
        String packagePath = classFileBase.getPackagePath();
        String simpleName = classFileBase.getSimpleName();
        if (this.matchesClass(classFileBase, (Set) object1, packagePath, simpleName, classHierarchyQuery)) {
            return ((this.isClassPlusSpec()) || (this.fieldSpecifier.matchesName(string) && (this.fieldType == null || this.fieldType.equals(object))));
        } else {
            return false;
        }
    }

    public void excludeTrimMethods(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            MethodInfo methodInfo1 = (MethodInfo) enumeration.nextElement();
            if (this.matchesMethod(trimProcessor1, methodInfo1)) {
                ProgramClass programClass1 = methodInfo1.getOwnerProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                    trimProcessor1.matchMethod(methodInfo1, "exclusion parameter '" + this.parameterText + "'");
                    this.warnPackageExclusionIgnored();
                    this.excludeClassIfCaret(trimProcessor1, programClass1);
                }
            }
        }
    }

    public void unexcludeTrimFields(TrimProcessor trimProcessor1, Enumeration enumeration, Set set1) throws ZkmException, IOException {
        while (enumeration.hasMoreElements()) {
            FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
            if (this.matchesField(fieldInfo, trimProcessor1)) {
                ProgramClass programClass1 = fieldInfo.getProgramClass();
                String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass1);
                String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass1);
                if (this.matchesClass(programClass1, set1, classMatchName, classSimpleMatchName, trimProcessor1)) {
                    trimProcessor1.unmatchField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                    this.warnPackageExclusionIgnored();
                    this.unexcludeClassIfCaret(trimProcessor1, programClass1);
                }
            }
        }
    }

    public static boolean matchesFieldAnnotations(
            AbstractFieldInfo abstractFieldInfo, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier, ClassHierarchyQuery classHierarchyQuery
    ) {
        return matchesAnnotations(abstractFieldInfo.enumerateAnnotationTypes(), aSTComplexAnnotationSpecifier, classHierarchyQuery);
    }

    public void applyNameUnexclusion(NameExclusionSet nameExclusionSet) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            int specifierKindValue;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (nameExclusionSet.hasOriginalNameCaches() || nameExclusionSet.isHierarchySealed() || nameExclusionSet.isHierarchyMarked());
                set1 = this.findAnnotationReferencingClasses(nameExclusionSet.getInputClassFiles(), bl);
                specifierKindValue = this.specifierKind;
            } else {
                specifierKindValue = this.specifierKind;
            }

            switch (specifierKindValue) {
                case 1:
                    Enumeration enumeration = nameExclusionSet.getExcludedPackages();

                    while (enumeration.hasMoreElements()) {
                        String string2 = (String) enumeration.nextElement();
                        if (this.matchesPackageName(string2)) {
                            nameExclusionSet.unexcludePackage(string2, "unexclusion parameter '" + this.parameterText + "'");
                        }
                    }
                    break;
                case 2:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (!this.hasCaretTag() && !this.isClassPlusSpec()) {
                            this.unexcludeNameClasses(nameExclusionSet, nameExclusionSet.getExcludedClasses(), set1);
                        } else {
                            this.unexcludeNameClasses(nameExclusionSet, nameExclusionSet.getCandidateClasses(), set1);
                        }
                    } else {
                        ProgramClass programClass4 = this.findLiteralClass();
                        if (programClass4 != null) {
                            this.unexcludeNameClass(nameExclusionSet, programClass4, set1);
                        }
                    }
                    break;
                case 3:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (this.fieldSpecifier.isLiteralName() && nameExclusionSet.canUseFieldNameIndex()) {
                            Enumeration enumeration2 = nameExclusionSet.findClassesDeclaringField(this.fieldSpecifier.getSpecText());
                            if (enumeration2 != null) {
                                while (enumeration2.hasMoreElements()) {
                                    ProgramClass programClass5 = (ProgramClass) enumeration2.nextElement();
                                    String classMatchName = AbstractExclusionSpec.getClassMatchName(programClass5);
                                    String classSimpleMatchName = AbstractExclusionSpec.getClassSimpleMatchName(programClass5);
                                    if (this.matchesClass(programClass5, set1, classMatchName, classSimpleMatchName, nameExclusionSet)) {
                                        ArrayEnumeration arrayEnumeration1 = programClass5.enumerateFields();

                                        while (arrayEnumeration1.hasMoreElements()) {
                                            FieldInfo fieldInfo = (FieldInfo) arrayEnumeration1.nextElement();
                                            if (this.matchesField(fieldInfo, nameExclusionSet)) {
                                                nameExclusionSet.unexcludeField(fieldInfo, "unexclusion parameter '" + this.parameterText + "'");
                                                this.unexcludePackageIfCaret(nameExclusionSet, classMatchName);
                                                this.unexcludeClassIfCaret(nameExclusionSet, programClass5);
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (this.hasCaretTag()) {
                            this.unexcludeNameFields(nameExclusionSet, nameExclusionSet.getCandidateFields(), set1);
                        } else {
                            this.unexcludeNameFields(nameExclusionSet, nameExclusionSet.getExcludedFields(), set1);
                        }
                    } else {
                        ProgramClass programClass3 = this.findLiteralClass();
                        if (programClass3 != null) {
                            this.unexcludeNameFields(nameExclusionSet, programClass3.enumerateFields(), set1);
                        }
                    }
                    break;
                case 4:
                    if (this.classNamePattern == null
                            || !this.classNamePattern.isLiteralName()
                            || this.packagePattern != null && !this.packagePattern.isLiteralName()) {
                        if (this.methodSpecifier.isLiteralName() && nameExclusionSet.canUseMethodNameIndex()) {
                            Enumeration enumeration1 = nameExclusionSet.findClassesDeclaringMethod(this.methodSpecifier.getSpecText());
                            if (enumeration1 != null) {
                                while (enumeration1.hasMoreElements()) {
                                    ProgramClass programClass2 = (ProgramClass) enumeration1.nextElement();
                                    String classMatchNameValue = AbstractExclusionSpec.getClassMatchName(programClass2);
                                    String classSimpleMatchNameValue = AbstractExclusionSpec.getClassSimpleMatchName(programClass2);
                                    if (this.matchesClass(programClass2, set1, classMatchNameValue, classSimpleMatchNameValue, nameExclusionSet)) {
                                        ArrayEnumeration arrayEnumeration = programClass2.enumerateMethods();

                                        while (arrayEnumeration.hasMoreElements()) {
                                            MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration.nextElement();
                                            if (this.matchesMethod(nameExclusionSet, methodInfo1)) {
                                                nameExclusionSet.unexcludeMethod(methodInfo1, "unexclusion parameter '" + this.parameterText + "'");
                                                this.unexcludePackageIfCaret(nameExclusionSet, classMatchNameValue);
                                                this.unexcludeClassIfCaret(nameExclusionSet, programClass2);
                                                if (this.plusSignatureClasses) {
                                                    this.unexcludeSignatureClasses(nameExclusionSet, methodInfo1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!this.hasCaretTag() && !this.hasPlusSignatureClasses()) {
                            this.unexcludeNameMethods(nameExclusionSet, nameExclusionSet.getExcludedMethods(), set1);
                        } else {
                            this.unexcludeNameMethods(nameExclusionSet, nameExclusionSet.getCandidateMethods(), set1);
                        }
                    } else {
                        ProgramClass programClass1 = this.findLiteralClass();
                        if (programClass1 != null) {
                            this.unexcludeNameMethods(nameExclusionSet, programClass1.enumerateMethods(), set1);
                        }
                    }
            }
        }
    }

    public void applyReferenceInclusion(ReferenceObfuscationExclusions referenceObfuscationExclusions) throws ZkmException, IOException {
        if (this.enabled) {
            Set set1 = null;
            if (this.hasReferencingAnnotation()) {
                boolean bl = HiddenOptionFlags.MATCH_ORIGINAL_NAMES
                        && (
                        referenceObfuscationExclusions.hasOriginalNameCaches()
                                || referenceObfuscationExclusions.isHierarchySealed()
                                || referenceObfuscationExclusions.isHierarchyMarked()
                );
                set1 = this.findAnnotationReferencingClasses(referenceObfuscationExclusions.getMemberOwnerClasses(), bl);
            }

            switch (this.specifierKind) {
                case 3:
                    this.includeReferenceFields(referenceObfuscationExclusions, referenceObfuscationExclusions.getUnmatchedFields(), set1);
                    break;
                case 4:
                    this.includeReferenceMethods(referenceObfuscationExclusions, referenceObfuscationExclusions.getUnmatchedMethods(), set1);
            }
        }
    }

    public ProgramClass findLiteralClass() {
        String namePatternValue;
        char ba;
        if (this.packagePattern != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.packagePattern.getNamePattern());
            stringBuilder.append(this.classNamePattern.getNamePattern());
            String namePatternValue2 = stringBuilder.toString();
            namePatternValue = namePatternValue2;
            ba = '.';
        } else {
            String namePattern = this.classNamePattern.getNamePattern();
            namePatternValue = namePattern;
            ba = '.';
        }

        String key = namePatternValue.replace(ba, '/');
        return ClassHierarchyNode.findProgramClass(key);
    }

    public final boolean hasLinkClassName() {
        return this.linkClassName != null;
    }

    public void setContainingClause(ASTContainingClause aSTContainingClause) {
        this.containingClause = aSTContainingClause;
    }
}
