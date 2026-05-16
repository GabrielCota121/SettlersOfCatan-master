package com.catan.network.util;

import java.nio.charset.StandardCharsets;

public class StringFunctions {
    public static byte[] getStringBytesAsIso88591(String string){
        return string.getBytes(StandardCharsets.ISO_8859_1);
    }
}
