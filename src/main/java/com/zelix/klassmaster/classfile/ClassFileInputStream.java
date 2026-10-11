package com.zelix.klassmaster.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClassFileInputStream extends DataInputStream {
    private static String initializationMarker;
    private static String digestAlgorithm;
    private static long readChunkSize;
    public ByteArrayInputStream byteStream;
    public byte[] digest;

    public byte[] getDigest() {
        return this.digest != null ? this.digest.clone() : null;
    }

    @Override
    public void reset() {
        this.byteStream.reset();
    }

    public static ClassFileInputStream fromStream(InputStream inputStream1, int ba) throws IOException {
        Boolean boolean1 = true;
        return fromStream(inputStream1, ba, boolean1);
    }

    public static ClassFileInputStream fromBytes(byte[] byteArray, boolean bl) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        return new ClassFileInputStream(byteArrayInputStream, byteArray, bl);
    }

    public static String getInitializationMarker() {
        return initializationMarker;
    }

    public static ClassFileInputStream fromStream(InputStream inputStream1, int ba, Boolean boolean1) throws IOException {
        try {
            byte[] byteArray = new byte[ba];
            int bc = 0;

            int bd;
            while (bc < ba && (bd = inputStream1.read(byteArray, bc, Math.min((int) readChunkSize, ba - bc))) != -1) {
                bc += bd;
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            return new ClassFileInputStream(byteArrayInputStream, byteArray, boolean1);
        } finally {
            inputStream1.close();
        }
    }

    public static void initializeMarker() {
        initializationMarker = "Lz93oc";
    }

    private ClassFileInputStream(ByteArrayInputStream byteArrayInputStream, byte[] byteArray, boolean bl) {
        super(byteArrayInputStream);
        this.byteStream = byteArrayInputStream;
        if (bl) {
            try {
                MessageDigest messageDigest1 = MessageDigest.getInstance(digestAlgorithm);
                this.digest = messageDigest1.digest(byteArray);
            } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                this.digest = new byte[0];
            }
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
        if (getInitializationMarker() != null) {
            initializeMarker();
        }

        digestAlgorithm = "SHA-1";
        readChunkSize = -5685789525918343168L;
    }
}
