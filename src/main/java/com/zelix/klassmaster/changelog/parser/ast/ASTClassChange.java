package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.ChangeLogClassKey;
import com.zelix.klassmaster.changelog.ChangeLogMemberKey;
import com.zelix.klassmaster.changelog.ClassNameNodeSetter;
import com.zelix.klassmaster.changelog.NewNameMapping;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.Map;

public class ASTClassChange extends ChangeLogEntryNode implements ClassNameNodeSetter {
    public ListMultimap methodChanges;
    public int childCount;
    public String oldClassName;
    private Map lineNumberChanges;
    public String newClassName;
    public String sourceName;
    public ListMultimap fieldChanges;

    public void setSourceName(String sourceNameArgument) {
        this.sourceName = sourceNameArgument;
    }

    public void addFieldChange(Object object, String name, String type, Integer integer) {
        if (this.fieldChanges == null) {
            int primeCapacity = ZkmUtils.getPrimeCapacity(this.childCount);
            this.fieldChanges = new ListMultimap(primeCapacity);
        }

        this.fieldChanges.addValue(new ChangeLogClassKey(name, type, integer), object);
    }

    @Override
    public void beginEntry(Object object, Object object1) {
        AbstractChangeLog abstractChangeLog = (AbstractChangeLog) object;
        this.childCount = (Integer) object1;
        abstractChangeLog.incrementClassCount();
    }

    @Override
    public void acceptClassName(Object object) {
        this.oldClassName = (String) object;
    }

    public void addLineNumberChange(Integer integer, Integer integer1) {
        if (this.lineNumberChanges == null) {
            this.lineNumberChanges = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.childCount));
        }

        this.lineNumberChanges.put(integer, integer1);
    }

    @Override
    public void applyToChangeLog(Object object) throws ZkmException, IOException {
        AbstractChangeLog abstractChangeLog = (AbstractChangeLog) object;
        AbstractChangeLog abstractChangeLog1;
        String newClassNameValue;
        if (this.newClassName == null) {
            this.newClassName = this.oldClassName;
            abstractChangeLog1 = abstractChangeLog;
            newClassNameValue = this.newClassName;
        } else {
            abstractChangeLog1 = abstractChangeLog;
            newClassNameValue = this.newClassName;
        }

        abstractChangeLog1.addClassMapping(newClassNameValue, this.oldClassName);
        if (this.sourceName != null) {
            abstractChangeLog.addSourceNameMapping(this.oldClassName, this.sourceName);
        }

        if (this.fieldChanges != null) {
            abstractChangeLog.addFieldMappings(this.oldClassName, this.fieldChanges);
        }

        if (this.methodChanges != null) {
            boolean bl = false;
            int ba = 27634284;
            char bb = '\ue1fb';
            ListMultimap listMultimap = this.methodChanges;
            String oldClassNameValue = this.oldClassName;
            abstractChangeLog.addMethodMappings(oldClassNameValue, listMultimap);
        }

        if (this.lineNumberChanges != null) {
            abstractChangeLog.addLineNumberMappings(this.oldClassName, this.lineNumberChanges);
        }
    }

    public void addMethodChange(
            String name,
            String string1,
            String type,
            Integer integer,
            String newName,
            String string4,
            boolean parametersObfuscated,
            String parameterChangeData,
            String paramChangeNodeData,
            Boolean boolean1
    ) {
        String parameterTypes = string1;
        String newParameterTypes = string4;
        if (this.methodChanges == null) {
            this.methodChanges = new ListMultimap(Math.min(40, this.childCount));
        }

        if (parameterTypes == null) {
            parameterTypes = "";
        }

        if (newParameterTypes == null) {
            newParameterTypes = "";
        }

        boolean signatureChanged = parameterChangeData != null && parameterChangeData.length() > 0 || parametersObfuscated;
        this.methodChanges
                .addValue(new ChangeLogMemberKey(name, parameterTypes, type, integer), new NewNameMapping(newName, newParameterTypes, parametersObfuscated, parameterChangeData, paramChangeNodeData, signatureChanged, boolean1));
    }

    public ASTClassChange() {
        super(7);
    }

    public void setNewClassName(String newClassNameArgument) {
        this.newClassName = newClassNameArgument;
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
