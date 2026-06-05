package com.example.network;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.example.SettlersOfCatan.SettlersOfCatanApplication;

/**
 * Sobe o servidor Spring Boot embutido na própria JVM, para o modo
 * single player. Assim os bots (que vivem no servidor) funcionam sem o
 * usuário precisar iniciar o servidor manualmente.
 */
public class EmbeddedServer {

    private static ConfigurableApplicationContext context;

    /** Sobe o servidor se ainda não estiver rodando. Bloqueia até pronto. */
    public static synchronized void startIfNeeded() {
        if (context != null && context.isRunning()) return;
        try {
            SpringApplication app =
                new SpringApplication(SettlersOfCatanApplication.class);
            context = app.run();
        } catch (Exception e) {
            System.out.println("Servidor embutido não subiu (porta pode "
                + "já estar em uso). Assumindo servidor externo: "
                + e.getMessage());
        }
    }

    public static synchronized boolean isRunning() {
        return context != null && context.isRunning();
    }

    public static synchronized void stop() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
