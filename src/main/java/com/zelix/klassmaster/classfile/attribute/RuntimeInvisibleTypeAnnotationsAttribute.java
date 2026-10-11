package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.IOException;
import java.io.PrintWriter;

public class RuntimeInvisibleTypeAnnotationsAttribute extends TypeAnnotationsAttribute {
    private static final String ATTRIBUTE_NAME = "RuntimeInvisibleTypeAnnotations";

    public RuntimeInvisibleTypeAnnotationsAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap, listMultimap1, printWriter, ATTRIBUTE_NAME);
    }
}
