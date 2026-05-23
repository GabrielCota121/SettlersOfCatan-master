package com.catan.network.client;

import com.catan.network.exception.ConexaoNaoEstabelecidaException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpSockets {
    private static ServerSocket input;
    private static Socket output = null;

    static {
        try {
            input = new ServerSocket(ClientPorts.getTcpPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void setOutput(Socket socket) {
        output = socket;
    }
    public static void sendMessage(String message){

    }
    public static String sendMessageAndAwaitResponse(String message) throws ConexaoNaoEstabelecidaException {
       if(output == null){
           throw new ConexaoNaoEstabelecidaException();
       }
       return null;
    }

}
