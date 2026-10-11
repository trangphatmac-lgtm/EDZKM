package com.zelix.klassmaster.proguard.mapping.parser.ast;

import com.zelix.klassmaster.proguard.ProGuardMappingNameSetter;
import com.zelix.klassmaster.proguard.ProGuardMappingTranslator;
import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingNode;
import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingSimpleNode;

import java.util.ArrayList;
import java.util.List;

public class ProGuardMappingASTQualifiedType extends ProGuardMappingSimpleNode {
    public int arrayDimensions;
    public List nameParts = new ArrayList();
    private static String arraySuffix;
    private static long separatorCharCode;

    public void addNamePart(Object object) {
        this.nameParts.add(object);
    }

    @Override
    public void translate(Object object, ProGuardMappingTranslator proGuardMappingTranslator) {
        ProGuardMappingNode proGuardMappingNode = (ProGuardMappingNode) object;
        super.translate(proGuardMappingNode, proGuardMappingTranslator);
        ProGuardMappingNameSetter proGuardMappingNameSetter = (ProGuardMappingNameSetter) proGuardMappingNode;
        StringBuilder stringBuilder = new StringBuilder();
        int namePartsSize = this.nameParts.size();

        for (int elementIndex = 0; elementIndex < namePartsSize; elementIndex++) {
            String string = (String) this.nameParts.get(elementIndex);
            stringBuilder.append(string);
            if (elementIndex < namePartsSize - 1) {
                stringBuilder.append((char) ((int) separatorCharCode));
            }
        }

        for (int i = 0; i < this.arrayDimensions; i++) {
            stringBuilder.append(arraySuffix);
        }

        proGuardMappingNameSetter.acceptTypeName(stringBuilder.toString());
    }

    public void incrementArrayDimensions() {
        this.arrayDimensions++;
    }

    public ProGuardMappingASTQualifiedType() {
        super(13);
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
        arraySuffix = "[]";
        separatorCharCode = 2516668711662780462L;
    }
}
