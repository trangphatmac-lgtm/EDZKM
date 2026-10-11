package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.IOException;

public abstract class ParsedAttributeBase extends Attribute {
    public byte[] rawBytes;
    public boolean valid = true;

    public ParsedAttributeBase(ClassFileComponent classFileComponent, ConstantUtf8 constantUtf8) {
        super(classFileComponent, constantUtf8, 2);
    }

    public final boolean isValid() {
        return this.valid;
    }

    public ParsedAttributeBase(
            ClassFileComponent classFileComponent, int constantPoolIndex, String attributeName, ClassFileInputStream classFileInputStream, ListMultimap listMultimap
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
    }
}
