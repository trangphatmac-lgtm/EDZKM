package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.MemberInfo;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class MemberListNode extends ListClassProperty {
    public int memberCount;
    public MemberInfo[] members;

    public boolean hasNextMember() {
        return super.cursor < this.memberCount;
    }

    public void updateMembers(MemberInfo[] memberInfos) throws ZkmException, IOException {
        this.initMembers(memberInfos);
        this.setChanged();
        this.notifyObservers();
    }

    public String nextMemberText() {
        return this.members[super.cursor++].getSourceName();
    }

    public int getMemberIndex(Object key) {
        if (super.indexMap == null) {
            super.indexMap = ZkmUtils.createHashMap((int) (this.memberCount * 1.5));
            int integer = 0;
            int bb = 0;

            for (int memberCountValue = this.memberCount; bb < memberCountValue; memberCountValue = this.memberCount) {
                super.indexMap.put(this.members[integer], super.integerCache.valueOf(integer));
                bb = ++integer;
            }
        }

        Object object1 = super.indexMap.get(key);
        return object1 != null ? (Integer) object1 : -1;
    }

    public MemberListNode(String propertyName, ClassFileComponent classFileComponent, MemberInfo[] memberInfos, IntegerCache integerCache1) {
        super(propertyName, classFileComponent, integerCache1);
        this.initMembers(memberInfos);
    }

    public MemberInfo getMember(int memberIndex) {
        return this.members[memberIndex];
    }

    public void initMembers(MemberInfo[] memberInfos) {
        int lengthValue = memberInfos.length;
        Vector vector = new Vector(lengthValue);

        for (int memberInfoIndex = 0; memberInfoIndex < lengthValue; memberInfoIndex++) {
            if (memberInfos[memberInfoIndex].isValid()) {
                vector.addElement(memberInfos[memberInfoIndex]);
            }
        }

        this.memberCount = vector.size();
        this.members = new MemberInfo[this.memberCount];
        vector.copyInto(this.members);
        Arrays.sort(this.members);
        super.indexMap = null;
        this.resetCursor();
    }
}
