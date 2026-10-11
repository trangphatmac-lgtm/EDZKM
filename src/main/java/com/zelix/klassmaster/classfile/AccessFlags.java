package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.VisitableNode;

import java.io.IOException;

public class AccessFlags extends ClassFileComponent {
    private int flags;

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public final void makePublic() {
        this.flags = makePublic(this.flags);
    }

    public static int makePrivate(int flags) {
        int updatedFlags = flags;
        updatedFlags &= -8;
        return updatedFlags | 2;
    }

    public static boolean isEnum(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 16384) != 0
                : flags != 0;
    }

    public final boolean isSynthetic() {
        return isSynthetic(this.flags);
    }

    public static boolean isInterface(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 512) != 0
                : flags != 0;
    }

    public static int setVolatileFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 64;
        } else {
            updatedFlags &= -65;
        }

        return updatedFlags;
    }

    static {
        new MutableInt(1);
        new MutableInt(2);
        new MutableInt(3);
        new MutableInt(4);
    }

    public final void markVarargs() {
        this.flags = setVarargsFlag(this.flags, true);
    }

    public final int getFlags() {
        return this.flags;
    }

    public static int addStatic(int flags) {
        return setStaticFlag(flags, true);
    }

    public static int addPublic(int flags) {
        int updatedFlags = flags;
        return updatedFlags | 1;
    }

    public static boolean isAnnotation(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 8192) != 0
                : flags != 0;
    }

    public final void makeProtected() {
        this.flags = makeProtected(this.flags);
    }

    public static int addEnum(int flags) {
        return setEnumFlag(flags, true);
    }

    public static int setVarargsFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 128;
        } else {
            updatedFlags &= -129;
        }

        return updatedFlags;
    }

    public final boolean isInterface() {
        return isInterface(this.flags);
    }

    public final void setSynthetic() {
        this.flags = setSyntheticFlag(this.flags, false);
    }

    public static int addTransient(int flags) {
        return setTransientFlag(flags, true);
    }

    public final boolean isStatic() {
        return isStatic(this.flags);
    }

    public static boolean isVarargs(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 128) != 0
                : flags != 0;
    }

    public static int addSynthetic(int flags) {
        return setSyntheticFlag(flags, true);
    }

    public static int makePackagePrivate(int flags) {
        int updatedFlags = flags;
        return updatedFlags & -8;
    }

    public final boolean isBridge() {
        return isBridge(this.flags);
    }

    public static int addAnnotation(int flags) {
        return setAnnotationFlag(flags, true);
    }

    @Override
    public void collectUsedConstants(char traversalTag, int traversalSeed, UsedConstantsCollector collector, char traversalMode) {
    }

    public static int setTransientFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 128;
        } else {
            updatedFlags &= -129;
        }

        return updatedFlags;
    }

    public static int setAbstractFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 1024;
        } else {
            updatedFlags &= -1025;
        }

        return updatedFlags;
    }

    public final void makePrivate() {
        this.flags = makePrivate(this.flags);
    }

    public static boolean isAbstract(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 1024) != 0
                : flags != 0;
    }

    public final void setStatic() {
        this.flags = setStaticFlag(this.flags, true);
    }

    public static String formatModifiers(int flags, int parentKind, boolean includeExtendedModifiers, boolean negated) {
        VisitableNode[] flowGuardNodes = ClassFileComponent.getFlowGuardNodes();
        StringBuilder modifiers = new StringBuilder();
        if (flowGuardNodes == null) {
            // Preserve the alternate behavior exposed by setFlowGuardNodes(null).
            if (flags != 0) {
                appendModifier(modifiers, "protected", negated);
                appendModifier(modifiers, "abstract", negated);
            }
            if (parentKind != 0) {
                appendModifier(modifiers, "static", negated);
            }
            if (flags != 0) {
                appendModifier(modifiers, "enum", negated);
            }
            if (parentKind != 0) {
                appendModifier(modifiers, "synthetic", negated);
            }
            return modifiers.toString();
        }

        if (isPublic(flags)) {
            appendModifier(modifiers, "public", negated);
        }
        if (isProtected(flags)) {
            appendModifier(modifiers, "protected", negated);
        }
        if (isPrivate(flags)) {
            appendModifier(modifiers, "private", negated);
        }
        if ((parentKind == 1 || parentKind == 3) && isAbstract(flags)) {
            appendModifier(modifiers, "abstract", negated);
        }
        if ((parentKind == 2 || parentKind == 3) && isStatic(flags)) {
            appendModifier(modifiers, "static", negated);
        }
        if (isFinal(flags)) {
            appendModifier(modifiers, "final", negated);
        }
        if (parentKind == 3) {
            if (isSynchronized(flags)) {
                appendModifier(modifiers, "synchronized", negated);
            }
        } else if (parentKind == 1 && includeExtendedModifiers && isSuper(flags)) {
            appendModifier(modifiers, "super", negated);
        }
        if (parentKind == 3) {
            if (isNative(flags)) {
                appendModifier(modifiers, "native", negated);
            }
        } else if ((parentKind == 1 || parentKind == 2) && isEnum(flags)) {
            appendModifier(modifiers, "enum", negated);
        }
        if (parentKind == 1 && isModule(flags)) {
            appendModifier(modifiers, "module", negated);
        }
        if (parentKind == 2) {
            if (isVolatile(flags)) {
                appendModifier(modifiers, "volatile", negated);
            }
        } else if (parentKind == 3 && isBridge(flags)) {
            appendModifier(modifiers, "bridge", negated);
        }
        if (parentKind == 2) {
            if (isTransient(flags)) {
                appendModifier(modifiers, "transient", negated);
            }
        } else if (parentKind == 3 && includeExtendedModifiers && isVarargs(flags)) {
            appendModifier(modifiers, "varargs", negated);
        }
        if (includeExtendedModifiers && isInterface(flags)) {
            appendModifier(modifiers, "interface", negated);
        }
        if (parentKind == 1) {
            if (isAnnotation(flags)) {
                appendModifier(modifiers, "annotation", negated);
            }
        } else if (parentKind == 3 && includeExtendedModifiers && isStrict(flags)) {
            appendModifier(modifiers, "strict", negated);
        }
        if (isSynthetic(flags)) {
            appendModifier(modifiers, "synthetic", negated);
        }
        return modifiers.toString();
    }

    private static void appendModifier(StringBuilder modifiers, String modifier, boolean negated) {
        if (negated) {
            modifiers.append("!");
        }
        modifiers.append(modifier);
        modifiers.append(" ");
    }

    public static String formatModifiers(int flags, int parentKind, boolean includeExtendedModifiers) {
        return formatModifiers(flags, parentKind, includeExtendedModifiers, false);
    }

    public static boolean isModule(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 32768) != 0
                : flags != 0;
    }

    public final boolean isSynchronized() {
        return isSynchronized(this.flags);
    }

    public static boolean isTransient(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 128) != 0
                : flags != 0;
    }

    public static int setFinalFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 16;
        } else {
            updatedFlags &= -17;
        }

        return updatedFlags;
    }

    public static int makeProtected(int flags) {
        int updatedFlags = flags;
        updatedFlags &= -8;
        return updatedFlags | 4;
    }

    public final boolean isPrivate() {
        return isPrivate(this.flags);
    }

    public static int setSynchronizedFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 32;
        } else {
            updatedFlags &= -33;
        }

        return updatedFlags;
    }

    public static boolean isVolatile(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 64) != 0
                : flags != 0;
    }

    public static int setNativeFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 256;
        } else {
            updatedFlags &= -257;
        }

        return updatedFlags;
    }

    public static int addBridge(int flags) {
        return setBridgeFlag(flags, true);
    }

    @Override
    public Object clone() {
        return new AccessFlags(this.getParent(), this.flags);
    }

    public static boolean isSynthetic(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 4096) != 0
                : flags != 0;
    }

    public final boolean isAbstract() {
        return isAbstract(this.flags);
    }

    public static int addVolatile(int flags) {
        return setVolatileFlag(flags, true);
    }

    public final boolean isModule() {
        return isModule(this.flags);
    }

    public static boolean isProtected(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 4) != 0
                : flags != 0;
    }

    public static boolean isNative(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 256) != 0
                : flags != 0;
    }

    public static boolean isFinal(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 16) != 0
                : flags != 0;
    }

    public final void setBridge() {
        this.flags = setBridgeFlag(this.flags, false);
    }

    public static boolean isSynchronized(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 32) != 0
                : flags != 0;
    }

    public static int addNative(int flags) {
        return setNativeFlag(flags, true);
    }

    public static int addAbstract(int flags) {
        return setAbstractFlag(flags, true);
    }

    public static boolean isPublic(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 1) != 0
                : flags != 0;
    }

    public final void setAnnotation() {
        this.flags = setAnnotationFlag(this.flags, false);
    }

    public static int setBridgeFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 64;
        } else {
            updatedFlags &= -65;
        }

        return updatedFlags;
    }

    public final boolean isVolatile() {
        return isVolatile(this.flags);
    }

    public AccessFlags(ClassFileComponent parent, ClassFileInputStream input) throws IOException {
        super(parent);
        this.flags = input.readUnsignedShort();
    }

    public static int addFinal(int flags) {
        return setFinalFlag(flags, true);
    }

    public static boolean isStrict(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 2048) != 0
                : flags != 0;
    }

    public static boolean isSuper(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 32) != 0
                : flags != 0;
    }

    public final void setEnum() {
        this.flags = setEnumFlag(this.flags, false);
    }

    public static int setInterfaceFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 512;
        } else {
            updatedFlags &= -513;
        }

        return updatedFlags;
    }

    public static int setSyntheticFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 4096;
        } else {
            updatedFlags &= -4097;
        }

        return updatedFlags;
    }

    public static int setStaticFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 8;
        } else {
            updatedFlags &= -9;
        }

        return updatedFlags;
    }

    public int getVisibilityRank() {
        if (this.isPublic()) {
            return 4;
        } else if (this.isProtected()) {
            return 3;
        } else {
            return this.isPrivate() ? 1 : 2;
        }
    }

    public final boolean isAnnotation() {
        return isAnnotation(this.flags);
    }

    public static int addInterface(int flags) {
        return setInterfaceFlag(flags, true);
    }

    public static int makePublic(int flags) {
        int updatedFlags = flags;
        updatedFlags &= -8;
        return updatedFlags | 1;
    }

    public final boolean isNative() {
        return isNative(this.flags);
    }

    public static int setAnnotationFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 8192;
        } else {
            updatedFlags &= -8193;
        }

        return updatedFlags;
    }

    public final boolean isPackagePrivate() {
        return isPackagePrivate(this.flags);
    }

    public final boolean isPublic() {
        return isPublic(this.flags);
    }

    public final void makePackagePrivate() {
        this.flags = makePackagePrivate(this.flags);
    }

    public final String formatModifiers(boolean includeExtendedModifiers) {
        return formatModifiers(this.flags, this.getParentKind(), includeExtendedModifiers);
    }

    public static int setEnumFlag(int flags, boolean enabled) {
        int updatedFlags = flags;
        if (enabled) {
            updatedFlags |= 16384;
        } else {
            updatedFlags &= -16385;
        }

        return updatedFlags;
    }

    public static boolean isBridge(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 64) != 0
                : flags != 0;
    }

    public static boolean isPrivate(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 2) != 0
                : flags != 0;
    }

    public final boolean isProtected() {
        return isProtected(this.flags);
    }

    public final void setFinal(boolean enabled) {
        this.flags = setFinalFlag(this.flags, enabled);
    }

    public final void setVarargs() {
        this.flags = setVarargsFlag(this.flags, false);
    }

    public final boolean isFinal() {
        return isFinal(this.flags);
    }

    public static int addSynchronized(int flags) {
        return setSynchronizedFlag(flags, true);
    }

    public String getModifierString() {
        return this.formatModifiers(false);
    }

    public final boolean isSuper() {
        return isSuper(this.flags);
    }

    public AccessFlags(ClassFileComponent parent, int flags) {
        super(parent);
        this.flags = flags;
    }

    public static int addProtected(int flags) {
        int updatedFlags = flags;
        return updatedFlags | 4;
    }

    public static boolean isStatic(int flags) {
        return ClassFileComponent.getFlowGuardNodes() != null
                ? (flags & 8) != 0
                : flags != 0;
    }

    public final boolean isEnum() {
        return isEnum(this.flags);
    }

    public final boolean isVarargs() {
        return isVarargs(this.flags);
    }

    public static boolean isPackagePrivate(int flags) {
        return (flags & 7) == 0;
    }

    public AccessFlags(ClassFileComponent parent) {
        super(parent);
        this.flags = 0;
    }

    public final boolean isTransient() {
        return isTransient(this.flags);
    }

    public static int addPrivate(int flags) {
        int updatedFlags = flags;
        return updatedFlags | 2;
    }
}
