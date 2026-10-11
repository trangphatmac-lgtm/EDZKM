package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.MethodArgsPattern;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.TypeTextHolder;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ZkmScriptASTMethodArguments extends ZkmScriptSimpleNode implements TypeTextHolder {
    public ArrayList argumentTypes = new ArrayList();
    public ArrayList argumentAnnotations = new ArrayList();

    public Pattern buildArgsRegex() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\\(");
        Iterator iterator = this.argumentTypes.iterator();

        while (iterator.hasNext()) {
            String string = (String) iterator.next();
            if (string.equals("*")) {
                if (this.argumentTypes.size() == 1) {
                    stringBuilder.append(".*");
                } else {
                    stringBuilder.append("((\\[*)(([BCDFIJSZ]|L[^;]+;)))");
                }
            } else if (string.equals("?")) {
                stringBuilder.append("((\\[*)(([BCDFIJSZ]|L[^;]+;)))");
            } else {
                stringBuilder.append(escapeRegexChars(string));
            }
        }

        stringBuilder.append("\\)");
        return Pattern.compile(stringBuilder.toString());
    }

    public String buildArgsDescriptor() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append('(');
        StringBuffer stringBuffer1;
        if (this.argumentTypes != null) {
            int argumentTypesSize = this.argumentTypes.size();

            for (int elementIndex = 0; elementIndex < argumentTypesSize; elementIndex++) {
                String string = (String) this.argumentTypes.get(elementIndex);
                stringBuffer.append(string);
            }

            stringBuffer1 = stringBuffer;
            byte bd = 41;
        } else {
            stringBuffer1 = stringBuffer;
            byte bc = 41;
        }

        stringBuffer1.append(')');
        return stringBuffer.toString();
    }

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ZkmScriptNode zkmScriptNode = (ZkmScriptNode) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }

        MethodSignatureNodeBase methodSignatureNodeBase = (MethodSignatureNodeBase) zkmScriptNode;
        methodSignatureNodeBase.setParameterAnnotations(((com.zelix.klassmaster.script.parser.ast.ASTComplexAnnotationSpecifier[]) (this.argumentAnnotations.toArray(new ASTComplexAnnotationSpecifier[this.argumentAnnotations.size()]))));
        methodSignatureNodeBase.setArgsPattern(new MethodArgsPattern(this.argumentTypes.size(), this.buildArgsDescriptor(), this.buildArgsRegex()));
    }

    public ZkmScriptASTMethodArguments() {
        super(213);
    }

    @Override
    public void setTypeText(Object typeText) {
        this.argumentTypes.add(typeText);
    }

    public static String escapeRegexChars(String string) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int characterIndex = 0; characterIndex < string.length(); characterIndex++) {
            char ba = string.charAt(characterIndex);
            switch (ba) {
                case '$':
                    stringBuilder.append("\\$");
                    break;
                case '[':
                    stringBuilder.append("\\[");
                    break;
                default:
                    stringBuilder.append(ba);
            }
        }

        return stringBuilder.toString();
    }

    public void addArgumentAnnotation(Object object) {
        this.argumentAnnotations.add(object);
    }
}
