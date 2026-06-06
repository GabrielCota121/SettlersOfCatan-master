package com.example.network;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para ConnectView.
 *
 * <p>Construtores, getRoot, setConnecting e as lambdas do botão Conectar e do
 * botão Jogar Sozinho são cobertas aqui. O método normalizeUrl (100% coberto)
 * permanece em ConnectViewNormalizeUrlTest.
 */
class ConnectViewTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void startJfx() throws Exception {
        if (!FX_STARTED.getAndSet(true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException e) {
                latch.countDown();
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX platform failed to start");
        }
        Platform.setImplicitExit(false);
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try { action.run(); }
            catch (Throwable t) { error.set(t); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX task timed out");
        if (error.get() != null) {
            if (error.get() instanceof Exception e) throw e;
            throw new RuntimeException(error.get());
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static Button getConnectBtn(ConnectView v) throws Exception {
        Field f = ConnectView.class.getDeclaredField("connectBtn");
        f.setAccessible(true);
        return (Button) f.get(v);
    }

    private static Label getStatusLabel(ConnectView v) throws Exception {
        Field f = ConnectView.class.getDeclaredField("statusLabel");
        f.setAccessible(true);
        return (Label) f.get(v);
    }

    private static void invokeSetConnecting(ConnectView v, boolean connecting, String msg) throws Exception {
        Method m = ConnectView.class.getDeclaredMethod("setConnecting", boolean.class, String.class);
        m.setAccessible(true);
        m.invoke(v, connecting, msg);
    }

    // ── Scene-graph helpers ───────────────────────────────────────────────────
    // Layout: root(BorderPane) → center(VBox[title,subtitle,form])
    //         form(VBox[urlLabel,urlField,hint,connectBtn,statusLabel,spLabel,spRow])
    //         spRow(HBox[botsLabel,botsSpinner,singleBtn])

    private static TextField getUrlField(ConnectView v) {
        VBox center = (VBox) ((BorderPane) v.getRoot()).getCenter();
        VBox form   = (VBox) center.getChildren().get(2);
        return (TextField) form.getChildren().get(1);
    }

    private static Button getSingleBtn(ConnectView v) {
        VBox center = (VBox) ((BorderPane) v.getRoot()).getCenter();
        VBox form   = (VBox) center.getChildren().get(2);
        HBox spRow  = (HBox) form.getChildren().get(6);
        return (Button) spRow.getChildren().get(2);
    }

    /** Polls the FX thread until connectBtn is re-enabled (connection finished) or 10 s passes. */
    private static void awaitConnectionResult(ConnectView view) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            AtomicBoolean disabled = new AtomicBoolean(true);
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try { disabled.set(getConnectBtn(view).isDisable()); }
                catch (Exception e) { disabled.set(false); }
                latch.countDown();
            });
            latch.await(3, TimeUnit.SECONDS);
            if (!disabled.get()) return;
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void constructor_singleArg_doesNotThrow() throws Exception {
        runOnFx(() -> assertDoesNotThrow(() -> new ConnectView(c -> {})));
    }

    @Test
    void constructor_twoArgs_withIntConsumer_doesNotThrow() throws Exception {
        runOnFx(() -> assertDoesNotThrow(() -> new ConnectView(c -> {}, n -> {})));
    }

    @Test
    void constructor_twoArgs_nullIntConsumer_doesNotThrow() throws Exception {
        runOnFx(() -> assertDoesNotThrow(() -> new ConnectView(c -> {}, null)));
    }

    // ── getRoot ───────────────────────────────────────────────────────────────

    @Test
    void getRoot_isNotNull() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            assertNotNull(view.getRoot());
        });
    }

    @Test
    void getRoot_returnsBorderPane() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            assertInstanceOf(BorderPane.class, view.getRoot());
        });
    }

    // ── setConnecting ─────────────────────────────────────────────────────────

    @Test
    void setConnecting_true_disablesConnectButton() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            invokeSetConnecting(view, true, "Conectando...");
            assertTrue(getConnectBtn(view).isDisable());
        });
    }

    @Test
    void setConnecting_false_enablesConnectButton() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            invokeSetConnecting(view, true, "Conectando...");
            invokeSetConnecting(view, false, "Erro");
            assertFalse(getConnectBtn(view).isDisable());
        });
    }

    @Test
    void setConnecting_setsLabelText() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            invokeSetConnecting(view, true, "Mensagem de status");
            assertEquals("Mensagem de status", getStatusLabel(view).getText());
        });
    }

    @Test
    void setConnecting_true_setsOrangeStyle() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            invokeSetConnecting(view, true, "msg");
            assertTrue(getStatusLabel(view).getStyle().contains("#f39c12"));
        });
    }

    @Test
    void setConnecting_false_setsRedStyle() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            invokeSetConnecting(view, false, "msg");
            assertTrue(getStatusLabel(view).getStyle().contains("#e74c3c"));
        });
    }

    // ── Connect button: empty URL (early-return branch) ───────────────────────

    @Test
    void connectBtn_emptyUrl_showsWarning() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            getUrlField(view).setText("");
            getConnectBtn(view).fire();
            assertEquals("⚠️ Informe o endereço do servidor.", getStatusLabel(view).getText());
        });
    }

    @Test
    void connectBtn_emptyUrl_doesNotDisableButton() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            getUrlField(view).setText("");
            getConnectBtn(view).fire();
            assertFalse(getConnectBtn(view).isDisable());
        });
    }

    // ── Connect button: valid URL (background thread + Platform.runLater) ─────

    @Test
    void connectBtn_validUrl_disablesButtonWhileConnecting() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            getUrlField(view).setText("ws://127.0.0.1:19876/catan"); // nothing listening → fast failure
            getConnectBtn(view).fire();
            assertTrue(getConnectBtn(view).isDisable(), "Button must be disabled while connecting");
        });
    }

    @Test
    void connectBtn_failedConnection_reEnablesButton() throws Exception {
        AtomicReference<ConnectView> ref = new AtomicReference<>();
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            getUrlField(view).setText("ws://127.0.0.1:19876/catan");
            getConnectBtn(view).fire();
            ref.set(view);
        });

        awaitConnectionResult(ref.get());

        AtomicBoolean enabled = new AtomicBoolean(false);
        runOnFx(() -> {
            try { enabled.set(!getConnectBtn(ref.get()).isDisable()); }
            catch (Exception e) { fail("Reflection error: " + e.getMessage()); }
        });
        assertTrue(enabled.get(), "Button must be re-enabled after a failed connection");
    }

    @Test
    void connectBtn_failedConnection_showsErrorMessage() throws Exception {
        AtomicReference<ConnectView> ref = new AtomicReference<>();
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {});
            getUrlField(view).setText("ws://127.0.0.1:19876/catan");
            getConnectBtn(view).fire();
            ref.set(view);
        });

        awaitConnectionResult(ref.get());

        AtomicReference<String> text = new AtomicReference<>("");
        runOnFx(() -> {
            try { text.set(getStatusLabel(ref.get()).getText()); }
            catch (Exception e) { fail("Reflection error: " + e.getMessage()); }
        });
        assertTrue(text.get().contains("❌"), "Error message should contain ❌, got: " + text.get());
    }

    // ── Single-player button ──────────────────────────────────────────────────

    @Test
    void singleBtn_nullIntConsumer_doesNotThrow() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {}, null);
            assertDoesNotThrow(() -> getSingleBtn(view).fire());
        });
    }

    @Test
    void singleBtn_nullIntConsumer_doesNotSetConnectingState() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {}, null);
            getSingleBtn(view).fire();
            assertFalse(getConnectBtn(view).isDisable());
        });
    }

    @Test
    void singleBtn_withIntConsumer_callsConsumerWithSpinnerValue() throws Exception {
        AtomicInteger captured = new AtomicInteger(-1);
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {}, captured::set);
            getSingleBtn(view).fire();
        });
        assertTrue(captured.get() >= 1 && captured.get() <= 3,
            "Spinner default is 3 (range 1-3), got: " + captured.get());
    }

    @Test
    void singleBtn_withIntConsumer_setsConnectingState() throws Exception {
        runOnFx(() -> {
            ConnectView view = new ConnectView(c -> {}, n -> {});
            getSingleBtn(view).fire();
            assertTrue(getConnectBtn(view).isDisable(), "Button must be disabled after starting single-player");
        });
    }

    // ── label() private helper ────────────────────────────────────────────────

    @Test
    void labelHelper_returnsLabelWithCorrectText() throws Exception {
        runOnFx(() -> {
            Method m = ConnectView.class.getDeclaredMethod("label", String.class);
            m.setAccessible(true);
            Label l = (Label) m.invoke(null, "Endereço:");
            assertEquals("Endereço:", l.getText());
        });
    }

    @Test
    void labelHelper_appliesNonEmptyStyle() throws Exception {
        runOnFx(() -> {
            Method m = ConnectView.class.getDeclaredMethod("label", String.class);
            m.setAccessible(true);
            Label l = (Label) m.invoke(null, "x");
            assertFalse(l.getStyle().isEmpty());
        });
    }
}
