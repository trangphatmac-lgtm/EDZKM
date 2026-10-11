package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ResolvedConstantModule;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleHashEntry extends ClassFileComponent {
    public ResolvedConstantModule module;
    public final byte[] hash;
    public boolean valid;

    @Override
    public void collectUsedConstants(char ba, int bb, UsedConstantsCollector usedConstantsCollector, char bc) {
        this.module.registerUsage(usedConstantsCollector, this, this.getParent());
    }

    public ModuleHashEntry(ClassFileComponent classFileComponent, ClassFileInputStream classFileInputStream) throws ClassFileFormatException, IOException {
        super(classFileComponent);
        int constantPoolIndex = classFileInputStream.readUnsignedShort();
        ConstantPoolEntry constantPoolEntry = classFileComponent.getConstantPoolEntry(constantPoolIndex);
        if (constantPoolEntry == null) {
            throw new ClassFileFormatException(
                    classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + constantPoolIndex + " : File is probably corrupt (T)"
            );
        }

        if (!(constantPoolEntry instanceof ResolvedConstantModule)) {
            throw new ClassFileFormatException(
                    classFileComponent.getOwningClass().getLocationName()
                            + " : Invalid attribute : "
                            + constantPoolIndex
                            + " : '"
                            + constantPoolEntry.getClass().getName()
                            + "' : File is probably corrupt (U)"
            );
        }

        this.module = (ResolvedConstantModule) constantPoolEntry;
        int bb = classFileInputStream.readUnsignedShort();
        this.hash = new byte[bb];

        for (int hashElementIndex = 0; hashElementIndex < bb; hashElementIndex++) {
            this.hash[hashElementIndex] = (byte) classFileInputStream.readUnsignedByte();
        }

        this.valid = true;
    }

    public void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException {
        ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) map1.get(this.module);
        DataOutputStream dataOutputStream1;
        byte[] hashValue;
        if (constantPoolEntry != null) {
            dataOutputStream.writeShort(constantPoolEntry.getIndex());
            dataOutputStream1 = dataOutputStream;
            hashValue = this.hash;
        } else {
            dataOutputStream.writeShort(this.module.getIndex());
            dataOutputStream1 = dataOutputStream;
            hashValue = this.hash;
        }

        dataOutputStream1.writeShort(hashValue.length);
        int hashElementIndex = 0;
        int bb = 0;

        for (byte[] hash = this.hash; bb < hash.length; hash = this.hash) {
            dataOutputStream.writeByte(this.hash[hashElementIndex]);
            bb = ++hashElementIndex;
        }
    }

    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.module.getIndex());
        dataOutputStream.writeShort(this.hash.length);
        int hashElementIndex = 0;
        int bb = 0;

        for (byte[] hash = this.hash; bb < hash.length; hash = this.hash) {
            dataOutputStream.writeByte(this.hash[hashElementIndex]);
            bb = ++hashElementIndex;
        }
    }
}
