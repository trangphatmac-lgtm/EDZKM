package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AnnotationsAttribute extends AnnotationAttributeBase {
    public AnnotationEntry[] annotations;

    @Override
    public void collectReferencedMembers(Set set1, Set set2, Set set3, Set set4) {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].collectReferencedMembers(set1, set2, set3, set4);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public void collectUsedConstants(char bc, int bd, UsedConstantsCollector usedConstantsCollector, char be) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    public String[] getAnnotationTypeNames() {
        if (!super.valid) {
            return new String[0];
        }

        String[] annotationTypeNames = new String[this.annotations.length];
        int ba = 0;
        int bb = 0;

        for (AnnotationEntry[] annotationEntrys = this.annotations; bb < annotationEntrys.length; annotationEntrys = this.annotations) {
            annotationTypeNames[ba] = this.annotations[ba].getTypeName();
            bb = ++ba;
        }

        return annotationTypeNames;
    }

    @Override
    public void remapClassNames(Object object2, Object object3, Object object, Object object1) throws ZkmProcessingException {
        HashMap hashMap = (HashMap) object1;
        HashMap hashMap1 = (HashMap) object;
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].updateAfterClassRename(hashMap1, hashMap);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public int getLength() {
        if (!super.valid) {
            return super.rawBytes.length;
        }

        int lengthValue = 2;

        for (AnnotationEntry annotationEntry : this.annotations) {
            lengthValue += annotationEntry.getByteLength();
        }

        this.length = lengthValue;
        return lengthValue;
    }

    @Override
    public void collectReferencedClasses(Set set1) {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].collectReferencedClasses(set1);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public void resolveReferences(ClassResolver classResolver1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].resolveReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
                annotationIndexValue = ++annotationIndex;
            }
        }
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
        if (super.valid) {
            dataOutputStream.writeShort(this.annotations.length);
            int annotationIndex = 0;
            int bb = 0;

            for (AnnotationEntry[] annotationEntrys = this.annotations; bb < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].writeRemapped(dataOutputStream, map1, scriptEnvironment1);
                bb = ++annotationIndex;
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.annotations.length);
            int annotationIndex = 0;
            int bb = 0;

            for (AnnotationEntry[] annotationEntrys = this.annotations; bb < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].write(15571, dataOutputStream, 54846, 41447);
                bb = ++annotationIndex;
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void applyMethodRenames() {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].updateAfterMethodRename();
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    public int getAnnotationCount() {
        return this.annotations.length;
    }

    @Override
    public void applyFieldRenames() {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                this.annotations[annotationIndex].updateAfterFieldRename();
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    public AnnotationsAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            PrintWriter printWriter,
            String string1
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
        byte[] rawBytesValue = new byte[this.length];
        classFileInputStream.read(rawBytesValue);
        ClassFileInputStream classFileInputStream1 = ClassFileInputStream.fromBytes(rawBytesValue, false);

        try {
            if (this.length >= 2) {
                int bc = classFileInputStream1.readUnsignedShort();
                this.annotations = new AnnotationEntry[bc];

                for (int annotationIndex = 0; annotationIndex < bc; annotationIndex++) {
                    this.annotations[annotationIndex] = AnnotationEntry.read(this, classFileInputStream1, listMultimap);
                    if (!this.annotations[annotationIndex].isValid()) {
                        super.valid = false;
                        super.rawBytes = rawBytesValue;
                        StringBuilder stringBuilder = new StringBuilder()
                                .append("ERROR: ")
                                .append(this.getLocationName())
                                .append(" : ")
                                .append(string1)
                                .append(" (C) ");
                        AnnotationEntry annotationEntry = this.annotations[annotationIndex];
                        Integer integer1 = 9539;
                        Integer integer = 761259100;
                        printWriter.println(stringBuilder.append(annotationEntry.getErrorMessage(0, integer, integer1)).toString());
                    }
                }
            } else {
                super.valid = false;
                super.rawBytes = rawBytesValue;
                printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (D) : length=" + this.length);
            }
        } catch (IOException iOException) {
            super.valid = false;
            super.rawBytes = rawBytesValue;
            printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " is possibly corrupt : " + iOException);
        } finally {
            classFileInputStream1.close();
        }
    }

    public AnnotationEntry findAnnotation(String string, boolean bl) {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationEntry[] annotationEntrys = this.annotations; annotationIndexValue < annotationEntrys.length; annotationEntrys = this.annotations) {
                String string1;
                if (bl) {
                    string1 = this.annotations[annotationIndex].getResolvedTypeName();
                } else {
                    string1 = this.annotations[annotationIndex].getTypeName();
                }

                if (string.equals(string1)) {
                    return this.annotations[annotationIndex];
                }

                annotationIndexValue = ++annotationIndex;
            }
        }

        return null;
    }

    @Override
    public boolean trimAnnotations(Object object, Object object1, Object object2) {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        TrimProcessor trimProcessor1 = (TrimProcessor) object;
        if (super.valid) {
            boolean bl = false;
            boolean bl1 = true;
            ArrayList arrayList = new ArrayList();
            int annotationIndex = 0;
            int bb = 0;

            for (AnnotationEntry[] annotationEntrys1 = this.annotations; bb < annotationEntrys1.length; annotationEntrys1 = this.annotations) {
                AnnotationEntry annotationEntry = this.annotations[annotationIndex];
                ClassFileBase classFileBase = annotationEntry.getAnnotationClass();
                if (trimProcessor1.isAnnotationRetained(classFileBase)) {
                    arrayList.add(annotationEntry);
                    bl1 = false;
                    String string = "Retaining element in '"
                            + this.getAttributeName()
                            + "' attribute in "
                            + (this.isClassLevel() ? "" : "'" + this.getOwnerName() + "' in ")
                            + "class '"
                            + this.getDisplayLocationName()
                            + "' because of annotation attribute exclusion for class '"
                            + classFileBase.getInputPath()
                            + "'.";
                    if (scriptEnvironment1.isVerbose()) {
                        scriptEnvironment1.getLogWriter().println("\t" + string);
                    }

                    ((PrintWriter) object2).println(string);
                } else {
                    bl = true;
                }

                if (bl) {
                    AnnotationEntry[] annotationEntrys = new AnnotationEntry[arrayList.size()];
                    this.annotations = ((com.zelix.klassmaster.classfile.attribute.AnnotationEntry[]) (arrayList.toArray(annotationEntrys)));
                }

                bb = ++annotationIndex;
            }

            return bl1;
        } else {
            return false;
        }
    }
}
