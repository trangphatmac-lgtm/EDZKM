package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.classfile.attribute.Attribute;
import com.zelix.klassmaster.classfile.constpool.AbstractConstantPool;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolProvider;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ObservableSupport;
import com.zelix.klassmaster.util.VisitableNode;

import java.io.IOException;

public abstract class ClassFileComponent extends ObservableSupport implements ConstantPoolProvider {
    private ClassFileComponent parent;
    private static VisitableNode[] flowGuardNodes;
    public static final IntegerCache integerCache = IntegerCache.getInstance();

    public String getClassSimpleName() {
        return this.parent.getClassSimpleName();
    }

    public String getInputPath() {
        return this.parent.getInputPath();
    }

    public void setParent(ClassFileComponent parent) {
        this.parent = parent;
    }

    public static VisitableNode[] getFlowGuardNodes() {
        return flowGuardNodes;
    }

    public abstract void collectUsedConstants(char controlTag, int controlSeed, UsedConstantsCollector collector, char controlMode);

    public String getOriginalClassName() {
        return this.parent.getOriginalClassName();
    }

    public String getLocationName() {
        return this.parent.getLocationName();
    }

    public static void setFlowGuardNodes(VisitableNode[] nodes) {
        flowGuardNodes = nodes;
    }

    public boolean isClassFile() {
        return false;
    }

    public String getDisplayLocationName() {
        return this.parent.getDisplayLocationName();
    }

    public ClassFileBase getOwningClass() {
        ClassFileComponent ancestor = this;

        while (ancestor.parent != null) {
            ancestor = ancestor.parent;
        }

        return (ClassFileBase) ancestor;
    }

    public String getPackagePath() {
        return this.parent.getPackagePath();
    }

    @Override
    public ConstantPoolEntry getConstantPoolEntry(int constantPoolIndex) {
        return this.parent.getConstantPoolEntry(constantPoolIndex);
    }

    public String formatDeclaration() throws ZkmProcessingException, IOException {
        return "";
    }

    public ClassFileComponent(ClassFileComponent parent) {
        this.parent = parent;
    }

    public String getOriginalDottedName() {
        return this.parent.getOriginalDottedName();
    }

    public final ProgramClass selectMatchingProgramClass(ProgramClass programClass) {
        return (ProgramClass) this.selectMatchingVariant(programClass);
    }

    public ClassFileComponent getParent() {
        return this.parent;
    }

    public final Attribute getEnclosingAttribute() {
        ClassFileComponent ancestor = this;

        while (ancestor.parent != null && !(ancestor instanceof Attribute)) {
            ancestor = ancestor.parent;
        }

        return (Attribute) ancestor;
    }

    public String getDottedClassName() {
        return this.parent.getDottedClassName();
    }

    public String getClassName() {
        return this.parent.getClassName();
    }

    public AbstractConstantPool getConstantPool() {
        return this.parent.getConstantPool();
    }

    static {
        VisitableNode[] initialFlowGuardNodes = new VisitableNode[4];
        setFlowGuardNodes(initialFlowGuardNodes);
    }

    public final ClassFileBase selectMatchingVariant(ClassFileBase candidateClass) {
        ClassFileBase owningClass = this.getOwningClass();
        ClassFileBase matchingClass;
        if (owningClass.isVersionedVariant() && candidateClass.hasVersionedVariants()) {
            matchingClass = candidateClass.selectVersionForRelease(owningClass.getReleaseVersion());
        } else {
            matchingClass = candidateClass;
        }

        return matchingClass;
    }

    public final int getParentKind() {
        if (this.parent instanceof ClassFileBase) {
            return 1;
        } else if (this.parent instanceof AbstractFieldInfo) {
            return 2;
        } else {
            return this.parent instanceof AbstractMethodInfo ? 3 : 4;
        }
    }
}
