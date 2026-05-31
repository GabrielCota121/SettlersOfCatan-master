package com.example.network;

import com.example.network.protocol.MessageType;
import com.example.network.protocol.NetworkMessage;
import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.RoomInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Tela de lobby do Catan (JavaFX). Lista salas, permite criar/entrar, mostra os
 * jogadores de uma sala com seus estados de "pronto" e dispara o início da
 * partida. Conversa com o servidor através do {@link GameWebSocketClient}.
 *
 * <p>Registra-se como listener de mensagens do cliente; ao iniciar a partida
 * (GAME_STARTED) entrega o {@link RoomInfo} final via {@code onGameStart} para
 * que o {@code Main} monte a cena do jogo (e reassuma o listener de mensagens).
 */
public class LobbyView {

    private static final String[] COLORS = {"RED", "BLUE", "ORANGE", "GREEN", "WHITE", "PURPLE"};

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final GameWebSocketClient client;
    private final BiConsumer<RoomInfo, Long> onGameStart;

    private final BorderPane root = new BorderPane();
    private final TextField nameField = new TextField();
    private final ComboBox<String> colorCombo = new ComboBox<>();
    private final VBox roomListContainer = new VBox(10);
    private final Label statusLabel = new Label();

    private RoomInfo currentRoom; // sala em que estamos (null = na lista)
    private Timeline refreshTimer;  // auto-refresh da lista de salas

    public LobbyView(GameWebSocketClient client, BiConsumer<RoomInfo, Long> onGameStart) {
        this.client = client;
        this.onGameStart = onGameStart;
        this.client.setOnMessage(this::handleMessage);
        buildRoomListScreen();
        client.listRooms();
    }

    public Pane getRoot() {
        return root;
    }

    // ----------------- Telas -----------------

