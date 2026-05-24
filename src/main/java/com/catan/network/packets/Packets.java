package com.catan.network.packets;

public enum Packets {
    SYN, SYNACK, ERROR, CONNECT, CONACK, START, READY;
    public static String getPacketName(Packets packet){
        return switch (packet) {
            case SYN -> "SYN";
            case SYNACK -> "SYNACK";
            case ERROR -> "ERROR";
            case CONNECT -> "CONNECT";
            case CONACK -> "CONACK";
            case START -> "START";
            case READY -> "READY";
        };
    }

}
