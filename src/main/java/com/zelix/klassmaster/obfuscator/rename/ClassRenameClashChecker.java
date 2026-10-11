package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.engine.ObfuscatorEngineBase;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassRenameClashChecker {
    public Set allLowerCaseNames;
    public Set fileClassLowerCaseNames;
    public ClassNameGenerator nameGenerator;
    public final Set fixedClasses = ZkmUtils.createHashSet();
    public final Set fixedClassNames = ZkmUtils.createHashSet();
    public final Map classesByName = ZkmUtils.createHashMap();
    public Set pendingNodes = ZkmUtils.createHashSet();
    public final ProgramClass[] programClasses;
    public final ClassFileBase[] allClassFiles;
    public ChangeLogMapping changeLogMapping;
    public boolean randomizeOrder;
    public final ClassHierarchy classHierarchy;
    public final ClassRepository classRepository;
    public final NameExclusionSet exclusionSet;
    public final HashMap oldToNewNames;
    public final HashMap newToOldNames;

    public void renameClasses(
            ClassNameGenerator classNameGenerator, HashMap hashMap, HashMap hashMap1, ListMultimap listMultimap, MessageReporter messageReporter1
    ) throws ZkmException, IOException {
        int keepInnerClassInfoMode = classNameGenerator.getKeepInnerClassInfoMode();
        int keepGenericsInfoMode = classNameGenerator.getKeepGenericsInfoMode();
        this.nameGenerator = classNameGenerator;
        this.checkChangeLogClashes(listMultimap);
        int lengthValue = this.programClasses.length;
        this.fileClassLowerCaseNames = classNameGenerator.fileClassLowerCaseNames;
        this.allLowerCaseNames = classNameGenerator.allLowerCaseNames;

        for (int programClasseIndex = 0; programClasseIndex < lengthValue; programClasseIndex++) {
            this.classesByName.put(this.programClasses[programClasseIndex].getClassName(), this.programClasses[programClasseIndex]);
            if (this.exclusionSet.isClassExcluded(this.programClasses[programClasseIndex]) || this.programClasses[programClasseIndex].getSimpleName().equals("package-info")) {
                this.programClasses[programClasseIndex].getClassName();
                this.fixedClasses.add(this.programClasses[programClasseIndex]);
            }
        }

        Iterator iterator1 = this.fixedClasses.iterator();

        while (iterator1.hasNext()) {
            ProgramClass programClass1 = (ProgramClass) iterator1.next();
            ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(programClass1.getClassName());
            classHierarchyNode.registerClassName(
                    classNameGenerator, this.oldToNewNames, this.newToOldNames, this.fixedClassNames, this.fileClassLowerCaseNames, this.allLowerCaseNames
            );
        }

        List list1 = this.classHierarchy.getTopProgramNodes();
        if (HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
        }

        if (this.randomizeOrder || HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
            ZkmUtils.shuffleList(list1, ZkmUtils.createRandom(2));
        }

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) list1.get(elementIndex);
            classHierarchyNode1.generateClassNames(
                    classNameGenerator, this.changeLogMapping, this.oldToNewNames, this.newToOldNames, this.fixedClasses, this.pendingNodes
            );
        }

        int pendingNodesSize = this.pendingNodes.size();

        int pendingNodesSizeValue;
        do {
            pendingNodesSizeValue = pendingNodesSize;
            Iterator iterator = this.pendingNodes.iterator();

            while (iterator.hasNext()) {
                ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) iterator.next();
                boolean bl = classHierarchyNode2.tryGenerateClassName(classNameGenerator, this.oldToNewNames, this.newToOldNames, iterator);
                if (bl) {
                    pendingNodesSize = this.pendingNodes.size();
                }
            }
        } while (pendingNodesSize > 0 && pendingNodesSize != pendingNodesSizeValue);

        Iterator iterator2 = this.pendingNodes.iterator();

        while (iterator2.hasNext()) {
            ClassHierarchyNode classHierarchyNode3 = (ClassHierarchyNode) iterator2.next();
            classHierarchyNode3.assignGeneratedClassName(classNameGenerator, this.oldToNewNames, this.newToOldNames);
        }

        if (HiddenOptionFlags.MARK_RENAMED_LOCALS) {
        }

        int programClasseIndexValue = 0;
        int bi = 0;

        for (ProgramClass[] programClass2 = this.programClasses; bi < programClass2.length; programClass2 = this.programClasses) {
            String className = this.programClasses[programClasseIndexValue].getClassName();
            String classNameValue = (String) this.oldToNewNames.get(className);
            if (classNameValue != null && !classNameValue.equals(className)) {
                this.programClasses[programClasseIndexValue].renameClass(classNameValue, hashMap);
            }

            bi = ++programClasseIndexValue;
        }

        if (this.changeLogMapping != null) {
            ObfuscatorEngineBase.applyChangeLogClassNames(
                    this.oldToNewNames, this.newToOldNames, hashMap, this.classRepository.getClasspathLoader(), this.changeLogMapping
            );
        }

        this.classRepository
                .applyNameChanges(this.allClassFiles, this.classRepository.getModuleInfoClasses(), keepInnerClassInfoMode, keepGenericsInfoMode, this.oldToNewNames, hashMap1, messageReporter1);
        this.classRepository.refreshIndexes();
    }

    public void checkChangeLogClashes(ListMultimap listMultimap) throws IOException {
        if (this.changeLogMapping != null) {
            ArrayList arrayList = this.changeLogMapping.getOldClassNames();
            int arrayListSize = arrayList.size();

            for (int elementIndex = 0; elementIndex < arrayListSize; elementIndex++) {
                String newClassNameValue = (String) arrayList.get(elementIndex);
                if (ClassHierarchyNode.isProgramClassName(newClassNameValue)
                        && (!this.exclusionSet.isClassNameExcluded(newClassNameValue) || !this.changeLogMapping.isSimpleNameChanged(newClassNameValue))) {
                    String newClassName = this.changeLogMapping.getNewClassName(newClassNameValue);
                    if (newClassName != null) {
                        ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(newClassName);
                        if (classHierarchyNode != null && !classHierarchyNode.isProgramClass()) {
                            this.changeLogMapping
                                    .logWarning(
                                            "\""
                                                    + ZkmUtils.slashesToDots(newClassName)
                                                    + "\" is specified as a new class name but it clashes with an existing class that has not been opened."
                                    );
                        }
                    } else {
                        newClassName = newClassNameValue;
                    }

                    if (listMultimap != null && listMultimap.containsKey(newClassName) && this.changeLogMapping.hasClassesOpenedFromFileSystem()) {
                        List list1 = listMultimap.getValues(newClassName);
                        String string2 = (String) list1.get(0);
                        this.changeLogMapping
                                .reportFatalError(
                                        "Class \""
                                                + ZkmUtils.slashesToDots(newClassNameValue)
                                                + "\" is mapped to \""
                                                + ZkmUtils.slashesToDots(newClassName)
                                                + "\" but this will result in a clash with package \""
                                                + ZkmUtils.slashesToDots(string2)
                                                + "\" that has been renamed to \""
                                                + ZkmUtils.slashesToDots(newClassName)
                                                + "\""
                                );
                    }

                    String packagePath = ClassFileBase.getPackagePath(newClassNameValue);
                    ArrayList arrayList1 = this.nameGenerator.getPackageClashCandidates(newClassName, packagePath);

                    for (int elementIndexValue = 0; elementIndexValue < arrayList1.size(); elementIndexValue++) {
                        String key = (String) arrayList1.get(elementIndexValue);
                        if (!key.equals(newClassNameValue) && ClassHierarchyNode.isProgramClassName(key) && this.exclusionSet.isClassNameExcluded(key)) {
                            String string4 = !this.changeLogMapping.hasClassMapping(key) ? " new" : "";
                            this.changeLogMapping
                                    .reportFatalError(
                                            "Class \""
                                                    + ZkmUtils.slashesToDots(newClassNameValue)
                                                    + "\" is mapped to \""
                                                    + ZkmUtils.slashesToDots(newClassName)
                                                    + "\" but this will result in a clash with excluded"
                                                    + string4
                                                    + " class \""
                                                    + ZkmUtils.slashesToDots(key)
                                                    + "\" that will be also be renamed to \""
                                                    + ZkmUtils.slashesToDots(newClassName)
                                                    + "\""
                                    );
                        }
                    }
                }
            }
        }
    }

    public ClassRenameClashChecker(
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            boolean randomizeOrder,
            ClassRepository classRepository1,
            ProgramClass[] programClass1,
            ClassFileBase[] classFileBases,
            ClassHierarchy classHierarchy1,
            HashMap hashMap,
            HashMap hashMap1
    ) {
        this.programClasses = programClass1;
        this.allClassFiles = classFileBases;
        this.changeLogMapping = changeLogMapping1;
        this.randomizeOrder = randomizeOrder;
        this.classHierarchy = classHierarchy1;
        this.classRepository = classRepository1;
        this.exclusionSet = nameExclusionSet;
        this.oldToNewNames = hashMap;
        this.newToOldNames = hashMap1;
    }

    public boolean isRandomizeOrder() {
        return this.randomizeOrder;
    }
}
