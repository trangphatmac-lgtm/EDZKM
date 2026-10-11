package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ClassConstantBase;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class InnerClassesAttribute extends Attribute implements Utf8ConstantReplaceable {
    public boolean valid = true;
    public int entryCount;
    public InnerClassEntry[] entries;
    public byte[] rawBytes;

    public void setValid() {
        this.valid = false;
    }

    public int removeTrimmedEntries(TrimProcessor trimProcessor1) {
        if (this.entries != null) {
            ArrayList arrayList = new ArrayList(this.entries.length);
            int entryCountValue = 0;
            int bb = 0;

            for (InnerClassEntry[] innerClassEntrys = this.entries; bb < innerClassEntrys.length; innerClassEntrys = this.entries) {
                if (!this.entries[entryCountValue].isInnerClassTrimmed(trimProcessor1)) {
                    arrayList.add(this.entries[entryCountValue]);
                }

                bb = ++entryCountValue;
            }

            entryCountValue = arrayList.size();
            if (entryCountValue < this.entries.length) {
                this.entries = ((com.zelix.klassmaster.classfile.attribute.InnerClassEntry[]) (arrayList.toArray(new InnerClassEntry[entryCountValue])));
                this.entryCount = entryCountValue;
            }
        }

        return this.entryCount;
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        Map map1 = (Map) object;
        ScriptEnvironment scriptEnvironment2 = scriptEnvironment1;
        Map map2 = map1;
        ScriptEnvironment scriptEnvironment3 = scriptEnvironment2;
        Map map3 = map2;
        DataOutputStream dataOutputStream1 = dataOutputStream;
        super.writeRemapped(dataOutputStream1, map3, scriptEnvironment3);
        if (this.valid) {
            dataOutputStream.writeShort(this.entryCount);
            int entryIndex = 0;
            int bb = 0;

            for (int entryCountValue = this.entryCount; bb < entryCountValue; entryCountValue = this.entryCount) {
                this.entries[entryIndex].writeRemapped(dataOutputStream, map1);
                bb = ++entryIndex;
            }
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (this.valid) {
            this.writeEntries(dataOutputStream);
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    public int removeEntriesForClasses(HashSet hashSet) {
        if (this.entries != null) {
            ArrayList arrayList = new ArrayList(this.entries.length);
            int entryCountValue = 0;
            int bb = 0;

            for (InnerClassEntry[] innerClassEntrys = this.entries; bb < innerClassEntrys.length; innerClassEntrys = this.entries) {
                if (!this.entries[entryCountValue].isInnerClassInSet(hashSet)) {
                    arrayList.add(this.entries[entryCountValue]);
                }

                bb = ++entryCountValue;
            }

            entryCountValue = arrayList.size();
            if (entryCountValue < this.entries.length) {
                this.entries = ((com.zelix.klassmaster.classfile.attribute.InnerClassEntry[]) (arrayList.toArray(new InnerClassEntry[entryCountValue])));
                this.entryCount = entryCountValue;
            }
        }

        return this.entryCount;
    }

    public InnerClassesAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        if (this.length >= 2) {
            this.entryCount = classFileInputStream.readUnsignedShort();
            if (this.entryCount * 8 + 2 == this.length) {
                this.entries = new InnerClassEntry[this.entryCount];
                int entryIndex = 0;
                int bd = 0;

                for (int entryCountValue = this.entryCount; bd < entryCountValue; entryCountValue = this.entryCount) {
                    this.entries[entryIndex] = new InnerClassEntry(this, classFileInputStream, listMultimap, listMultimap1);
                    if (this.valid) {
                        if (!this.entries[entryIndex].valid) {
                            this.setValid();
                            printWriter.println("ERROR: " + this.getClassName() + " : " + "Invalid InnerClasses Attribute" + " (3)");
                        } else {
                            String innerSimpleName = this.entries[entryIndex].getInnerSimpleName();
                            if (innerSimpleName != null) {
                                String innerClassName = this.entries[entryIndex].getInnerClassName();
                                String outerClassName = this.entries[entryIndex].getOuterClassName();
                                if (outerClassName != null && outerClassName.length() > innerClassName.length() - innerSimpleName.length()) {
                                    printWriter.println(
                                            "ERROR: "
                                                    + this.getClassName()
                                                    + " : "
                                                    + "Invalid InnerClasses Attribute"
                                                    + " (5) : '"
                                                    + outerClassName
                                                    + "' : '"
                                                    + innerClassName
                                                    + "' : '"
                                                    + innerSimpleName
                                                    + "'"
                                    );
                                }
                            }
                        }
                    }

                    bd = ++entryIndex;
                }

                if (!this.valid) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(this.length);
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                    this.writeEntries(dataOutputStream);
                    this.rawBytes = byteArrayOutputStream.toByteArray();
                    this.entries = null;
                }
            } else {
                this.setValid();
                printWriter.println("ERROR: " + this.getClassName() + " : " + "Invalid InnerClasses Attribute" + " (1)");
                this.rawBytes = new byte[this.length];
                this.rawBytes[0] = (byte) (this.entryCount >>> 8 & 0xFF);
                this.rawBytes[1] = (byte) (this.entryCount >>> 0 & 0xFF);
                int bc = this.length - 2;
                classFileInputStream.read(this.rawBytes, 2, bc);
            }
        } else {
            this.setValid();
            printWriter.println("ERROR: " + this.getClassName() + " : " + "Invalid InnerClasses Attribute" + " (2)");
            this.rawBytes = new byte[this.length];
            classFileInputStream.read(this.rawBytes);
        }

        if (this.valid) {
            ((ClassFileBase) classFileComponent).setInnerClassesTrimPending();
        } else {
            ((ClassFileBase) classFileComponent).setInnerClassesRemovalPending();
        }
    }

    @Override
    public int getLength() {
        return 2 + this.entryCount * 8;
    }

    @Override
    public void collectUsedConstants(char bc, int bd, UsedConstantsCollector usedConstantsCollector, char be) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        if (this.entries != null) {
            int entryIndex = 0;
            int entryIndexValue = entryIndex;

            for (InnerClassEntry[] innerClassEntrys = this.entries; entryIndexValue < innerClassEntrys.length; innerClassEntrys = this.entries) {
                this.entries[entryIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
                entryIndexValue = ++entryIndex;
            }
        }
    }

    public void syncInnerNames() {
        if (this.valid) {
            int entryIndex = 0;
            int entryIndexValue = entryIndex;

            for (int entryCountValue = this.entryCount; entryIndexValue < entryCountValue; entryCountValue = this.entryCount) {
                this.entries[entryIndex].syncInnerNameWithClass();
                entryIndexValue = ++entryIndex;
            }
        }
    }

    public void writeEntries(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.entryCount);
        int entryIndex = 0;
        int bb = 0;

        for (int entryCountValue = this.entryCount; bb < entryCountValue; entryCountValue = this.entryCount) {
            this.entries[entryIndex].writeTo(dataOutputStream);
            bb = ++entryIndex;
        }
    }

    public void collectReferencedClasses(HashSet hashSet) {
        if (this.valid) {
            int entryIndex = 0;
            int entryIndexValue = entryIndex;

            for (int entryCountValue = this.entryCount; entryIndexValue < entryCountValue; entryCountValue = this.entryCount) {
                this.entries[entryIndex].collectReferencedClasses(hashSet);
                entryIndexValue = ++entryIndex;
            }
        }
    }

    public InnerClassEntry findEntryForClass(ClassConstantBase classConstantBase) {
        if (this.valid) {
            int entryIndex = 0;
            int entryIndexValue = entryIndex;

            for (int entryCountValue = this.entryCount; entryIndexValue < entryCountValue; entryCountValue = this.entryCount) {
                InnerClassEntry innerClassEntry1 = this.entries[entryIndex];
                if (innerClassEntry1.innerClass != null && innerClassEntry1.innerClass.getClassName().equals(classConstantBase.getClassName())) {
                    return innerClassEntry1;
                }

                entryIndexValue = ++entryIndex;
            }
        }

        return null;
    }
}
