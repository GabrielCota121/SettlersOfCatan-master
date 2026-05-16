package com.catan.network;

import com.catan.network.client.ScanForGames;
import com.catan.network.packets.PacketBuilder;
import com.catan.network.server.SynListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class NetworkTests {
    public static void main(String[] args) throws IOException {
        // verificando se o tamanho do caractere é fixo
        System.out.println("Tamanho de alberto (bytes) em String: "+"alberto".getBytes(StandardCharsets.ISO_8859_1).length);
        System.out.println("Tamanho de alberto (bytes) em String: "+"álbertô".getBytes(StandardCharsets.ISO_8859_1).length);
        // verificando se tem quebra de linha no iso 8859-1
        System.out.println(new String("Teste\nOutra Linha".getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1));
       // iniciando o listener de syn
        SynListener listener = new SynListener();
        listener.start();
        // iniciando o scan de partidas
        ScanForGames scan = new ScanForGames();
        scan.scan();
    }
}
