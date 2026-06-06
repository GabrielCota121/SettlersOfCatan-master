package com.example.network;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testa o método privado normalizeUrl via reflexão.
 * ConnectView é uma classe JavaFX — apenas a lógica pura de URL é testada aqui
 * (sem inicializar o toolkit JavaFX).
 */
class ConnectViewNormalizeUrlTest {

    private String normalizeUrl(String raw) throws Exception {
        Method m = ConnectView.class.getDeclaredMethod("normalizeUrl", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, raw);
    }

    @Test
    void testHttpsViraWss() throws Exception {
        String result = normalizeUrl("https://meu-servidor.ngrok-free.app");
        assertTrue(result.startsWith("wss://"), "Deve começar com wss://");
    }

    @Test
    void testHttpViraWs() throws Exception {
        String result = normalizeUrl("http://localhost:8080");
        assertTrue(result.startsWith("ws://"), "Deve começar com ws://");
    }

    @Test
    void testLocalhostSemEsquema_adicionaWs() throws Exception {
        String result = normalizeUrl("localhost:8080");
        assertTrue(result.startsWith("ws://"));
    }

    @Test
    void testUrlSemCatan_adicionaPath() throws Exception {
        String result = normalizeUrl("ws://localhost:8080");
        assertTrue(result.endsWith("/catan"), "Deve terminar com /catan");
    }

    @Test
    void testUrlComBarraFinal_removeBarra() throws Exception {
        String result = normalizeUrl("ws://localhost:8080/");
        assertFalse(result.endsWith("//catan"), "Não deve ter barra dupla antes de /catan");
        assertTrue(result.endsWith("/catan"));
    }

    @Test
    void testUrlJaCorreta_naoAltera() throws Exception {
        String result = normalizeUrl("ws://localhost:8080/catan");
        assertEquals("ws://localhost:8080/catan", result);
    }

    @Test
    void testIpSemEsquema_adicionaWs() throws Exception {
        String result = normalizeUrl("192.168.1.10:8080");
        assertTrue(result.startsWith("ws://"));
    }

    @Test
    void testDominioExternoSemEsquema_adicionaWss() throws Exception {
        String result = normalizeUrl("meu-servidor.ngrok-free.app");
        assertTrue(result.startsWith("wss://"));
    }

    @Test
    void testWssSemCatan_adicionaPath() throws Exception {
        String result = normalizeUrl("wss://meu-servidor.ngrok-free.app");
        assertTrue(result.endsWith("/catan"));
    }

    @Test
    void testWssJaCorreta_naoAltera() throws Exception {
        String result = normalizeUrl("wss://meu-servidor.ngrok-free.app/catan");
        assertEquals("wss://meu-servidor.ngrok-free.app/catan", result);
    }

    @Test
    void testHttpsComPorta_converteParaWss() throws Exception {
        String result = normalizeUrl("https://exemplo.com:443");
        assertTrue(result.startsWith("wss://"));
        assertTrue(result.endsWith("/catan"));
    }

    @Test
    void testHttpComPorta_converteParaWs() throws Exception {
        String result = normalizeUrl("http://localhost:8080/catan");
        assertTrue(result.startsWith("ws://"));
        assertTrue(result.endsWith("/catan"));
        assertFalse(result.contains("/catan/catan"));
    }

    @Test
    void testUrlComBarraFinalEDominioExterno_adicionaPathCorreto() throws Exception {
        String result = normalizeUrl("wss://servidor.exemplo.com/");
        assertTrue(result.endsWith("/catan"));
        assertFalse(result.endsWith("//catan"));
    }

    @Test
    void testIp127_adicionaWs() throws Exception {
        String result = normalizeUrl("127.0.0.1:8080");
        assertTrue(result.startsWith("ws://"));
        assertTrue(result.endsWith("/catan"));
    }

    @Test
    void testUrlComCatanNoCaminho_naoAdicionaNovamente() throws Exception {
        String result = normalizeUrl("ws://localhost:8080/catan");
        assertFalse(result.contains("/catan/catan"), "Não deve duplicar /catan");
    }

    @Test
    void testNgrokComHttps_converteEAdicionaCatan() throws Exception {
        String result = normalizeUrl("https://abc123.ngrok-free.app");
        assertTrue(result.startsWith("wss://abc123.ngrok-free.app"));
        assertTrue(result.endsWith("/catan"));
    }
}
