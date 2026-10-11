package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.SetMultiMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationInfo extends ClassFileComponent implements AnnotationElementValueType, Utf8ConstantReplaceable {
    public int arrayDimensions;
    public ClassFileBase annotationClass;
    public boolean valid = true;
    public TypeAnnotation typeTarget;
    public String errorMessage;
    public ConstantUtf8 typeConstant;
    public int elementCount;
    public AnnotationElementPair[] elements;

    @Override
    public void write(int ba, DataOutputStream dataOutputStream, int bb, int bc) throws IOException {
        long bm = (long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48;
        long bn = ((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L;
        int bd = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) >>> 32);
        int be = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) << 32 >>> 48);
        int bf = (int) (bn << 48 >>> 48);
        bn = bm ^ 0L;
        int bg = (int) ((bm ^ 0L) >>> 32);
        int bh = (int) ((bm ^ 0L) << 32 >>> 48);
        int bi = (int) (bn << 48 >>> 48);
        this.typeTarget.write(bg, dataOutputStream, (char) bh, (char) bi);
        dataOutputStream.writeShort(this.typeConstant.getIndex());
        dataOutputStream.writeShort(this.elementCount);
        int elementIndex = 0;
        int bk = 0;
        int elementCount = this.elementCount;

        while (true) {
            if (bk < elementCount) {
                this.elements[elementIndex].write(bd, dataOutputStream, (char) be, (char) bf);
            } else if (bb > 0) {
                return;
            }

            bk = ++elementIndex;
            elementCount = this.elementCount;
        }
    }

    @Override
    public void updateAfterFieldRename() {
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue = this.elementCount; bb < elementCountValue; elementCountValue = this.elementCount) {
            this.elements[elementIndex].updateAfterFieldRename();
            bb = ++elementIndex;
        }
    }

    public AnnotationInfo(
            TypeAnnotationsAttribute typeAnnotationsAttribute, ClassFileInputStream classFileInputStream, ListMultimap listMultimap, ListMultimap listMultimap1
    ) throws IOException {
        super(typeAnnotationsAttribute);
        this.typeTarget = new TypeAnnotation(this, classFileInputStream, listMultimap1);
        if (!this.typeTarget.isValid()) {
            this.valid = false;
            this.errorMessage = this.typeTarget.getErrorMessage();
        } else {
            int constantPoolIndex = classFileInputStream.readUnsignedShort();
            ConstantPoolEntry constantPoolEntry = this.getConstantPoolEntry(constantPoolIndex);
            if (constantPoolEntry != null) {
                if (constantPoolEntry instanceof ConstantUtf8) {
                    this.typeConstant = (ConstantUtf8) constantPoolEntry;
                    String value = this.typeConstant.getValue();
                    if (value.startsWith("L")) {
                        if (value.endsWith(";")) {
                            listMultimap.addValue(this.typeConstant, this);
                            this.elementCount = classFileInputStream.readUnsignedShort();
                            this.elements = new AnnotationElementPair[this.elementCount];
                            int elementIndex = 0;
                            int bc = 0;

                            for (int elementCountValue = this.elementCount; bc < elementCountValue; elementCountValue = this.elementCount) {
                                this.elements[elementIndex] = new AnnotationElementPair(this, classFileInputStream, listMultimap);
                                if (!this.elements[elementIndex].isValid()) {
                                    this.valid = false;
                                    this.errorMessage = this.elements[elementIndex].getErrorMessage();
                                    return;
                                }

                                bc = ++elementIndex;
                            }

                            return;
                        }

                        this.valid = false;
                    } else {
                        this.valid = false;
                    }

                    this.errorMessage = "Invalid annotation type '" + value + "'";
                    return;
                }

                this.valid = false;
            } else {
                this.valid = false;
            }

            this.errorMessage = "Invalid type index : "
                    + constantPoolIndex
                    + (constantPoolEntry != null ? " : tag=" + constantPoolEntry.getTag() + " : \"" + constantPoolEntry.getDisplayString() + "\"" : "");
        }
    }

    public void resolveReferences(ClassResolver classResolver1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        MutableInt mutableInt = new MutableInt(0);
        String key = ClassFileBase.extractClassName(this.typeConstant.getValue(), mutableInt);
        if (key != null) {
            ClassFileBase classFileBase = this.getOwningClass();
            Integer integer = classFileBase.hasReleaseVersion() ? classFileBase.getReleaseVersion() : null;
            this.annotationClass = classResolver1.getVersionedClass(
                    key, integer, "analyzing annotations in class '" + this.getDisplayLocationName() + "' (D)", ignoreMissingReferencesSpec1
            );
        }

        this.arrayDimensions = mutableInt.getValue();
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue = this.elementCount; bb < elementCountValue; elementCountValue = this.elementCount) {
            this.elements[elementIndex].resolveReferences(this.annotationClass, classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
            bb = ++elementIndex;
        }
    }

    public boolean removeDeadLabelTargets(HashSet hashSet, SetMultiMap setMultiMap) {
        return this.typeTarget.unlinkRemovedLabels(hashSet, setMultiMap);
    }

    @Override
    public void collectUsedConstants(char ba, int be, UsedConstantsCollector usedConstantsCollector, char bf) {
        ClassFileComponent.getFlowGuardNodes();
        this.typeConstant.registerUsage(usedConstantsCollector, this, this.getParent());
        int elementIndex = 0;
        int elementIndexValue = 0;

        for (int elementCountValue = this.elementCount; elementIndexValue < elementCountValue; elementCountValue = this.elementCount) {
            if (ba >= 0) {
                this.elements[elementIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
                elementIndex++;
            }

            elementIndexValue = elementIndex;
        }

        this.typeTarget.collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        Map map1 = (Map) object;
        this.typeTarget.writeRemapped(dataOutputStream, map1, scriptEnvironment1);
        ConstantUtf8 constantUtf8 = (ConstantUtf8) map1.get(this.typeConstant);
        DataOutputStream dataOutputStream1;
        int elementCountValue;
        if (constantUtf8 != null) {
            dataOutputStream.writeShort(constantUtf8.getIndex());
            dataOutputStream1 = dataOutputStream;
            elementCountValue = this.elementCount;
        } else {
            dataOutputStream.writeShort(this.typeConstant.getIndex());
            dataOutputStream1 = dataOutputStream;
            elementCountValue = this.elementCount;
        }

        dataOutputStream1.writeShort(elementCountValue);
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue2 = this.elementCount; bb < elementCountValue2; elementCountValue2 = this.elementCount) {
            this.elements[elementIndex].writeRemapped(dataOutputStream, map1, scriptEnvironment1);
            bb = ++elementIndex;
        }
    }

    @Override
    public int getByteLength() {
        int byteLength = this.typeTarget.getByteLength();
        byteLength += 4;
        int elementIndex = 0;
        int bc = 0;

        for (int elementCountValue = this.elementCount; bc < elementCountValue; elementCountValue = this.elementCount) {
            byteLength += this.elements[elementIndex].getByteLength();
            bc = ++elementIndex;
        }

        return byteLength;
    }

    public ClassFileBase getAnnotationClass() {
        return this.annotationClass;
    }

    @Override
    public void updateAfterMethodRename() {
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue = this.elementCount; bb < elementCountValue; elementCountValue = this.elementCount) {
            this.elements[elementIndex].updateAfterMethodRename();
            bb = ++elementIndex;
        }
    }

    public boolean referencesTypeArguments() {
        return this.typeTarget.requiresSignature();
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public void updateAfterClassRename(Object object, Object object1) {
        HashMap hashMap = (HashMap) object;
        HashMap hashMap1 = (HashMap) object1;
        if (this.annotationClass != null) {
            String string = ClassFileBase.extractClassName(this.typeConstant.getValue());
            if (!this.annotationClass.getClassName().equals(string)) {
                String value = ClassFileBase.toTypeDescriptor(this.annotationClass.getClassName(), this.arrayDimensions);
                this.typeConstant.setValue(value);
            }
        }

        int elementIndex = 0;
        int elementIndexValue = elementIndex;

        for (int elementCountValue = this.elementCount; elementIndexValue < elementCountValue; elementCountValue = this.elementCount) {
            this.elements[elementIndex].updateAfterClassRename(hashMap, hashMap1);
            elementIndexValue = ++elementIndex;
        }
    }

    @Override
    public void collectReferencedClasses(Object object) {
        Set set1 = (Set) object;
        if (this.valid) {
            if (this.annotationClass != null) {
                set1.add(this.annotationClass);
            }

            int elementIndex = 0;
            int bb = 0;

            for (int elementCountValue = this.elementCount; bb < elementCountValue; elementCountValue = this.elementCount) {
                this.elements[elementIndex].collectReferencedClasses(set1);
                bb = ++elementIndex;
            }

            this.typeTarget.collectReferencedClasses(set1);
        }
    }

    public void collectLabelReferences(SetMultiMap setMultiMap) {
        this.typeTarget.registerLabelTargets(setMultiMap);
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        if (this.typeConstant == constantUtf8) {
            this.typeConstant = constantUtf81;
        }
    }

    public boolean isThrowsTarget() {
        return this.typeTarget.isThrowsTarget();
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void collectReferencedMembers(Set set1, Set set2, Set set3, Set set4) {
        if (this.annotationClass != null && this.annotationClass.isProgramClass()) {
            set1.add((ProgramClass) this.annotationClass);
        }

        int elementIndex = 0;
        int elementIndexValue = elementIndex;

        for (int elementCountValue = this.elementCount; elementIndexValue < elementCountValue; elementCountValue = this.elementCount) {
            this.elements[elementIndex].collectReferencedMembers(this.annotationClass, set1, set2, set3, set4);
            elementIndexValue = ++elementIndex;
        }

        this.typeTarget.finishReferenceCollection();
    }
}
