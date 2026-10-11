



package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ClassFileComponent;

import java.io.IOException;
import java.util.Map;

import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;

import java.io.DataOutputStream;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;

import java.util.ArrayList;

import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;

public class NestMembersAttribute extends ParsedAttributeBase implements Utf8ConstantReplaceable, ClassConstantReplaceable {
    public ResolvedClassConstant[] memberClasses;

    @Override
    public int getLength() {
        return 2 + this.memberClasses.length * 2;
    }

    public int removeTrimmedMembers(final TrimProcessor trimProcessor) {
        if (super.valid) {
            final ArrayList list = new ArrayList(this.memberClasses.length);
            final ResolvedClassConstant[] memberClasses = this.memberClasses;
            for (int length = memberClasses.length, memberClasseIndex = 0; memberClasseIndex < length; ++memberClasseIndex) {
                final ResolvedClassConstant resolvedClassConstant = memberClasses[memberClasseIndex];
                final ProgramClass programClass = ClassHierarchyNode.findProgramClass(resolvedClassConstant.getClassName());
                if (programClass == null || trimProcessor.isClassOrOuterExcluded(programClass)) {
                    list.add(resolvedClassConstant);
                }
            }
            if (list.size() < this.memberClasses.length) {
                this.memberClasses = (ResolvedClassConstant[]) list.toArray(new ResolvedClassConstant[list.size()]);
            }
        }
        return this.memberClasses.length;
    }

    @Override
    public void writeRemapped(final DataOutputStream dataOutputStream, final Object o, final Object o2) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.memberClasses.length);
            final ResolvedClassConstant[] memberClasses = this.memberClasses;
            for (int length = memberClasses.length, memberClasseIndex = 0; memberClasseIndex < length; ++memberClasseIndex) {
                final ResolvedClassConstant resolvedClassConstant = memberClasses[memberClasseIndex];
                final ConstantPoolEntry constantPoolEntry = ((com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry) (((Map) o).get(resolvedClassConstant)));
                if (constantPoolEntry != null) {
                    dataOutputStream.writeShort(constantPoolEntry.getIndex());
                } else {
                    dataOutputStream.writeShort(resolvedClassConstant.getIndex());
                }
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    public NestMembersAttribute(final ClassFileComponent classFileComponent, final int constantPoolIndex, final String attributeName, final ClassFileInputStream classFileInputStream, final ListMultimap listMultimap) throws ClassFileFormatException, IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        classFileInputStream.read(super.rawBytes = new byte[this.length]);
        final ClassFileInputStream fromBytes = ClassFileInputStream.fromBytes(super.rawBytes, false);
        Throwable throwableValue = null;
        try {
            final int unsignedShort = fromBytes.readUnsignedShort();
            this.memberClasses = new ResolvedClassConstant[unsignedShort];
            for (int memberClasseIndex = 0; memberClasseIndex < unsignedShort; ++memberClasseIndex) {
                final int unsignedShort2 = fromBytes.readUnsignedShort();
                final ConstantPoolEntry constantPoolEntry = classFileComponent.getConstantPoolEntry(unsignedShort2);
                if (constantPoolEntry == null) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in '" + "NestMembers" + "' attribute : " + unsignedShort2 + " : '" + this.getLocationName() + "' : File is probably corrupt (AX)");
                }
                if (!(constantPoolEntry instanceof ResolvedClassConstant)) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Invalid '" + "NestMembers" + "' attribute : " + unsignedShort2 + " : '" + this.getLocationName() + "' : File is probably corrupt (AY)");
                }
                this.memberClasses[memberClasseIndex] = (ResolvedClassConstant) constantPoolEntry;
            }
            if (fromBytes != null) {
                fromBytes.close();
            }
        } catch (final Throwable throwable) {
            throwableValue = throwable;
            throw throwable;
        } finally {
            if (fromBytes != null) {
                if (throwableValue != null) {
                    try {
                        fromBytes.close();
                    } catch (final Throwable exception) {
                        throwableValue.addSuppressed(exception);
                    }
                } else {
                    fromBytes.close();
                }
            }
        }
        ResolvedClassConstant[] array = this.memberClasses;
        if (!ZkmUtils.allUnique(array)) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid 'NestMembers' attribute in class '");
            stringBuilder.append(this.getLocationName());
            stringBuilder.append("' : Duplicate class names : ");
            int memberClasseIndexValue = 0;
            int j = 0;
            ResolvedClassConstant[] array2 = this.memberClasses;
            while (j < array2.length) {
                stringBuilder.append(this.memberClasses[memberClasseIndexValue].getValueString());
                if (memberClasseIndexValue < this.memberClasses.length - 1) {
                    stringBuilder.append(", ");
                }
                memberClasseIndexValue = (j = memberClasseIndexValue + 1);
                array2 = this.memberClasses;
            }
            ZkmAssert.assertTrue(false, new String[]{stringBuilder.toString()});
        }
    }

    @Override
    public void replaceClassConstant(final ResolvedClassConstant resolvedClassConstant, final ResolvedClassConstant resolvedClassConstant2) {
        int memberClasseIndex = 0;
        int i = 0;
        ResolvedClassConstant[] array = this.memberClasses;
        while (i < array.length) {
            if (this.memberClasses[memberClasseIndex] == resolvedClassConstant) {
                this.memberClasses[memberClasseIndex] = resolvedClassConstant2;
            }
            memberClasseIndex = (i = memberClasseIndex + 1);
            array = this.memberClasses;
        }
    }

    @Override
    public void collectUsedConstants(final char c, final int n, final UsedConstantsCollector usedConstantsCollector, final char c2) {
        this.nameConstant.registerUsage(usedConstantsCollector, this, this.getParent());
        if (super.valid) {
            int i;
            int memberClasseIndex = i = 0;
            ResolvedClassConstant[] array = this.memberClasses;
            while (i < array.length) {
                this.memberClasses[memberClasseIndex].registerUsage(usedConstantsCollector, this, this.getParent());
                memberClasseIndex = (i = memberClasseIndex + 1);
                array = this.memberClasses;
            }
        }
    }

    @Override
    public void write(final DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.memberClasses.length);
            final ResolvedClassConstant[] memberClasses = this.memberClasses;
            for (int length = memberClasses.length, memberClasseIndex = 0; memberClasseIndex < length; ++memberClasseIndex) {
                dataOutputStream.writeShort(memberClasses[memberClasseIndex].getIndex());
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void remapClassNames(final Object o, final Object o2, final Object o3, final Object o4) throws ZkmProcessingException {
    }
}
