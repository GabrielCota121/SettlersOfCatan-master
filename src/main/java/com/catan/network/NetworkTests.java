package com.catan.network;

import com.catan.network.client.ScanForGames;
import com.catan.network.exception.NomeServerMuitoLongoException;
import com.catan.network.server.ServerData;
import com.catan.network.server.SynListener;
import javax.swing.*;
import java.io.IOException;
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
        SynListener listener = new SynListener();
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

    }
}
