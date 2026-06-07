package com.example.network;

import com.example.network.room.RoomManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketConfigTest {

    @Test
    void construtor_criaInstanciaComHandlerValido() {
        GameWebSocketHandler handler = new GameWebSocketHandler(new RoomManager());
        WebSocketConfig config = new WebSocketConfig(handler);

        assertNotNull(config);
    }

    @Test
    void implementaWebSocketConfigurer() {
        GameWebSocketHandler handler = new GameWebSocketHandler(new RoomManager());
        WebSocketConfig config = new WebSocketConfig(handler);

        assertTrue(config instanceof WebSocketConfigurer);
    }

    @Test
    void possuiMetodoRegisterWebSocketHandlersComAssinaturaEsperada() throws Exception {
        Method method = WebSocketConfig.class.getMethod(
                "registerWebSocketHandlers",
                WebSocketHandlerRegistry.class
        );

        assertNotNull(method);
        assertEquals(void.class, method.getReturnType());
    }
}
