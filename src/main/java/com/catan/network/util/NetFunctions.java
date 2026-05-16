package com.catan.network.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;

public class NetFunctions {
    public static boolean isIpV4Address(InetAddress ip) {
        return ip instanceof Inet4Address;
    }
    public static boolean isPortValid(int port){
        return port >= 0 && port <= 65535;
    }
    public static boolean isPortFree(int port){
        try{
            (new ServerSocket(port)).close();
            return true;
        }catch(IOException e){
            return false;
        }
    }
    public static String adjustPortStringSize(String porta){
        StringBuilder portaBuilder = new StringBuilder(porta);
        while(portaBuilder.length()<5){ // enquanto tiver menos de 5 caracteres
            portaBuilder.insert(0, "0");
        }
        return portaBuilder.toString();
    }
    public static String adjustTextStringSize(String text, int correctLength){
        StringBuilder textBuilder = new StringBuilder(text);
        while(textBuilder.length()<correctLength){
            textBuilder.append("\0");
        }
        return textBuilder.toString();
    }
}
