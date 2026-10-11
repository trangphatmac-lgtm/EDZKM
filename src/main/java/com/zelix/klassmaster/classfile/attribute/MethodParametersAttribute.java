package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.AbstractConstantPool;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.obfuscator.parameters.ChangedMethodDescriptor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class MethodParametersAttribute extends ParsedAttributeBase {
    private MethodParameterEntry[] parameters;

    @Override
    public void collectUsedConstants(char bc, int bd, UsedConstantsCollector usedConstantsCollector, char be) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        if (super.valid) {
            int lengthValue = this.parameters.length;

            for (int parameterIndex = 0; parameterIndex < lengthValue; parameterIndex++) {
                this.parameters[parameterIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
            }
        }
    }

    public MethodParametersAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        byte[] rawBytesValue = new byte[this.length];
        classFileInputStream.read(rawBytesValue);
        ClassFileInputStream classFileInputStream1 = ClassFileInputStream.fromBytes(rawBytesValue, false);

        try {
            if (this.length >= 2) {
                int bc = classFileInputStream1.readUnsignedByte();
                this.parameters = new MethodParameterEntry[bc];

                for (int parameterIndex = 0; parameterIndex < bc; parameterIndex++) {
                    this.parameters[parameterIndex] = new MethodParameterEntry(this, classFileInputStream1, listMultimap);
                    if (!this.parameters[parameterIndex].isValid()) {
                        super.valid = false;
                        super.rawBytes = rawBytesValue;
                        printWriter.println(
                                "ERROR: " + this.getDisplayLocationName() + " : " + this.getAttributeName() + " (A) " + this.parameters[parameterIndex].getErrorMessage()
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

    public void insertAddedParameters(ChangedMethodDescriptor changedMethodDescriptor, AbstractConstantPool abstractConstantPool, List list1) {
        int paramCount = changedMethodDescriptor.getParamCount();
        changedMethodDescriptor.getAddedParamCount();
        MethodParameterEntry[] methodParameterEntrys = new MethodParameterEntry[paramCount];
        int[] paramIndices = changedMethodDescriptor.getParamIndices();
        int paramIndiceIndex = 0;
        int paramIndice = paramIndices[0];
        int be = 0;

        for (int methodParameterEntryIndex = 0; methodParameterEntryIndex < paramCount; methodParameterEntryIndex++) {
            if (methodParameterEntryIndex == paramIndice) {
                methodParameterEntrys[methodParameterEntryIndex] = new MethodParameterEntry(this, abstractConstantPool, list1);
                if (++paramIndiceIndex < paramIndices.length) {
                    paramIndice = paramIndices[paramIndiceIndex];
                } else {
                    paramIndice = 32767;
                }
            } else {
                methodParameterEntrys[methodParameterEntryIndex] = this.parameters[be++];
            }
        }

        this.parameters = methodParameterEntrys;
        this.getLength();
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            int lengthValue = this.parameters.length;
            dataOutputStream.writeByte(lengthValue);

            for (int parameterIndex = 0; parameterIndex < lengthValue; parameterIndex++) {
                this.parameters[parameterIndex].writeTo(dataOutputStream);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    public void obfuscateNames() {
        int lengthValue = this.parameters.length;

        for (int parameterIndex = 0; parameterIndex < lengthValue; parameterIndex++) {
            this.parameters[parameterIndex].obfuscateName();
        }
    }

    @Override
    public int getLength() {
        if (!super.valid) {
            return super.rawBytes.length;
        }

        int lengthValue = 1;
        int lengthValue2 = this.parameters.length;

        for (int parameterIndex = 0; parameterIndex < lengthValue2; parameterIndex++) {
            lengthValue += this.parameters[parameterIndex].getEntrySize();
        }

        this.length = lengthValue;
        return this.length;
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
            int lengthValue = this.parameters.length;
            dataOutputStream.writeByte(lengthValue);

            for (int parameterIndex = 0; parameterIndex < lengthValue; parameterIndex++) {
                this.parameters[parameterIndex].writeRemapped(dataOutputStream, map1);
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
