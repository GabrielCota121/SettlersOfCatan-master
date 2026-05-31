package com.example.network;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

/**
 * Tela inicial de conexão ao servidor Catan.
 *
 * <p>O jogador informa a URL do servidor (localhost para jogar sozinho, ou a URL
 * do ngrok para jogar com pessoas de outras máquinas) e seu nome/cor antes de
 * entrar no lobby.
 *
 * <p>A conexão WebSocket é tentada em background (sem travar a UI); o resultado
 * é entregue via {@code onConnected}.
 */
public class ConnectView {

    private static final String DEFAULT_LOCAL  = "ws://127.0.0.1:8080/catan";

    private final BorderPane root = new BorderPane();
    private final Label statusLabel = new Label();
    private final Button connectBtn = new Button("Conectar");

    public ConnectView(Consumer<GameWebSocketClient> onConnected) {
        build(onConnected);
    }

    public Pane getRoot() { return root; }

    private void build(Consumer<GameWebSocketClient> onConnected) {
        root.setStyle("-fx-background-color: #1a252f;");

        // ---- título ----
        Label title = new Label("Settlers of Catan");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 38));
        title.setTextFill(Color.web("#f0c040"));

        Label subtitle = new Label("Conectar ao servidor");
        subtitle.setFont(Font.font("Georgia", 18));
        subtitle.setTextFill(Color.web("#bdc3c7"));

        // ---- campo URL ----
        Label urlLabel = label("Endereço do servidor:");
        TextField urlField = new TextField(DEFAULT_LOCAL);
        urlField.setPromptText("ws://127.0.0.1:8080/catan  ou  https://xxxx.ngrok-free.app");
        urlField.setPrefWidth(480);
        urlField.setStyle("-fx-font-size: 14px;");

        // ---- dica ngrok ----
        Label hint = new Label("Para jogar com amigos em outras máquinas, use sua URL do ngrok aqui.");
        hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-style: italic;");

        // ---- status ----
        statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 14px;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(480);

        // ---- botão ----
        connectBtn.setStyle("""
                -fx-background-color: #2ecc71;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-font-size: 16px;
                -fx-padding: 10 40 10 40;
                -fx-background-radius: 8;
                """);
        connectBtn.setDefaultButton(true);

        connectBtn.setOnAction(e -> {
            String rawUrl = urlField.getText().trim();
            if (rawUrl.isEmpty()) {
                statusLabel.setText("⚠️ Informe o endereço do servidor.");
                return;
            }
            String url = normalizeUrl(rawUrl);
            setConnecting(true, "Conectando a " + url + " …");

            // Conexão em thread separada para não travar a UI.
            Thread t = new Thread(() -> {
                GameWebSocketClient client = new GameWebSocketClient();
                boolean ok = client.connect(url);
                Platform.runLater(() -> {
                    if (ok) {
                        onConnected.accept(client);
                    } else {
                        setConnecting(false, "❌ Falha ao conectar. Verifique se o servidor está rodando e a URL está correta.");
                    }
                });
            }, "ws-connect");
            t.setDaemon(true);
            t.start();
        });

        // ---- layout ----
        VBox form = new VBox(14,
                urlLabel, urlField, hint,
                connectBtn, statusLabel);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(520);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 12;");

        VBox center = new VBox(24, title, subtitle, form);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(60));
        root.setCenter(center);
    }

    private void setConnecting(boolean connecting, String message) {
        connectBtn.setDisable(connecting);
        statusLabel.setText(message);
        statusLabel.setStyle(connecting
                ? "-fx-text-fill: #f39c12; -fx-font-size: 14px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
    }

    /** Garante esquema correto (ws/wss) e path /catan ao final. */
    private static String normalizeUrl(String raw) {
        String url = raw;

        // 1. Corrige o esquema
        if (url.startsWith("https://")) url = "wss://" + url.substring(8);
        else if (url.startsWith("http://")) url = "ws://" + url.substring(7);
        else if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            boolean isLocal = url.startsWith("localhost") || url.matches("\\d+\\.\\d+\\.\\d+\\.\\d+.*");
            url = (isLocal ? "ws://" : "wss://") + url;
        }

        // 2. Garante que termina com /catan
        // Remove barra final se houver, depois adiciona o path.
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!url.contains("/catan")) url = url + "/catan";

        return url;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 14px;");
        return l;
    }
}
