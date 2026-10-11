package com.zelix.klassmaster.obfuscator.rename;

public class NamedFlag {
    public String name;
    public boolean blocked;

    public void setBlocked() {
        this.blocked = true;
    }

    public NamedFlag(String nameArgument, boolean bl) {
        this.name = nameArgument;
        this.blocked = true;
    }

    public String getName() {
        return this.name;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public NamedFlag(String nameArgument) {
        this.name = nameArgument;
    }
}
