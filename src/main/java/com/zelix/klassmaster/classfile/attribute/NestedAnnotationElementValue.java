package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NestedAnnotationElementValue extends AnnotationElementValue {
    public AnnotationElementValue[] elementValues;
    private static final String INVALID_NESTED_MESSAGE = "Invalid nested annotation : ";

    @Override
    public void updateAfterClassRename(Object object, Object object1) {
        HashMap hashMap1 = (HashMap) object1;
        HashMap hashMap = (HashMap) object;
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].updateAfterClassRename(hashMap, hashMap1);
            bb = ++elementValueIndex;
        }
    }

    @Override
    public void updateAfterFieldRename() {
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].updateAfterFieldRename();
            bb = ++elementValueIndex;
        }
    }

    @Override
    public void collectReferencedMembers(Object object, Object object1, Object object2, Object object3) {
        Set set4 = (Set) object2;
        Set set2 = (Set) object;
        Set set1 = (Set) object3;
        Set set3 = (Set) object1;
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].collectReferencedMembers(set2, set3, set4, set1);
            bb = ++elementValueIndex;
        }
    }

    @Override
    public String getValueTypeDescriptor() {
        String valueTypeDescriptor = null;
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            if (elementValueIndex == 0) {
                valueTypeDescriptor = this.elementValues[elementValueIndex].getValueTypeDescriptor();
                if (valueTypeDescriptor == null) {
                    return null;
                }
            } else {
                String valueTypeDescriptorValue = this.elementValues[elementValueIndex].getValueTypeDescriptor();
                if (valueTypeDescriptorValue == null || !valueTypeDescriptor.equals(valueTypeDescriptorValue)) {
                    return null;
                }
            }

            bb = ++elementValueIndex;
        }

        return valueTypeDescriptor != null ? "[" + valueTypeDescriptor : null;
    }

    public AnnotationElementValue[] getElementValues() {
        return this.elementValues;
    }

    @Override
    public void collectReferencedClasses(Object object) {
        Set set1 = (Set) object;
        if (this.isValid()) {
            int elementValueIndex = 0;
            int elementValueIndexValue = elementValueIndex;

            for (AnnotationElementValue[] annotationElementValues = this.elementValues;
                 elementValueIndexValue < annotationElementValues.length;
                 annotationElementValues = this.elementValues
            ) {
                this.elementValues[elementValueIndex].collectReferencedClasses(set1);
                elementValueIndexValue = ++elementValueIndex;
            }
        }
    }

    @Override
    public void resolveReferences(Object object, Object object1, Object object2) throws ZkmException, IOException {
        ClassResolver classResolver1 = (ClassResolver) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object2;
        IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1 = (IgnoreMissingReferencesSpec) object1;
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].resolveReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
            bb = ++elementValueIndex;
        }
    }

    @Override
    public boolean isArrayOrClassValue() {
        return true;
    }

    @Override
    public void write(int ba, DataOutputStream dataOutputStream, int bb, int bc) throws IOException {
        long bi = ((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L;
        int bd = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) >>> 32);
        int be = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) << 32 >>> 48);
        int bf = (int) (bi << 48 >>> 48);
        dataOutputStream.writeByte(this.getTag());
        dataOutputStream.writeShort(this.elementValues.length);
        int elementValueIndex = 0;
        int bh = 0;
        AnnotationElementValue[] annotationElementValues = this.elementValues;

        while (true) {
            if (bh < annotationElementValues.length) {
                this.elementValues[elementValueIndex].write(bd, dataOutputStream, (char) be, (char) bf);
            } else if (bc > 0) {
                return;
            }

            bh = ++elementValueIndex;
            annotationElementValues = this.elementValues;
        }
    }

    @Override
    public String getClassValueName() {
        return null;
    }

    @Override
    public void collectUsedConstants(char bd, int ba, UsedConstantsCollector usedConstantsCollector, char be) {
        int elementValueIndex = 0;
        int bc = 0;
        AnnotationElementValue[] annotationElementValues = this.elementValues;

        while (true) {
            if (bc < annotationElementValues.length) {
                this.elementValues[elementValueIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
            } else if (ba > 0) {
                return;
            }

            bc = ++elementValueIndex;
            annotationElementValues = this.elementValues;
        }
    }

    @Override
    public boolean hasClassValue() {
        return false;
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        Map map1 = (Map) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        dataOutputStream.writeByte(this.getTag());
        dataOutputStream.writeShort(this.elementValues.length);
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].writeRemapped(dataOutputStream, map1, scriptEnvironment1);
            bb = ++elementValueIndex;
        }
    }

    @Override
    public void updateAfterMethodRename() {
        int elementValueIndex = 0;
        int bb = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bb < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            this.elementValues[elementValueIndex].updateAfterMethodRename();
            bb = ++elementValueIndex;
        }
    }

    @Override
    public int getByteLength() {
        int byteLength = 1;
        byteLength += 2;
        int elementValueIndex = 0;
        int bc = 0;

        for (AnnotationElementValue[] annotationElementValues = this.elementValues;
             bc < annotationElementValues.length;
             annotationElementValues = this.elementValues
        ) {
            byteLength += this.elementValues[elementValueIndex].getByteLength();
            bc = ++elementValueIndex;
        }

        return byteLength;
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
    }

    public NestedAnnotationElementValue(ClassFileComponent classFileComponent, int tag, ClassFileInputStream classFileInputStream, ListMultimap listMultimap) throws IOException {
        super(classFileComponent, tag);
        int bb = classFileInputStream.readUnsignedShort();
        this.elementValues = new AnnotationElementValue[bb];

        for (int elementValueIndex = 0; elementValueIndex < bb; elementValueIndex++) {
            AnnotationElementValue annotationElementValue = AnnotationElementValue.readElementValue(this, classFileInputStream, listMultimap);
            if (!annotationElementValue.isValid()) {
                this.setValid();
                this.setErrorMessage(INVALID_NESTED_MESSAGE + annotationElementValue.getErrorMessage());
                return;
            }

            this.elementValues[elementValueIndex] = annotationElementValue;
        }
    }
}
