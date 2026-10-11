package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.exclude.AccessFlagsSpec;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.ZkmScriptTokenMgrError;
import com.zelix.klassmaster.script.parser.ZkmScriptParseException;
import com.zelix.klassmaster.script.parser.ZkmScriptParser;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.script.parser.ast.ASTComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.parser.ast.ASTContainingClause;
import com.zelix.klassmaster.script.parser.ast.ASTRenameFilterParameter;
import com.zelix.klassmaster.util.ChangeObservable;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class RenameFilterModel extends ChangeObservable {
    public List interfaceNames = new ArrayList();
    public List parameterTypes = new ArrayList();
    public List throwsNames = new ArrayList();
    public AccessFlagsSpec classAccessFlags;
    public AccessFlagsSpec memberAccessFlags;
    public String archivePattern;
    public List packageNames;
    public boolean packageNameOnly;
    public boolean excludeContainingPackage;
    public List searchNames;
    public String className;
    public boolean excludeContainingClass;
    public ASTContainingClause containingClause;
    public String superclassName;
    public String classAnnotation;
    public String fieldName;
    public String fieldType;
    public String methodName;
    public String linkedMethodName;
    public String memberAnnotation;
    public boolean includeSignatureClasses;
    public int parameterKind;
    public ScriptEnvironment scriptEnvironment;

    public boolean isLinkedClass() {
        return this.parameterKind == 2 && this.className.contains("<link>");
    }

    public void setMemberProtected(Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.requireProtected();
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotTransient(boolean memberNotTransient) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(2);
        }

        this.memberAccessFlags.setTransientForbidden(memberNotTransient);
        this.setChanged();
        this.notifyObservers();
    }

    public String getLinkPrefix() {
        return this.className.substring(0, this.className.indexOf("<link>"));
    }

    public void setMemberTransient(boolean memberTransient) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(2);
        }

        this.memberAccessFlags.setTransientRequired(memberTransient);
        this.setChanged();
        this.notifyObservers();
    }

    public void setClassNotFinal(boolean classNotFinal) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setFinalForbidden(classNotFinal);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberStatic(boolean staticRequired, Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.setStaticRequired(staticRequired);
        this.setChanged();
        this.notifyObservers();
    }

    public void setFieldType(String fieldTypeArgument) throws ZkmException, IOException {
        this.fieldType = fieldTypeArgument;
        this.setChanged();
        this.notifyObservers();
    }

    public void setMethodName(String methodNameArgument) throws ZkmException, IOException {
        this.methodName = methodNameArgument;
        this.setChanged();
        this.notifyObservers();
    }

    public void setClassAnnotation(String classAnnotationArgument) throws ZkmException, IOException {
        String classAnnotationValue = classAnnotationArgument;
        if (classAnnotationValue.startsWith("@")) {
            classAnnotationValue = classAnnotationValue.substring(1);
            this.classAnnotation = classAnnotationValue;
        } else {
            this.classAnnotation = classAnnotationValue;
        }

        this.setChanged();
        this.notifyObservers();
    }

    public String getPackagePattern() {
        StringBuffer stringBuffer = new StringBuffer();
        int packageNamesSize = this.packageNames.size();
        if (packageNamesSize > 0) {
            for (int elementIndex = 0; elementIndex < packageNamesSize; elementIndex++) {
                stringBuffer.append((String) this.packageNames.get(elementIndex) + ".");
            }
        }

        return stringBuffer.toString();
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void setPackagePattern(String packagePatternArgument) throws ZkmException, IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(packagePatternArgument, ".");
        this.packageNames = new ArrayList();

        while (stringTokenizer.hasMoreTokens()) {
            String string1 = stringTokenizer.nextToken().trim();
            this.packageNames.add(string1);
        }

        this.setChanged();
        this.notifyObservers();
    }

    public String getFieldType() {
        return this.fieldType;
    }

    public String getParameterTypesText() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.parameterTypes != null) {
            int elementIndex = 0;
            int bb = 0;

            for (List list1 = this.parameterTypes; bb < list1.size(); list1 = this.parameterTypes) {
                stringBuffer.append((String) this.parameterTypes.get(elementIndex));
                if (elementIndex < this.parameterTypes.size() - 1) {
                    stringBuffer.append(", ");
                }

                bb = ++elementIndex;
            }
        }

        return stringBuffer.toString();
    }

    public void setClassPublic() throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.requirePublic();
        this.setChanged();
        this.notifyObservers();
    }

    public void setInterfaceNames(String interfaceNamesArgument) throws ZkmException, IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(interfaceNamesArgument, ",");
        this.interfaceNames = new ArrayList();

        while (stringTokenizer.hasMoreTokens()) {
            this.interfaceNames.add(stringTokenizer.nextToken().trim());
        }

        this.setChanged();
        this.notifyObservers();
    }

    public void setClassNotAbstract(boolean classNotAbstract) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setAbstractForbidden(classNotAbstract);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberFinal(boolean finalRequired, Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.setFinalRequired(finalRequired);
        this.setChanged();
        this.notifyObservers();
    }

    public String getThrowsText() {
        if (this.throwsNames != null && this.throwsNames.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            int elementIndex = 0;
            int bb = 0;

            for (List list1 = this.throwsNames; bb < list1.size(); list1 = this.throwsNames) {
                stringBuffer.append((String) this.throwsNames.get(elementIndex));
                if (elementIndex < this.throwsNames.size() - 1) {
                    stringBuffer.append(", ");
                }

                bb = ++elementIndex;
            }

            return stringBuffer.toString();
        } else {
            return "";
        }
    }

    public void setMemberNotSynchronized(boolean memberNotSynchronized) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setSynchronizedForbidden(memberNotSynchronized);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberPrivate(Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.requirePrivate();
        this.setChanged();
        this.notifyObservers();
    }

    public ScriptEnvironment getScriptEnvironment() {
        return this.scriptEnvironment;
    }

    public String getSuperclassName() {
        return this.superclassName != null ? this.superclassName : "";
    }

    public String getTextBeforeLink(String string) {
        return string.substring(0, string.indexOf("<link>"));
    }

    public static void executeFilterParameter(ScriptEnvironment scriptEnvironment1, String string) throws ZkmException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(string + ";"));
        ZkmScriptParser zkmScriptParser = new ZkmScriptParser(bufferedReader);

        try {
            zkmScriptParser.SingleRenameFilterParameter().execute(null, scriptEnvironment1);
        } catch (ZkmScriptParseException zkmScriptParseException) {
            throw new ZkmProcessingException(zkmScriptParseException.getMessage());
        } catch (ZkmScriptTokenMgrError zkmScriptTokenMgrError) {
            throw new ZkmProcessingException(zkmScriptTokenMgrError.getMessage());
        }
    }

    public void setMemberAbstract(boolean memberAbstract) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setAbstractRequired(memberAbstract);
        this.setChanged();
        this.notifyObservers();
    }

    public boolean isExcludeContainingPackage() {
        return this.excludeContainingPackage;
    }

    public void setExcludeContainingPackage(boolean excludeContainingPackage) throws ZkmException, IOException {
        this.excludeContainingPackage = excludeContainingPackage;
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberAnnotation(String memberAnnotationArgument) throws ZkmException, IOException {
        String memberAnnotationValue = memberAnnotationArgument;
        if (memberAnnotationValue.startsWith("@")) {
            memberAnnotationValue = memberAnnotationValue.substring(1);
            this.memberAnnotation = memberAnnotationValue;
        } else {
            this.memberAnnotation = memberAnnotationValue;
        }

        this.setChanged();
        this.notifyObservers();
    }

    public void setLinkedClassSuffix(String linkedClassSuffix) throws ZkmException, IOException {
        this.className = "<link>" + linkedClassSuffix;
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotVolatile(boolean memberNotVolatile) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(2);
        }

        this.memberAccessFlags.setVolatileForbidden(memberNotVolatile);
        this.setChanged();
        this.notifyObservers();
    }

    public void setClassFinal(boolean classFinal) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setFinalRequired(classFinal);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotAbstract(boolean memberNotAbstract) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setAbstractForbidden(memberNotAbstract);
        this.setChanged();
        this.notifyObservers();
    }

    public final String toScriptText() {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        StringBuilder script = new StringBuilder();
        if (flowControlKey != 0 && this.classAnnotation != null && this.classAnnotation.length() > 0) {
            if (!this.classAnnotation.startsWith("@") && !this.classAnnotation.startsWith("(")) {
                script.append('@');
            }
            script.append(this.classAnnotation);
            script.append(" ");
        }

        if ((flowControlKey == 0 || this.classAccessFlags != null) && this.isClassPackagePrivate()) {
            script.append("package ");
        }
        script.append(flowControlKey != 0 && this.classAccessFlags == null ? "" : this.classAccessFlags.toSpecString());
        int packageCount = this.packageNames.size();

        if (flowControlKey == 0) {
            // This mode is publicly selectable; preserve its distinct rendering and null behavior.
            script.append("!");
            boolean useFirstSearchName = false;
            if (this.interfaceNames.size() > 0) {
                script.append(" search ");
                if (this.searchNames.size() > 0) {
                    String firstSearchName = (String) this.searchNames.get(0);
                    script.append(firstSearchName);
                    useFirstSearchName = true;
                }
            }
            if (!useFirstSearchName) {
                if (this.memberAccessFlags != null && script.length() != 0) {
                    script.append("package ");
                }
                script.append(this.memberAccessFlags.toSpecString());
            }
        } else {
            if (this.archivePattern != null) {
                script.append("\"");
                script.append(this.archivePattern);
                script.append("\"");
                script.append("!");
            }
            if (packageCount > 0) {
                for (int packageIndex = 0; packageIndex < packageCount; packageIndex++) {
                    script.append((String) this.packageNames.get(packageIndex) + '.');
                }
                if (this.packageNameOnly) {
                    script.append(".");
                }
                if (this.excludeContainingPackage) {
                    script.append("^");
                }
            }
            if (this.className != null) {
                script.append(this.className);
                if (this.excludeContainingClass) {
                    script.append("^");
                }
            }
            if (this.containingClause != null) {
                script.append(' ' + this.containingClause.toScriptText());
            }
            if (this.superclassName != null && this.superclassName.length() > 0) {
                script.append(" extends " + this.superclassName);
            }
            if (this.interfaceNames.size() > 0) {
                script.append(" implements ");
                for (int interfaceIndex = 0; interfaceIndex < this.interfaceNames.size(); interfaceIndex++) {
                    script.append((String) this.interfaceNames.get(interfaceIndex));
                    if (interfaceIndex < this.interfaceNames.size() - 1) {
                        script.append(", ");
                    }
                }
            }
            if (this.searchNames.size() > 0) {
                script.append(" search ");
                for (int searchIndex = 0; searchIndex < this.searchNames.size(); searchIndex++) {
                    script.append((String) this.searchNames.get(searchIndex));
                    if (searchIndex < this.searchNames.size() - 1) {
                        script.append(", ");
                    }
                }
            }
            if (this.memberAnnotation != null && this.memberAnnotation.length() > 0) {
                script.append(" ");
                if (!this.memberAnnotation.startsWith("@") && !this.memberAnnotation.startsWith("(")) {
                    script.append('@');
                }
                script.append(this.memberAnnotation);
                script.append(" ");
            }
            if (this.memberAccessFlags != null) {
                if (script.length() > 0 && script.charAt(script.length() - 1) != ' ') {
                    script.append(" ");
                }
                if (this.isMemberPackagePrivate()) {
                    script.append("package ");
                }
            }
            script.append(this.memberAccessFlags == null ? "" : this.memberAccessFlags.toSpecString());
            if (this.fieldType != null) {
                if (script.length() > 0 && script.charAt(script.length() - 1) != ' ') {
                    script.append(' ');
                }
                script.append(this.fieldType);
            }
            if (this.fieldName != null) {
                if (script.length() > 0 && script.charAt(script.length() - 1) != ' ') {
                    script.append(' ');
                }
                script.append(this.fieldName);
            }
            if (this.linkedMethodName != null || this.methodName != null) {
                if (script.length() > 0 && script.charAt(script.length() - 1) != ' ') {
                    script.append(" ");
                }
                if (this.linkedMethodName != null) {
                    script.append(this.linkedMethodName);
                } else {
                    script.append(this.methodName);
                }
                script.append('(');
                if (this.parameterTypes != null && this.parameterTypes.size() > 0) {
                    for (int parameterIndex = 0; parameterIndex < this.parameterTypes.size(); parameterIndex++) {
                        script.append((String) this.parameterTypes.get(parameterIndex));
                        if (parameterIndex < this.parameterTypes.size() - 1) {
                            script.append(", ");
                        }
                    }
                }
                script.append(')');
                if (this.throwsNames.size() > 0) {
                    script.append(" throws ");
                    for (int exceptionIndex = 0; exceptionIndex < this.throwsNames.size(); exceptionIndex++) {
                        script.append((String) this.throwsNames.get(exceptionIndex));
                        if (exceptionIndex < this.throwsNames.size() - 1) {
                            script.append(", ");
                        }
                    }
                }
            }
        }

        if (this.includeSignatureClasses) {
            script.append(" +signatureClasses");
        }
        return script.toString();
    }

    public String getMemberAnnotation() {
        return this.memberAnnotation != null ? this.memberAnnotation : "";
    }

    public void setClassName(String classNameArgument) throws ZkmException, IOException {
        boolean bl = !this.className.equals(classNameArgument);
        this.className = classNameArgument;
        if (bl) {
            this.setChanged();
            this.notifyObservers();
        }
    }

    public void setMemberSynchronized(boolean memberSynchronized) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setSynchronizedRequired(memberSynchronized);
        this.setChanged();
        this.notifyObservers();
    }

    public RenameFilterModel(ASTRenameFilterParameter aSTRenameFilterParameter) {
        if (aSTRenameFilterParameter.classAccessFlags != null) {
            this.classAccessFlags = (AccessFlagsSpec) aSTRenameFilterParameter.classAccessFlags.clone();
        }

        if (aSTRenameFilterParameter.memberAccessFlags != null) {
            this.memberAccessFlags = (AccessFlagsSpec) aSTRenameFilterParameter.memberAccessFlags.clone();
        }

        this.archivePattern = aSTRenameFilterParameter.archivePathPattern;
        this.packageNames = aSTRenameFilterParameter.getPackageSegments();
        if (aSTRenameFilterParameter.packagePattern != null) {
            this.packageNameOnly = aSTRenameFilterParameter.packagePattern.hasDotSuffix();
            this.excludeContainingPackage = aSTRenameFilterParameter.packagePattern.hasCaretTag();
        }

        if (aSTRenameFilterParameter.packagePattern != null) {
        }

        this.searchNames = ZkmUtils.copyArrayList(aSTRenameFilterParameter.linkPackageNames);
        if (aSTRenameFilterParameter.linkClassName != null) {
            this.className = aSTRenameFilterParameter.linkClassName;
        } else if (aSTRenameFilterParameter.classNamePattern != null) {
            this.className = aSTRenameFilterParameter.classNamePattern.getSpecText();
            this.excludeContainingClass = aSTRenameFilterParameter.classNamePattern.hasCaretTag();
            if (this.excludeContainingClass) {
                this.className = this.className.substring(0, this.className.length() - 1);
            }
        }

        if (aSTRenameFilterParameter.containingClause != null) {
            this.containingClause = aSTRenameFilterParameter.containingClause;
        }

        if (aSTRenameFilterParameter.extendsClassName != null) {
            this.superclassName = ASTRenameFilterParameter.toDottedName(aSTRenameFilterParameter.extendsClassName);
        }

        if (aSTRenameFilterParameter.implementsNames != null) {
            for (int elementIndex = 0; elementIndex < aSTRenameFilterParameter.implementsNames.size(); elementIndex++) {
                String string = (String) aSTRenameFilterParameter.implementsNames.get(elementIndex);
                this.interfaceNames.add(ASTRenameFilterParameter.toDottedName(string));
            }
        }

        if (aSTRenameFilterParameter.classAnnotation != null) {
            this.classAnnotation = aSTRenameFilterParameter.classAnnotation.getSpecText();
            if (this.classAnnotation.startsWith("@")) {
                this.classAnnotation = this.classAnnotation.substring(1);
            }
        }

        if (aSTRenameFilterParameter.fieldSpecifier != null) {
            this.fieldName = aSTRenameFilterParameter.fieldSpecifier.getSpecText();
        }

        if (aSTRenameFilterParameter.fieldType != null) {
            this.fieldType = ASTRenameFilterParameter.toJavaTypeName(aSTRenameFilterParameter.fieldType);
        }

        if (aSTRenameFilterParameter.methodSpecifier != null) {
            this.methodName = aSTRenameFilterParameter.methodSpecifier.getSpecText();
        }

        this.linkedMethodName = aSTRenameFilterParameter.linkMethodSignature;
        if (aSTRenameFilterParameter.argsPattern != null && aSTRenameFilterParameter.argsPattern.argsText.length() > 0) {
            String string1 = ASTRenameFilterParameter.formatArgsWithAnnotations(aSTRenameFilterParameter.argsPattern, (ASTComplexAnnotationSpecifier[]) null);
            StringTokenizer stringTokenizer = new StringTokenizer(string1, ",");

            while (stringTokenizer.hasMoreTokens()) {
                this.parameterTypes.add(stringTokenizer.nextToken());
            }
        }

        if (aSTRenameFilterParameter.memberAnnotation != null) {
            this.memberAnnotation = aSTRenameFilterParameter.memberAnnotation.getSpecText();
            if (this.memberAnnotation.startsWith("@")) {
                this.memberAnnotation = this.memberAnnotation.substring(1);
            }
        }

        RenameFilterModel renameFilterModel1;
        boolean includeSignatureClassesValue;
        if (aSTRenameFilterParameter.throwsTypes != null) {
            for (int elementIndexValue = 0; elementIndexValue < aSTRenameFilterParameter.throwsTypes.size(); elementIndexValue++) {
                String string2 = (String) aSTRenameFilterParameter.throwsTypes.get(elementIndexValue);
                this.throwsNames.add(ASTRenameFilterParameter.toDottedName(string2));
            }

            renameFilterModel1 = this;
            includeSignatureClassesValue = aSTRenameFilterParameter.plusSignatureClasses;
        } else {
            renameFilterModel1 = this;
            includeSignatureClassesValue = aSTRenameFilterParameter.plusSignatureClasses;
        }

        renameFilterModel1.includeSignatureClasses = includeSignatureClassesValue;
        this.parameterKind = aSTRenameFilterParameter.specifierKind;
        this.scriptEnvironment = aSTRenameFilterParameter.scriptEnvironment;
    }

    @Override
    public String toString() {
        return this.toScriptText();
    }

    public void setThrowsNames(String throwsNamesArgument) throws ZkmException, IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(throwsNamesArgument, ",");
        this.throwsNames = new ArrayList();

        while (stringTokenizer.hasMoreTokens()) {
            this.throwsNames.add(stringTokenizer.nextToken().trim());
        }

        this.setChanged();
        this.notifyObservers();
    }

    public boolean isExcludeContainingClass() {
        return this.excludeContainingClass;
    }

    public boolean isClassExcluded() {
        return this.parameterKind == 2 || this.excludeContainingClass;
    }

    public String getClassAnnotation() {
        return this.classAnnotation != null ? this.classAnnotation : "";
    }

    public void setMemberPackagePrivate(Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.requirePackage();
        this.setChanged();
        this.notifyObservers();
    }

    public void setFieldName(String fieldNameArgument) throws ZkmException, IOException {
        this.fieldName = fieldNameArgument;
        this.setChanged();
        this.notifyObservers();
    }

    public void clearClassAccess() throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.clearRequiredAccess();
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotFinal(boolean finalForbidden, Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.setFinalForbidden(finalForbidden);
        this.setChanged();
        this.notifyObservers();
    }

    public String buildDescription(int ba) {
        String string;
        String string1;
        switch (ba) {
            case 1:
                string = "Exclude the name of ";
                string1 = "Also exclude the name of ";
                break;
            case 2:
                string = "Exclude ";
                string1 = "Also exclude ";
                break;
            case 3:
                string = "Exclude references to ";
                string1 = "Also exclude references to ";
                break;
            default:
                string = "Exclude the name of ";
                string1 = "Also exclude the name of ";
        }

        int bb = 1;
        char bc = 'a';
        StringBuilder stringBuilder = new StringBuilder();
        switch (this.parameterKind) {
            case 1:
                stringBuilder.append(string + "all packages:" + HiddenOptionFlags.LINE_SEPARATOR);
                if (this.packageNames != null && this.packageNames.size() > 0) {
                    stringBuilder.append("\t" + bb + ") that match \"" + this.getPackagePattern() + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }
                break;
            case 2:
                if (this.isLinkedClass()) {
                    if (this.hasLinkPrefix()) {
                        stringBuilder.append(
                                "Exclude the prefix \""
                                        + this.getLinkPrefix()
                                        + "\" and the suffix \""
                                        + this.getLinkSuffix()
                                        + "\" from being renamed and link the non-prefix and non-suffix parts of the name in all classes:"
                                        + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    } else {
                        stringBuilder.append(
                                "Exclude the suffix \""
                                        + this.getLinkSuffix()
                                        + "\" from being renamed and link the non-suffix part of the name in all classes:"
                                        + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    }

                    if (this.classAnnotation != null) {
                        stringBuilder.append(
                                "\t" + bb++ + ") that are annotated by a class matching \"" + this.classAnnotation + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    }

                    if (this.packageNames != null && this.packageNames.size() > 0) {
                        stringBuilder.append(
                                "\t" + bb++ + ") that are in a package that matches \"" + this.getPackagePattern() + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    } else {
                        stringBuilder.append("\t" + bb++ + ") that are not in a package" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    if (this.hasLinkPrefix()) {
                        stringBuilder.append(
                                "\t"
                                        + bb++
                                        + ") with a name that starts with \""
                                        + this.getLinkPrefix()
                                        + "\" and ends with \""
                                        + this.getLinkSuffix()
                                        + "\""
                                        + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    } else {
                        stringBuilder.append("\t" + bb++ + ") with a name that ends with \"" + this.getLinkSuffix() + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    String superclassNameValue = this.getSuperclassName();
                    if (superclassNameValue.length() > 0) {
                        stringBuilder.append("\t" + bb++ + ") that extend \"" + superclassNameValue + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    String interfaceNamesText = this.getInterfaceNamesText();
                    if (interfaceNamesText.length() > 0) {
                        stringBuilder.append("\t" + bb + ") that implement \"" + interfaceNamesText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }
                } else {
                    stringBuilder.append(string + "all classes:" + HiddenOptionFlags.LINE_SEPARATOR);
                    if (this.classAnnotation != null) {
                        stringBuilder.append(
                                "\t" + bb++ + ") that are annotated by a class matching \"" + this.classAnnotation + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    }

                    String classAccessText = this.getClassAccessText();
                    if (classAccessText.length() > 0) {
                        stringBuilder.append("\t" + bb++ + ") that are \"" + classAccessText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    if (this.packageNames != null && this.packageNames.size() > 0) {
                        stringBuilder.append(
                                "\t" + bb++ + ") that are in a package that matches \"" + this.getPackagePattern() + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                        );
                    } else {
                        stringBuilder.append("\t" + bb++ + ") that are not in a package" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    stringBuilder.append("\t" + bb++ + ") with a name that matches \"" + this.className + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    String containingText = this.getContainingText();
                    if (containingText.length() > 0) {
                        stringBuilder.append("\t" + bb++ + ") that contains fields or methods matching \"" + containingText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    String superclassNameValue2 = this.getSuperclassName();
                    if (superclassNameValue2.length() > 0) {
                        stringBuilder.append("\t" + bb++ + ") that extend \"" + superclassNameValue2 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }

                    String interfaceNamesTextValue = this.getInterfaceNamesText();
                    if (interfaceNamesTextValue.length() > 0) {
                        stringBuilder.append("\t" + bb + ") that implement \"" + interfaceNamesTextValue + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }
                }

                if (this.packageNames != null && this.packageNames.size() > 0 && this.excludeContainingPackage) {
                    stringBuilder.append(string1 + "any package containing a matching class" + HiddenOptionFlags.LINE_SEPARATOR);
                }
                break;
            case 3:
                stringBuilder.append(string + "all fields:" + HiddenOptionFlags.LINE_SEPARATOR);
                String memberAccessText = this.getMemberAccessText();
                if (this.memberAnnotation != null) {
                    stringBuilder.append(
                            "\t" + bb++ + ") that are annotated by a class matching \"" + this.memberAnnotation + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                    );
                }

                if (memberAccessText.length() > 0) {
                    stringBuilder.append("\t" + bb++ + ") that are \"" + memberAccessText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.fieldType != null && this.fieldType.length() > 0) {
                    stringBuilder.append("\t" + bb++ + ") that are of type \"" + this.fieldType + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuilder.append("\t" + bb++ + ") that have a name matching \"" + this.fieldName + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                stringBuilder.append("\t" + bb + ") that are contained within a class:" + HiddenOptionFlags.LINE_SEPARATOR);
                if (this.classAnnotation != null) {
                    StringBuilder stringBuilder1 = new StringBuilder().append("\t\t");
                    bc = 'b';
                    stringBuilder.append(
                            stringBuilder1.append('a')
                                    .append(") that is annotated by a class matching \"")
                                    .append(this.classAnnotation)
                                    .append("\"")
                                    .append(HiddenOptionFlags.LINE_SEPARATOR)
                                    .toString()
                    );
                }

                String classAccessTextValue = this.getClassAccessText();
                if (classAccessTextValue.length() > 0) {
                    stringBuilder.append("\t\t" + bc++ + ") that is \"" + classAccessTextValue + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.packageNames != null && this.packageNames.size() > 0) {
                    stringBuilder.append(
                            "\t\t" + bc++ + ") that is in a package that matches \"" + this.getPackagePattern() + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                    );
                } else {
                    stringBuilder.append("\t\t" + bc++ + ") that is not in a package" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuilder.append("\t\t" + bc++ + ") with a name that matches \"" + this.className + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                String superclassNameValue3 = this.getSuperclassName();
                if (superclassNameValue3.length() > 0) {
                    stringBuilder.append("\t\t" + bc++ + ") that extends \"" + superclassNameValue3 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                String interfaceNamesTextValue2 = this.getInterfaceNamesText();
                if (interfaceNamesTextValue2.length() > 0) {
                    stringBuilder.append("\t\t" + bc + ") that implements \"" + interfaceNamesTextValue2 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.packageNames != null && this.packageNames.size() > 0 && this.excludeContainingPackage) {
                    stringBuilder.append(string1 + "any package containing a class containing a matching field" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.excludeContainingClass) {
                    stringBuilder.append(string1 + "any class containing a matching field" + HiddenOptionFlags.LINE_SEPARATOR);
                }
                break;
            case 4:
                if (this.isLinkedMethod()) {
                    stringBuilder.append(
                            "Exclude the suffix \""
                                    + this.getTextBeforeLink(this.linkedMethodName)
                                    + "\" from being renamed in all methods:"
                                    + HiddenOptionFlags.LINE_SEPARATOR
                    );
                } else {
                    stringBuilder.append(string + "all methods:" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.memberAnnotation != null) {
                    stringBuilder.append(
                            "\t" + bb++ + ") that are annotated by a class matching \"" + this.memberAnnotation + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                    );
                }

                String memberAccessTextValue = this.getMemberAccessText();
                String linkedMethodNameValue;
                if (memberAccessTextValue.length() > 0) {
                    stringBuilder.append("\t" + bb++ + ") that are \"" + memberAccessTextValue + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    linkedMethodNameValue = this.linkedMethodName;
                } else {
                    linkedMethodNameValue = this.linkedMethodName;
                }

                if (linkedMethodNameValue != null) {
                    stringBuilder.append(
                            "\t"
                                    + bb++
                                    + ") that have a name matching starting with \""
                                    + this.getTextBeforeLink(this.linkedMethodName)
                                    + "\""
                                    + HiddenOptionFlags.LINE_SEPARATOR
                    );
                } else {
                    stringBuilder.append("\t" + bb++ + ") that have a name matching \"" + this.methodName + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                String parameterTypesText = this.getParameterTypesText();
                if (parameterTypesText.length() > 0) {
                    if (parameterTypesText.equals("*")) {
                        stringBuilder.append("\t" + bb++ + ") that have any argument types" + HiddenOptionFlags.LINE_SEPARATOR);
                    } else {
                        stringBuilder.append("\t" + bb++ + ") that have arguments matching \"" + parameterTypesText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                    }
                } else {
                    stringBuilder.append("\t" + bb++ + ") that have no arguments" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                String throwsText = this.getThrowsText();
                if (throwsText.length() > 0) {
                    stringBuilder.append("\t" + bb++ + ") that throw \"" + throwsText + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuilder.append("\t" + bb + ") that are contained within a class:" + HiddenOptionFlags.LINE_SEPARATOR);
                if (this.classAnnotation != null) {
                    stringBuilder.append(
                            "\t\t" + bc++ + ") that is annotated by a class matching \"" + this.classAnnotation + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                    );
                }

                String classAccessTextValue2 = this.getClassAccessText();
                if (classAccessTextValue2.length() > 0) {
                    stringBuilder.append("\t\t" + bc++ + ") that is \"" + classAccessTextValue2 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.packageNames != null && this.packageNames.size() > 0) {
                    stringBuilder.append(
                            "\t\t" + bc++ + ") that is in a package that matches \"" + this.getPackagePattern() + "\"" + HiddenOptionFlags.LINE_SEPARATOR
                    );
                } else {
                    stringBuilder.append("\t\t" + bc++ + ") that is not in a package" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuilder.append("\t\t" + bc++ + ") with a name that matches \"" + this.className + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                String superclassNameValue4 = this.getSuperclassName();
                if (superclassNameValue4.length() > 0) {
                    stringBuilder.append("\t\t" + bc++ + ") that extends \"" + superclassNameValue4 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                String interfaceNamesTextValue3 = this.getInterfaceNamesText();
                if (interfaceNamesTextValue3.length() > 0) {
                    stringBuilder.append("\t\t" + bc + ") that implements \"" + interfaceNamesTextValue3 + "\"" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.packageNames != null && this.packageNames.size() > 0 && this.excludeContainingPackage) {
                    stringBuilder.append(string1 + "any package containing a class containing a matching method" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.excludeContainingClass) {
                    stringBuilder.append(string1 + "any class containing a matching method" + HiddenOptionFlags.LINE_SEPARATOR);
                }

                if (this.isLinkedMethod()) {
                    stringBuilder.append(
                            "Also rename the non-prefix part of the method name to match that of any corresponding field name in the class"
                                    + HiddenOptionFlags.LINE_SEPARATOR
                    );
                }

                if (this.includeSignatureClasses) {
                    stringBuilder.append(string1 + "any class that appears in the method arguments" + HiddenOptionFlags.LINE_SEPARATOR);
                }
        }

        if (ba == 3) {
            stringBuilder.append(
                    "Note that the ZKM Script language also allows you to specify the reference obfuscation"
                            + HiddenOptionFlags.LINE_SEPARATOR
                            + "based upon the methods from which the references are made."
                            + HiddenOptionFlags.LINE_SEPARATOR
            );
        }

        return stringBuilder.toString();
    }

    public String getContainingText() {
        return this.containingClause != null ? this.containingClause.toScriptText().substring("containing".length()) : "";
    }

    public void setClassNotInterface(boolean classNotInterface) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setInterfaceForbidden(classNotInterface);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNative(boolean memberNative) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setNativeRequired(memberNative);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotNative(boolean memberNotNative) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(3);
        }

        this.memberAccessFlags.setNativeForbidden(memberNotNative);
        this.setChanged();
        this.notifyObservers();
    }

    public void clearMemberAccess(Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.clearRequiredAccess();
        this.setChanged();
        this.notifyObservers();
    }

    public void setParameterTypes(String parameterTypesArgument) throws ZkmException, IOException {
        StringTokenizer stringTokenizer = new StringTokenizer(parameterTypesArgument, ",");
        this.parameterTypes = new ArrayList();

        while (stringTokenizer.hasMoreTokens()) {
            this.parameterTypes.add(stringTokenizer.nextToken().trim());
        }

        this.setChanged();
        this.notifyObservers();
    }

    public String getLinkSuffix() {
        return this.className.substring(this.className.indexOf("<link>") + "<link>".length());
    }

    public String getMemberAccessText() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.isMemberPackagePrivate()) {
            stringBuffer.append("package ");
        }

        stringBuffer.append(this.memberAccessFlags != null ? this.memberAccessFlags.toSpecString() : "");
        return stringBuffer.toString();
    }

    public void setClassInterface(boolean classInterface) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setInterfaceRequired(classInterface);
        this.setChanged();
        this.notifyObservers();
    }

    public boolean isMemberPackagePrivate() {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        if (flowControlKey != 0 && this.memberAccessFlags == null) {
            return false;
        }
        return this.memberAccessFlags.isPackageRequired();
    }

    public void setClassAbstract(boolean classAbstract) throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.setAbstractRequired(classAbstract);
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberVolatile(boolean memberVolatile) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(2);
        }

        this.memberAccessFlags.setVolatileRequired(memberVolatile);
        this.setChanged();
        this.notifyObservers();
    }

    public void setExcludeContainingClass(boolean excludeContainingClass) throws ZkmException, IOException {
        this.excludeContainingClass = excludeContainingClass;
        this.setChanged();
        this.notifyObservers();
    }

    public String getClassName() {
        return this.className;
    }

    public boolean isClassPackagePrivate() {
        int flowControlKey = ZkmScriptSimpleNode.getFlowControlKey();
        if (flowControlKey != 0 && this.classAccessFlags == null) {
            return false;
        }
        return this.classAccessFlags.isPackageRequired();
    }

    public void setMemberPublic(Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.requirePublic();
        this.setChanged();
        this.notifyObservers();
    }

    public AccessFlagsSpec getMemberAccessFlags() {
        return this.memberAccessFlags;
    }

    public String getClassAccessText() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.isClassPackagePrivate()) {
            stringBuffer.append("package ");
        }

        stringBuffer.append(this.classAccessFlags != null ? this.classAccessFlags.toSpecString() : "");
        return stringBuffer.toString();
    }

    public String getInterfaceNamesText() {
        if (this.interfaceNames != null && this.interfaceNames.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            int elementIndex = 0;
            int bb = 0;

            for (List list1 = this.interfaceNames; bb < list1.size(); list1 = this.interfaceNames) {
                stringBuffer.append((String) this.interfaceNames.get(elementIndex));
                if (elementIndex < this.interfaceNames.size() - 1) {
                    stringBuffer.append(", ");
                }

                bb = ++elementIndex;
            }

            return stringBuffer.toString();
        } else {
            return "";
        }
    }

    public void setSuperclassName(String superclassNameArgument) throws ZkmException, IOException {
        this.superclassName = superclassNameArgument;
        this.setChanged();
        this.notifyObservers();
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public void setClassPackagePrivate() throws ZkmException, IOException {
        if (this.classAccessFlags == null) {
            this.classAccessFlags = new AccessFlagsSpec(1);
        }

        this.classAccessFlags.requirePackage();
        this.setChanged();
        this.notifyObservers();
    }

    public void setMemberNotStatic(boolean staticForbidden, Integer integer) throws ZkmException, IOException {
        if (this.memberAccessFlags == null) {
            this.memberAccessFlags = new AccessFlagsSpec(integer);
        }

        this.memberAccessFlags.setStaticForbidden(staticForbidden);
        this.setChanged();
        this.notifyObservers();
    }

    public int getParameterKind() {
        return this.parameterKind;
    }

    public boolean isLinkedMethod() {
        return this.parameterKind == 4 && this.linkedMethodName != null;
    }

    public AccessFlagsSpec getClassAccessFlags() {
        return this.classAccessFlags;
    }

    public boolean hasLinkPrefix() {
        return this.className.indexOf("<link>") > 0;
    }
}
