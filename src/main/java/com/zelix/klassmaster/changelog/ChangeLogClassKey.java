package com.zelix.klassmaster.changelog;

public class ChangeLogClassKey extends ChangeLogMemberRecord implements Comparable {
    @Override
    public int hashCode() {
        return this.v == null ? this.H.hashCode() : this.H.hashCode() ^ this.v.hashCode();
    }

    public final int compareToKey(ChangeLogClassKey changeLogClassKey1) {
        return this.H.compareTo(changeLogClassKey1.H);
    }

    public ChangeLogClassKey(String name, String type, int accessFlags) {
        super(name, type, accessFlags);
    }

    @Override
    public int compareTo(Object other) {
        return this.compareToKey((ChangeLogClassKey) other);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChangeLogClassKey)) {
            return false;
        }

        ChangeLogClassKey changeLogClassKey1 = (ChangeLogClassKey) other;
        return this.H.equals(changeLogClassKey1.H)
                && (this.v == null && changeLogClassKey1.v == null || this.v != null && changeLogClassKey1.v != null && this.v.equals(changeLogClassKey1.v));
    }
}
