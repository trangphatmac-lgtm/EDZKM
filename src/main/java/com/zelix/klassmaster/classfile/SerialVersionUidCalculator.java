package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.classfile.attribute.ConstantValueAttribute;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.util.ArrayEnumeration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class SerialVersionUidCalculator implements Serializable {
    // Preserve the serialized identity from before the public comparator fields were renamed.
    private static final long serialVersionUID = -1860100888229714965L;

    public static final String SERIAL_VERSION_ALGORITHM = System.getProperty("SERIAL_VER_ALGORITHM", "1.5");
    public static final Comparator FIELD_NAME_COMPARATOR = new FieldNameComparator();
    public static final Comparator METHOD_SIGNATURE_COMPARATOR = new MethodSignatureComparator();

    public static long computeSerialVersionUid(ProgramClass programClass1, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        String className = programClass1.getClassName();
        if (!classMemberLookup1.isSubclass(className, "java/lang/Enum")
                && !classMemberLookup1.isSubclass(className, "java/lang/reflect/Proxy")
                && !className.equals("java/lang/Enum")
                && (
                classMemberLookup1.implementsInterface(className, "java/io/Serializable")
                        || classMemberLookup1.implementsInterface(className, "java/io/Externalizable")
                        || className.equals("java/io/Externalizable")
                        || className.equals("java/io/Serializable")
        )) {
            FieldInfo fieldInfo = classMemberLookup1.findField(className, "serialVersionUID", "J");
            if (fieldInfo != null) {
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    return constantValueAttribute.getLongValue();
                }
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512);
            long ba = 0L;

            try {
                MessageDigest messageDigest1 = MessageDigest.getInstance("SHA-1");
                DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, messageDigest1);
                DataOutputStream dataOutputStream = new DataOutputStream(digestOutputStream);
                dataOutputStream.writeUTF(programClass1.getDottedClassName());
                int innerClassAccessFlags = programClass1.getInnerClassAccessFlags();
                String serialVersionAlgorithm;
                if (innerClassAccessFlags == -1) {
                    innerClassAccessFlags = programClass1.getAccessFlags();
                    serialVersionAlgorithm = SERIAL_VERSION_ALGORITHM;
                } else {
                    serialVersionAlgorithm = SERIAL_VERSION_ALGORITHM;
                }

                if (!serialVersionAlgorithm.equals("1.4")) {
                    innerClassAccessFlags &= 1553;
                }

                if (programClass1.isInterface()) {
                    if (programClass1.getMethodCount() > 0) {
                        innerClassAccessFlags |= 1024;
                        dataOutputStream.writeInt(innerClassAccessFlags);
                    } else {
                        innerClassAccessFlags &= -1025;
                        dataOutputStream.writeInt(innerClassAccessFlags);
                    }
                } else {
                    dataOutputStream.writeInt(innerClassAccessFlags);
                }

                String[] interfaceNames = programClass1.getInterfaceNames();
                Arrays.sort(interfaceNames);

                for (int interfaceNameIndex = 0; interfaceNameIndex < interfaceNames.length; interfaceNameIndex++) {
                    String string1 = interfaceNames[interfaceNameIndex].replace('/', '.');
                    dataOutputStream.writeUTF(string1);
                }

                ArrayEnumeration arrayEnumeration = programClass1.enumerateFields();
                arrayEnumeration.sortElements(FIELD_NAME_COMPARATOR);

                for (boolean moreElements = arrayEnumeration.hasMoreElements(); moreElements; moreElements = arrayEnumeration.hasMoreElements()) {
                    FieldInfo fieldInfo1 = (FieldInfo) arrayEnumeration.nextElement();
                    if (!fieldInfo1.isStrictlyPrivate() || !fieldInfo1.isStatic() && !fieldInfo1.isTransient()) {
                        dataOutputStream.writeUTF(fieldInfo1.getSourceName());
                        int accessFlags = fieldInfo1.getAccessFlags();
                        if (!SERIAL_VERSION_ALGORITHM.equals("1.4")) {
                            accessFlags &= 223;
                            dataOutputStream.writeInt(accessFlags);
                        } else {
                            dataOutputStream.writeInt(accessFlags);
                        }

                        dataOutputStream.writeUTF(fieldInfo1.getDescriptor());
                    }
                }

                if (classMemberLookup1.declaresMethod(MethodSignature.STATIC_INITIALIZER, programClass1)) {
                    dataOutputStream.writeUTF("<clinit>");
                    dataOutputStream.writeInt(8);
                    dataOutputStream.writeUTF("()V");
                }

                ArrayEnumeration arrayEnumeration1 = programClass1.enumerateMethods();
                arrayEnumeration1.sortElements(METHOD_SIGNATURE_COMPARATOR);
                Vector vector1 = new Vector();
                Vector vector = new Vector();

                for (boolean moreElementsValue = arrayEnumeration1.hasMoreElements(); moreElementsValue; moreElementsValue = arrayEnumeration1.hasMoreElements()) {
                    MethodInfo methodInfo1 = (MethodInfo) arrayEnumeration1.nextElement();
                    if (!methodInfo1.isStrictlyPrivate()) {
                        if (methodInfo1.isConstructor()) {
                            vector1.addElement(methodInfo1);
                        } else if (!methodInfo1.isStaticInitializer()) {
                            vector.addElement(methodInfo1);
                        }
                    }
                }

                for (int elementIndex = 0; elementIndex < vector1.size(); elementIndex++) {
                    MethodInfo methodInfo2 = (MethodInfo) vector1.elementAt(elementIndex);
                    dataOutputStream.writeUTF(methodInfo2.getJvmName());
                    int accessFlagsValue = methodInfo2.getAccessFlags();
                    if (!SERIAL_VERSION_ALGORITHM.equals("1.4")) {
                        accessFlagsValue &= 3391;
                        dataOutputStream.writeInt(accessFlagsValue);
                    } else {
                        dataOutputStream.writeInt(accessFlagsValue);
                    }

                    dataOutputStream.writeUTF(methodInfo2.getDescriptor().replace('/', '.'));
                }

                for (int elementIndexValue = 0; elementIndexValue < vector.size(); elementIndexValue++) {
                    MethodInfo methodInfo3 = (MethodInfo) vector.elementAt(elementIndexValue);
                    dataOutputStream.writeUTF(methodInfo3.getJvmName());
                    int accessFlagsValue2 = methodInfo3.getAccessFlags();
                    if (!SERIAL_VERSION_ALGORITHM.equals("1.4")) {
                        accessFlagsValue2 &= 3391;
                        dataOutputStream.writeInt(accessFlagsValue2);
                    } else {
                        dataOutputStream.writeInt(accessFlagsValue2);
                    }

                    dataOutputStream.writeUTF(methodInfo3.getDescriptor().replace('/', '.'));
                }

                dataOutputStream.flush();
                byte[] byteArray = messageDigest1.digest();
                int byteArrayElementIndex = 0;
                int bm = 0;
                byte bn = 8;

                while (true) {
                    int lengthValue = byteArray.length;
                    if (bm >= Math.min(8, lengthValue)) {
                        break;
                    }

                    ba += (long) (byteArray[byteArrayElementIndex] & 0xFF) << byteArrayElementIndex * 8;
                    bm = ++byteArrayElementIndex;
                    bn = 8;
                }
            } catch (IOException iOException) {
                ba = 0L;
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                throw new ZkmProcessingException(noSuchAlgorithmException.toString());
            }

            return ba;
        } else {
            return 0L;
        }
    }

    private SerialVersionUidCalculator() {
    }
}
