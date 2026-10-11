package com.zelix.klassmaster.util;

public class ByteConversionUtils {
    public static final int lowByte(int ba) {
        return ba & 0xFF;
    }

    public static byte[] intToBytes(int ba) {
        return new byte[]{(byte) (ba >>> 24), (byte) (ba >>> 16), (byte) (ba >>> 8), (byte) ba};
    }

    public static byte[] longToBytes(long ba) {
        return new byte[]{
                (byte) (ba >>> 56), (byte) (ba >>> 48), (byte) (ba >>> 40), (byte) (ba >>> 32), (byte) (ba >>> 24), (byte) (ba >>> 16), (byte) (ba >>> 8), (byte) ba
        };
    }

    public static final int toUnsignedByte(int ba) {
        return ba < 0 ? 256 + ba : ba;
    }

    public static final int bytesToUnsignedShort(int ba, int bb) {
        return (ba & 0xFF) << 8 | bb & 0xFF;
    }

    public static long bytesToLong(byte[] byteArray) {
        return (byteArray[0] & 255L) << 56
                | (byteArray[1] & 255L) << 48
                | (byteArray[2] & 255L) << 40
                | (byteArray[3] & 255L) << 32
                | (byteArray[4] & 255L) << 24
                | (byteArray[5] & 255L) << 16
                | (byteArray[6] & 255L) << 8
                | byteArray[7] & 255L;
    }

    public static final int highByte(int ba) {
        return (ba & 0xFF00) >> 8;
    }

    private ByteConversionUtils() {
    }
}
