package com.zelix.klassmaster.changelog;

public class ChangeLogMemberKey extends ChangeLogMemberRecord implements Comparable {
    /** parameterTypes; the field name is retained for serialized settings compatibility. */
    public final String n;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChangeLogMemberKey)) {
            return false;
        }

        ChangeLogMemberKey changeLogMemberKey1 = (ChangeLogMemberKey) other;
        return this.H.equals(changeLogMemberKey1.H)
                && this.n.equals(changeLogMemberKey1.n)
                && (this.v == null && changeLogMemberKey1.v == null || this.v != null && changeLogMemberKey1.v != null && this.v.equals(changeLogMemberKey1.v));
    }

    public final int compareToKey(ChangeLogMemberKey changeLogMemberKey1) {
        int ba = this.H.compareTo(changeLogMemberKey1.H);
        if (ba != 0) {
            return ba;
        }

        ba = this.n.compareTo(changeLogMemberKey1.n);
        return ba == 0 && this.v != null && changeLogMemberKey1.v != null ? this.v.compareTo(changeLogMemberKey1.v) : ba;
    }

    @Override
    public int hashCode() {
        return this.v == null ? this.H.hashCode() ^ this.n.hashCode() : this.H.hashCode() ^ this.n.hashCode() ^ this.v.hashCode();
    }

    public ChangeLogMemberKey(String name, String parameterTypes, String type, int accessFlags) {
        super(name, type, accessFlags);
        this.n = parameterTypes;
    }

    public String getParameterTypes() {
        return this.n;
    }

    @Override
    public int compareTo(Object other) {
        return this.compareToKey((ChangeLogMemberKey) other);
    }
}
