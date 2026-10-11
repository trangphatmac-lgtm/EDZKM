package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.NameAndTypeHolder;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedNameAndType;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class EnclosingMethodAttribute extends ParsedAttributeBase implements NameAndTypeHolder, ClassConstantReplaceable {
    public AbstractMethodInfo enclosingMethod;
    public ResolvedClassConstant enclosingClassConstant;
    public ResolvedNameAndType methodNameAndType;

    @Override
    public void write(DataOutputStream output) throws IOException {
        super.write(output);
        if (super.valid) {
            output.writeShort(this.enclosingClassConstant.getIndex());
            output.writeShort(this.methodNameAndType == null ? 0 : this.methodNameAndType.getIndex());
        } else {
            output.write(super.rawBytes);
        }
    }

    @Override
    public void replaceClassConstant(ResolvedClassConstant previousConstant, ResolvedClassConstant replacementConstant) {
        if (this.enclosingClassConstant == previousConstant) {
            this.enclosingClassConstant = replacementConstant;
        }
    }

    @Override
    public ResolvedNameAndType replaceNameAndType(ResolvedNameAndType replacementNameAndType) {
        ResolvedNameAndType previousNameAndType = this.methodNameAndType;
        this.methodNameAndType = replacementNameAndType;
        return previousNameAndType;
    }

    public EnclosingMethodAttribute(
            ClassFileComponent parent,
            int nameIndex,
            String attributeName,
            ClassFileInputStream input,
            ListMultimap attributeReferences,
            ListMultimap methodReferences,
            ListMultimap classReferences,
            PrintWriter diagnostics
    ) throws IOException {
        super(parent, nameIndex, attributeName, input, attributeReferences);
        byte[] attributeBytes = new byte[this.length];
        input.read(attributeBytes);
        ClassFileInputStream attributeInput = ClassFileInputStream.fromBytes(attributeBytes, false);
        if (this.length == 4) {
            int classIndex = attributeInput.readUnsignedShort();
            int methodIndex = attributeInput.readUnsignedShort();
            ConstantPoolEntry classEntry = this.getConstantPoolEntry(classIndex);
            if (classEntry instanceof ResolvedClassConstant) {
                this.enclosingClassConstant = (ResolvedClassConstant) classEntry;
                classReferences.addValue(this.enclosingClassConstant, this);
            } else {
                super.valid = false;
                diagnostics.println(
                        "ERROR: "
                                + this.getLocationName()
                                + " : "
                                + "Invalid EnclosingMethod Attribute"
                                + " (A) : index="
                                + classIndex
                                + (classEntry != null ? " : tag=" + classEntry.getTag() : "")
                );
            }

            if (methodIndex > 0) {
                ConstantPoolEntry methodEntry = this.getConstantPoolEntry(methodIndex);
                if (methodEntry instanceof ResolvedNameAndType) {
                    this.methodNameAndType = (ResolvedNameAndType) methodEntry;
                    methodReferences.addValue(this.methodNameAndType, this);
                    ((ClassFileBase) parent).markHasEnclosingMethod();
                } else {
                    super.valid = false;
                    diagnostics.println(
                            "ERROR: "
                                    + this.getLocationName()
                                    + " : "
                                    + "Invalid EnclosingMethod Attribute"
                                    + " (B) : index="
                                    + methodIndex
                                    + (methodEntry != null ? " : tag=" + methodEntry.getTag() : "")
                    );
                }
            }

            if (!super.valid) {
                super.rawBytes = attributeBytes;
            }
        } else {
            super.valid = false;
            super.rawBytes = attributeBytes;
            diagnostics.println("ERROR: " + this.getLocationName() + " : " + "Invalid EnclosingMethod Attribute" + " (C) : length=" + this.length);
        }
    }

    public String getEnclosingClassName() {
        return super.valid ? this.enclosingClassConstant.getClassName() : null;
    }

    public void updateAfterMethodRename() {
        if (super.valid && this.methodNameAndType != null) {
            if (this.enclosingMethod != null && !this.enclosingMethod.getJvmName().equals(this.methodNameAndType.getName())) {
                this.methodNameAndType.setName(this.enclosingMethod.getJvmName());
            }

            if (this.enclosingMethod != null && !this.enclosingMethod.getDescriptor().equals(this.methodNameAndType.getDescriptor())) {
                this.methodNameAndType.setDescriptor(this.enclosingMethod.getDescriptor());
            }
        }
    }

    @Override
    public void writeRemapped(DataOutputStream output, Object constantMapping, Object environment) throws IOException {
        Map remappedConstants = (Map) constantMapping;
        ScriptEnvironment scriptEnvironment = (ScriptEnvironment) environment;
        super.writeRemapped(output, remappedConstants, scriptEnvironment);
        if (super.valid) {
            ResolvedClassConstant remappedClass = (ResolvedClassConstant) remappedConstants.get(this.enclosingClassConstant);
            if (remappedClass != null) {
                output.writeShort(remappedClass.getIndex());
            } else {
                output.writeShort(this.enclosingClassConstant.getIndex());
            }

            if (this.methodNameAndType == null) {
                output.writeShort(0);
            } else {
                ResolvedNameAndType remappedMethod = (ResolvedNameAndType) remappedConstants.get(this.methodNameAndType);
                if (remappedMethod != null) {
                    output.writeShort(remappedMethod.getIndex());
                } else {
                    output.writeShort(this.methodNameAndType.getIndex());
                }
            }
        } else {
            output.write(super.rawBytes);
        }
    }

    @Override
    public void collectUsedConstants(char controlTag, int controlSeed, UsedConstantsCollector collector, char controlMode) {
        ClassFileComponent.getFlowGuardNodes();
        collector.markUsed(this.nameConstant, this, this);
        if (super.valid) {
            if (this.enclosingClassConstant != null) {
                this.enclosingClassConstant.registerUsage(collector, this, this);
            }
            if (this.methodNameAndType != null) {
                this.methodNameAndType.registerUsage(collector, this, this);
            }
        }
    }

    public void resolveEnclosingMethod(ClassResolver resolver) throws ZkmException, IOException {
        if (super.valid) {
            ClassFileBase enclosingClass = this.getOwningClass();
            Integer releaseVersion = enclosingClass.hasReleaseVersion() ? enclosingClass.getReleaseVersion() : null;
            enclosingClass = resolver.getVersionedClass(
                    this.enclosingClassConstant.getClassName(), releaseVersion, "analyzing enclosing method in class '" + this.getDisplayLocationName() + "' (A)"
            );
            if (enclosingClass != null && this.methodNameAndType != null) {
                this.enclosingMethod = enclosingClass.findMethod(new MethodSignature(this.methodNameAndType.getName(), this.methodNameAndType.getDescriptor()));
            }
        }
    }
}
