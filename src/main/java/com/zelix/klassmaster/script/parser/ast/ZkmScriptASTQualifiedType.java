package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.TypeTextHolder;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ZkmScriptASTQualifiedType extends ZkmScriptSimpleNode {
    public int arrayDimensions;
    public static Map primitiveDescriptors;
    public ArrayList nameParts = new ArrayList();

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ZkmScriptNode zkmScriptNode = (ZkmScriptNode) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }

        TypeTextHolder typeTextHolder = (TypeTextHolder) zkmScriptNode;
        typeTextHolder.setTypeText(this.toDescriptor());
    }

    public ZkmScriptASTQualifiedType() {
        super(215);
    }

    public void incrementArrayDimensions() {
        this.arrayDimensions++;
    }

    public String toDescriptor() {
        StringBuilder stringBuilder = new StringBuilder();
        int namePartsSize = this.nameParts.size();
        if (namePartsSize != 0) {
            int bb = 0;
            int bd = bb;

            for (int arrayDimensionsValue = this.arrayDimensions; bd < arrayDimensionsValue; arrayDimensionsValue = this.arrayDimensions) {
                stringBuilder.append("[");
                bd = ++bb;
            }

            if (namePartsSize == 1) {
                String key = (String) this.nameParts.get(0);
                String string = (String) primitiveDescriptors.get(key);
                if (string != null) {
                    stringBuilder.append(string);
                } else {
                    stringBuilder.append("L");
                    stringBuilder.append(key);
                    stringBuilder.append(";");
                }
            } else {
                stringBuilder.append("L");

                for (int elementIndex = 0; elementIndex < namePartsSize; elementIndex++) {
                    String string2 = (String) this.nameParts.get(elementIndex);
                    if (elementIndex > 0) {
                        stringBuilder.append("/");
                    }

                    stringBuilder.append(string2);
                }

                stringBuilder.append(";");
            }
        }

        return stringBuilder.toString();
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public void addNamePart(Object object) {
        this.nameParts.add(object);
    }

    private static void staticInit() {
        primitiveDescriptors = ZkmUtils.createHashMap(23);
        primitiveDescriptors.put("byte", "B");
        primitiveDescriptors.put("char", "C");
        primitiveDescriptors.put("double", "D");
        primitiveDescriptors.put("float", "F");
        primitiveDescriptors.put("int", "I");
        primitiveDescriptors.put("long", "J");
        primitiveDescriptors.put("short", "S");
        primitiveDescriptors.put("boolean", "Z");
    }
}
