package com.free.media.mediacodec.utils;

public class Utils {
    private static final String TAG = "CodecDemoUtils";
    
    public static int toInt(byte[] b) {
        int b3 = 0XFF & b[3];
        int b2 = 0XFF & b[2];
        int b1 = 0XFF & b[1];
        int b0 = 0XFF & b[0];
        return ((b3 << 24) + (b2 <<16) +
                (b1 << 8) + (b0 << 0));
    }
    
    public static int byte2Int(byte b) {
        return (int)(b & 0xFF);
    }
    
    public static short toShort(byte[] b) {
        int b1 = 0XFF & b[1];
        int b0 = 0XFF & b[0];
        return (short) ((b1 << 8) + (b0 << 0));
    }

}
