package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.AccessFlags;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ResolvedConstantModule;
import com.zelix.klassmaster.classfile.constpool.ResolvedPackageConstant;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;

public class ModuleExportsEntry extends ClassFileComponent {
    public ResolvedPackageConstant exportedPackage;
    public final AccessFlags exportsFlags;
    public final ResolvedConstantModule[] exportsTo;

    @Override
    public void collectUsedConstants(char ba, int bb, UsedConstantsCollector usedConstantsCollector, char bc) {
        usedConstantsCollector.markUsed(this.exportedPackage, this, this.getParent());

        for (ResolvedConstantModule resolvedConstantModule : this.exportsTo) {
            usedConstantsCollector.markUsed(resolvedConstantModule, this, this.getParent());
        }
    }

    public void writeRemapped(DataOutputStream dataOutputStream) throws IOException {
        this.writeTo(dataOutputStream);
    }

    public ModuleExportsEntry(ClassFileComponent classFileComponent, ClassFileInputStream classFileInputStream) throws ClassFileFormatException, IOException {
        super(classFileComponent);
        int constantPoolIndex = classFileInputStream.readUnsignedShort();
        ConstantPoolEntry constantPoolEntry = classFileComponent.getConstantPoolEntry(constantPoolIndex);
        if (constantPoolEntry == null) {
            throw new ClassFileFormatException(
                    classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + constantPoolIndex + " : File is probably corrupt (P)"
            );
        }

        if (!(constantPoolEntry instanceof ResolvedPackageConstant)) {
            throw new ClassFileFormatException(
                    classFileComponent.getOwningClass().getLocationName()
                            + " : Invalid attribute : "
                            + constantPoolIndex
                            + " : '"
                            + constantPoolEntry.getClass().getName()
                            + "' : File is probably corrupt (Q)"
            );
        }

        this.exportedPackage = (ResolvedPackageConstant) constantPoolEntry;
        this.exportsFlags = new AccessFlags(this, classFileInputStream);
        int bb = classFileInputStream.readUnsignedShort();
        this.exportsTo = new ResolvedConstantModule[bb];

        for (int exportsToElementIndex = 0; exportsToElementIndex < bb; exportsToElementIndex++) {
            int constantPoolIndexValue = classFileInputStream.readUnsignedShort();
            constantPoolEntry = classFileComponent.getConstantPoolEntry(constantPoolIndexValue);
            if (constantPoolEntry == null) {
                throw new ClassFileFormatException(
                        classFileComponent.getOwningClass().getLocationName()
                                + " : Illegal constant pool index in attribute : "
                                + constantPoolIndexValue
                                + " : File is probably corrupt (R)"
                );
            }

            if (!(constantPoolEntry instanceof ResolvedConstantModule)) {
                throw new ClassFileFormatException(
                        classFileComponent.getOwningClass().getLocationName()
                                + " : Invalid attribute : "
                                + constantPoolIndexValue
                                + " : '"
                                + constantPoolEntry.getClass().getName()
                                + "' : File is probably corrupt (S)"
                );
            }

            this.exportsTo[exportsToElementIndex] = (ResolvedConstantModule) constantPoolEntry;
        }

        if (!ZkmUtils.allUnique(this.exportsTo)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid 'Module' attribute in class '");
            stringBuilder.append(this.getLocationName());
            stringBuilder.append("' : Duplicate 'export to' module names : ");
            int exportsToElementIndexValue = 0;
            int bf = 0;

            for (ResolvedConstantModule[] resolvedConstantModules = this.exportsTo; bf < resolvedConstantModules.length; resolvedConstantModules = this.exportsTo) {
                stringBuilder.append(this.exportsTo[exportsToElementIndexValue].getValueString());
                if (exportsToElementIndexValue < this.exportsTo.length - 1) {
                    stringBuilder.append(", ");
                }

                bf = ++exportsToElementIndexValue;
            }

            ZkmAssert.assertTrue(false, new String[]{stringBuilder.toString()});
        }
    }

    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.exportedPackage.getIndex());
        dataOutputStream.writeShort(this.exportsFlags.getFlags());
        dataOutputStream.writeShort(this.exportsTo.length);

        for (ResolvedConstantModule resolvedConstantModule : this.exportsTo) {
            dataOutputStream.writeShort(resolvedConstantModule.getIndex());
        }
    }
}
