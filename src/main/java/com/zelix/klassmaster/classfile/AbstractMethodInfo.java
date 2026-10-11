package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.classfile.attribute.Attribute;
import com.zelix.klassmaster.classfile.attribute.CodeAttributeBody;
import com.zelix.klassmaster.classfile.attribute.ExceptionsAttribute;
import com.zelix.klassmaster.classfile.attribute.MethodParametersAttribute;
import com.zelix.klassmaster.classfile.attribute.ParameterAnnotationsAttribute;
import com.zelix.klassmaster.classfile.constpool.AbstractConstantPool;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.MethodOverrideAnalyzer;
import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.classfile.insn.MethodKey;
import com.zelix.klassmaster.engine.ProcessingStatistics;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.parameters.ChangedMethodDescriptor;
import com.zelix.klassmaster.util.EmptyEnumeration;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.VisitableNode;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class AbstractMethodInfo extends MemberInfo {
    public List parameterTypes;
    public String methodName;
    public boolean lambdaImplementation;
    public boolean placeholder;
    public String lowerCaseName;
    public String descriptor;
    public int codeAttributeIndex_int = -1;
    public int exceptionsAttributeIndex = -1;
    public int lambdaArgCount = -1;

    @Override
    public boolean isField() {
        return false;
    }

    public static String formatMethodSignature(String string, String descriptorArgument) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.append('(');
        List list1 = ConstantPoolEntry.parseParameterTypes(descriptorArgument, true);
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            String descriptorValue = (String) list1.get(elementIndex);
            stringBuilder.append(ConstantPoolEntry.descriptorToJavaType(descriptorValue));
            if (elementIndex < list1Size - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(')');
        return stringBuilder.toString();
    }

    public final boolean isParameterSlotsShuffled() {
        return this.hasStateFlag(8);
    }

    public final boolean hasChangedParameters() {
        return this.hasStateFlag(2);
    }

    public final MethodSignature getSignature() {
        return new MethodSignature(this.getJvmName(), this.getDescriptor());
    }

    public void addToInheritedMethods(MethodOverrideAnalyzer methodOverrideAnalyzer, Map map1, boolean bl, Object value) {
        if (!this.isConstructor() && !this.isStaticInitializer() && !this.isStatic() && (this.isAbstract() || !this.isStrictlyPrivate())) {
            MethodSignature methodSignature1 = this.getSignature();
            ClassFileBase classFileBase = (ClassFileBase) map1.put(methodSignature1, value);
            if (classFileBase != null) {
                map1.put(methodSignature1, classFileBase);
            }

            if (bl) {
                methodOverrideAnalyzer.reserveSignature(methodSignature1);
            }
        }
    }

    public final boolean hadMethodParameters() {
        return this.hasStateFlag(32);
    }

    public final boolean isConstructor() {
        return this.methodName.equals("<init>");
    }

    public abstract void collectCodeTypeReferences(Set set1, Set set2, Set set3, Set set4) throws ZkmProcessingException;

    public final void setHadLocalVariableTable(boolean hadLocalVariableTableArgument) {
        this.setStateFlag(16);
    }

    public String getOriginalNameWithParameters() {
        return this.getOriginalMemberName() + MethodSignature.formatParameterTypes(this.getOriginalParameterDescriptor(), null);
    }

    public void cacheNameAndDescriptor() {
        if (this.isValid()) {
            this.descriptor = this.descriptorConstant.getValue();
            this.methodName = this.nameConstant.getValue();
            this.lowerCaseName = this.getSourceName().toLowerCase();
            this.parameterTypes = ConstantPoolEntry.parseParameterTypes(this.descriptor, true);
        }
    }

    @Override
    public String toOriginalDisplayString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ConstantPoolEntry.descriptorToJavaType(this.getOriginalDescriptor()));
        stringBuilder.append(" ");
        stringBuilder.append(this.getOriginalMemberName());
        stringBuilder.append('(');
        List list1 = ConstantPoolEntry.parseParameterTypes(this.getOriginalDescriptor(), false);

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            stringBuilder.append(ConstantPoolEntry.descriptorToJavaType((String) list1.get(elementIndex)));
            stringBuilder.append(elementIndex < list1.size() - 1 ? ", " : "");
        }

        stringBuilder.append(')');
        return stringBuilder.toString();
    }

    public boolean isLambdaImplementation() {
        return this.lambdaImplementation;
    }

    public String getParameterDescriptor() {
        int ba = this.descriptor.lastIndexOf(41);
        return this.descriptor.substring(0, ba + 1);
    }

    public int getCreationKind() {
        return this.creationKind;
    }

    public ArrayList getExceptionClassNames() {
        ArrayList arrayList = new ArrayList();
        ExceptionsAttribute exceptionsAttribute = null;
        if (this.exceptionsAttributeIndex != -1) {
            exceptionsAttribute = (ExceptionsAttribute) this.attributes[this.exceptionsAttributeIndex];
        }

        if (exceptionsAttribute != null) {
            for (int exceptionClasseIndex = 0; exceptionClasseIndex < exceptionsAttribute.getExceptionCount(); exceptionClasseIndex++) {
                arrayList.add(exceptionsAttribute.getExceptionClass(exceptionClasseIndex).getClassName());
            }
        }

        return arrayList;
    }

    public final List getWidenedParameterTypes() {
        List list1 = ConstantPoolEntry.getParameterTypes(this.descriptor);
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            String string = (String) list1.get(elementIndex);
            if (string.equals("B") || string.equals("C") || string.equals("S") || string.equals("Z")) {
                list1.set(elementIndex, "I");
            }
        }

        return list1;
    }

    public String getReturnTypeName() {
        return ConstantPoolEntry.descriptorToJavaType(this.descriptor, false, true);
    }

    public void updateParameterAnnotations(ChangedMethodDescriptor changedMethodDescriptor) {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ParameterAnnotationsAttribute) {
                ((ParameterAnnotationsAttribute) this.attributes[attributeIndex]).insertAddedParameters(changedMethodDescriptor);
            }
        }
    }

    public void setLambdaArgCount(int lambdaArgCount) {
        this.lambdaArgCount = lambdaArgCount;
    }

    public final void collectStatistics(ProcessingStatistics processingStatistics1) {
        if (this.codeAttributeIndex_int != -1) {
            ((CodeAttributeBody) this.attributes[this.codeAttributeIndex_int]).recordStatistics(processingStatistics1);
        }
    }

    public final void markVarargs() {
        this.accessFlags.markVarargs();
    }

    @Override
    public String getJvmName() {
        return this.methodName;
    }

    public String getOriginalParameterDescriptor() {
        int ba = this.originalDescriptor.lastIndexOf(41);
        return this.originalDescriptor.substring(0, ba + 1);
    }

    public abstract MethodBytecode getBytecode() throws ZkmProcessingException;

    public MethodSignature getOriginalSignature() {
        return new MethodSignature(this.getOriginalName(), this.getOriginalDescriptor());
    }

    public String getNameWithParameters(HashMap hashMap) {
        return this.getSourceName() + MethodSignature.formatParameterTypes(this.getParameterDescriptor(), hashMap);
    }

    public List getParameterTypes() {
        return this.parameterTypes != null ? Collections.unmodifiableList(this.parameterTypes) : Collections.emptyList();
    }

    public int getLambdaArgCount() {
        return this.lambdaArgCount;
    }

    public final boolean isStaticInitializer() {
        return this.methodName.equals("<clinit>");
    }

    @Override
    public final String getDescriptor() {
        return this.descriptor;
    }

    public final String getSourceNameAndDescriptor() {
        return this.getSourceName() + this.getDescriptor();
    }

    public final String buildDeclarationWithParamNames(Map map1) {
        return this.buildDeclaration(true, map1);
    }

    public boolean hasLambdaArgCount() {
        return this.lambdaArgCount > -1;
    }

    @Override
    public String getLowerCaseName() {
        return this.lowerCaseName;
    }

    @Override
    public final boolean isBridge() {
        return this.accessFlags.isBridge();
    }

    public final String getNameAndDescriptor() {
        return this.methodName + this.descriptor;
    }

    public final void setParameterSlotsShuffled() {
        this.setStateFlag(8);
    }

    public final void setVarargs() {
        this.accessFlags.setVarargs();
    }

    public int getParameterCount() {
        return this.parameterTypes != null ? this.parameterTypes.size() : 0;
    }

    public static int getSlotSize(String string) {
        if (string.equals("D")) {
            return 2;
        } else {
            return string.equals("J") ? 2 : 1;
        }
    }

    @Override
    public String toDisplayString() {
        return this.getModifierString() + this.getSignature().toDisplayString();
    }

    public Enumeration enumerateParameterAnnotationTypes(int ba) {
        HashSet hashSet = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (hashSet == null) {
                hashSet = ZkmUtils.createHashSet(13);
            }

            if (this.attributes[attributeIndex] instanceof ParameterAnnotationsAttribute) {
                String[] originalAnnotationTypes = ((ParameterAnnotationsAttribute) this.attributes[attributeIndex]).getOriginalAnnotationTypes(ba);

                for (int originalAnnotationTypeIndex = 0; originalAnnotationTypeIndex < originalAnnotationTypes.length; originalAnnotationTypeIndex++) {
                    hashSet.add(originalAnnotationTypes[originalAnnotationTypeIndex]);
                }
            }
        }

        return hashSet == null ? new EmptyEnumeration() : Collections.enumeration(hashSet);
    }

    public static void sortMostSpecificFirst(String[] strings, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        for (int i = 0; i < strings.length - 1; i++) {
            int bb = i;
            String string = strings[i];

            for (int j = i + 1; j < strings.length; j++) {
                String string1 = strings[j];
                if (classMemberLookup1.isSubclass(string1, string) || classMemberLookup1.implementsInterface(string1, string)) {
                    bb = j;
                    string = string1;
                }
            }

            if (bb > i) {
                strings[bb] = strings[i];
                strings[i] = string;
            }
        }
    }

    
    
    public static AbstractMethodInfo selectMostSpecificReturnType(AbstractMethodInfo[] abstractMethodInfos, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        if (abstractMethodInfos != null && abstractMethodInfos.length != 0) {
            if (abstractMethodInfos.length == 1) {
                return abstractMethodInfos[0];
            }

            HashMap hashMap = ZkmUtils.createHashMap();
            int ba = 0;
            String[] strings = new String[abstractMethodInfos.length];

            for (int abstractMethodInfoIndex = 0; abstractMethodInfoIndex < abstractMethodInfos.length; abstractMethodInfoIndex++) {
                String returnDescriptor = abstractMethodInfos[abstractMethodInfoIndex].getReturnDescriptor();
                int bc = 0;

                while (returnDescriptor.charAt(bc) == '[') {
                    bc++;
                }

                if (abstractMethodInfoIndex == 0) {
                    ba = bc;
                } else if (bc != ba) {
                    return null;
                }

                if (bc > 0) {
                    returnDescriptor = returnDescriptor.substring(bc);
                }

                if (returnDescriptor.endsWith(";")) {
                    returnDescriptor = returnDescriptor.substring(1, returnDescriptor.length() - 1);
                }

                strings[abstractMethodInfoIndex] = returnDescriptor;
            }

            int bd;
            try {
                sortMostSpecificFirst(strings, classMemberLookup1);
                bd = 0;
            } catch (ClassFileLoadException classFileLoadException) {
                return null;
            }

            while (true) {
                try {
                    if (bd >= strings.length - 1) {
                        return (AbstractMethodInfo) hashMap.get(strings[0]);
                    }

                    if (!classMemberLookup1.isSubclass(strings[bd], strings[bd + 1]) && !classMemberLookup1.implementsInterface(strings[bd], strings[bd + 1])) {
                        return null;
                    }
                } catch (ClassFileLoadException classFileLoadException1) {
                    return null;
                }

                bd++;
            }
        } else {
            return null;
        }
    }

    public AbstractMethodInfo(ClassFileComponent classFileComponent, ClassFileInputStream classFileInputStream, ListMultimap listMultimap) throws ClassFileFormatException, IOException {
        super(classFileComponent, classFileInputStream, listMultimap);
        this.cacheNameAndDescriptor();
    }

    public MethodKey toMethodKey() {
        return new MethodKey(this.getOwnerClassName(), this.getJvmName(), this.getDescriptor());
    }

    public final void setHadMethodParameters() {
        this.setStateFlag(32);
    }

    public final String buildDeclaration() {
        return this.buildDeclaration(true);
    }

    public final String buildDeclaration(boolean bl) {
        return this.buildDeclaration(bl, (Map) null);
    }

    public final void setBridge() {
        this.accessFlags.setBridge();
    }

    public void updateMethodParametersAttribute(ChangedMethodDescriptor changedMethodDescriptor, AbstractConstantPool abstractConstantPool, List list1) {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof MethodParametersAttribute) {
                MethodParametersAttribute methodParametersAttribute = (MethodParametersAttribute) this.attributes[attributeIndex];
                this.getWidenedParameterTypes();
                this.isStatic();
                AbstractConstantPool abstractConstantPool1 = abstractConstantPool;
                methodParametersAttribute.insertAddedParameters(changedMethodDescriptor, abstractConstantPool1, list1);
                break;
            }
        }
    }

    public Enumeration enumerateDeclaredParameterAnnotationTypes(int ba) {
        HashSet hashSet = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (hashSet == null) {
                hashSet = ZkmUtils.createHashSet(13);
            }

            if (this.attributes[attributeIndex] instanceof ParameterAnnotationsAttribute) {
                String[] annotationTypes = ((ParameterAnnotationsAttribute) this.attributes[attributeIndex]).getAnnotationTypes(ba);

                for (int annotationTypeIndex = 0; annotationTypeIndex < annotationTypes.length; annotationTypeIndex++) {
                    hashSet.add(annotationTypes[annotationTypeIndex]);
                }
            }
        }

        return hashSet == null ? new EmptyEnumeration() : Collections.enumeration(hashSet);
    }

    public final int getArgumentSlotCount() {
        int argumentSlotCount = this.accessFlags.isStatic() ? 0 : 1;

        for (int elementIndex = 0; elementIndex < this.parameterTypes.size(); elementIndex++) {
            if (((String) this.parameterTypes.get(elementIndex)).equals("D")) {
                argumentSlotCount += 2;
            } else if (((String) this.parameterTypes.get(elementIndex)).equals("J")) {
                argumentSlotCount += 2;
            } else {
                argumentSlotCount++;
            }
        }

        return argumentSlotCount;
    }

    public final void setHasChangedParameters() {
        this.setStateFlag(2);
    }

    public List getParameterTypeNames() {
        return ConstantPoolEntry.getParameterJavaTypes(this.descriptor);
    }

    public final boolean isVarargs() {
        return this.accessFlags.isVarargs();
    }

    public void markLambdaImplementation() {
        this.lambdaImplementation = true;
    }

    @Override
    public final void setName(String string) throws ZkmException, IOException {
        if (this.isConstructor()) {
            this.setChanged();
            this.notifyObservers(new MutableInt(0), this, null);
        } else {
            this.methodName = string;
            super.setName(string);
        }

        this.lowerCaseName = this.getSourceName().toLowerCase();
    }

    public final boolean isNameChanged() {
        return this.hasStateFlag(4);
    }

    @Override
    public final void setDescriptor(String descriptorArgument) throws ZkmException, IOException {
        this.descriptor = descriptorArgument;
        this.parameterTypes = ConstantPoolEntry.parseParameterTypes(this.descriptor, true);
        super.setDescriptor(descriptorArgument);
    }

    public AbstractMethodInfo(ClassFileBase classFileBase, ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81, Attribute[] attributes1, int creationKindArgument) {
        super(classFileBase, constantUtf8, constantUtf81, attributes1, creationKindArgument);
        this.cacheNameAndDescriptor();
    }

    public final String buildDeclaration(boolean bl, Map map1) {
        String[] strings = null;
        String string2;
        if (map1 != null) {
            int integerValue = 0;
            Iterator iterator = map1.keySet().iterator();

            while (iterator.hasNext()) {
                Integer integer = (Integer) iterator.next();
                if (integer > integerValue) {
                    integerValue = integer;
                }
            }

            strings = new String[integerValue + 1];
            iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                strings[((Integer) (entry.getKey()))] = (String) entry.getValue();
            }

            string2 = "local";
        } else {
            string2 = "local";
        }

        String string = string2;
        StringBuilder stringBuilder = new StringBuilder();
        int bb = this.accessFlags.isStatic() ? 0 : 1;

        for (int elementIndex = 0; elementIndex < this.parameterTypes.size(); elementIndex++) {
            if (bl) {
                stringBuilder.append(ConstantPoolEntry.descriptorToJavaType((String) this.parameterTypes.get(elementIndex)));
                stringBuilder.append(" ");
                if (strings != null && strings.length > bb && strings[bb] != null) {
                    stringBuilder.append(strings[bb]);
                } else {
                    stringBuilder.append(string + bb);
                }

                if (elementIndex < this.parameterTypes.size() - 1) {
                    stringBuilder.append(", ");
                }

                bb += getSlotSize((String) this.parameterTypes.get(elementIndex));
            } else {
                stringBuilder.append(ConstantPoolEntry.descriptorToJavaType((String) this.parameterTypes.get(elementIndex)));
                stringBuilder.append(elementIndex < this.parameterTypes.size() - 1 ? ", " : "");
            }
        }

        String modifierString;
        if (this.methodName.equals("<init>")) {
            modifierString = this.accessFlags.getModifierString() + this.getClassSimpleName() + "(" + stringBuilder + ")";
        } else if (this.methodName.equals("<clinit>")) {
            modifierString = this.accessFlags.getModifierString();
        } else {
            modifierString = this.accessFlags.getModifierString()
                    + ConstantPoolEntry.descriptorToJavaType(this.descriptor)
                    + " "
                    + this.methodName
                    + "("
                    + stringBuilder
                    + ")";
        }

        return modifierString;
    }

    public final boolean hadLocalVariableTable() {
        return this.hasStateFlag(16);
    }

    public abstract void collectReachableMethods(Set set1, Set set2) throws ZkmProcessingException;

    public AbstractMethodInfo(
            ClassFileComponent classFileComponent, ClassFileInputStream classFileInputStream, ListMultimap listMultimap, PrintWriter printWriter
    ) throws ClassFileFormatException, IOException {
        super(classFileComponent, classFileInputStream, listMultimap, printWriter);
        this.cacheNameAndDescriptor();
    }

    public FieldNameTypeSignature getNameTypeSignature() {
        return new FieldNameTypeSignature(this.getJvmName(), this.getDescriptor());
    }

    public String getReturnDescriptor() {
        int ba = this.descriptor.lastIndexOf(41);
        return this.descriptor.substring(ba + 1);
    }

    public final void setNameChanged(boolean nameChanged) {
        if (nameChanged) {
            this.setStateFlag(4);
        } else {
            this.clearStateFlag(4);
        }
    }

    public boolean isInstanceNonConstructor() {
        return !this.isStatic() && !this.isConstructor();
    }

    public boolean isSameMethod(AbstractMethodInfo abstractMethodInfo1) {
        VisitableNode[] visitableNodes1 = ClassFileComponent.getFlowGuardNodes();
        boolean sameMethod = this.methodName.equals(abstractMethodInfo1.methodName);
        if (visitableNodes1 != null) {
            if (sameMethod) {
                sameMethod = this.descriptor.equals(abstractMethodInfo1.descriptor);
                if (visitableNodes1 == null) {
                    return sameMethod;
                }

                if (sameMethod) {
                    sameMethod = this.getClassName().equals(abstractMethodInfo1.getClassName());
                    if (visitableNodes1 == null) {
                        return sameMethod;
                    }

                    if (sameMethod) {
                        return true;
                    }
                }
            }

            sameMethod = false;
        }

        return sameMethod;
    }

    @Override
    public final String getSourceName() {
        if (this.methodName.equals("<init>")) {
            return this.getClassSimpleName();
        } else {
            return this.methodName.equals("<clinit>") ? "static" : this.methodName;
        }
    }

    public final String formatNameAndParameters(boolean bl) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder1;
        char bc;
        if (bl) {
            stringBuilder.append(this.getJvmName());
            stringBuilder1 = stringBuilder;
            bc = '(';
        } else {
            stringBuilder.append(this.getSourceName());
            stringBuilder1 = stringBuilder;
            bc = '(';
        }

        stringBuilder1.append(bc);
        int parameterTypesSize = this.parameterTypes.size();

        for (int elementIndex = 0; elementIndex < parameterTypesSize; elementIndex++) {
            String descriptorValue = (String) this.parameterTypes.get(elementIndex);
            stringBuilder.append(ConstantPoolEntry.descriptorToJavaType(descriptorValue));
            if (elementIndex < parameterTypesSize - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(')');
        return stringBuilder.toString();
    }
}
