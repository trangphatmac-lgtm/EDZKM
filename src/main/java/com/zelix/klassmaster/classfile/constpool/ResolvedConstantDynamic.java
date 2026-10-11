package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ResolvedConstantDynamic extends ResolvedDynamicRef {
    public static ConstantPoolTag TAG;
    public AbstractFieldInfo referencedField;

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Override
    public void collectReferencedProgramClasses(Set set1, Set set2, Set set3, Set set4) {
        super.collectReferencedProgramClasses(set1, set2, set3, set4);
        if (this.referencedField != null && this.referencedField.isProgramMember()) {
            set3.add((FieldInfo) this.referencedField);
            ClassFileBase classFileBase = this.referencedField.getOwningClass();
            if (this.referencedField.isStatic()) {
                set2.add((ProgramClass) classFileBase);
            } else {
                set1.add((ProgramClass) classFileBase);
            }
        }
    }

    @Override
    public ConstantPoolTag getTag() {
        return TAG;
    }

    public ResolvedConstantDynamic(int index, AbstractConstantPool abstractConstantPool, int bootstrapMethodIndex, ResolvedNameAndType resolvedNameAndType) {
        super(index, abstractConstantPool, bootstrapMethodIndex, resolvedNameAndType);
    }

    @Override
    public void collectReferencedClasses(Set set1, Set set2, Set set3, Set set4) {
        super.collectReferencedClasses(set1, set2, set3, set4);
        if (this.referencedField != null) {
            set3.add(this.referencedField);
            ClassFileBase classFileBase = this.referencedField.getOwningClass();
            if (!ClassHierarchyNode.isUnknownClass(classFileBase.getClassName())) {
                if (this.referencedField.isStatic()) {
                    set2.add((ProgramClass) classFileBase);
                } else {
                    set1.add(classFileBase);
                }
            }
        }
    }

    public void resolveTypeClass(ClassResolver classResolver1) throws ZkmException, IOException {
        super.nameAndType.getName();
        String returnPart = MethodSignature.getReturnPart(super.nameAndType.getDescriptor());
        if (returnPart.charAt(0) != '[' && !ConstantPoolEntry.isPrimitiveDescriptor(returnPart)) {
            String key = returnPart.substring(1, returnPart.length() - 1);
            ConstantPoolEntry[] constantPoolEntrys = this.bootstrapMethod.copyArguments();
            HashSet hashSet = ZkmUtils.createHashSet();

            for (int constantPoolEntryIndex = 0; constantPoolEntryIndex < constantPoolEntrys.length; constantPoolEntryIndex++) {
                if (constantPoolEntrys[constantPoolEntryIndex] instanceof ResolvedMethodType) {
                    hashSet.add(((ResolvedMethodType) constantPoolEntrys[constantPoolEntryIndex]).getDescriptor());
                } else if (constantPoolEntrys[constantPoolEntryIndex] instanceof ResolvedMethodHandleConstant) {
                    hashSet.add(((ResolvedMethodHandleConstant) constantPoolEntrys[constantPoolEntryIndex]).getMemberRef().getDescriptor());
                }
            }

            ClassFileBase classFileBase = this.getOwnerClass();
            Integer integer = classFileBase.hasReleaseVersion() ? classFileBase.getReleaseVersion() : null;
            classResolver1.getVersionedClass(
                    key, integer, "looking for class '" + ZkmUtils.slashesToDots(key) + "' which is referenced in class '" + this.getOwnerFilePath() + "'"
            );
        }
    }

    private static void staticInit() {
        TAG = ConstantPoolTag.CONSTANT_DYNAMIC;
    }
}
