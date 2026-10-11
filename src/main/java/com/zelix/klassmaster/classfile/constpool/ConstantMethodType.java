package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantMethodType extends MethodTypeConstantBase implements ResolvableConstant {
    public final int descriptorIndex;

    @Override
    public boolean isUnresolved() {
        return true;
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(MethodTypeConstantBase.TAG.getTagValue());
        dataOutputStream.writeShort(this.descriptorIndex);
    }

    public ConstantMethodType(int indexArgument, ClassFileInputStream classFileInputStream, AbstractConstantPool abstractConstantPool) throws IOException {
        super(indexArgument, abstractConstantPool);
        this.descriptorIndex = classFileInputStream.readUnsignedShort();
    }

    @Override
    public ConstantPoolEntry resolve(ListMultimap listMultimap, ListMultimap listMultimap1, ListMultimap listMultimap2, ListMultimap listMultimap3) throws ClassFileFormatException {
        return this.resolveMethodType(listMultimap);
    }

    public ResolvedMethodType resolveMethodType(ListMultimap listMultimap) throws ClassFileFormatException {
        try {
            ConstantPoolEntry constantPoolEntry = this.constantPool.getConstantPoolEntry(this.descriptorIndex);
            if (constantPoolEntry instanceof ConstantUtf8) {
                ResolvedMethodType resolvedMethodType = new ResolvedMethodType(this.getIndex(), this.constantPool, (ConstantUtf8) constantPoolEntry);
                if (listMultimap != null) {
                    listMultimap.addValue((ConstantUtf8) constantPoolEntry, resolvedMethodType);
                }

                String descriptorValue = resolvedMethodType.getDescriptor();
                if (descriptorValue.startsWith("(") && descriptorValue.indexOf(")") != -1 && descriptorValue.indexOf(")") < descriptorValue.length() - 1) {
                    return resolvedMethodType;
                } else {
                    throw new ClassFileFormatException(
                            this.constantPool.getClassLocationDescription()
                                    + " : "
                                    + "Invalid method descriptor"
                                    + " : "
                                    + descriptorValue
                                    + " : "
                                    + ConstantPoolEntry.getTagName(this.getTag())
                                    + " entry"
                    );
                }
            } else {
                throw new ClassFileFormatException(
                        this.constantPool.getClassLocationDescription()
                                + " : "
                                + "Invalid Constant Pool UTF8 index"
                                + " : "
                                + this.descriptorIndex
                                + " : "
                                + ConstantPoolEntry.getTagName(this.getTag())
                                + " entry"
                );
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            String string = this.constantPool.getClassLocationDescription()
                    + " : "
                    + "Constant Pool index out of range"
                    + " : "
                    + arrayIndexOutOfBoundsException.getMessage()
                    + " : "
                    + ConstantPoolEntry.getTagName(this.getTag())
                    + " entry";
            throw new ClassFileFormatException(string);
        }
    }
}
