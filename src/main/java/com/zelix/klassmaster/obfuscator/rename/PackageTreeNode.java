package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackageTreeNode extends PackageTreeNodeBase {
    public boolean requireLowerCaseStart;
    public final PackageTreeNodeBase parent;
    public final String simpleName;

    public PackageTreeNode(PackageTreeNodeBase packageTreeNodeBase, String simpleNameArgument, boolean bl) {
        this.parent = packageTreeNodeBase;
        this.simpleName = simpleNameArgument;
        if (!bl) {
            this.setArchiveOnly(false);
        }
    }

    public void collectRetainedPackageNames(Map map1, NameExclusionSet nameExclusionSet, ChangeLogMapping changeLogMapping1, String string) throws IOException {
        Iterator iterator = super.children.iterator();

        while (iterator.hasNext()) {
            String value = string;
            PackageTreeNode packageTreeNode1 = (PackageTreeNode) iterator.next();
            String packagePath = packageTreeNode1.getPackagePath();
            if (changeLogMapping1 != null && changeLogMapping1.hasPackageMapping(packagePath)) {
                String newPackageName = changeLogMapping1.getNewPackageName(packagePath);
                if (newPackageName != null) {
                    value = newPackageName;
                } else {
                    value = packagePath;
                }
            } else if (nameExclusionSet.isPackageExcluded(packagePath)) {
                value = packagePath;
            }

            map1.put(packagePath, value);
            packageTreeNode1.collectRetainedPackageNames(map1, nameExclusionSet, changeLogMapping1, value);
        }
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void indexInto(Object object) {
        String packagePath = this.getPackagePath();
        ((Map) object).put(packagePath, this);
    }

    public PackageTreeNode getTopLevelAncestor() {
        return this.parent.isRoot() ? this : ((PackageTreeNode) this.parent).getTopLevelAncestor();
    }

    public void setRequireLowerCaseStart() {
        this.requireLowerCaseStart = true;
    }

    public void assignNewNamesRecursively(
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            PackageNameGeneratorBase packageNameGeneratorBase,
            Map map1,
            ListMultimap listMultimap,
            TwoKeyMap twoKeyMap,
            ListMultimap listMultimap1,
            boolean bl
    ) throws IOException {
        HashSet hashSet = ZkmUtils.createHashSet();
        List list1 = listMultimap1.getValues(this.parent.getPackagePath());
        if (list1 != null) {
            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                ProgramClass programClass1 = (ProgramClass) list1.get(elementIndex);
                if (nameExclusionSet.isClassExcluded(programClass1)) {
                    hashSet.add(programClass1.getSimpleName());
                }
            }
        }

        this.assignNewName(nameExclusionSet, changeLogMapping1, packageNameGeneratorBase, map1, listMultimap, twoKeyMap, hashSet);
        PackageTreeNode[] packageTreeNodes = (PackageTreeNode[]) super.children.toArray(new PackageTreeNode[super.children.size()]);
        if (!bl && !HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
            Arrays.sort(packageTreeNodes);
        } else {
            ZkmUtils.shuffleList(Arrays.asList(packageTreeNodes), ZkmUtils.createRandom(16));
        }

        for (int packageTreeNodeIndex = 0; packageTreeNodeIndex < packageTreeNodes.length; packageTreeNodeIndex++) {
            packageTreeNodes[packageTreeNodeIndex]
                    .assignNewNamesRecursively(nameExclusionSet, changeLogMapping1, packageNameGeneratorBase, map1, listMultimap, twoKeyMap, listMultimap1, bl);
        }
    }

    @Override
    public void collectPath(Object object) {
        String packagePath = this.getPackagePath();
        ((ArrayList) object).add(packagePath);
    }

    public void assignNewName(
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            PackageNameGeneratorBase packageNameGeneratorBase,
            Map map1,
            ListMultimap listMultimap,
            TwoKeyMap twoKeyMap,
            Set set1
    ) throws IOException {
        String packagePath = this.getPackagePath();
        boolean bl = this.isTopLevel() && this.requireLowerCaseStart;
        if (!map1.containsKey(packagePath)) {
            String packagePathValue = (String) map1.get(this.parent.getPackagePath());
            if (packagePathValue == null) {
                packagePathValue = this.parent.getPackagePath();
            }

            String string2 = packageNameGeneratorBase.generatePackageName(
                    packagePathValue, this.isArchiveOnly(), bl, listMultimap, twoKeyMap, set1, changeLogMapping1, nameExclusionSet
            );
            String value = (packagePathValue.length() > 0 ? packagePathValue + "/" : "") + string2;
            map1.put(packagePath, value);
            listMultimap.addValue(value, packagePath);
        }
    }

    public boolean isTopLevel() {
        return this.parent.isRoot();
    }

    public String getSimpleName() {
        return this.simpleName;
    }

    @Override
    public String getPackagePath() {
        String string = this.parent == null ? null : this.parent.getPackagePath();
        String simpleNameValue;
        if (string != null) {
            if (string.length() > 0) {
                return string + "/" + this.simpleName;
            }

            simpleNameValue = this.simpleName;
        } else {
            simpleNameValue = this.simpleName;
        }

        return simpleNameValue;
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
