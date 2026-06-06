package com.example.model.view;

import com.example.model.game.ResourceType;
import com.example.model.player.Player;
import com.example.model.player.ResourceWallet;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class PlayerHandViewTest {

    @BeforeAll
    static void initJFX() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // Toolkit já rodando
        }
    }

    @Test
    void testOpacityBranchesAndFaliure() throws Exception {
        runOnFX(() -> {
            PlayerHandView view = new PlayerHandView(100);
            ResourceWallet wallet = new ResourceWallet();
            
            // Adiciona madeira (ativa branch 1.0 de opacidade)
            wallet.addResource(ResourceType.WOOD, 1);
            view.update(wallet);
            
            StackPane woodPane = (StackPane) view.getChildren().get(0); 
            assertEquals(1.0, woodPane.getOpacity(), "Deve estar totalmente visível (1.0)");

            // Remove madeira (ativa branch 0.5 de opacidade)
            wallet.removeResource(ResourceType.WOOD, 1);
            view.update(wallet);
            assertEquals(0.5, woodPane.getOpacity(), "Deve estar semi-transparente (0.5)");
            
            // Testa update via Player (delegação)
            Player p = new Player(1, "Juliana", "Blue");
            assertDoesNotThrow(() -> view.update(p));
        });
    }

    @Test
    void testConstructorVariations() throws Exception {
        runOnFX(() -> {
            // Cobre o construtor padrão (que chama o de 80)
            PlayerHandView def = new PlayerHandView();
            assertNotNull(def);
            
            // Cobre o Math.max(10, ...) passando valor baixo
            PlayerHandView small = new PlayerHandView(2);
            assertNotNull(small);
        });
    }

    // Método auxiliar para rodar no thread do JavaFX
    private void runOnFX(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            r.run();
            latch.countDown();
        });
        latch.await();
    }
}