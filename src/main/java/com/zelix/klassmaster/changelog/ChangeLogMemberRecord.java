package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.AccessFlags;

import java.io.Serializable;

public abstract class ChangeLogMemberRecord implements Serializable {
    /** name; the field name is retained for serialized settings compatibility. */
    public final String H;
    /** type; the field name is retained for serialized settings compatibility. */
    public final String v;
    /** accessFlags; the field name is retained for serialized settings compatibility. */
    public final int Y;

    public ChangeLogMemberRecord(String name, String type, int accessFlags) {
        this.H = name;
        this.v = type;
        this.Y = accessFlags;
    }

    public String getName() {
        return this.H;
    }

    public String getType() {
        return this.v;
    }

    public boolean isPrivate() {
        return AccessFlags.isPrivate(this.Y);
    }
}
