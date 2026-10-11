package com.zelix.klassmaster.proguard.mapping.parser.ast;

import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingSimpleNode;

public abstract class ProGuardNameChangeNode extends ProGuardMappingSimpleNode {
    public String newName;
    public String oldName;

    public final void setNewName(String newNameArgument) {
        this.newName = newNameArgument;
    }

    public final String getNewName() {
        return this.newName;
    }

    public final void setOldName(String oldNameArgument) {
        this.oldName = oldNameArgument;
    }

    public ProGuardNameChangeNode(int ba) {
        super(ba);
    }

    public final String getOldName() {
        return this.oldName;
    }
}
