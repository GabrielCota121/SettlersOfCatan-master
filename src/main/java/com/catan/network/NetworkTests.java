package com.catan.network;

import com.catan.network.client.ClientUdpListener;
import com.catan.network.client.FoundServers;
import com.catan.network.client.ScanForGames;
import com.catan.network.exception.NomeServerMuitoLongoException;
import com.catan.network.packets.PacketBuilder;
import com.catan.network.server.ServerData;
import com.catan.network.server.ServerUdpListener;
import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import static java.lang.Thread.sleep;

public class NetworkTests {
    public static void main(String[] args) throws IOException, InterruptedException, NomeServerMuitoLongoException {
        char type = JOptionPane.showInputDialog(null, "Cliente ou servidor? (s/c)").charAt(0);
        // verificando se o tamanho do caractere é fixo
        System.out.println("Tamanho de alberto (bytes) em String: "+"alberto".getBytes(StandardCharsets.ISO_8859_1).length);
        System.out.println("Tamanho de alberto (bytes) em String: "+"álbertô".getBytes(StandardCharsets.ISO_8859_1).length);
        // verificando se tem quebra de linha no iso 8859-1
        System.out.println(new String("Teste\nOutra Linha".getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1));
        // verificando se ele parseia corretamente
        System.out.println(Integer.parseInt("002556"));
       // iniciando o listener de syn
        ServerUdpListener listener = new ServerUdpListener();
        ScanForGames scan = new ScanForGames();
        if(type == 's'){
            ServerData.setNomeServer(JOptionPane.showInputDialog("Qual o nome do servidor? 20 caracteres."));
            listener.start();
        }else if(type == 'c'){
            scan.scan();
        }else{
            listener.start();
            scan.scan();
        }
        scan.join();
        int conectar = Integer.parseInt(JOptionPane.showInputDialog("Em qual server conectar? Há "+ FoundServers.getAllFoundServers().size()+" servers disponíveis."));

        ClientUdpListener listenerC = new ClientUdpListener();
        listenerC.run();

        DatagramPacket packet = PacketBuilder.buildConnect(FoundServers.getAllFoundServers().get(conectar-1));

        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
    }
}
