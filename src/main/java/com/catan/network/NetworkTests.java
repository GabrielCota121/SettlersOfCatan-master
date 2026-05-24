package com.catan.network;

import com.catan.network.client.ClientPorts;
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

public class NetworkTests {
    public static void main(String[] args) throws IOException, InterruptedException, NomeServerMuitoLongoException {
        char type = JOptionPane.showInputDialog(null, "Cliente ou servidor? (s/c)").charAt(0);
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
        listenerC.start();

        DatagramPacket packet = PacketBuilder.buildConnect(FoundServers.getAllFoundServers().get(conectar-1), ClientPorts.getUdpPort());
        System.out.println(FoundServers.getAllFoundServers().get(conectar-1).toString());
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        System.exit(0);
    }
}
