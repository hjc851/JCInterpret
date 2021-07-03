package jcinterpret.core.utils;

public class ByteUtils {
    public static short readUByte(byte[] arr, int offset) {
        return (short) (arr[offset] & 0xff);
    }

    // (branchbyte1 << 8 | branchbyte2)
    public static short readShort(byte[] arr, int offset) {
        short b1 = readUByte(arr, offset);
        short b2 = readUByte(arr, offset+1);

        short s = (short) ((b1 << 8) | b2);
        return s;
    }
}
