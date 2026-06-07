package com.example.SettlersOfCatan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class SettlersOfCatanApplicationTest {

    @Test
    void contextLoads() {
        // Verifica se o contexto do Spring Boot sobe corretamente
    }

    @Test
    void mainMethodRunsWithoutException() {
        assertDoesNotThrow(() -> SettlersOfCatanApplication.main(new String[]{}));
    }

    @Test
    void mainMethodWithArgsRunsWithoutException() {
        assertDoesNotThrow(() -> SettlersOfCatanApplication.main(new String[]{"--spring.main.web-application-type=none"}));
    }
}