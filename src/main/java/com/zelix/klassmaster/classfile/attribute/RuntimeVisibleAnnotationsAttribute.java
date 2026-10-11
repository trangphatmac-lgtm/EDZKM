package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.IOException;
import java.io.PrintWriter;

public class RuntimeVisibleAnnotationsAttribute extends AnnotationsAttribute {
    private static final String ATTRIBUTE_NAME = "RuntimeVisibleAnnotations";

    public RuntimeVisibleAnnotationsAttribute(
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