    private void buildRoomListScreen() {
        currentRoom = null;
        root.setStyle("-fx-background-color: #2c3e50;");

        Label title = new Label("Ilha de Catan — Lobby");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        nameField.setPromptText("Seu nome");
        nameField.setMaxWidth(220);
        if (client.getPlayerName() != null) nameField.setText(client.getPlayerName());

        colorCombo.getItems().setAll(COLORS);
        colorCombo.getSelectionModel().selectFirst();
        colorCombo.setMaxWidth(140);

        Label nameLbl = label("Nome:");
        Label colorLbl = label("Cor:");
        HBox identityRow = new HBox(10, nameLbl, nameField, colorLbl, colorCombo);
        identityRow.setAlignment(Pos.CENTER_LEFT);

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Nome da sala");
        roomNameField.setMaxWidth(220);

        Spinner<Integer> maxPlayersSpinner = new Spinner<>(2, 4, 4);
        maxPlayersSpinner.setMaxWidth(80);

        Button createBtn = new Button("Criar sala");
        createBtn.setStyle("-fx-base: #2ecc71; -fx-font-weight: bold;");
        createBtn.setOnAction(e -> {
            if (!ensureName()) return;
            client.createRoom(roomNameField.getText(), maxPlayersSpinner.getValue(), colorCombo.getValue());
        });

        HBox createRow = new HBox(10, label("Sala:"), roomNameField, label("Máx:"), maxPlayersSpinner, createBtn);
        createRow.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("Atualizar");
        refreshBtn.setOnAction(e -> client.listRooms());

        Label listTitle = new Label("Salas disponíveis");
        listTitle.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 18px; -fx-font-weight: bold;");
        HBox listHeader = new HBox(15, listTitle, refreshBtn);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(roomListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #2c3e50; -fx-background-color: #2c3e50;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        statusLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");

        VBox content = new VBox(15, title, identityRow, createRow, new Separator(), listHeader, scroll, statusLabel);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #2c3e50;");
        root.setCenter(content);
        startRefresh();
    }

    private void buildRoomScreen(RoomInfo room) {
        stopRefresh();
        currentRoom = room;

        Label title = new Label("Sala: " + room.getName());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label sub = new Label(room.getPlayerCount() + "/" + room.getMaxPlayers() + " jogadores — " + room.getStatus());
        sub.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 14px;");

        VBox playersBox = new VBox(8);
        boolean iAmHost = false;
        boolean iAmReady = false;
        String myName = client.getPlayerName();

        for (PlayerInfo p : room.getPlayers()) {
            boolean isMe = myName != null && myName.equals(p.getName());
            if (isMe) {
                iAmHost = p.isHost();
                iAmReady = p.isReady();
            }
            String tag = (p.isHost() ? " 👑 (host)" : (p.isReady() ? " ✅ pronto" : " ⏳ aguardando"));
            Label pl = new Label("• " + p.getName() + " [" + p.getColor() + "]" + tag + (isMe ? "  (você)" : ""));
            pl.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
            playersBox.getChildren().add(pl);
        }

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        Button leaveBtn = new Button("Sair da sala");
        leaveBtn.setStyle("-fx-base: #e74c3c; -fx-font-weight: bold;");
        leaveBtn.setOnAction(e -> client.leaveRoom());
        controls.getChildren().add(leaveBtn);

        if (iAmHost) {
            Button startBtn = new Button("Iniciar partida");
            startBtn.setStyle("-fx-base: #2ecc71; -fx-font-weight: bold;");
            startBtn.setOnAction(e -> client.startGame());
            controls.getChildren().add(startBtn);
        } else {
            ToggleButton readyBtn = new ToggleButton(iAmReady ? "Pronto ✅" : "Marcar pronto");
            readyBtn.setSelected(iAmReady);
            readyBtn.setStyle("-fx-base: #3498db; -fx-font-weight: bold;");
            readyBtn.setOnAction(e -> client.setReady(readyBtn.isSelected()));
            controls.getChildren().add(readyBtn);
        }

        statusLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;");

        VBox content = new VBox(15, title, sub, new Separator(), playersBox, controls, statusLabel);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: #2c3e50;");
        root.setCenter(content);
    }

    private void renderRoomList(List<RoomInfo> rooms) {
        roomListContainer.getChildren().clear();
        if (rooms == null || rooms.isEmpty()) {
            Label empty = new Label("Nenhuma sala ainda. Crie a primeira!");
            empty.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            roomListContainer.getChildren().add(empty);
            return;
        }
        for (RoomInfo room : rooms) {
            HBox card = new HBox(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

            Label info = new Label(room.getName() + "   (" + room.getPlayerCount() + "/" + room.getMaxPlayers()
                    + ")   " + room.getStatus());
            info.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
            HBox.setHgrow(info, Priority.ALWAYS);
            info.setMaxWidth(Double.MAX_VALUE);

            Button joinBtn = new Button("Entrar");
            joinBtn.setStyle("-fx-base: #3498db; -fx-font-weight: bold;");
            boolean joinable = "WAITING".equals(room.getStatus()) && room.getPlayerCount() < room.getMaxPlayers();
            joinBtn.setDisable(!joinable);
            joinBtn.setOnAction(e -> {
                if (!ensureName()) return;
                client.joinRoom(room.getId(), colorCombo.getValue());
            });

            card.getChildren().addAll(info, joinBtn);
            roomListContainer.getChildren().add(card);
        }
    }

    // ----------------- Mensagens do servidor -----------------

    private void handleMessage(NetworkMessage msg) {
        try {
            switch (msg.getType()) {
                case MessageType.ROOM_LIST -> {
                    List<RoomInfo> rooms = convertRoomList(msg.getData().get("rooms"));
                    if (currentRoom == null) renderRoomList(rooms);
                }
                case MessageType.ROOM_JOINED -> {
                    RoomInfo room = convertRoom(msg.getData().get("room"));
                    if (room != null) buildRoomScreen(room);
                }
                case MessageType.ROOM_UPDATE -> {
                    RoomInfo room = convertRoom(msg.getData().get("room"));
                    if (room != null && currentRoom != null) buildRoomScreen(room);
                }
                case MessageType.ROOM_LEFT -> buildRoomListScreen();
                case MessageType.GAME_STARTED -> {
                    stopRefresh();
                    RoomInfo room = convertRoom(msg.getData().get("room"));
                    long seed = msg.getLong("seed", 0L);
                    if (onGameStart != null) onGameStart.accept(room, seed);
                }
                case MessageType.ERROR -> statusLabel.setText("⚠️ " + msg.getString("message"));
                default -> { }
            }
        } catch (Exception ex) {
            statusLabel.setText("⚠️ Erro ao processar mensagem: " + ex.getMessage());
            System.err.println("[LobbyView] erro em handleMessage: " + ex);
        }
    }

    // ----------------- Auxiliares -----------------

    /** Inicia o timer de auto-refresh (a cada 3 s pede a lista de salas atualizada). */
    private void startRefresh() {
        if (refreshTimer != null) refreshTimer.stop();
        refreshTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentRoom == null && client.isConnected()) client.listRooms();
        }));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void stopRefresh() {
        if (refreshTimer != null) { refreshTimer.stop(); refreshTimer = null; }
    }

    private boolean ensureName() {
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            statusLabel.setText("⚠️ Digite seu nome antes de continuar.");
            return false;
        }
        client.setPlayerName(name.trim());
        return true;
    }

    private List<RoomInfo> convertRoomList(Object raw) {
        if (raw == null) return List.of();
        return MAPPER.convertValue(raw,
                MAPPER.getTypeFactory().constructCollectionType(List.class, RoomInfo.class));
    }

    private RoomInfo convertRoom(Object raw) {
        return raw == null ? null : MAPPER.convertValue(raw, RoomInfo.class);
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 14px;");
        return l;
    }
}
