



package com.zelix.klassmaster.classfile.attribute;

import java.util.Map;

import com.zelix.klassmaster.script.ScriptEnvironment;

import java.io.DataOutputStream;

import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;

import java.io.IOException;

import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.exceptions.ZkmRuntimeException;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.AccessFlags;
import com.zelix.klassmaster.classfile.constpool.ResolvedConstantModule;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.Utf8ConstantReplaceable;

public class ModuleAttribute extends ParsedAttributeBase implements Utf8ConstantReplaceable, ClassConstantReplaceable {
    public ResolvedConstantModule module;
    public final AccessFlags moduleFlags;
    public ConstantUtf8 moduleVersion;
    public final ModuleRequiresEntry[] requires;
    public final ModuleExportsEntry[] exports;
    public final ModuleOpensEntry[] opens;
    public final ResolvedClassConstant[] uses;
    public final ModuleProvidesEntry[] provides;

    public ModuleAttribute(final ClassFileComponent classFileComponent, final int constantPoolIndex, final String attributeName, final ClassFileInputStream classFileInputStream, final ListMultimap listMultimap, final ListMultimap listMultimap2) throws ClassFileFormatException, IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        classFileInputStream.read(super.rawBytes = new byte[this.length]);
        try (final ClassFileInputStream fromBytes = ClassFileInputStream.fromBytes(super.rawBytes, false)) {
            final int unsignedShort = fromBytes.readUnsignedShort();
            final ConstantPoolEntry constantPoolEntry = classFileComponent.getConstantPoolEntry(unsignedShort);
            if (constantPoolEntry == null) {
                super.valid = false;
                throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + unsignedShort + " : File is probably corrupt (J)");
            }
            if (!(constantPoolEntry instanceof ResolvedConstantModule)) {
                super.valid = false;
                throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Invalid attribute : " + unsignedShort + " : '" + ((ResolvedConstantModule) constantPoolEntry).getClass().getName() + "' : File is probably corrupt (K)");
            }
            this.module = (ResolvedConstantModule) constantPoolEntry;
            this.moduleFlags = new AccessFlags(this, fromBytes);
            final int unsignedShort2 = fromBytes.readUnsignedShort();
            if (unsignedShort2 != 0) {
                final ConstantPoolEntry constantPoolEntry2 = classFileComponent.getConstantPoolEntry(unsignedShort2);
                if (constantPoolEntry2 == null) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + unsignedShort2 + " : File is probably corrupt (L)");
                }
                if (!(constantPoolEntry2 instanceof ConstantUtf8)) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Invalid attribute : " + unsignedShort2 + " : '" + ((ConstantUtf8) constantPoolEntry2).getClass().getName() + "' : File is probably corrupt (M)");
                }
                listMultimap.addValue(this.moduleVersion = (ConstantUtf8) constantPoolEntry2, this);
            }
            final int unsignedShort3 = fromBytes.readUnsignedShort();
            this.requires = new ModuleRequiresEntry[unsignedShort3];
            for (int requireIndex = 0; requireIndex < unsignedShort3; ++requireIndex) {
                try {
                    try {
                        this.requires[requireIndex] = new ModuleRequiresEntry(this, fromBytes, listMultimap);
                    } catch (final ZkmRuntimeException zkmRuntimeException) {
                        throw zkmRuntimeException;
                    }
                } catch (final ClassFileFormatException classFileFormatException) {
                    super.valid = false;
                    throw classFileFormatException;
                }
            }
            final int unsignedShort4 = fromBytes.readUnsignedShort();
            this.exports = new ModuleExportsEntry[unsignedShort4];
            for (int exportIndex = 0; exportIndex < unsignedShort4; ++exportIndex) {
                try {
                    try {
                        this.exports[exportIndex] = new ModuleExportsEntry(this, fromBytes);
                    } catch (final ZkmRuntimeException zkmRuntimeExceptionValue) {
                        throw zkmRuntimeExceptionValue;
                    }
                } catch (final ClassFileFormatException classFileFormatExceptionValue) {
                    super.valid = false;
                    throw classFileFormatExceptionValue;
                }
            }
            final int unsignedShort5 = fromBytes.readUnsignedShort();
            this.opens = new ModuleOpensEntry[unsignedShort5];
            for (int openIndex = 0; openIndex < unsignedShort5; ++openIndex) {
                try {
                    try {
                        this.opens[openIndex] = new ModuleOpensEntry(this, fromBytes);
                    } catch (final ZkmRuntimeException zkmRuntimeExceptionValue2) {
                        throw zkmRuntimeExceptionValue2;
                    }
                } catch (final ClassFileFormatException classFileFormatExceptionValue2) {
                    super.valid = false;
                    throw classFileFormatExceptionValue2;
                }
            }
            final int unsignedShort6 = fromBytes.readUnsignedShort();
            this.uses = new ResolvedClassConstant[unsignedShort6];
            for (int useIndex = 0; useIndex < unsignedShort6; ++useIndex) {
                final int unsignedShort7 = fromBytes.readUnsignedShort();
                final ConstantPoolEntry constantPoolEntry3 = classFileComponent.getConstantPoolEntry(unsignedShort7);
                if (constantPoolEntry3 == null) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Illegal constant pool index in attribute : " + unsignedShort7 + " : File is probably corrupt (N)");
                }
                if (!(constantPoolEntry3 instanceof ResolvedClassConstant)) {
                    super.valid = false;
                    throw new ClassFileFormatException(classFileComponent.getOwningClass().getLocationName() + " : Invalid attribute : " + unsignedShort7 + " : '" + ((ResolvedClassConstant) constantPoolEntry3).getClass().getName() + "' : File is probably corrupt (O)");
                }
                listMultimap2.addValue(this.uses[useIndex] = (ResolvedClassConstant) constantPoolEntry3, this);
            }
            final int unsignedShort8 = fromBytes.readUnsignedShort();
            this.provides = new ModuleProvidesEntry[unsignedShort8];
            for (int provideIndex = 0; provideIndex < unsignedShort8; ++provideIndex) {
                try {
                    try {
                        this.provides[provideIndex] = new ModuleProvidesEntry(this, fromBytes, listMultimap2);
                    } catch (final ZkmRuntimeException zkmRuntimeExceptionValue3) {
                        throw zkmRuntimeExceptionValue3;
                    }
                } catch (final ClassFileFormatException classFileFormatExceptionValue3) {
                    super.valid = false;
                    throw classFileFormatExceptionValue3;
                }
            }
            if (fromBytes != null) {
                fromBytes.close();
            }
        }
    }

    public String getModuleName() {
        return this.module.getName();
    }

    @Override
    public void replaceClassConstant(final ResolvedClassConstant resolvedClassConstant, final ResolvedClassConstant resolvedClassConstant2) {
        for (int length = this.uses.length, useIndex = 0; useIndex < length; ++useIndex) {
            if (this.uses[useIndex] == resolvedClassConstant) {
                this.uses[useIndex] = resolvedClassConstant2;
            }
        }
    }

    @Override
    public void remapClassNames(final Object o, final Object o2, final Object o3, final Object o4) throws ZkmProcessingException {
    }

    @Override
    public void collectUsedConstants(final char c, final int n, final UsedConstantsCollector usedConstantsCollector, final char c2) {
        ClassFileComponent.getFlowGuardNodes();
        this.nameConstant.registerUsage(usedConstantsCollector, this, this.getParent());
        this.module.registerUsage(usedConstantsCollector, this, this.getParent());
        if (this.moduleVersion != null) {
            this.moduleVersion.registerUsage(usedConstantsCollector, this, this.getParent());
        }
        if (super.valid) {
            final ModuleRequiresEntry[] requires = this.requires;
            for (int length = requires.length, requireIndex = 0; requireIndex < length; ++requireIndex) {
                final ModuleRequiresEntry moduleRequiresEntry = requires[requireIndex];
                if (c2 > '\0') {
                    moduleRequiresEntry.collectUsedConstants('\0', 1103830477, usedConstantsCollector, '\u978d');
                }
            }
            final ModuleExportsEntry[] exports = this.exports;
            for (int length2 = exports.length, exportIndex = 0; exportIndex < length2; ++exportIndex) {
                final ModuleExportsEntry moduleExportsEntry = exports[exportIndex];
                if (n > 0) {
                    moduleExportsEntry.collectUsedConstants('\0', 1103830477, usedConstantsCollector, '\u978d');
                }
            }
            final ModuleOpensEntry[] opens = this.opens;
            for (int length3 = opens.length, openIndex = 0; openIndex < length3; ++openIndex) {
                final ModuleOpensEntry moduleOpensEntry = opens[openIndex];
                if (c2 > '\0') {
                    moduleOpensEntry.collectUsedConstants('\0', 1103830477, usedConstantsCollector, '\u978d');
                }
            }
            final ResolvedClassConstant[] uses = this.uses;
            for (int length4 = uses.length, useIndex = 0; useIndex < length4; ++useIndex) {
                final ResolvedClassConstant resolvedClassConstant = uses[useIndex];
                if (c >= '\0') {
                    resolvedClassConstant.registerUsage(usedConstantsCollector, this, this.getParent());
                }
            }
            final ModuleProvidesEntry[] provides = this.provides;
            for (int length5 = provides.length, provideIndex = 0; provideIndex < length5; ++provideIndex) {
                provides[provideIndex].collectUsedConstants('\0', 1103830477, usedConstantsCollector, '\u978d');
            }
        }
    }

    @Override
    public void writeRemapped(final DataOutputStream dataOutputStream, final Object o, final Object o2) throws IOException {
        final ScriptEnvironment scriptEnvironment = (ScriptEnvironment) o2;
        final Map map = (Map) o;
        super.writeRemapped(dataOutputStream, map, scriptEnvironment);
        if (super.valid) {
            dataOutputStream.writeShort(this.module.getIndex());
            dataOutputStream.writeShort(this.moduleFlags.getFlags());
            DataOutputStream dataOutputStream2;
            ModuleRequiresEntry[] array;
            if (this.moduleVersion != null) {
                final ConstantPoolEntry constantPoolEntry = ((com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry) (map.get(this.moduleVersion)));
                if (constantPoolEntry != null) {
                    dataOutputStream.writeShort(constantPoolEntry.getIndex());
                } else {
                    dataOutputStream.writeShort(this.moduleVersion.getIndex());
                }
                dataOutputStream2 = dataOutputStream;
                array = this.requires;
            } else {
                dataOutputStream.writeShort(0);
                dataOutputStream2 = dataOutputStream;
                array = this.requires;
            }
            dataOutputStream2.writeShort(array.length);
            final ModuleRequiresEntry[] requires = this.requires;
            for (int length = requires.length, requireIndex = 0; requireIndex < length; ++requireIndex) {
                requires[requireIndex].writeRemapped(dataOutputStream, map);
            }
            dataOutputStream.writeShort(this.exports.length);
            final ModuleExportsEntry[] exports = this.exports;
            for (int length2 = exports.length, exportIndex = 0; exportIndex < length2; ++exportIndex) {
                exports[exportIndex].writeRemapped(dataOutputStream);
            }
            dataOutputStream.writeShort(this.opens.length);
            final ModuleOpensEntry[] opens = this.opens;
            for (int length3 = opens.length, openIndex = 0; openIndex < length3; ++openIndex) {
                opens[openIndex].writeRemapped(dataOutputStream);
            }
            dataOutputStream.writeShort(this.uses.length);
            final ResolvedClassConstant[] uses = this.uses;
            for (int length4 = uses.length, useIndex = 0; useIndex < length4; ++useIndex) {
                final ResolvedClassConstant resolvedClassConstant = uses[useIndex];
                final ResolvedClassConstant resolvedClassConstant2 = ((com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant) (map.get(resolvedClassConstant)));
                if (resolvedClassConstant2 != null) {
                    dataOutputStream.writeShort(resolvedClassConstant2.getIndex());
                } else {
                    dataOutputStream.writeShort(resolvedClassConstant.getIndex());
                }
            }
            dataOutputStream.writeShort(this.provides.length);
            final ModuleProvidesEntry[] provides = this.provides;
            for (int length5 = provides.length, provideIndex = 0; provideIndex < length5; ++provideIndex) {
                provides[provideIndex].writeRemapped(dataOutputStream, map);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void replaceUtf8Constant(final ConstantUtf8 constantUtf8, final ConstantUtf8 moduleVersion) {
        if (this.moduleVersion != null && this.moduleVersion == constantUtf8) {
            this.moduleVersion = moduleVersion;
        } else {
            super.replaceUtf8Constant(constantUtf8, moduleVersion);
        }
    }

    @Override
    public void write(final DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.module.getIndex());
            dataOutputStream.writeShort(this.moduleFlags.getFlags());
            dataOutputStream.writeShort((this.moduleVersion == null) ? 0 : this.moduleVersion.getIndex());
            dataOutputStream.writeShort(this.requires.length);
            final ModuleRequiresEntry[] requires = this.requires;
            for (int length = requires.length, requireIndex = 0; requireIndex < length; ++requireIndex) {
                requires[requireIndex].writeTo(dataOutputStream);
            }
            dataOutputStream.writeShort(this.exports.length);
            final ModuleExportsEntry[] exports = this.exports;
            for (int length2 = exports.length, exportIndex = 0; exportIndex < length2; ++exportIndex) {
                exports[exportIndex].writeTo(dataOutputStream);
            }
            dataOutputStream.writeShort(this.opens.length);
            final ModuleOpensEntry[] opens = this.opens;
            for (int length3 = opens.length, openIndex = 0; openIndex < length3; ++openIndex) {
                opens[openIndex].writeTo(dataOutputStream);
            }
            dataOutputStream.writeShort(this.uses.length);
            final ResolvedClassConstant[] uses = this.uses;
            for (int length4 = uses.length, useIndex = 0; useIndex < length4; ++useIndex) {
                dataOutputStream.writeShort(uses[useIndex].getIndex());
            }
            dataOutputStream.writeShort(this.provides.length);
            final ModuleProvidesEntry[] provides = this.provides;
            for (int length5 = provides.length, provideIndex = 0; provideIndex < length5; ++provideIndex) {
                provides[provideIndex].writeTo(dataOutputStream);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }
}
