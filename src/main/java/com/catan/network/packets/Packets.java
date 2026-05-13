package com.catan.network.packets;

public enum Packets {
    SYN, SYNACK, ERROR;
    public static String getPacketName(Packets packet){
        return switch (packet) {
            case SYN -> "SYN";
            case SYNACK -> "SYNACK";
            case ERROR -> "ERROR";
        };
    }

}
