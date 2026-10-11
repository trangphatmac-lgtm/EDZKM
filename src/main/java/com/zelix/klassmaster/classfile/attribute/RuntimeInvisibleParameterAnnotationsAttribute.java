package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.IOException;
import java.io.PrintWriter;

public class RuntimeInvisibleParameterAnnotationsAttribute extends ParameterAnnotationsAttribute {
    private static final String ATTRIBUTE_NAME = "RuntimeInvisibleParameterAnnotations";

    public RuntimeInvisibleParameterAnnotationsAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap, printWriter, ATTRIBUTE_NAME);
    }
}
