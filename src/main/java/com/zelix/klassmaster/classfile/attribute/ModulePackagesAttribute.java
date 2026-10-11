



package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ClassFileComponent;

import java.io.IOException;
import java.io.DataOutputStream;

import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.constpool.ResolvedPackageConstant;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;

public class ModulePackagesAttribute extends ParsedAttributeBase implements Utf8ConstantReplaceable {
    public final ResolvedPackageConstant[] packages;

    @Override
    public void collectUsedConstants(final char c, final int n, final UsedConstantsCollector usedConstantsCollector, final char c2) {
        this.nameConstant.registerUsage(usedConstantsCollector, this, this.getParent());
        if (super.valid) {
            final ResolvedPackageConstant[] packages = this.packages;
            for (int length = packages.length, packageIndex = 0; packageIndex < length; ++packageIndex) {
                packages[packageIndex].registerUsage(usedConstantsCollector, this, this.getParent());
            }
        }
    }

    @Override
    public void writeRemapped(final DataOutputStream dataOutputStream, final Object o, final Object o2) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.packages.length);
            final ResolvedPackageConstant[] packages = this.packages;
            for (int length = packages.length, packageIndex = 0; packageIndex < length; ++packageIndex) {
                dataOutputStream.writeShort(packages[packageIndex].getIndex());
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    public ModulePackagesAttribute(final ClassFileComponent classFileComponent, final int constantPoolIndex, final String attributeName, final ClassFileInputStream classFileInputStream, final ListMultimap listMultimap) throws ClassFileFormatException, IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        classFileInputStream.read(super.rawBytes = new byte[this.length]);
        final ClassFileInputStream fromBytes = ClassFileInputStream.fromBytes(super.rawBytes, false);
        Throwable throwableValue = null;
        try {
            final int unsignedShort = fromBytes.readUnsignedShort();
            this.packages = new ResolvedPackageConstant[unsignedShort];
            for (int packageIndex = 0; packageIndex < unsignedShort; ++packageIndex) {
                final int unsignedShort2 = fromBytes.readUnsignedShort();
                final ConstantPoolEntry constantPoolEntry = classFileComponent.getConstantPoolEntry(unsignedShort2);
                if (constantPoolEntry == null) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + unsignedShort2 + " : File is probably corrupt (AD)");
                }
                if (!(constantPoolEntry instanceof ResolvedPackageConstant)) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Invalid attribute : " + unsignedShort2 + " : '" + ((ResolvedPackageConstant) constantPoolEntry).getClass().getName() + "' : File is probably corrupt (AE)");
                }
                this.packages[packageIndex] = (ResolvedPackageConstant) constantPoolEntry;
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
        ResolvedPackageConstant[] array = this.packages;
        if (!ZkmUtils.allUnique(array)) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid 'Module' attribute in class '");
            stringBuilder.append(this.getLocationName());
            stringBuilder.append("' : Duplicate package names : ");
            int packageIndexValue = 0;
            int j = 0;
            ResolvedPackageConstant[] array2 = this.packages;
            while (j < array2.length) {
                stringBuilder.append(this.packages[packageIndexValue].getValueString());
                if (packageIndexValue < this.packages.length - 1) {
                    stringBuilder.append(", ");
                }
                packageIndexValue = (j = packageIndexValue + 1);
                array2 = this.packages;
            }
            ZkmAssert.assertTrue(false, new String[]{stringBuilder.toString()});
        }
    }

    @Override
    public void remapClassNames(final Object o, final Object o2, final Object o3, final Object o4) throws ZkmProcessingException {
    }

    @Override
    public void write(final DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.packages.length);
            final ResolvedPackageConstant[] packages = this.packages;
            for (int length = packages.length, packageIndex = 0; packageIndex < length; ++packageIndex) {
                dataOutputStream.writeShort(packages[packageIndex].getIndex());
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }
}
