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
import com.zelix.klassmaster.util.SetMultiMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TypeAnnotationsAttribute extends AnnotationAttributeBase {
    public AnnotationInfo[] annotations;

    public boolean isEmpty() {
        return this.annotations.length == 0;
    }

    public void registerLabelTargets(SetMultiMap setMultiMap) {
        AnnotationInfo[] annotationInfos = this.annotations;
        int lengthValue = annotationInfos.length;

        for (int annotationInfoIndex = 0; annotationInfoIndex < lengthValue; annotationInfoIndex++) {
            annotationInfos[annotationInfoIndex].collectLabelReferences(setMultiMap);
        }
    }

    @Override
    public final void remapClassNames(Object object2, Object object3, Object object, Object object1) throws ZkmProcessingException {
        HashMap hashMap = (HashMap) object;
        HashMap hashMap1 = (HashMap) object1;
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].updateAfterClassRename(hashMap, hashMap1);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (super.valid) {
            dataOutputStream.writeShort(this.annotations.length);
            int annotationIndex = 0;
            int bb = 0;

            for (AnnotationInfo[] annotationInfos = this.annotations; bb < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].write(15571, dataOutputStream, 54846, 41447);
                bb = ++annotationIndex;
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    @Override
    public void resolveReferences(ClassResolver classResolver1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].resolveReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public boolean trimAnnotations(Object object, Object object1, Object object2) {
        TrimProcessor trimProcessor1 = (TrimProcessor) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        if (super.valid) {
            boolean bl = false;
            boolean bl1 = true;
            ArrayList arrayList = new ArrayList();
            int annotationIndex = 0;
            int bb = 0;

            for (AnnotationInfo[] annotationInfos1 = this.annotations; bb < annotationInfos1.length; annotationInfos1 = this.annotations) {
                AnnotationInfo annotationInfo = this.annotations[annotationIndex];
                ClassFileBase classFileBase = annotationInfo.getAnnotationClass();
                if (trimProcessor1.isAnnotationRetained(classFileBase)) {
                    arrayList.add(annotationInfo);
                    bl1 = false;
                    String string = "Retaining element in '"
                            + this.getAttributeName()
                            + "' attribute in "
                            + (this.isClassLevel() ? "" : "'" + this.getOwnerName() + "' in ")
                            + "class '"
                            + this.getDisplayLocationName()
                            + "' because of annotation attribute exclusion for class '"
                            + annotationInfo.getInputPath()
                            + "'.";
                    if (scriptEnvironment1.isVerbose()) {
                        scriptEnvironment1.getLogWriter().println("\t" + string);
                    }

                    ((PrintWriter) object2).println(string);
                } else {
                    bl = true;
                }

                if (bl) {
                    AnnotationInfo[] annotationInfos = new AnnotationInfo[arrayList.size()];
                    this.annotations = ((com.zelix.klassmaster.classfile.attribute.AnnotationInfo[]) (arrayList.toArray(annotationInfos)));
                }

                bb = ++annotationIndex;
            }

            return bl1;
        } else {
            return false;
        }
    }

    @Override
    public void applyMethodRenames() {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].updateAfterMethodRename();
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public void collectReferencedClasses(Set set1) {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].collectReferencedClasses(set1);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    public int removeThrowsAnnotations() {
        ArrayList arrayList = new ArrayList(this.annotations.length);

        for (AnnotationInfo annotationInfo : this.annotations) {
            if (!annotationInfo.isThrowsTarget()) {
                arrayList.add(annotationInfo);
            }
        }

        AnnotationInfo[] annotationInfos;
        if (arrayList.size() < this.annotations.length) {
            this.annotations = ((com.zelix.klassmaster.classfile.attribute.AnnotationInfo[]) (arrayList.toArray(new AnnotationInfo[arrayList.size()])));
            annotationInfos = this.annotations;
        } else {
            annotationInfos = this.annotations;
        }

        return annotationInfos.length;
    }

    @Override
    public void applyFieldRenames() {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].updateAfterFieldRename();
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

            for (AnnotationInfo[] annotationInfos = this.annotations; bb < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].writeRemapped(dataOutputStream, map1, scriptEnvironment1);
                bb = ++annotationIndex;
            }
        } else {
            dataOutputStream.write(super.rawBytes);
        }
    }

    public TypeAnnotationsAttribute(
            ClassFileComponent classFileComponent,
            int constantPoolIndex,
            String attributeName,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
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
                this.annotations = new AnnotationInfo[bc];

                for (int annotationIndex = 0; annotationIndex < bc; annotationIndex++) {
                    this.annotations[annotationIndex] = new AnnotationInfo(this, classFileInputStream1, listMultimap, listMultimap1);
                    if (!this.annotations[annotationIndex].isValid()) {
                        super.valid = false;
                        super.rawBytes = rawBytesValue;
                        printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (E) " + this.annotations[annotationIndex].getErrorMessage());
                    }
                }
            } else {
                super.valid = false;
                super.rawBytes = rawBytesValue;
                printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " (F) : length=" + this.length);
            }
        } catch (IOException iOException) {
            super.valid = false;
            super.rawBytes = rawBytesValue;
            printWriter.println("ERROR: " + this.getLocationName() + " : " + string1 + " is possibly corrupt : " + iOException);
        } finally {
            classFileInputStream1.close();
        }
    }

    public boolean removeSignatureDependentAnnotations() {
        boolean bl = false;
        ArrayList arrayList = new ArrayList(this.annotations.length);

        for (AnnotationInfo annotationInfo : this.annotations) {
            if (!annotationInfo.referencesTypeArguments()) {
                arrayList.add(annotationInfo);
            } else {
                bl = true;
            }
        }

        if (arrayList.size() < this.annotations.length) {
            this.annotations = ((com.zelix.klassmaster.classfile.attribute.AnnotationInfo[]) (arrayList.toArray(new AnnotationInfo[arrayList.size()])));
        }

        return bl;
    }

    public void removeAnnotationsAtLabels(HashSet hashSet, SetMultiMap setMultiMap) {
        ArrayList arrayList = new ArrayList(this.annotations.length);

        for (AnnotationInfo annotationInfo : this.annotations) {
            if (!annotationInfo.removeDeadLabelTargets(hashSet, setMultiMap)) {
                arrayList.add(annotationInfo);
            }
        }

        if (arrayList.size() < this.annotations.length) {
            this.annotations = ((com.zelix.klassmaster.classfile.attribute.AnnotationInfo[]) (arrayList.toArray(new AnnotationInfo[arrayList.size()])));
        }
    }

    @Override
    public void collectReferencedMembers(Set set1, Set set2, Set set3, Set set4) {
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].collectReferencedMembers(set1, set2, set3, set4);
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public final void collectUsedConstants(char bc, int bd, UsedConstantsCollector usedConstantsCollector, char be) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        if (super.valid) {
            int annotationIndex = 0;
            int annotationIndexValue = annotationIndex;

            for (AnnotationInfo[] annotationInfos = this.annotations; annotationIndexValue < annotationInfos.length; annotationInfos = this.annotations) {
                this.annotations[annotationIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
                annotationIndexValue = ++annotationIndex;
            }
        }
    }

    @Override
    public int getLength() {
        int lengthValue = 2;

        for (AnnotationInfo annotationInfo : this.annotations) {
            lengthValue += annotationInfo.getByteLength();
        }

        this.length = lengthValue;
        return lengthValue;
    }
}
