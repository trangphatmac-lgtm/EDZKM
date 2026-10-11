package com.zelix.klassmaster.config;

import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ast.ASTComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.parser.ast.ASTGroupingsStatement;
import com.zelix.klassmaster.script.parser.ast.ASTRenameFilterParameter;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GroupingsSpec implements ClassHierarchyQuery {
    public TwoKeyMap parametersByGroup = new TwoKeyMap();
    public TwoKeyMap classesByGroup = new TwoKeyMap();
    public ClassRepository classRepository;
    public List groupingsStatements;
    public ScriptEnvironment scriptEnvironment;

    public final void resolveGroupClasses() throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();
        Enumeration enumeration = this.parametersByGroup.keys();

        while (enumeration.hasMoreElements()) {
            String string = (String) enumeration.nextElement();
            arrayList.add(string);
        }

        Collections.sort(arrayList);

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            String key = (String) arrayList.get(elementIndex);
            Iterator iterator = this.parametersByGroup.getInnerMap(key).keySet().iterator();

            while (iterator.hasNext()) {
                ASTRenameFilterParameter aSTRenameFilterParameter = (ASTRenameFilterParameter) iterator.next();
                if (!aSTRenameFilterParameter.isClassSpecifier()) {
                    this.scriptEnvironment
                            .logWarning(
                                    "Parameter '" + aSTRenameFilterParameter + "' in 'groupings' statement is not a class exclusion parameter. It will be ignored.", true
                            );
                } else if (aSTRenameFilterParameter.hasLinkClassName()) {
                    this.scriptEnvironment
                            .logWarning("Parameter '" + aSTRenameFilterParameter + "' in 'groupings' statement cannot use the '<link>' syntax. It will be ignored.", true);
                } else {
                    if (aSTRenameFilterParameter.isClassPlusSpec()) {
                        this.scriptEnvironment
                                .logWarning(
                                        "Parameter '" + aSTRenameFilterParameter + "' in 'groupings' statement cannot use the '" + "+" + "' tag. The tag will be ignored.",
                                        true
                                );
                    }

                    HashSet hashSet = ZkmUtils.createHashSet();
                    aSTRenameFilterParameter.collectGroupingClasses(this, hashSet);
                    if (hashSet.size() > 0) {
                        Iterator iterator1 = hashSet.iterator();

                        while (iterator1.hasNext()) {
                            ProgramClass programClass1 = (ProgramClass) iterator1.next();
                            this.classesByGroup.putValue(key, programClass1, programClass1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public final boolean implementsAnnotatedInterface(String key, String string1, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) throws ZkmException, IOException {
        return this.classRepository.implementsAnnotatedInterface(key, string1, aSTComplexAnnotationSpecifier);
    }

    public GroupingsSpec(ClassRepository classRepository1, List list1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        this.classRepository = classRepository1;
        this.groupingsStatements = list1;
        this.scriptEnvironment = scriptEnvironment1;
        if (classRepository1.hasProgramClasses()) {
            this.collectGroupingParameters();
            this.resolveGroupClasses();
        }
    }

    @Override
    public final boolean isHierarchyMarked() {
        return this.classRepository.isHierarchyMarked();
    }

    @Override
    public final boolean extendsAnnotatedClassByOriginalName(String key, String string1, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) throws ZkmException, IOException {
        return this.classRepository.extendsAnnotatedClassByOriginalName(key, string1, aSTComplexAnnotationSpecifier);
    }

    @Override
    public String getOriginalClassName(Object object) {
        String key = (String) object;
        return this.classRepository.getOriginalClassName(key);
    }

    public Enumeration enumerateClasses() {
        return this.classRepository.enumerateProgramClasses();
    }

    @Override
    public final boolean extendsAnnotatedClass(String key, String string1, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) throws ZkmException, IOException {
        return this.classRepository.extendsAnnotatedClass(key, string1, aSTComplexAnnotationSpecifier);
    }

    @Override
    public final boolean isHierarchySealed() {
        return this.classRepository.isHierarchySealed();
    }

    @Override
    public final boolean isObjectMethodSignature(Object object) {
        MethodSignature methodSignature1 = (MethodSignature) object;
        return this.classRepository.isObjectMethodSignature(methodSignature1);
    }

    @Override
    public Set getClassAnnotations(String string, Integer integer, boolean bl) throws ZkmException, IOException {
        Boolean boolean1 = bl;
        Integer integer1 = integer;
        String string1 = string;
        return this.getClassAnnotations(true, string1, integer1, boolean1);
    }

    @Override
    public Set getClassAnnotations(Object object, String string, Integer integer, boolean bl) throws ZkmException, IOException {
        return this.classRepository.getClassAnnotations(string, integer, bl);
    }

    @Override
    public final boolean implementsAnnotatedInterfaceByOriginalName(String key, String string1, ASTComplexAnnotationSpecifier aSTComplexAnnotationSpecifier) throws ZkmException, IOException {
        return this.classRepository.implementsAnnotatedInterfaceByOriginalName(key, string1, aSTComplexAnnotationSpecifier);
    }

    @Override
    public final boolean implementsInterfaceByOriginalName(String keyArgument, String key) throws ZkmException, IOException {
        return this.classRepository.implementsInterfaceByOriginalName(keyArgument, key);
    }

    @Override
    public final boolean isSubclassByOriginalName(String key, String keyArgument) throws ZkmException, IOException {
        return this.classRepository.isSubclassByOriginalName(key, keyArgument);
    }

    public ReadOnlyMultiMap getGroupedClasses() {
        return new ReadOnlyMultiMap(this.classesByGroup);
    }

    @Override
    public final boolean hasOriginalNameCaches() {
        return this.classRepository.hasOriginalNameCaches();
    }

    @Override
    public ClassResolver getClassResolver() {
        return this.classRepository.getClassResolver();
    }

    public boolean isLoggingEnabled() {
        return this.scriptEnvironment.isVerbose();
    }

    public void printGroupingParameters() {
        PrintWriter printWriter = this.scriptEnvironment.getLogWriter();
        ArrayList arrayList = new ArrayList();
        Enumeration enumeration = this.parametersByGroup.keys();

        while (enumeration.hasMoreElements()) {
            String string = (String) enumeration.nextElement();
            arrayList.add(string);
        }

        Collections.sort(arrayList);
        printWriter.println("\tGroupings parameters:");

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            String key = (String) arrayList.get(elementIndex);
            printWriter.println("\t\t\"" + key + "\"");
            Iterator iterator = this.parametersByGroup.getInnerMap(key).keySet().iterator();

            while (iterator.hasNext()) {
                printWriter.println("\t\t\t\"" + ((ASTRenameFilterParameter) iterator.next()).toScriptText() + "\"");
            }
        }
    }

    @Override
    public final boolean implementsInterface(String key, String keyArgument) throws ZkmException, IOException {
        return this.classRepository.implementsInterface(key, keyArgument);
    }

    public PrintWriter getLogWriter() {
        return this.scriptEnvironment.getLogWriter();
    }

    public final void collectGroupingParameters() {
        int ba = 0;
        int groupingsStatementsSize = this.groupingsStatements.size();

        for (int elementIndex = 0; elementIndex < groupingsStatementsSize; elementIndex++) {
            ArrayList arrayList = ((ASTGroupingsStatement) this.groupingsStatements.get(elementIndex)).getGroupings();

            for (int elementIndexValue = 0; elementIndexValue < arrayList.size(); elementIndexValue++) {
                String key = "GROUPING_" + ba++;
                ArrayList arrayList1 = (ArrayList) arrayList.get(elementIndexValue);

                for (int elementIndexValue2 = 0; elementIndexValue2 < arrayList1.size(); elementIndexValue2++) {
                    ASTRenameFilterParameter aSTRenameFilterParameter = (ASTRenameFilterParameter) arrayList1.get(elementIndexValue2);
                    this.parametersByGroup.putValue(key, aSTRenameFilterParameter, aSTRenameFilterParameter);
                }
            }
        }
    }

    @Override
    public final boolean isSubclass(String key, String keyArgument) throws ZkmException, IOException {
        return this.classRepository.isSubclass(key, keyArgument);
    }
}
