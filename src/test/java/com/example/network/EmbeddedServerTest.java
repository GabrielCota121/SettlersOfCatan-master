package com.example.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedServerTest {

    @AfterEach
    void tearDown() {
        EmbeddedServer.stop();
    }

    @Test
    void testIsRunning_inicialmente_retornaFalse() {
        // Garante estado limpo
        EmbeddedServer.stop();
        assertFalse(EmbeddedServer.isRunning());
    }

    @Test
    void testStop_semServidor_naoLancaExcecao() {
        assertDoesNotThrow(() -> EmbeddedServer.stop());
    }

    @Test
    void testStop_duasVezes_naoLancaExcecao() {
        EmbeddedServer.stop();
        assertDoesNotThrow(() -> EmbeddedServer.stop());
    }

    @Test
    void testIsRunning_aposStop_retornaFalse() {
        EmbeddedServer.stop();
        assertFalse(EmbeddedServer.isRunning());
    }

    // Nota: testStartIfNeeded() requer o contexto Spring completo.
    // Para testes de integração, use @SpringBootTest separadamente.
}
