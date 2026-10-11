package com.zelix.klassmaster.classfile.insn;

public class MemberRefKey {
    public final String className;
    public final String memberName;
    public final String descriptor;
    private final int hash;

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MemberRefKey)) {
            return false;
        }

        MemberRefKey memberRefKey1 = (MemberRefKey) other;
        return this.hash == memberRefKey1.hash
                && this.className.equals(memberRefKey1.className)
                && this.memberName.equals(memberRefKey1.memberName)
                && this.descriptor.equals(memberRefKey1.descriptor);
    }

    public MemberRefKey(String classNameArgument, String memberNameArgument, String descriptorArgument) {
        this.className = classNameArgument;
        this.memberName = memberNameArgument;
        this.descriptor = descriptorArgument;
        this.hash = classNameArgument.hashCode() ^ memberNameArgument.hashCode() ^ descriptorArgument.hashCode();
    }
}
