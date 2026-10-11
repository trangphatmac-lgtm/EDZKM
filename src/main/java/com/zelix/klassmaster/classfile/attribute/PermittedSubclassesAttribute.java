


package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.attribute.ParsedAttributeBase;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class PermittedSubclassesAttribute
        extends ParsedAttributeBase
        implements Utf8ConstantReplaceable,
        ClassConstantReplaceable {
    public ResolvedClassConstant[] permittedSubclasses;

    public int removeTrimmedSubclasses(TrimProcessor trimProcessor) {
        if (this.valid) {
            ArrayList<ResolvedClassConstant> arrayList = new ArrayList<ResolvedClassConstant>(this.permittedSubclasses.length);
            for (ResolvedClassConstant resolvedClassConstant : this.permittedSubclasses) {
                ProgramClass programClass = ClassHierarchyNode.findProgramClass(resolvedClassConstant.getClassName());
                if (programClass != null && !trimProcessor.isClassOrOuterExcluded(programClass)) continue;
                arrayList.add(resolvedClassConstant);
            }
            if (arrayList.size() < this.permittedSubclasses.length) {
                this.permittedSubclasses = arrayList.toArray(new ResolvedClassConstant[arrayList.size()]);
            }
        }
        return this.permittedSubclasses.length;
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf82) {
        super.replaceUtf8Constant(constantUtf8, constantUtf82);
    }

    


    @Override
    public void replaceClassConstant(ResolvedClassConstant resolvedClassConstant, ResolvedClassConstant resolvedClassConstant2) {
        int permittedSubclasseIndex = 0;
        int n2 = 0;
        ResolvedClassConstant[] resolvedClassConstantArray = this.permittedSubclasses;
        while (n2 < resolvedClassConstantArray.length) {
            if (this.permittedSubclasses[permittedSubclasseIndex] == resolvedClassConstant) {
                this.permittedSubclasses[permittedSubclasseIndex] = resolvedClassConstant2;
                return;
            }
            n2 = ++permittedSubclasseIndex;
            resolvedClassConstantArray = this.permittedSubclasses;
        }
    }

    public void collectReferencedClasses(HashSet hashSet) {
        if (this.valid) {
            ResolvedClassConstant[] resolvedClassConstantArray = this.permittedSubclasses;
            int lengthValue = resolvedClassConstantArray.length;
            for (int resolvedClassConstantArrayElementIndex = 0; resolvedClassConstantArrayElementIndex < lengthValue; ++resolvedClassConstantArrayElementIndex) {
                ProgramClass programClass = ClassHierarchyNode.findProgramClass(resolvedClassConstantArray[resolvedClassConstantArrayElementIndex].getClassName());
                if (programClass == null) continue;
                hashSet.add(programClass);
            }
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object2) throws IOException {
        super.write(dataOutputStream);
        if (this.valid) {
            dataOutputStream.writeShort(this.permittedSubclasses.length);
            for (ResolvedClassConstant resolvedClassConstant : this.permittedSubclasses) {
                ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) ((Map) object).get(resolvedClassConstant);
                if (constantPoolEntry != null) {
                    dataOutputStream.writeShort(constantPoolEntry.getIndex());
                    continue;
                }
                dataOutputStream.writeShort(resolvedClassConstant.getIndex());
            }
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    @Override
    public void remapClassNames(Object object, Object object2, Object object3, Object object4) throws ZkmProcessingException {
    }

    @Override
    public void collectUsedConstants(char c, int n, UsedConstantsCollector usedConstantsCollector, char c2) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        ResolvedClassConstant[] resolvedClassConstantArray = this.permittedSubclasses;
        int lengthValue = resolvedClassConstantArray.length;
        for (int resolvedClassConstantArrayElementIndex = 0; resolvedClassConstantArrayElementIndex < lengthValue; ++resolvedClassConstantArrayElementIndex) {
            resolvedClassConstantArray[resolvedClassConstantArrayElementIndex].registerUsage(usedConstantsCollector, this, this);
        }
    }

    




    public PermittedSubclassesAttribute(ClassFileComponent classFileComponent, int constantPoolIndexArgument, String attributeName, ClassFileInputStream classFileInputStream, ListMultimap listMultimap, ListMultimap listMultimap2, PrintWriter printWriter) throws IOException {
        super(classFileComponent, constantPoolIndexArgument, attributeName, classFileInputStream, listMultimap);
        byte[] byArray = new byte[this.length];
        classFileInputStream.read(byArray);
        ClassFileInputStream classFileInputStream2 = ClassFileInputStream.fromBytes(byArray, false);
        try {
            int n2 = classFileInputStream2.readUnsignedShort();
            this.permittedSubclasses = new ResolvedClassConstant[n2];
            for (int permittedSubclasseIndex = 0; permittedSubclasseIndex < n2; ++permittedSubclasseIndex) {
                int constantPoolIndex = classFileInputStream2.readUnsignedShort();
                ConstantPoolEntry constantPoolEntry = this.getConstantPoolEntry(constantPoolIndex);
                if (constantPoolEntry instanceof ResolvedClassConstant) {
                    this.permittedSubclasses[permittedSubclasseIndex] = (ResolvedClassConstant) constantPoolEntry;
                    listMultimap2.addValue(this.permittedSubclasses[permittedSubclasseIndex], this);
                    continue;
                }
                this.valid = false;
                this.rawBytes = byArray;
                printWriter.println("ERROR: " + this.getDisplayLocationName() + " : " + this.getAttributeName() + " (A) Invalid permitted class index " + constantPoolIndex);
            }
        } catch (Throwable failure) {
            if (classFileInputStream2 != null) {
                try {
                    classFileInputStream2.close();
                    throw failure;
                } catch (Throwable closeFailure) {
                    // Preserve the existing self-suppression failure when close succeeds.
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
        if (classFileInputStream2 != null) {
            classFileInputStream2.close();
        }
        if (!this.valid) return;
        ((ClassFileBase) classFileComponent).setHasPermittedSubclasses();
    }

    @Override
    public int getLength() {
        int lengthValue = 2 + this.permittedSubclasses.length * 2;
        this.replaceLength(lengthValue);
        return lengthValue;
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (this.valid) {
            dataOutputStream.writeShort(this.permittedSubclasses.length);
            for (ResolvedClassConstant resolvedClassConstant : this.permittedSubclasses) {
                dataOutputStream.writeShort(resolvedClassConstant.getIndex());
            }
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }
}
