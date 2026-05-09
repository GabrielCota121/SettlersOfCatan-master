package com.catan.network.util;

import java.net.Inet4Address;
import java.net.InetAddress;

public class netFunctions {
    public static boolean isIpV4Address(InetAddress ip) {
        return ip instanceof Inet4Address;
    }
}
