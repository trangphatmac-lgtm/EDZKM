package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.insn.InstructionUsageClearFlag;
import com.zelix.klassmaster.classfile.insn.InstructionUsageFlag;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.classfile.insn.LabelTargetHolder;
import com.zelix.klassmaster.classfile.insn.LocalVariableList;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.RankedValue;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class LocalVariableTableBase extends Attribute {
    public boolean valid = true;
    public final LocalVariableList localVariableList;
    public int entryCount;
    public LocalVariableEntry[] entries;
    public byte[] rawBytes;

    @Override
    public final void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (this.valid) {
            this.writeEntries(dataOutputStream);
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    public void registerLabelTargets(SetMultiMap setMultiMap) {
        if (this.valid) {
            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                this.entries[entryIndex].registerLabelTargets(setMultiMap);
            }
        }
    }

    public void removeEntriesAtLabels(HashSet hashSet, SetMultiMap setMultiMap) {
        if (this.valid && hashSet.size() > 0) {
            ArrayList arrayList = new ArrayList(this.entries.length);

            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                if (hashSet.contains(this.entries[entryIndex].getStartLabel()) && hashSet.contains(this.entries[entryIndex].getEndLabel())) {
                    setMultiMap.removeValue(this.entries[entryIndex].getStartLabel(), this.entries[entryIndex]);
                    setMultiMap.removeValue(this.entries[entryIndex].getEndLabel(), this.entries[entryIndex]);
                    this.clearUnsharedLabelUsage(this.entries[entryIndex].getStartLabel(), setMultiMap);
                    this.clearUnsharedLabelUsage(this.entries[entryIndex].getEndLabel(), setMultiMap);
                } else {
                    arrayList.add(this.entries[entryIndex]);
                }
            }

            if (arrayList.size() < this.entries.length) {
                LocalVariableEntry[] localVariableEntrys = new LocalVariableEntry[arrayList.size()];
                this.entries = ((com.zelix.klassmaster.classfile.attribute.LocalVariableEntry[]) (arrayList.toArray(localVariableEntrys)));
                this.entryCount = this.entries.length;
                this.length = this.entryCount * 10 + 2;
            }

            for (LocalVariableEntry localVariableEntry : this.entries) {
                if (!localVariableEntry.getStartLabel().hasUsageBits(2)) {
                    localVariableEntry.getStartLabel().addUsageFlag(InstructionUsageFlag.SET_USAGE_LOCAL_VARIABLE);
                }

                if (!localVariableEntry.getEndLabel().hasUsageBits(2)) {
                    localVariableEntry.getEndLabel().addUsageFlag(InstructionUsageFlag.SET_USAGE_LOCAL_VARIABLE);
                }
            }
        }
    }

    @Override
    public void remapClassNames(Object object, Object object1, Object object3, Object object2) throws ZkmProcessingException {
        int bb = (Integer) object;
        int ba = (Integer) object1;
        HashMap hashMap = (HashMap) object2;
        if (this.valid) {
            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                LocalVariableEntry localVariableEntry = this.entries[entryIndex];
                HashMap hashMap1 = hashMap;
                Integer integer = ba;
                localVariableEntry.remapDescriptorClassNames(bb, integer, hashMap1);
            }
        }
    }

    public final void writeEntries(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.entries.length);

        for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
            this.entries[entryIndex].writeTo(dataOutputStream);
        }
    }

    @Override
    public final void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        Map map1 = (Map) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ScriptEnvironment scriptEnvironment2 = scriptEnvironment1;
        Map map2 = map1;
        ScriptEnvironment scriptEnvironment3 = scriptEnvironment2;
        Map map3 = map2;
        DataOutputStream dataOutputStream1 = dataOutputStream;
        super.writeRemapped(dataOutputStream1, map3, scriptEnvironment3);
        if (this.valid) {
            dataOutputStream.writeShort(this.entries.length);

            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                this.entries[entryIndex].writeRemapped(dataOutputStream, map1);
            }
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    public boolean clearUnsharedLabelUsage(LabelInstruction labelInstruction, SetMultiMap setMultiMap) {
        if (!this.valid) {
            return false;
        }

        Set set1 = setMultiMap.getValues(labelInstruction);
        if (set1 != null) {
            Iterator iterator = set1.iterator();

            while (iterator.hasNext()) {
                if ((LabelTargetHolder) iterator.next() instanceof LocalVariableEntry) {
                    return false;
                }
            }
        }

        labelInstruction.clearUsageFlag(InstructionUsageClearFlag.CLEAR_USAGE_LOCAL_VARIABLE);
        return true;
    }

    public abstract LocalVariableEntry readEntry(
            ClassFileInputStream classFileInputStream, LocalVariableList localVariableList1, ListMultimap listMultimap, ListMultimap listMultimap1
    ) throws IOException;

    @Override
    public final int getLength() {
        return this.valid ? 2 + this.entries.length * 10 : this.rawBytes.length;
    }

    public final void obfuscateNames() {
        if (this.valid) {
            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                this.entries[entryIndex].obfuscateName();
            }
        }
    }

    public void retainEntriesForLocals(Set set1) {
        if (this.valid) {
            ArrayList arrayList = new ArrayList(this.entries.length);

            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                LocalVariableEntry localVariableEntry = this.entries[entryIndex];
                if (set1.contains(integerCache.valueOf(localVariableEntry.getLocalIndex()))) {
                    arrayList.add(localVariableEntry);
                }
            }

            if (arrayList.size() < this.entries.length) {
                LocalVariableEntry[] localVariableEntrys = new LocalVariableEntry[arrayList.size()];
                this.entries = ((com.zelix.klassmaster.classfile.attribute.LocalVariableEntry[]) (arrayList.toArray(localVariableEntrys)));
                this.entryCount = this.entries.length;
                this.length = this.entryCount * 10 + 2;
            }
        }
    }

    public LocalVariableTableBase(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            LocalVariableList localVariableList1,
            ListMultimap listMultimap,
            PrintWriter printWriter,
            ListMultimap listMultimap1,
            String string1
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        this.localVariableList = localVariableList1;
        byte[] rawBytesValue = new byte[this.length];
        classFileInputStream.read(rawBytesValue);
        ClassFileInputStream classFileInputStream1 = ClassFileInputStream.fromBytes(rawBytesValue, false);

        try {
            if (this.length >= 2) {
                this.entryCount = classFileInputStream1.readUnsignedShort();
                if (this.entryCount * 10 + 2 == this.length) {
                    this.entries = new LocalVariableEntry[this.entryCount];

                    for (int entryIndex = 0; entryIndex < this.entryCount; entryIndex++) {
                        this.entries[entryIndex] = this.readEntry(classFileInputStream1, this.localVariableList, listMultimap, listMultimap1);
                        if (this.valid && !this.entries[entryIndex].isValid()) {
                            this.valid = false;
                            printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (C)");
                            this.rawBytes = rawBytesValue;
                            break;
                        }
                    }
                } else {
                    this.valid = false;
                    printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (A) : count=" + this.entryCount + " length=" + this.length);
                    this.rawBytes = rawBytesValue;
                }
            } else {
                this.valid = false;
                printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (B) : length=" + this.length);
                this.rawBytes = rawBytesValue;
            }
        } finally {
            classFileInputStream1.close();
        }
    }

    public Map getLocalNamesByIndex() {
        if (!this.valid) {
            return null;
        }

        ArrayList arrayList = new ArrayList(this.entries.length);
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.entries.length));

        for (LocalVariableEntry localVariableEntry : this.entries) {
            if (localVariableEntry.isValid()) {
                arrayList.add(new RankedValue(localVariableEntry.getLocalIndex(), localVariableEntry.getVariableName()));
            }
        }

        Collections.sort(arrayList);
        int rank = -1;
        Iterator iterator = arrayList.iterator();

        while (iterator.hasNext()) {
            RankedValue rankedValue = (RankedValue) iterator.next();
            if (rankedValue.getRank() > rank) {
                rank = rankedValue.getRank();
                hashMap.put(integerCache.valueOf(rank), rankedValue.getValue());
            }
        }

        return hashMap;
    }

    @Override
    public void collectUsedConstants(char bb, int bc, UsedConstantsCollector usedConstantsCollector, char bd) {
        if (this.valid) {
            usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());

            for (int entryIndex = 0; entryIndex < this.entries.length; entryIndex++) {
                this.entries[entryIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
            }
        }
    }
}
