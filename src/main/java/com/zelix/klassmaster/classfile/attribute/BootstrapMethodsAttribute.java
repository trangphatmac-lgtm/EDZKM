package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodHandleConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BootstrapMethodsAttribute extends ParsedAttributeBase {
    public BootstrapMethodEntry[] entries;

    public BootstrapMethodEntry addEntry(ResolvedMethodHandleConstant resolvedMethodHandleConstant, ConstantPoolEntry[] constantPoolEntrys) {
        BootstrapMethodEntry[] bootstrapMethodEntrys = new BootstrapMethodEntry[this.entries.length + 1];
        System.arraycopy(this.entries, 0, bootstrapMethodEntrys, 0, this.entries.length);
        BootstrapMethodEntry bootstrapMethodEntry = new BootstrapMethodEntry(this, resolvedMethodHandleConstant, constantPoolEntrys, this.entries.length);
        bootstrapMethodEntrys[this.entries.length] = bootstrapMethodEntry;
        this.entries = bootstrapMethodEntrys;
        this.length = this.getLength();
        return bootstrapMethodEntry;
    }

    public BootstrapMethodEntry getEntry(int entryIndex) {
        if (entryIndex >= 0 && entryIndex < this.entries.length) {
            return this.entries[entryIndex];
        } else {
            throw new IllegalArgumentException(
                    "Index out of bounds in '"
                            + this.getEnclosingAttribute().getAttributeName()
                            + "' attribute in class '"
                            + this.getDisplayLocationName()
                            + "' : "
                            + entryIndex
                            + ">"
                            + (this.entries.length - 1)
            );
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        Map map1 = (Map) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ScriptEnvironment scriptEnvironment2 = scriptEnvironment1;
        Map map2 = map1;
        ScriptEnvironment scriptEnvironment3 = scriptEnvironment2;
        Map map3 = map2;
        DataOutputStream dataOutputStream1 = dataOutputStream;
        super.writeRemapped(dataOutputStream1, map3, scriptEnvironment3);
        if (super.valid) {
            dataOutputStream.writeShort(this.entries.length);
            BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries;
            int lengthValue = bootstrapMethodEntrys.length;

            for (int bootstrapMethodEntryIndex = 0; bootstrapMethodEntryIndex < lengthValue; bootstrapMethodEntryIndex++) {
                bootstrapMethodEntrys[bootstrapMethodEntryIndex].writeRemapped(dataOutputStream, map1);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void collectUsedConstants(char bd, int ba, UsedConstantsCollector usedConstantsCollector, char be) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        int entryIndex = 0;
        int bc = 0;
        BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries;

        while (true) {
            if (bc < bootstrapMethodEntrys.length) {
                this.entries[entryIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
            } else if (ba >= 0) {
                return;
            }

            bc = ++entryIndex;
            bootstrapMethodEntrys = this.entries;
        }
    }

    public boolean isEmpty() {
        return this.entries.length == 0;
    }

    public void collectMethodHandleTargets(Set set1) {
        if (super.valid) {
            BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries;
            int lengthValue = bootstrapMethodEntrys.length;

            for (int bootstrapMethodEntryIndex = 0; bootstrapMethodEntryIndex < lengthValue; bootstrapMethodEntryIndex++) {
                bootstrapMethodEntrys[bootstrapMethodEntryIndex].collectMethodHandleTargets(set1);
            }
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.entries.length);
            BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries;
            int lengthValue = bootstrapMethodEntrys.length;

            for (int bootstrapMethodEntryIndex = 0; bootstrapMethodEntryIndex < lengthValue; bootstrapMethodEntryIndex++) {
                bootstrapMethodEntrys[bootstrapMethodEntryIndex].write(dataOutputStream);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    public void retainEntries(Set set1) {
        ArrayList arrayList = new ArrayList(this.entries.length);

        for (BootstrapMethodEntry bootstrapMethodEntry : this.entries) {
            if (set1.contains(bootstrapMethodEntry)) {
                arrayList.add(bootstrapMethodEntry);
            }
        }

        if (arrayList.size() < this.entries.length) {
            this.entries = ((com.zelix.klassmaster.classfile.attribute.BootstrapMethodEntry[]) (arrayList.toArray(new BootstrapMethodEntry[arrayList.size()])));
            this.length = this.getLength();
        }
    }

    public BootstrapMethodsAttribute(ClassFileBase classFileBase, ConstantUtf8 constantUtf8) {
        super(classFileBase, constantUtf8);
        this.entries = new BootstrapMethodEntry[0];
    }

    public BootstrapMethodIndex buildStringConcatIndex() {
        int lengthValue = this.entries.length;
        BootstrapMethodIndex bootstrapMethodIndex1 = new BootstrapMethodIndex((ProgramClass) this.getOwningClass(), this);

        for (int entryIndex = 0; entryIndex < lengthValue; entryIndex++) {
            BootstrapMethodEntry bootstrapMethodEntry = this.entries[entryIndex];
            if (bootstrapMethodEntry.isInvokeStaticHandle() && bootstrapMethodEntry.matchesBootstrapMethod()) {
                ConstantPoolEntry[] constantPoolEntrys = bootstrapMethodEntry.copyArguments();
                ConstantPoolEntry constantPoolEntry = constantPoolEntrys[0];
                if (constantPoolEntry instanceof ResolvedStringConstant) {
                    String editableValue = ((ResolvedStringConstant) constantPoolEntry).getEditableValue();
                    MutableInt mutableInt = new MutableInt();
                    List list1 = BootstrapMethodIndex.tokenizeRecipe(editableValue, mutableInt);
                    if (list1 != null) {
                        boolean bl = false;
                        Iterator iterator = list1.iterator();

                        while (iterator.hasNext()) {
                            String string1 = (String) iterator.next();
                            if (string1.length() >= 2) {
                                bl = true;
                                break;
                            }
                        }

                        boolean bl1 = false;
                        if (constantPoolEntrys.length > 1) {
                            for (int constantPoolEntryIndex = 1; constantPoolEntryIndex < constantPoolEntrys.length; constantPoolEntryIndex++) {
                                if (!(constantPoolEntrys[constantPoolEntryIndex] instanceof ResolvedStringConstant)) {
                                    bl1 = true;
                                    break;
                                }

                                if (!bl && ((ResolvedStringConstant) constantPoolEntrys[constantPoolEntryIndex]).getValueString().length() >= 2) {
                                    bl = true;
                                }
                            }
                        }

                        if (bl && !bl1) {
                            bootstrapMethodIndex1.registerConcatBootstrap(bootstrapMethodEntry, list1, mutableInt.getValue());
                        }
                    }
                }
            }
        }

        if (!bootstrapMethodIndex1.isEmpty()) {
            bootstrapMethodIndex1.expandRecipeConstants();
        }

        return bootstrapMethodIndex1;
    }

    @Override
    public int getLength() {
        int lengthValue = 2;

        for (BootstrapMethodEntry bootstrapMethodEntry : this.entries) {
            lengthValue += bootstrapMethodEntry.getByteLength();
        }

        this.length = lengthValue;
        return lengthValue;
    }

    public BootstrapMethodsAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3,
            ListMultimap listMultimap4,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        byte[] rawBytesValue = new byte[this.length];
        classFileInputStream.read(rawBytesValue);
        ClassFileInputStream classFileInputStream1 = ClassFileInputStream.fromBytes(rawBytesValue, false);

        try {
            if (this.length >= 2) {
                int bc = classFileInputStream1.readUnsignedShort();
                this.entries = new BootstrapMethodEntry[bc];

                for (int index = 0; index < bc; index++) {
                    this.entries[index] = new BootstrapMethodEntry(this, classFileInputStream1, index, listMultimap1, listMultimap2, listMultimap3, listMultimap4);
                    if (!this.entries[index].isValid()) {
                        super.valid = false;
                        super.rawBytes = rawBytesValue;
                        printWriter.println(
                                "ERROR: " + this.getDisplayLocationName() + " : " + this.getAttributeName() + " (C) " + this.entries[index].getErrorMessage()
                        );
                    }
                }
            } else {
                super.valid = false;
                super.rawBytes = rawBytesValue;
                printWriter.println("ERROR: " + this.getDisplayLocationName() + " : " + this.getAttributeName() + " : length=" + this.length);
            }
        } catch (IOException iOException) {
            super.valid = false;
            super.rawBytes = rawBytesValue;
            printWriter.println("ERROR: " + this.getDisplayLocationName() + " : " + this.getAttributeName() + " is possibly corrupt : " + iOException);
        } finally {
            classFileInputStream1.close();
        }
    }

    public void updateRecordObjectMethodsNames() throws ZkmException, IOException {
        if (super.valid && this.getOwningClass().getSuperclassName().equals("java/lang/Record")) {
            BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries;
            int lengthValue = bootstrapMethodEntrys.length;

            for (int bootstrapMethodEntryIndex = 0; bootstrapMethodEntryIndex < lengthValue; bootstrapMethodEntryIndex++) {
                bootstrapMethodEntrys[bootstrapMethodEntryIndex].updateObjectMethodsNames();
            }
        }
    }

    public void renumberEntries() {
        int index = 0;
        int bb = 0;

        for (BootstrapMethodEntry[] bootstrapMethodEntrys = this.entries; bb < bootstrapMethodEntrys.length; bootstrapMethodEntrys = this.entries) {
            this.entries[index].setIndex(index);
            bb = ++index;
        }
    }
}
