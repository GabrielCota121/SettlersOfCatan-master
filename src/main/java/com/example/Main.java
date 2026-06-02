package com.example;

import com.example.model.board.*;
import com.example.model.building.BuildingCost;
import com.example.model.building.City;
import com.example.model.building.Road;
import com.example.model.building.Settlement;
import com.example.model.cards.IDevelopmentCard;
import com.example.model.cards.KnightCard;
import com.example.model.cards.MonopolyCard;
import com.example.model.cards.RoadBuildingCard;
import com.example.model.cards.VictoryPointCard;
import com.example.model.cards.YearOfPlentyCard;
import com.example.model.game.CatanGameManager;
import com.example.model.game.ResourceType;
import com.example.model.game.Turn;
import com.example.model.logging.IGameLogger;
import com.example.model.logging.WebSocketLogger;
import com.example.model.player.Player;
import com.example.model.state.*;
import com.example.model.trade.TradeOffer;
import com.example.model.view.PieceDrawer;
import com.example.model.view.PlayerHandView;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import com.example.network.ConnectView;
import com.example.network.GameWebSocketClient;
import com.example.network.LobbyView;
import com.example.network.protocol.BuildingDTO;
import com.example.network.protocol.GameStateDTO;
import com.example.network.protocol.MessageType;
import com.example.network.protocol.PlayerInfo;
import com.example.network.protocol.PlayerStateDTO;
import com.example.network.protocol.RoomInfo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

public class Main extends Application {

    // Dimensões calculadas em tempo de execução com base na tela real do usuário.
    private static int WIDTH;
    private static int HEIGHT;
    private static int SIDEBAR_W;   // largura de cada sidebar lateral
    private static int BOTTOM_H;    // altura do menu inferior



    private GameWebSocketClient gameClient;

    // --- ESTADO DE REDE (servidor autoritativo) ---
    private boolean online = false;
    private int lastDiceTotal = 0;   // total do último lançamento; 0 = nenhum lançamento ainda
    private final Map<String, Vertex> vertexById = new HashMap<>();
    private final Map<String, Edge> edgeById = new HashMap<>();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private long lastRenderTime = 0;
    private final long MIN_FRAME_TIME_NS = 1_000_000_000 / 120;
    private int framesThisSecond = 0;
    private CatanGameManager gameManager;

    private final Map<ResourceType, Image> tileImages = new HashMap<>();
    private final Map<Integer, Image> numberImages = new HashMap<>();
    private final Map<String, Image> settlementImages = new HashMap<>();
    private Image devCardDeckImage;
    private final Map<String, Image> devCardImages = new HashMap<>();
    private final Map<String, Image> cityImages = new HashMap<>();
    private final Map<String, Image> roadImages = new HashMap<>();
    private final Map<String, Image> portImages = new HashMap<>();
    private final Map<String, Image> dockImages = new HashMap<>();

    private Image robberImage;

    private VBox rightSidebar;
    private VBox bankSidebarBox;
    private TextArea logArea;
    private Label playerNameLabel;
    private ImageView playerIconView;
    private PlayerHandView handView;
    private Player myPlayer = null;
    // jogador local deste cliente — definido em startGame quando online

    private double zoomLevel = 0.18;
    private double offsetX = 60;
    private double offsetY = 0;

    private double lastMouseX;
    private double lastMouseY;
    private boolean wasDragged = false;

    private final String[] coresAssets = {
            "black", "blue", "bronze", "green", "lightblue",
            "orange", "purple", "red", "silver", "white", "yellow"
    };

    @Override
    public void start(Stage primaryStage) {
        // Calcula dimensões com base na tela real — funciona em qualquer resolução.
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WIDTH    = (int) screen.getWidth();
        HEIGHT   = (int) screen.getHeight();
        SIDEBAR_W = (int) Math.min(300, screen.getWidth() * 0.17); // ~17 % da largura, máx 300 px
        BOTTOM_H  = (int) Math.min(170, screen.getHeight() * 0.20); // ~20 % da altura, máx 170 px

        ConnectView connectView = new ConnectView(client -> {
            this.gameClient = client;
            LobbyView lobby = new LobbyView(gameClient, (room, seed) -> startGame(primaryStage, room, seed));
            primaryStage.setTitle("Catan — Lobby");
            primaryStage.getScene().setRoot(lobby.getRoot());
        });

        Scene scene = new Scene(connectView.getRoot(), WIDTH, HEIGHT);
        primaryStage.setTitle("Catan — Conectar");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /** Monta a cena do jogo a partir dos jogadores da sala (ou padrões, se offline).
     *  Quando {@code seed != 0}, o tabuleiro é gerado de forma determinística para
     *  bater exatamente com o do servidor e dos demais clientes. */
    private void startGame(Stage primaryStage, RoomInfo room, long seed) {
        loadAssets();
        Runnable[] updateActionUIRef = new Runnable[1];

        Board board = (seed != 0L) ? BoardFactory.createStandardBoard(seed) : BoardFactory.createStandardBoard();
        this.online = (gameClient != null && gameClient.isConnected() && seed != 0L);
        vertexById.clear();
        edgeById.clear();
        for (Vertex v : board.getVertices()) vertexById.put(v.getId(), v);
        for (Edge e : board.getEdges()) edgeById.put(e.getId(), e);

        List<Player> players = new ArrayList<>();
        if (room != null && room.getPlayers() != null && !room.getPlayers().isEmpty()) {
            int id = 1;
            for (PlayerInfo pi : room.getPlayers()) {
                players.add(new Player(id++, pi.getName(), pi.getColor()));
            }
        } else {
            players.add(new Player(1, "Cauã", "RED"));
            players.add(new Player(2, "Marcelle", "PURPLE"));
            players.add(new Player(3, "Gabriel", "BLACK"));
            players.add(new Player(4, "Lucas", "GREEN"));
        }

        // Embaralha a ordem dos jogadores para variar quem começa
        if (online) Collections.shuffle(players);

        if (online && gameClient != null) {
            String myName = gameClient.getPlayerName();
            myPlayer = players.stream()
                .filter(p -> p.getName().equals(myName))
                .findFirst()
                .orElse(null);
            System.out.println("Jogador local identificado: " +
                (myPlayer != null ? myPlayer.getName() : "null"));
        }

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        logArea.setPrefWidth(SIDEBAR_W);

        IGameLogger logger = new WebSocketLogger() {
            @Override
            public void log(String message) {
                Platform.runLater(() -> {
                    logArea.appendText("🎲 " + message + "\n");
                });
            }
        };

        gameManager = new CatanGameManager(board, players, logger);
        gameManager.getCurrentTurn().setState(new SetupState(false));
        logger.log("Bem-vindo à Ilha de Catan! GLGL!");
        logger.log("Fase atual: " + gameManager.getCurrentTurn().getState().getName());
        logger.log(gameManager.getCurrentTurn().getCurrentPlayer().getName() + " começa!");

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setOnMousePressed(event -> {
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            wasDragged = false;
        });

        canvas.setOnMouseDragged(event -> {
            offsetX += event.getX() - lastMouseX;
            offsetY += event.getY() - lastMouseY;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            wasDragged = true;
            render(gc, board, false);
        });

        canvas.setOnScroll(event -> {
            if (event.getDeltaY() == 0) return;
            double zoomFactor = 1.1;
            if (event.getDeltaY() > 0) zoomLevel *= zoomFactor;
            else zoomLevel /= zoomFactor;
            zoomLevel = Math.max(0.1, Math.min(zoomLevel, 1.5));
            render(gc, board, false);
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #78b7e8;");

        StackPane canvasContainer = new StackPane(canvas);
        root.setCenter(canvasContainer);

        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        canvas.widthProperty().addListener(observable -> render(gc, board, true));
        canvas.heightProperty().addListener(observable -> render(gc, board, true));

        handView = new PlayerHandView();
        HBox devCardsBox = new HBox(10);
        devCardsBox.setAlignment(javafx.geometry.Pos.CENTER);

        for (Player p : players) {
            p.getWallet().setOnWalletChangedListener(() -> {
                handView.update(gameManager.getCurrentTurn().getCurrentPlayer());
                if (updateActionUIRef[0] != null) {
                    updateActionUIRef[0].run();
                }
            });
        }

        PlayerHandView bankHandView = new PlayerHandView();
        VBox bankInfoBox = new VBox(5);
        bankInfoBox.setAlignment(javafx.geometry.Pos.CENTER);

        int labelFontSize = Math.max(11, (int)(BOTTOM_H * 0.11));
        Label bankNameLabel = new Label("Banco");
        bankNameLabel.setStyle("-fx-font-size: " + labelFontSize + "px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        int playerIconH = (int)(BOTTOM_H * 0.25);
        ImageView bankIconView = new ImageView();
        bankIconView.setFitHeight(playerIconH);
        bankIconView.setPreserveRatio(true);

        try {
            Image bankIcon = new Image(getClass().getResourceAsStream("/assets/bank/bankicon.png"));
            bankIconView.setImage(bankIcon);
        } catch (Exception e) {
            System.out.println("Ícone do banco não encontrado.");
        }

        bankInfoBox.getChildren().addAll(bankNameLabel, bankIconView);
        gameManager.getBank().getWallet().setOnWalletChangedListener(() -> {
            bankHandView.update(gameManager.getBank().getWallet());
        });
        bankHandView.update(gameManager.getBank().getWallet());

        bankHandView.setScaleX(0.7);
        bankHandView.setScaleY(0.7);
        javafx.scene.Group bankHandGroup = new javafx.scene.Group(bankHandView);

        bankSidebarBox = new VBox(10, bankInfoBox, bankHandGroup);
        bankSidebarBox.setAlignment(javafx.geometry.Pos.CENTER);
        bankSidebarBox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10; -fx-background-radius: 8;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        VBox playerInfoBox = new VBox(5);
        playerInfoBox.setAlignment(javafx.geometry.Pos.CENTER);

        playerNameLabel = new Label();
        playerNameLabel.setStyle("-fx-font-size: " + Math.max(13, (int)(BOTTOM_H * 0.14)) + "px; -fx-font-weight: bold; -fx-text-fill: white;");

        playerIconView = new ImageView();
        playerIconView.setFitHeight(playerIconH);
        playerIconView.setPreserveRatio(true);
        playerInfoBox.getChildren().addAll(playerNameLabel, playerIconView);

        Image passTurnImg = null;
        Image cantPassTurnImg = null;
        Image tradeImg = null;
        Image bgButtonImg = null;
        Image[] diceImgs = new Image[7];

        try {
            passTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/passturn.png"));
            cantPassTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/cantpassturn.png"));
            tradeImg = new Image(getClass().getResourceAsStream("/assets/trade/tradeicon.png"));
            bgButtonImg = new Image(getClass().getResourceAsStream("/assets/background/bgbutton.png"));
            for (int i = 1; i <= 6; i++) {
                String path = "/assets/dice/" + i + ".png"; // Adicionado .png se aplicável ao seu asset
                var stream = getClass().getResourceAsStream(path);
                if (stream != null) {
                    diceImgs[i] = new Image(stream);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar botões visuais: " + e.getMessage());
        }

        // Alturas dos botões calculadas proporcionalmente ao menu inferior.
        int btnH  = (int)(BOTTOM_H * 0.44); // botões principais (dados, pass turn, dev card)
        int bgH   = (int)(BOTTOM_H * 0.42); // background dos botões
        int iconH = (int)(BOTTOM_H * 0.30); // ícones dentro dos botões
        int cardH = (int)(BOTTOM_H * 0.44); // cartas de desenvolvimento na mão
        int diceFontSize = Math.max(12, (int)(BOTTOM_H * 0.12));

        ImageView passTurnView = new ImageView(cantPassTurnImg);
        passTurnView.setFitHeight(btnH);
        passTurnView.setPreserveRatio(true);

        ImageView dice1View = new ImageView(diceImgs[1]);
        ImageView dice2View = new ImageView(diceImgs[6]);
        dice1View.setFitHeight(btnH);
        dice1View.setPreserveRatio(true);
        dice2View.setFitHeight(btnH);
        dice2View.setPreserveRatio(true);

        HBox diceBox = new HBox(5, dice1View, dice2View);
        diceBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label diceResultLabel = new Label("");
        diceResultLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: " + diceFontSize + "px; -fx-font-weight: bold;");

        int rollBtnFont = Math.max(12, (int)(BOTTOM_H * 0.10));
        Button rollDiceBtn = new Button("🎲  Rolar Dados");
        rollDiceBtn.setStyle(
                "-fx-background-color: #27ae60;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: " + rollBtnFont + "px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 6 14 6 14;" +
                "-fx-background-radius: 8;");

        VBox diceWithResult = new VBox(4, diceBox, diceResultLabel, rollDiceBtn);
        diceWithResult.setAlignment(javafx.geometry.Pos.CENTER);

        ImageView tradeIconView = new ImageView(tradeImg);
        tradeIconView.setFitHeight(iconH);
        tradeIconView.setPreserveRatio(true);

        ImageView tradeBgView = new ImageView(bgButtonImg);
        tradeBgView.setFitHeight(bgH);
        tradeBgView.setPreserveRatio(true);

        StackPane tradeButtonPane = new StackPane(tradeBgView, tradeIconView);

        ImageView buyDevCardView = new ImageView(devCardDeckImage);
        buyDevCardView.setFitHeight(iconH);
        buyDevCardView.setPreserveRatio(true);

        ImageView devCardBgView = new ImageView(bgButtonImg);
        devCardBgView.setFitHeight(bgH);
        devCardBgView.setPreserveRatio(true);

        StackPane devCardButtonPane = new StackPane(devCardBgView, buyDevCardView);

        HBox diceAndTradeBox = new HBox(10, devCardButtonPane, tradeButtonPane, diceWithResult);
        diceAndTradeBox.setAlignment(javafx.geometry.Pos.CENTER);

        VBox turnControlsBox = new VBox(8, diceAndTradeBox, passTurnView);
        turnControlsBox.setAlignment(javafx.geometry.Pos.CENTER);

        Image finalPassTurnImg = passTurnImg;
        Image finalCantPassTurnImg = cantPassTurnImg;

        Runnable updateDevCardsUI = () -> {
            devCardsBox.getChildren().clear();
            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            ITurnState state = gameManager.getCurrentTurn().getState();

            List<IDevelopmentCard> playableCards = currentPlayer.getPlayableCards();
            List<IDevelopmentCard> newCards = currentPlayer.getNewCards();

            List<IDevelopmentCard> allCards = new ArrayList<>();
            if (playableCards != null) allCards.addAll(playableCards);
            if (newCards != null) allCards.addAll(newCards);

            if (!allCards.isEmpty()) {
                for (IDevelopmentCard card : allCards) {
                    ImageView cardView = new ImageView();
                    cardView.setFitHeight(cardH);
                    cardView.setPreserveRatio(true);

                    boolean isCorrectState = state instanceof MainState || state instanceof WaitingRollState;
                    boolean isPlayable = playableCards != null && playableCards.contains(card);
                    boolean canPlay = isCorrectState && isPlayable;

                    cardView.setOpacity(canPlay ? 1.0 : 0.5);
                    cardView.setCursor(canPlay ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

                    String cardName = card.getName().toLowerCase().replace(" ", "");
                    try {
                        Image cardImg = new Image(getClass().getResourceAsStream("/assets/developmentcards/" + cardName + ".png"));
                        cardView.setImage(cardImg);
                    } catch (Exception e) {
                        System.out.println("Imagem da carta não encontrada.");
                    }

                    cardView.setOnMouseClicked(e -> {
                        if (!canPlay) {
                            if (!isPlayable) {

                                gameManager.getLogger().log(
                                    "Você não pode jogar uma carta comprada no mesmo turno!");
                            } else {
                                gameManager.getLogger().log(
                                    "Você não pode jogar cartas agora!");
                            }
                            return;
                        }
                        if (online) {
                            // Envia a intenção ao servidor com o tipo de carta
                            String cardAction = switch (card.getName()) {
                                case "Knight"         -> "PLAY_KNIGHT";
                                case "Monopoly"       -> "PLAY_MONOPOLY";
                                case "Road Building"  -> "PLAY_ROAD_BUILDING";
                                case "Year of Plenty" -> "PLAY_YEAR_OF_PLENTY";
                                case "Victory Point"  -> "PLAY_VICTORY_POINT";
                                default               -> null;
                            };
                            if (cardAction != null) {
                                gameClient.sendIntent(cardAction, null);
                            }
                            return;
                        }
                        // Modo offline: lógica local original
                        boolean success = state.playDevelopmentCard(
                            card, gameManager.getCurrentTurn());
                        if (success && updateActionUIRef[0] != null) {
                            updateActionUIRef[0].run();
                        }
                    });
                    devCardsBox.getChildren().add(cardView);
                }
            }
        };

        Runnable updateActionUI = () -> {
            ITurnState state = gameManager.getCurrentTurn().getState();
            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();

            passTurnView.setImage(state.canEndTurn() ? finalPassTurnImg : finalCantPassTurnImg);
            passTurnView.setCursor(state.canEndTurn() ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            boolean canRoll = state.canRollDice();
            diceBox.setOpacity(canRoll ? 1.0 : 0.3);
            rollDiceBtn.setDisable(!canRoll);
            rollDiceBtn.setStyle(
                    "-fx-background-color: " + (canRoll ? "#27ae60" : "#555") + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: " + rollBtnFont + "px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 6 14 6 14;" +
                    "-fx-background-radius: 8;");

            boolean isMainState = state instanceof MainState;
            tradeButtonPane.setOpacity(isMainState ? 1.0 : 0.3);
            tradeButtonPane.setCursor(isMainState ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            devCardButtonPane.setOpacity((isMainState && currentPlayer.canAfford(BuildingCost.DEVELOPMENT_CARD)) ? 1.0 : 0.3);
            devCardButtonPane.setCursor(isMainState ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            int d1 = gameManager.getDice1().getResult();
            int d2 = gameManager.getDice2().getResult();

            if (d1 > 0 && d1 <= 6 && diceImgs[d1] != null) dice1View.setImage(diceImgs[d1]);
            if (d2 > 0 && d2 <= 6 && diceImgs[d2] != null) dice2View.setImage(diceImgs[d2]);

            int total = d1 + d2;
            if (total > 1) {
                lastDiceTotal = total;
                diceResultLabel.setText(d1 + " + " + d2 + " = " + total);
                diceResultLabel.setStyle(total == 7
                        ? "-fx-text-fill: #e74c3c; -fx-font-size: 22px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #f1c40f; -fx-font-size: 22px; -fx-font-weight: bold;");
            }

            updateSidebar();
            updateDevCardsUI.run();
        };

        updateActionUIRef[0] = updateActionUI;

        Runnable bindPlayerToUI = () -> {
            // Novo turno: apaga o destaque dos tiles do turno anterior.
            lastDiceTotal = 0;
            diceResultLabel.setText("");

            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            // Em modo online: sempre mostra a mão do jogador local.
            // Em modo local/offline: mostra o jogador da vez (comportamento original).
            Player viewPlayer = (online && myPlayer != null) ? myPlayer : currentPlayer;
            handView.update(viewPlayer);
            playerNameLabel.setText(currentPlayer.getName());
            String colorName = currentPlayer.getColor().toLowerCase();
            String imagePath = "/assets/settlement/" + colorName + "set.png";
            try {
                Image iconImg = new Image(getClass().getResource(imagePath).toExternalForm());
                playerIconView.setImage(iconImg);
            } catch (Exception e) {
                System.out.println("Erro ao carregar asset do jogador.");
            }
            updateActionUI.run();
        };

        rollDiceBtn.setOnAction(e -> {
            if (online) { gameClient.sendIntent("ROLL_DICE", null); return; }
            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            boolean success = gameManager.rollDice(currentPlayer);
            if (!success) {
                gameManager.getLogger().log("Você não pode rolar os dados agora!");
            }
            updateActionUI.run();
        });

        passTurnView.setOnMouseClicked(e -> {
            if (online) { gameClient.sendIntent("END_TURN", null); return; }
            Turn currentTurn = gameManager.getCurrentTurn();
            boolean success = currentTurn.getState().endTurn(currentTurn);
            if (!success) {
                gameManager.getLogger().log("Você não pode passar o turno agora!");
            }
            updateActionUI.run();
        });

        tradeButtonPane.setOnMouseClicked(e -> {
            ITurnState currentState = gameManager.getCurrentTurn().getState();
            if (!(currentState instanceof MainState)) {
                gameManager.getLogger().log(
                    "Você só pode propor trocas na MainState!");
                return;
            }
            // Em modo online: só o jogador da vez pode iniciar troca
            if (online && myPlayer != null) {
                Player currentPlayer =
                    gameManager.getCurrentTurn().getCurrentPlayer();
                if (!myPlayer.equals(currentPlayer)) {
                    gameManager.getLogger().log(
                        "Você só pode iniciar uma troca no seu turno!");
                    return;
                }
            }
            buildTradeOptionsSidebar();
        });

        devCardButtonPane.setOnMouseClicked(e -> {
            if (online) { gameClient.sendIntent("BUY_DEV_CARD", null); return; }
            ITurnState currentState = gameManager.getCurrentTurn().getState();
            if (currentState instanceof MainState) {
                boolean success = currentState.buyDevelopmentCard(gameManager.getCurrentTurn());
                if (!success) {
                    gameManager.getLogger().log("Não possui recursos suficientes.");
                }
                updateActionUI.run();
            } else {
                gameManager.getLogger().log("Apenas compre cartas de desenvolvimento na sua rodada!");
            }
        });

        canvas.setOnMouseReleased(event -> {
            if (wasDragged) {
                render(gc, board, true);
                return;
            }

            double worldX = (event.getX() - offsetX) / zoomLevel;
            double worldY = (event.getY() - offsetY) / zoomLevel;

            ITurnState currentState = gameManager.getCurrentTurn().getState();

            if (online) {
                // LADRÃO tem prioridade sobre construções
                if (currentState instanceof MoveRobberState) {
                    Tile clickedTile = findTileAt(board, worldX, worldY);
                    if (clickedTile != null) {
                        gameClient.sendIntent("MOVE_ROBBER",
                            String.valueOf(clickedTile.getId()));
                    }
                    render(gc, board, true);
                    return;
                }

                // Construções normais
                Vertex v = findVertexAt(board, worldX, worldY);
                if (v != null) {
                    gameClient.sendIntent(
                        v.isEmpty() ? "BUILD_SETTLEMENT" : "BUILD_CITY", v.getId());
                } else {
                    Edge edge = findEdgeAt(board, worldX, worldY);
                    if (edge != null) gameClient.sendIntent("BUILD_ROAD", edge.getId());
                }
                render(gc, board, true);
                return;
            }

            // --- Modo offline: lógica original intacta ---
            if (currentState instanceof MoveRobberState) {
                Tile clickedTile = findTileAt(board, worldX, worldY);
                if (clickedTile != null) {
                    MoveRobberState robberState = (MoveRobberState) currentState;
                    List<Player> victims = robberState.moveRobber(
                        clickedTile, gameManager.getCurrentTurn());
                    victims.removeIf(p -> p.getWallet().getTotalCards() == 0);
                    if (victims.isEmpty()) {
                        robberState.executeSteal(null, gameManager.getCurrentTurn());
                        updateActionUI.run();
                    } else if (victims.size() == 1) {
                        robberState.executeSteal(victims.get(0),
                            gameManager.getCurrentTurn());
                        updateActionUI.run();
                    } else {
                        buildStealVictimSidebar(victims, robberState, updateActionUI);
                    }
                }
                render(gc, board, true);
                return;
            }

            // Construções offline normais
            Vertex v = findVertexAt(board, worldX, worldY);
            if (v != null) {
                boolean builtSettlement = currentState.buildSettlement(v, gameManager.getCurrentTurn());
                if (builtSettlement) {
                    updateActionUI.run();
                } else {
                    boolean builtCity = currentState.buildCity(v, gameManager.getCurrentTurn());
                    if (builtCity) {
                        updateActionUI.run();
                    }
                }
            } else {
                Edge edge = findEdgeAt(board, worldX, worldY);
                if (edge != null) {
                    boolean builtRoad = currentState.buildRoad(edge, gameManager.getCurrentTurn());
                    if (builtRoad) {
                        updateActionUI.run();
                    }
                }
            }
            render(gc, board, true);
        });

        rightSidebar = new VBox(15);
        rightSidebar.setPrefWidth(SIDEBAR_W);
        rightSidebar.setMinWidth(SIDEBAR_W);
        rightSidebar.setMaxWidth(SIDEBAR_W);
        rightSidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 12;");
        rightSidebar.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        javafx.scene.control.ScrollPane rightScroll = new javafx.scene.control.ScrollPane(rightSidebar);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rightScroll.setStyle("-fx-background: #34495e; -fx-background-color: #34495e; -fx-border-color: #34495e;");

        root.setRight(rightScroll);

        VBox leftSidebar = new VBox(10);
        leftSidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 10;");
        Label logTitle = new Label("Histórico");
        logTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        leftSidebar.getChildren().addAll(logTitle, logArea);
        javafx.scene.layout.VBox.setVgrow(logArea, javafx.scene.layout.Priority.ALWAYS);
        root.setLeft(leftSidebar);

        bindPlayerToUI.run();
        gameManager.setOnTurnChangedListener(bindPlayerToUI);

        Label myNameLabel = new Label(
            online && myPlayer != null ? "Você: " + myPlayer.getName() : ""
        );
        myNameLabel.setStyle(
            "-fx-text-fill: #2ecc71; -fx-font-size: 11px; -fx-font-weight: bold;"
        );

        HBox bottomMenu = new HBox(12, playerInfoBox, myNameLabel, handView, devCardsBox, spacer, turnControlsBox);
        bottomMenu.setPrefHeight(BOTTOM_H);
        // BorderPane coloca o bottom com largura TOTAL (incluindo atrás das sidebars).
        // Adicionamos padding lateral igual às sidebars para o conteúdo ficar visível.
        bottomMenu.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10 " + (SIDEBAR_W + 8) + "px 10 " + (SIDEBAR_W + 8) + "px;");
        bottomMenu.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        root.setBottom(bottomMenu);
        render(gc, board, true);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            int playerIndex = -1;
            switch (event.getCode()) {
                case DIGIT1: case NUMPAD1: playerIndex = 0; break;
                case DIGIT2: case NUMPAD2: playerIndex = 1; break;
                case DIGIT3: case NUMPAD3: playerIndex = 2; break;
                case DIGIT4: case NUMPAD4: playerIndex = 3; break;
                case DIGIT0: case NUMPAD0: case ESCAPE:
                    bindPlayerToUI.run();
                    event.consume();
                    return;
                case Q:
                    if (!online) {
                        gameManager.getCurrentTurn()
                            .getCurrentPlayer()
                            .getWallet().addResource(ResourceType.WOOD, 1);
                        updateActionUIRef[0].run();
                    }
                    event.consume();
                    return;
                case W:
                    if (!online) {
                        gameManager.getCurrentTurn()
                            .getCurrentPlayer()
                            .getWallet().addResource(ResourceType.BRICK, 1);
                        updateActionUIRef[0].run();
                    }
                    event.consume();
                    return;
                case E:
                    if (!online) {
                        gameManager.getCurrentTurn()
                            .getCurrentPlayer()
                            .getWallet().addResource(ResourceType.WOOL, 1);
                        updateActionUIRef[0].run();
                    }
                    event.consume();
                    return;
                case R:
                    if (!online) {
                        gameManager.getCurrentTurn()
                            .getCurrentPlayer()
                            .getWallet().addResource(ResourceType.WHEAT, 1);
                        updateActionUIRef[0].run();
                    }
                    event.consume();
                    return;
                case T:
                    if (!online) {
                        gameManager.getCurrentTurn()
                            .getCurrentPlayer()
                            .getWallet().addResource(ResourceType.ORE, 1);
                        updateActionUIRef[0].run();
                    }
                    event.consume();
                    return;
                default:
                    return;
            }
            List<Player> allPlayers = gameManager.getPlayers();
            if (playerIndex >= 0 && playerIndex < allPlayers.size()) {
                Player selectedPlayer = allPlayers.get(playerIndex);

                // Em rede, impede ver a mão de outro jogador.
                if (online && selectedPlayer.getWallet().isHidden()) {
                    gameManager.getLogger().log("Não é possível ver a mão de outros jogadores em partidas online.");
                    event.consume();
                    return;
                }

                handView.update(selectedPlayer);
                playerNameLabel.setText(selectedPlayer.getName());

                String colorName = selectedPlayer.getColor().toLowerCase();
                String imagePath = "/assets/settlement/" + colorName + "set.png";
                try {
                    Image iconImg = new Image(getClass().getResource(imagePath).toExternalForm());
                    playerIconView.setImage(iconImg);
                } catch (Exception e) {
                    System.out.println("Imagem ausente: " + imagePath);
                }
                event.consume();
            }
        });

        primaryStage.setScene(scene);

        // Em rede: o servidor é a fonte da verdade. Recebemos snapshots (GAME_STATE)
        // e os aplicamos ao modelo local apenas para renderizar; os cliques viram
        // intenções enviadas ao servidor.
        if (online) {
            logArea.appendText("🌐 Partida em rede — sala " + gameClient.getCurrentRoomId() + "\n");
            gameClient.setOnMessage(msg -> {
                switch (msg.getType()) {
                    case MessageType.GAME_STATE -> {
                        GameStateDTO st = objectMapper.convertValue(msg.getData().get("state"), GameStateDTO.class);
                        applyGameState(st);
                        // Garante que a handView sempre mostre o jogador local
                        // independente de quem é o turno atual.
                        if (myPlayer != null) {
                            handView.update(myPlayer);
                        }
                        bindPlayerToUI.run();
                        render(gc, board, true);
                    }
                    case MessageType.GAME_EVENT -> logArea.appendText("🎲 " + msg.getString("message") + "\n");
                    case MessageType.TRADE_UPDATE -> {
                        com.example.network.protocol.TradeStatusDTO trade =
                            objectMapper.convertValue(
                                msg.getData().get("trade"),
                                com.example.network.protocol.TradeStatusDTO.class
                            );
                        Platform.runLater(() -> applyTradeUpdate(trade));
                    }
                    case MessageType.DISCARD_REQUIRED -> {
                        // Mostra na log quem precisa descartar
                        @SuppressWarnings("unchecked")
                        List<String> discardPlayers = (List<String>) msg.getData().get("players");
                        if (discardPlayers != null) {
                            Platform.runLater(() ->
                                logArea.appendText("🃏 Descarte necessário: " + String.join(", ", discardPlayers) + "\n")
                            );
                        }
                    }
                    case MessageType.DISCARD_WAITING -> {
                        @SuppressWarnings("unchecked")
                        List<String> remaining = (List<String>) msg.getData().get("remaining");
                        if (remaining != null) {
                            Platform.runLater(() ->
                                logArea.appendText("⏳ Aguardando descarte de: " + String.join(", ", remaining) + "\n")
                            );
                        }
                    }
                    default -> { /* ROOM_* já não importam dentro do jogo */ }
                }
            });
        }

        javafx.animation.AnimationTimer fpsTimer = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 1_000_000_000) {
                    primaryStage.setTitle("Catan Toca do Cota | FPS: " + framesThisSecond);
                    framesThisSecond = 0;
                    lastUpdate = now;
                }
            }
        };
        fpsTimer.start();
        primaryStage.show();
    }

    /**
     * Reconcilia o modelo local com o estado autoritativo recebido do servidor.
     * Sobrescreve tudo que é renderizado (turno, construções, ladrão, dados,
     * recursos, pontos e banco), de modo que o snapshot do servidor sempre vence.
     */
    private void applyGameState(GameStateDTO state) {
        if (state == null) return;
        Board board = gameManager.getBoard();

        Map<String, Player> byName = new HashMap<>();
        for (Player p : gameManager.getPlayers()) byName.put(p.getName(), p);

        // Turno: jogador da vez + estado (reconstruído pelo nome para o gating da UI).
        Player current = byName.get(state.getCurrentPlayerName());
        if (current != null) gameManager.getCurrentTurn().setCurrentPlayer(current);
        gameManager.getCurrentTurn().setState(stateFromSnapshot(state, byName));

        // Dados — atualiza o destaque dos tiles no próximo render.
        gameManager.getDice1().setResult(state.getDice1());
        gameManager.getDice2().setResult(state.getDice2());
        int d1s = state.getDice1(), d2s = state.getDice2();
        lastDiceTotal = (d1s > 0 && d2s > 0) ? d1s + d2s : 0;

        // Ladrão.
        if (state.getRobberTileId() >= 0) {
            for (Tile t : board.getTiles()) {
                if (t.getId() == state.getRobberTileId()) { gameManager.getRobber().move(t); break; }
            }
        }

        // Reconstrói WaitingDiscardState no modelo local quando o servidor
        // informa que há jogadores pendentes de descarte.
        if (state.getDiscardPendingPlayers() != null &&
                !state.getDiscardPendingPlayers().isEmpty()) {
            Map<String, Player> byName2 = new HashMap<>();
            for (Player p : gameManager.getPlayers()) byName2.put(p.getName(), p);

            List<Player> pending = state.getDiscardPendingPlayers().stream()
                .map(byName2::get)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

            if (!(gameManager.getCurrentTurn().getState() instanceof WaitingDiscardState)) {
                gameManager.getCurrentTurn().setState(new WaitingDiscardState(pending));
            }
        }

        // Quando o servidor confirma MoveRobberState, mostra sidebar
        // de escolha de vítima se houver candidatos
        if (state.getStateName() != null &&
                state.getStateName().contains("Robber") &&
                online && myPlayer != null &&
                myPlayer.getName().equals(state.getCurrentPlayerName())) {
            // É o jogador local quem move o ladrão — nada a fazer aqui,
            // o clique já mandou MOVE_ROBBER. Sidebar de vítima será
            // tratada pelo próximo GAME_STATE após o servidor processar.
        }

        // Construções: limpa e reconstrói a partir do snapshot.
        for (Vertex v : board.getVertices()) v.setBuilding(null);
        for (Edge e : board.getEdges()) e.setBuilding(null);
        for (BuildingDTO b : state.getBuildings()) {
            Vertex v = vertexById.get(b.getLocationId());
            Player owner = byName.get(b.getOwnerName());
            if (v != null && owner != null) {
                v.setBuilding("CITY".equals(b.getType()) ? new City(owner, v) : new Settlement(owner, v));
            }
        }
        for (BuildingDTO r : state.getRoads()) {
            Edge e = edgeById.get(r.getLocationId());
            Player owner = byName.get(r.getOwnerName());
            if (e != null && owner != null) e.setBuilding(new Road(owner, e));
        }

        // Jogadores: recursos, pontos, contadores e cartas de desenvolvimento.
        for (PlayerStateDTO ps : state.getPlayers()) {
            Player p = byName.get(ps.getName());
            if (p == null) continue;
            p.setVictoryPoints(ps.getVictoryPoints());
            p.setNumKnights(ps.getNumKnights());
            p.setLongestRoad(ps.getLongestRoad());
            p.setNumSettlements(ps.getNumSettlements());
            p.setNumCities(ps.getNumCities());
            p.setNumRoads(ps.getNumRoads());

            if (ps.isHiddenResources()) {
                // Jogador cujos recursos NÃO pertencem a este cliente: armazena só
                // o total (para a sidebar) e mantém os valores individuais zerados.
                p.getWallet().setHiddenTotal(ps.getNumResources());
                // Dev cards: só quantidade visível, sem nomes.
                p.getPlayableCards().clear();
                p.getNewCards().clear();
            } else {
                // Próprio jogador: recebe tudo em claro.
                p.getWallet().clearHidden();
                for (ResourceType type : ResourceType.values()) {
                    if (type == ResourceType.DESERT) continue;
                    Integer amt = ps.getResources().get(type.name());
                    p.getWallet().setResource(type, amt == null ? 0 : amt);
                }
                p.getPlayableCards().clear();
                p.getNewCards().clear();
                // O servidor envia as cartas na ordem: playable primeiro, new depois.
                // VictoryPointCard nunca deve aparecer como jogável — vai para newCards
                // para ficar visível mas não clicável (o ponto já foi concedido ao comprar).
                for (String cardName : ps.getDevCards()) {
                    IDevelopmentCard c = devCardFromName(cardName);
                    if (c != null) {
                        if (c instanceof VictoryPointCard) {
                            p.addNewCard(c); // VP card: visível mas não jogável
                        } else {
                            p.addPlayableCard(c);
                        }
                    }
                }
            }
        }

        // Banco.
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;
            Integer amt = state.getBank().get(type.name());
            gameManager.getBank().getWallet().setResource(type, amt == null ? 0 : amt);
        }
    }

    /** Reconstrói um {@link ITurnState} a partir do snapshot, só para gating da UI. */
    private ITurnState stateFromSnapshot(GameStateDTO s,
            Map<String, Player> byName) {
        if (s.getWinnerName() != null) {
            Player winner = byName.get(s.getWinnerName());
            if (winner != null) return new GameOverState(winner);
        }
        if (s.isSetupPhase()) return new SetupState(s.isSetupSecondPass());

        String n = s.getStateName();
        if (n == null) return new WaitingRollState();

        // Fase Principal
        if ("Fase Principal".equals(n)) return new MainState();

        // Aguardando rolar os dados
        if ("Aguardando rolar os dados!".equals(n)) return new WaitingRollState();

        // Ladrão
        if (n.contains("Robber") || n.contains("mover o Robber")) {
            return new MoveRobberState(new MainState());
        }

        // Descarte de cartas (dado 7)
        if (n.contains("descartar") || n.contains("descart")) {
            if (s.getDiscardPendingPlayers() != null &&
                    !s.getDiscardPendingPlayers().isEmpty()) {
                List<Player> pending = s.getDiscardPendingPlayers()
                    .stream()
                    .map(byName::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
                return new WaitingDiscardState(pending);
            }
            return new MainState();
        }

        // Trocas em andamento (estado representado pelo activeTrade no DTO)
        if (n.contains("Aguardando Resposta") || s.getActiveTrade() != null) {
            return new MainState();
        }

        // Default: deixa o estado como MainState para não bloquear UI
        return new MainState();
    }

    private IDevelopmentCard devCardFromName(String name) {
        if (name == null) return null;
        return switch (name) {
            case "Knight" -> new KnightCard();
            case "Monopoly" -> new MonopolyCard();
            case "Road Building" -> new RoadBuildingCard();
            case "Victory Point" -> new VictoryPointCard();
            case "Year of Plenty" -> new YearOfPlentyCard();
            default -> null;
        };
    }

    private void updateSidebar() {
        if (rightSidebar == null) return;
        rightSidebar.getChildren().clear();
        ITurnState state = gameManager.getCurrentTurn().getState();

        if (state instanceof WaitingDiscardState discardState) {
            List<Player> pending = discardState.getPendingPlayers();
            if (!pending.isEmpty()) {
                buildDiscardSidebar(pending.get(0), discardState);
            }
        } else if (state instanceof PlayerTradeState tradeState) {
            buildPlayerTradeResponseSidebar(tradeState);
        } else if (state instanceof MonopolyState) {
            buildMonopolySidebar();
        } else if (state instanceof YearOfPlentyState) {
            buildYearOfPlentySidebar();
        }
        buildPlayerOverviewSidebar();
    }

    private void buildMonopolySidebar() {
        rightSidebar.getChildren().clear();
        Label titleLabel = new Label("Monopoly");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label infoLabel = new Label("Escolha o recurso para monopolizar!");
        infoLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true; -fx-text-alignment: center;");

        ComboBox<ResourceType> resourceCombo = new ComboBox<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) resourceCombo.getItems().add(type);
        }

        Button confirmBtn = new Button("Monopolizar Recurso!");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #9b59b6; -fx-pref-width: 150px;");
        confirmBtn.setOnAction(e -> {
            ResourceType selected = resourceCombo.getValue();
            if (selected != null) {
                if (online) {
                    gameClient.sendIntent("PLAY_MONOPOLY", selected.name());
                } else {
                    ITurnState state = gameManager.getCurrentTurn().getState();
                    if (state instanceof MonopolyState) {
                        ((MonopolyState) state).chooseResource(selected, gameManager.getCurrentTurn());
                        updateSidebar();
                    }
                }
            } else {
                gameManager.getLogger().log("Selecione um recurso primeiro!");
            }
        });
        rightSidebar.getChildren().addAll(titleLabel, infoLabel, resourceCombo, confirmBtn);
    }

    private void buildYearOfPlentySidebar() {
        rightSidebar.getChildren().clear();
        Label titleLabel = new Label("Year of Plenty");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label infoLabel = new Label("Escolha 2 recursos para pegar do banco!");
        infoLabel.setStyle("-fx-text-fill: white; -fx-wrap-text: true; -fx-text-alignment: center;");

        HBox combosBox = new HBox(10);
        combosBox.setAlignment(javafx.geometry.Pos.CENTER);

        ComboBox<ResourceType> res1Combo = new ComboBox<>();
        ComboBox<ResourceType> res2Combo = new ComboBox<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                res1Combo.getItems().add(type);
                res2Combo.getItems().add(type);
            }
        }
        combosBox.getChildren().addAll(res1Combo, res2Combo);

        Button confirmBtn = new Button("Pegar Recursos");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71; -fx-pref-width: 150px;");
        confirmBtn.setOnAction(e -> {
            ResourceType r1 = res1Combo.getValue();
            ResourceType r2 = res2Combo.getValue();
            if (r1 != null && r2 != null) {
                if (online) {
                    try {
                        Map<String, String> payload = new HashMap<>();
                        payload.put("res1", r1.name());
                        payload.put("res2", r2.name());
                        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(payload);
                        gameClient.sendIntent("PLAY_YEAR_OF_PLENTY", json);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    ITurnState state = gameManager.getCurrentTurn().getState();
                    if (state instanceof YearOfPlentyState) {
                        ((YearOfPlentyState) state).chooseResources(r1, r2, gameManager.getCurrentTurn());
                        updateSidebar();
                    }
                }
            } else {
                gameManager.getLogger().log("Selecione os dois recursos primeiro!");
            }
        });
        rightSidebar.getChildren().addAll(titleLabel, infoLabel, combosBox, confirmBtn);
    }

    private void buildTradeOptionsSidebar() {
        rightSidebar.getChildren().clear();
        Label titleLabel = new Label("Troca!");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Button btnBank = new Button("Trocar com o Banco");
        btnBank.setStyle("-fx-font-weight: bold; -fx-base: #3498db; -fx-pref-width: 200px;");
        btnBank.setOnAction(e -> buildBankTradeSidebar());

        Button btnPlayers = new Button("Trocar com Jogadores");
        btnPlayers.setStyle("-fx-font-weight: bold; -fx-base: #9b59b6; -fx-pref-width: 200px;");
        btnPlayers.setOnAction(e -> buildPlayerTradeCreationSidebar());

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-font-weight: bold; -fx-base: #e74c3c; -fx-pref-width: 200px;");
        btnCancel.setOnAction(e -> updateSidebar());

        rightSidebar.getChildren().addAll(titleLabel, btnBank, btnPlayers, btnCancel);
        buildPlayerOverviewSidebar();
    }

    private void buildPlayerTradeCreationSidebar() {
        rightSidebar.getChildren().clear();
        Player proposer = gameManager.getCurrentTurn().getCurrentPlayer();

        Label titleLabel = new Label("Criar Proposta");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox offerBox = new VBox(5);
        Label offerLabel = new Label("Ofereço:");
        offerLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        offerBox.getChildren().add(offerLabel);

        VBox requestBox = new VBox(5);
        Label requestLabel = new Label("Quero:");
        requestLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        requestBox.getChildren().add(requestLabel);

        Map<ResourceType, Spinner<Integer>> offerSpinners = new HashMap<>();
        Map<ResourceType, Spinner<Integer>> requestSpinners = new HashMap<>();

        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            int playerHas = proposer.getWallet().getResourceAmount(type);
            Spinner<Integer> offerSpinner = new Spinner<>(0, playerHas, 0);
            offerSpinner.setPrefWidth(70);
            offerSpinners.put(type, offerSpinner);

            HBox offerRow = new HBox(10, new Label(type.name()), offerSpinner);
            offerRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            offerRow.getChildren().get(0).setStyle("-fx-text-fill: white;");
            offerBox.getChildren().add(offerRow);

            Spinner<Integer> requestSpinner = new Spinner<>(0, 99, 0);
            requestSpinner.setPrefWidth(70);
            requestSpinners.put(type, requestSpinner);

            HBox requestRow = new HBox(10, new Label(type.name()), requestSpinner);
            requestRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            requestRow.getChildren().get(0).setStyle("-fx-text-fill: white;");
            requestBox.getChildren().add(requestRow);
        }

        Button confirmBtn = new Button("Fazer Proposta");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #9b59b6;");
        confirmBtn.setOnAction(e -> {
            Map<ResourceType, Integer> offered = new HashMap<>();
            Map<ResourceType, Integer> requested = new HashMap<>();
            for (ResourceType type : ResourceType.values()) {
                if (type == ResourceType.DESERT) continue;
                offered.put(type, offerSpinners.get(type).getValue());
                requested.put(type, requestSpinners.get(type).getValue());
            }
            if (online) {
                // Monta o payload e envia ao servidor
                try {
                    Map<String, Integer> giveMap = new HashMap<>();
                    Map<String, Integer> wantMap = new HashMap<>();
                    for (ResourceType rt : ResourceType.values()) {
                        if (rt == ResourceType.DESERT) continue;
                        int g = offered.getOrDefault(rt, 0);
                        int w = requested.getOrDefault(rt, 0);
                        if (g > 0) giveMap.put(rt.name(), g);
                        if (w > 0) wantMap.put(rt.name(), w);
                    }
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("give", giveMap);
                    payload.put("want", wantMap);
                    String json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(payload);
                    gameClient.sendIntent("PROPOSE_TRADE", json);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                // Fluxo local original — mantém como estava
                try {
                    TradeOffer offer = new TradeOffer(proposer, offered, requested);
                    Turn currentTurn = gameManager.getCurrentTurn();
                    currentTurn.setState(new PlayerTradeState(offer, gameManager.getPlayers()));
                    updateSidebar();
                } catch (IllegalArgumentException ex) {
                    gameManager.getLogger().log(ex.getMessage());
                }
            }
        });

        Button cancelBtn = new Button("Voltar");
        cancelBtn.setStyle("-fx-font-weight: bold; -fx-base: #95a5a6;");
        cancelBtn.setOnAction(e -> buildTradeOptionsSidebar());

        HBox buttonsBox = new HBox(10, confirmBtn, cancelBtn);
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        rightSidebar.getChildren().addAll(titleLabel, offerBox, requestBox, buttonsBox);
        buildPlayerOverviewSidebar();
    }

    private void buildBankTradeSidebar() {
        rightSidebar.getChildren().clear();

        Player player = gameManager.getCurrentTurn().getCurrentPlayer();
        if (online && myPlayer != null) player = myPlayer;
        final Player tradingPlayer = player;

        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 14; -fx-background-color: #1a2538;");

        Label title = new Label("Troca com o Banco");
        title.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px;" +
                       "-fx-font-weight: bold;");
        box.getChildren().add(title);

        Label hint = new Label("Escolha o que dar e o que receber.");
        hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        box.getChildren().add(hint);

        // Estado mutável das seleções
        Map<ResourceType, Integer> giveSelection = new HashMap<>();
        Map<ResourceType, Integer> receiveSelection = new HashMap<>();
        for (ResourceType rt : ResourceType.values()) {
            if (rt == ResourceType.DESERT) continue;
            giveSelection.put(rt, 0);
            receiveSelection.put(rt, 0);
        }

        // Botão de confirmar
        Button confirmBtn = new Button("Confirmar troca");
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;" +
                             "-fx-wrap-text: true;");

        // Função que valida a troca conforme regras do banco
        Runnable validateAndUpdate = () -> {
            int totalGive = giveSelection.values()
                .stream().mapToInt(Integer::intValue).sum();
            int totalReceive = receiveSelection.values()
                .stream().mapToInt(Integer::intValue).sum();

            if (totalGive == 0 && totalReceive == 0) {
                statusLabel.setText("");
                confirmBtn.setDisable(true);
                return;
            }

            // Calcula quantos recursos o jogador "pode ganhar" com o que dá,
            // considerando as tradeRates dele (4:1, 3:1 ou 2:1 com portos)
            int allowedReceive = 0;
            for (Map.Entry<ResourceType, Integer> e : giveSelection.entrySet()) {
                if (e.getValue() == 0) continue;
                int rate = tradingPlayer.getTradeRates()
                    .getOrDefault(e.getKey(), 4);
                if (e.getValue() % rate != 0) {
                    statusLabel.setText("Quantidade de " + e.getKey()
                        + " precisa ser múltiplo de " + rate);
                    confirmBtn.setDisable(true);
                    return;
                }
                allowedReceive += e.getValue() / rate;
            }

            if (allowedReceive != totalReceive) {
                statusLabel.setText("Troca inválida: você daria "
                    + totalGive + " e receberia " + totalReceive
                    + " (válido: " + allowedReceive + ")");
                confirmBtn.setDisable(true);
                return;
            }

            // Verifica se o jogador tem os recursos
            for (Map.Entry<ResourceType, Integer> e : giveSelection.entrySet()) {
                if (tradingPlayer.getWallet().getResourceAmount(e.getKey())
                        < e.getValue()) {
                    statusLabel.setText(
                        "Você não tem recursos suficientes de " + e.getKey());
                    confirmBtn.setDisable(true);
                    return;
                }
            }

            statusLabel.setText("✅ Troca válida");
            confirmBtn.setDisable(false);
        };

        // Constrói as linhas de seleção (dar / receber)
        VBox giveBox = new VBox(4);
        Label giveTitle = new Label("Você dá:");
        giveTitle.setStyle("-fx-text-fill: #ecf0f1; -fx-font-weight: bold;");
        giveBox.getChildren().add(giveTitle);

        for (ResourceType rt : ResourceType.values()) {
            if (rt == ResourceType.DESERT) continue;
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label rtLabel = new Label(rt.name() +
                " (tenho " +
                tradingPlayer.getWallet().getResourceAmount(rt) + ")");
            rtLabel.setStyle("-fx-text-fill: white; -fx-min-width: 130px;");

            Label countLabel = new Label("0");
            countLabel.setStyle("-fx-text-fill: #f1c40f; -fx-min-width: 30px;");

            Button minusBtn = new Button("-");
            Button plusBtn = new Button("+");

            minusBtn.setOnAction(ev -> {
                int cur = giveSelection.get(rt);
                if (cur > 0) {
                    giveSelection.put(rt, cur - 1);
                    countLabel.setText(String.valueOf(cur - 1));
                    validateAndUpdate.run();
                }
            });
            plusBtn.setOnAction(ev -> {
                int cur = giveSelection.get(rt);
                if (tradingPlayer.getWallet().getResourceAmount(rt) > cur) {
                    giveSelection.put(rt, cur + 1);
                    countLabel.setText(String.valueOf(cur + 1));
                    validateAndUpdate.run();
                }
            });

            row.getChildren().addAll(rtLabel, minusBtn, countLabel, plusBtn);
            giveBox.getChildren().add(row);
        }

        VBox receiveBox = new VBox(4);
        Label receiveTitle = new Label("Você recebe:");
        receiveTitle.setStyle("-fx-text-fill: #ecf0f1; -fx-font-weight: bold;");
        receiveBox.getChildren().add(receiveTitle);

        for (ResourceType rt : ResourceType.values()) {
            if (rt == ResourceType.DESERT) continue;
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label rtLabel = new Label(rt.name());
            rtLabel.setStyle("-fx-text-fill: white; -fx-min-width: 130px;");

            Label countLabel = new Label("0");
            countLabel.setStyle("-fx-text-fill: #2ecc71; -fx-min-width: 30px;");

            Button minusBtn = new Button("-");
            Button plusBtn = new Button("+");

            minusBtn.setOnAction(ev -> {
                int cur = receiveSelection.get(rt);
                if (cur > 0) {
                    receiveSelection.put(rt, cur - 1);
                    countLabel.setText(String.valueOf(cur - 1));
                    validateAndUpdate.run();
                }
            });
            plusBtn.setOnAction(ev -> {
                int cur = receiveSelection.get(rt);
                receiveSelection.put(rt, cur + 1);
                countLabel.setText(String.valueOf(cur + 1));
                validateAndUpdate.run();
            });

            row.getChildren().addAll(rtLabel, minusBtn, countLabel, plusBtn);
            receiveBox.getChildren().add(row);
        }

        // Estilo do botão confirmar
        confirmBtn.setStyle("-fx-background-color: #27ae60;" +
            "-fx-text-fill: white; -fx-font-weight: bold;" +
            "-fx-padding: 8 14; -fx-cursor: hand;");
        confirmBtn.setDisable(true);

        confirmBtn.setOnAction(e -> {
            if (online) {
                try {
                    Map<String, Integer> giveMap = new HashMap<>();
                    Map<String, Integer> wantMap = new HashMap<>();
                    for (Map.Entry<ResourceType, Integer> en
                            : giveSelection.entrySet()) {
                        if (en.getValue() > 0)
                            giveMap.put(en.getKey().name(), en.getValue());
                    }
                    for (Map.Entry<ResourceType, Integer> en
                            : receiveSelection.entrySet()) {
                        if (en.getValue() > 0)
                            wantMap.put(en.getKey().name(), en.getValue());
                    }
                    com.fasterxml.jackson.databind.ObjectMapper om =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("give", giveMap);
                    payload.put("receive", wantMap);
                    gameClient.sendIntent("BANK_TRADE", om.writeValueAsString(payload));
                } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                // Modo offline: aplica diretamente
                for (Map.Entry<ResourceType, Integer> en
                        : giveSelection.entrySet()) {
                    if (en.getValue() > 0)
                        tradingPlayer.getWallet()
                            .removeResource(en.getKey(), en.getValue());
                }
                for (Map.Entry<ResourceType, Integer> en
                        : receiveSelection.entrySet()) {
                    if (en.getValue() > 0)
                        tradingPlayer.getWallet()
                            .addResource(en.getKey(), en.getValue());
                }
                gameManager.getLogger().log(
                    tradingPlayer.getName() + " trocou com o banco.");
            }
            buildTradeOptionsSidebar();
        });

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setStyle("-fx-background-color: #c0392b;" +
            "-fx-text-fill: white; -fx-padding: 8 14;");
        cancelBtn.setOnAction(e -> buildTradeOptionsSidebar());

        HBox btnRow = new HBox(8, confirmBtn, cancelBtn);
        box.getChildren().addAll(giveBox, receiveBox, statusLabel, btnRow);
        rightSidebar.getChildren().add(box);
    }

    private void render(GraphicsContext gc, Board board, boolean force) {
        long now = System.nanoTime();
        if (!force && (now - lastRenderTime < MIN_FRAME_TIME_NS)) {
            return;
        }
        lastRenderTime = now;
        framesThisSecond++;

        double w = gc.getCanvas().getWidth();
        double h = gc.getCanvas().getHeight();

        gc.clearRect(0, 0, w, h);
        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(zoomLevel, zoomLevel);

        drawBoard(gc, board);
        gc.restore();
    }

    private void loadAssets() {
        try {
            for (ResourceType type : ResourceType.values()) {
                String name = type.name().toLowerCase() + "tile.png";
                tileImages.put(type, new Image(getClass().getResourceAsStream("/assets/tile/" + name)));
            }
            for (int i = 2; i <= 12; i++) {
                if (i == 7) continue;
                numberImages.put(i, new Image(getClass().getResourceAsStream("/assets/numbers/" + i + ".png")));
            }
            for (String cor : coresAssets) {
                settlementImages.put(cor, new Image(getClass().getResourceAsStream("/assets/settlement/" + cor + "set.png")));
                cityImages.put(cor, new Image(getClass().getResourceAsStream("/assets/city/" + cor + "city.png")));
                roadImages.put(cor, new Image(getClass().getResourceAsStream("/assets/road/" + cor + "road.png")));
            }
            String[] portFiles = {"3to1port.png", "brickport.png", "woolport.png", "woodport.png", "oreport.png", "wheatport.png"};
            for (String pf : portFiles) {
                portImages.put(pf, new Image(getClass().getResourceAsStream("/assets/port/" + pf)));
            }
            dockImages.put("0", new Image(getClass().getResourceAsStream("/assets/dock/dock0.png")));
            dockImages.put("30", new Image(getClass().getResourceAsStream("/assets/dock/dock30.png")));
            dockImages.put("-30", new Image(getClass().getResourceAsStream("/assets/dock/dockneg30.png")));

            robberImage = new Image(getClass().getResourceAsStream("/assets/robber/robber.png"));
            devCardDeckImage = new Image(getClass().getResourceAsStream("/assets/developmentcards/development.png"));
            devCardImages.put("Knight", new Image(getClass().getResourceAsStream("/assets/developmentcards/knight.png")));
            devCardImages.put("Monopoly", new Image(getClass().getResourceAsStream("/assets/developmentcards/monopoly.png")));
            devCardImages.put("Road Building", new Image(getClass().getResourceAsStream("/assets/developmentcards/roadbuilding.png")));
            devCardImages.put("Victory Point", new Image(getClass().getResourceAsStream("/assets/developmentcards/victorypoint.png")));
            devCardImages.put("Year of Plenty", new Image(getClass().getResourceAsStream("/assets/developmentcards/yearofplenty.png")));
        } catch (Exception e) {
            System.err.println("Erro ao carregar assets do jogo.");
        }
    }

    private String getAssetColorName(Player p) {
        return p.getColor().toLowerCase();
    }

    private Vertex findVertexAt(Board board, double worldX, double worldY) {
        double vertexTolerance = 150.0;
        for (Vertex v : board.getVertices()) {
            if (Math.hypot(v.getX() - worldX, v.getY() - worldY) <= vertexTolerance) return v;
        }
        return null;
    }

    private Edge findEdgeAt(Board board, double worldX, double worldY) {
        double edgeTolerance = 150.0;
        for (Edge e : board.getEdges()) {
            if (pointToLineDistance(worldX, worldY, e.getV1().getX(), e.getV1().getY(), e.getV2().getX(), e.getV2().getY()) <= edgeTolerance) return e;
        }
        return null;
    }

    private Tile findTileAt(Board board, double worldX, double worldY) {
        double tileTolerance = 100.0;
        for (Tile tile : board.getTiles()) {
            double cx = computeCenterX(tile.getVertices());
            double cy = computeCenterY(tile.getVertices());
            if (Math.hypot(cx - worldX, cy - worldY) <= tileTolerance) return tile;
        }
        return null;
    }

    private void drawBoard(GraphicsContext gc, Board board) {
        drawTiles(gc, board);
        drawPorts(gc, board);
        drawEdges(gc, board);
        drawVertices(gc, board);
        drawRobber(gc, board);
    }

    private void drawTiles(GraphicsContext gc, Board board) {
        for (Tile tile : board.getTiles()) {
            Vertex[] vertices = tile.getVertices();
            double centerX = computeCenterX(vertices);
            double centerY = computeCenterY(vertices);

            Image tileImg = tileImages.get(tile.getResource());
            if (tileImg != null) {
                gc.drawImage(tileImg, centerX - (tileImg.getWidth() / 2), centerY - (tileImg.getHeight() / 2));
            }

            // Destaque dourado nos tiles que produzem com o número sorteado.
            boolean isActive = lastDiceTotal > 1
                    && tile.getResource() != ResourceType.DESERT
                    && tile.getNumberToken() == lastDiceTotal;
            if (isActive) {
                double[] xs = new double[6];
                double[] ys = new double[6];
                for (int i = 0; i < 6; i++) { xs[i] = vertices[i].getX(); ys[i] = vertices[i].getY(); }
                // Overlay amarelo-dourado semitransparente sobre o tile.
                gc.setGlobalAlpha(0.30);
                gc.setFill(Color.GOLD);
                gc.fillPolygon(xs, ys, 6);
                gc.setGlobalAlpha(1.0);
                // Contorno dourado brilhante.
                gc.setStroke(Color.GOLD);
                gc.setLineWidth(14);
                gc.strokePolygon(xs, ys, 6);
                gc.setLineWidth(1);
            }

            if (tile.getResource() != ResourceType.DESERT) {
                Image numImg = numberImages.get(tile.getNumberToken());
                if (numImg != null) {
                    gc.drawImage(numImg, centerX - (numImg.getWidth() / 2), centerY - (numImg.getHeight() / 2) + 80);
                }
            }
        }
    }

    private void drawEdges(GraphicsContext gc, Board board) {
        for (Edge edge : board.getEdges()) {
            if (!edge.isEmpty()) {
                Player owner = edge.getBuilding().getOwner();
                Image roadImg = roadImages.get(getAssetColorName(owner));
                PieceDrawer.drawRoad(gc, edge.getV1().getX(), edge.getV1().getY(), edge.getV2().getX(), edge.getV2().getY(), roadImg);
            }
        }
    }

    private void drawVertices(GraphicsContext gc, Board board) {
        for (Vertex vertex : board.getVertices()) {
            if (!vertex.isEmpty()) {
                Player owner = vertex.getBuilding().getOwner();
                String colorName = getAssetColorName(owner);
                if (vertex.getBuilding() instanceof Settlement) {
                    PieceDrawer.drawSettlement(gc, vertex.getX(), vertex.getY(), settlementImages.get(colorName));
                } else {
                    PieceDrawer.drawCity(gc, vertex.getX(), vertex.getY(), cityImages.get(colorName));
                }
            }
        }
    }

    private void drawPorts(GraphicsContext gc, Board board) {
        List<Edge> processedEdges = new ArrayList<>();
        for (Edge edge : board.getEdges()) {
            Vertex v1 = edge.getV1();
            Vertex v2 = edge.getV2();

            if (v1.hasPort() && v2.hasPort() && v1.getPort() == v2.getPort()) {
                if (processedEdges.contains(edge)) continue;
                processedEdges.add(edge);
                Port port = v1.getPort();

                Tile landTile = null;
                if (v1.getAdjacentTiles() != null && v2.getAdjacentTiles() != null) {
                    for (Tile t : v1.getAdjacentTiles()) {
                        if (v2.getAdjacentTiles().contains(t)) { landTile = t; break; }
                    }
                }
                if (landTile == null) continue;

                double mx = (v1.getX() + v2.getX()) / 2.0;
                double my = (v1.getY() + v2.getY()) / 2.0;
                double cx = computeCenterX(landTile.getVertices());
                double cy = computeCenterY(landTile.getVertices());

                double dx = mx - cx;
                double dy = my - cy;
                double length = Math.hypot(dx, dy);
                if (length > 0) { dx /= length; dy /= length; }

                double edgeDx = v2.getX() - v1.getX();
                double edgeDy = v2.getY() - v1.getY();
                double edgeAngle = Math.toDegrees(Math.atan2(edgeDy, edgeDx));

                while (edgeAngle < 0) edgeAngle += 180;
                while (edgeAngle >= 180) edgeAngle -= 180;

                String dockKey = "0";
                if (Math.abs(edgeAngle - 90) < 15) dockKey = "0";
                else if (Math.abs(edgeAngle - 30) < 15 || Math.abs(edgeAngle - 210) < 15) dockKey = "-30";
                else if (Math.abs(edgeAngle - 150) < 15 || Math.abs(edgeAngle - 330) < 15) dockKey = "30";

                double dockOffset = 50.0;
                double dockX = mx + dx * dockOffset;
                double dockY = my + dy * dockOffset;

                Image dImg = dockImages.get(dockKey);
                if (dImg != null) gc.drawImage(dImg, dockX - dImg.getWidth() / 2.0, dockY - dImg.getHeight() / 2.0);

                double portDist = 260.0;
                double px = mx + dx * portDist;
                double py = my + dy * portDist;

                String portName = (port.getResource() == null) ? "3to1port.png" : port.getResource().name().toLowerCase() + "port.png";
                Image pImg = portImages.get(portName);
                if (pImg != null) gc.drawImage(pImg, px - pImg.getWidth() / 2.0, py - pImg.getHeight() / 2.0);
            }
        }
    }

    private void drawRobber(GraphicsContext gc, Board board) {
        com.example.model.board.Robber robber = gameManager.getRobber();
        if (robber != null && robber.getCurrentTile() != null && robberImage != null) {
            Tile tile = robber.getCurrentTile();
            double centerX = computeCenterX(tile.getVertices());
            double centerY = computeCenterY(tile.getVertices());
            gc.drawImage(robberImage, centerX - (robberImage.getWidth() / 2) - 200, centerY - (robberImage.getHeight() / 2));
        }
    }

    private void buildPlayerTradeResponseSidebar(PlayerTradeState tradeState) {
        rightSidebar.getChildren().clear();
        Turn currentTurn = gameManager.getCurrentTurn();

        Label titleLabel = new Label("Negociação");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        if (tradeState.isWaitingForProposer()) {
            Label infoLabel = new Label(tradeState.getOffer().getProposer().getName() + ", escolha com quem trocar:");
            infoLabel.setStyle("-fx-text-fill: yellow; -fx-wrap-text: true;");
            rightSidebar.getChildren().addAll(titleLabel, infoLabel);

            for (Player acceptedPlayer : tradeState.getOffer().getAcceptedBy()) {
                Button acceptPartnerBtn = new Button("Fechar com " + acceptedPlayer.getName());
                acceptPartnerBtn.setStyle("-fx-base: #2ecc71; -fx-font-weight: bold;");
                acceptPartnerBtn.setOnAction(e -> {
                    tradeState.executeTrade(acceptedPlayer, currentTurn);
                    updateSidebar();
                });
                rightSidebar.getChildren().add(acceptPartnerBtn);
            }

            Button cancelBtn = new Button("Cancelar Troca");
            cancelBtn.setStyle("-fx-base: #e74c3c; -fx-font-weight: bold;");
            cancelBtn.setOnAction(e -> {
                tradeState.cancelTrade(currentTurn);
                updateSidebar();
            });
            rightSidebar.getChildren().add(cancelBtn);
            return;
        }

        Player target = tradeState.getCurrentTargetPlayer();
        if (target != null) {
            Label targetLabel = new Label("Vez de " + target.getName() + " responder!");
            targetLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px; -fx-font-weight: bold;");

            Label offerLabel = new Label(tradeState.getOffer().getProposer().getName() + " ofereceu uma troca.");
            offerLabel.setStyle("-fx-text-fill: white;");

            Button btnAccept = new Button("Aceitar");
            btnAccept.setStyle("-fx-base: #2ecc71; -fx-font-weight: bold;");

            if (!tradeState.getOffer().canPlayerAfford(target)) {
                btnAccept.setDisable(true);
                btnAccept.setText("Sem Recursos");
            }

            btnAccept.setOnAction(e -> {
                tradeState.registerResponse(true, currentTurn);
                updateSidebar();
            });

            Button btnReject = new Button("Recusar");
            btnReject.setStyle("-fx-base: #e74c3c; -fx-font-weight: bold;");
            btnReject.setOnAction(e -> {
                tradeState.registerResponse(false, currentTurn);
                updateSidebar();
            });

            HBox buttons = new HBox(10, btnAccept, btnReject);
            buttons.setAlignment(javafx.geometry.Pos.CENTER);
            rightSidebar.getChildren().addAll(titleLabel, targetLabel, offerLabel, buttons);
        }
    }

    private void buildPlayerOverviewSidebar() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        rightSidebar.getChildren().add(spacer);

        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0 5 0;");
        rightSidebar.getChildren().add(separator);

        if (bankSidebarBox != null) rightSidebar.getChildren().add(bankSidebarBox);

        for (Player p : gameManager.getPlayers()) {
            HBox playerBox = new HBox(15);
            playerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            playerBox.setStyle("-fx-background-color: #2c3e50; -fx-padding: 8; -fx-background-radius: 8;");

            VBox settlementBox = new VBox(2);
            settlementBox.setAlignment(javafx.geometry.Pos.CENTER);

            Label vpLabel = new Label(p.getVictoryPoints() + " \u2605");
            vpLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold; -fx-font-size: 20px;");

            ImageView iconView = new ImageView();
            iconView.setFitHeight(25);
            iconView.setPreserveRatio(true);
            String colorName = p.getColor().toLowerCase();
            try {
                Image iconImg = new Image(getClass().getResource("/assets/settlement/" + colorName + "set.png").toExternalForm());
                iconView.setImage(iconImg);
            } catch (Exception e) {
                System.out.println("Erro ao carregar mini-ícone.");
            }

            settlementBox.getChildren().addAll(vpLabel, iconView);

            VBox infoBox = new VBox(5);
            infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label nameLabel = new Label(p.getName());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");

            int numResources = p.getWallet().getTotalCards();
            int numDevCards = 0;
            if (p.getPlayableCards() != null) numDevCards += p.getPlayableCards().size();
            if (p.getNewCards() != null) numDevCards += p.getNewCards().size();
            int numKnights = p.getPlayableCards() != null ? (int) p.getPlayableCards().stream().filter(c -> c.getName().equals("Knight")).count() : 0; // Fallback seguro baseado no modelo
            int longestRoad = 0; // Customizar caso seu modelo forneça getLongestRoad() exposto publicamente

            HBox statsBox = new HBox(12);
            statsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            HBox resBox = new HBox(5);
            resBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView resIcon = new ImageView();
            resIcon.setFitHeight(25);
            resIcon.setPreserveRatio(true);
            try { resIcon.setImage(new Image(getClass().getResourceAsStream("/assets/resources/resourceback.png"))); } catch (Exception e) {}
            Label resLabel = new Label(String.valueOf(numResources));
            resLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            resBox.getChildren().addAll(resIcon, resLabel);

            HBox devBox = new HBox(5);
            devBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView devIcon = new ImageView();
            devIcon.setFitHeight(25);
            devIcon.setPreserveRatio(true);
            try { devIcon.setImage(new Image(getClass().getResourceAsStream("/assets/developmentcards/development.png"))); } catch (Exception e) {}
            Label devLabel = new Label(String.valueOf(numDevCards));
            devLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            devBox.getChildren().addAll(devIcon, devLabel);

            HBox knightBox = new HBox(5);
            knightBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView knightIcon = new ImageView();
            knightIcon.setFitHeight(25);
            knightIcon.setPreserveRatio(true);
            try { knightIcon.setImage(new Image(getClass().getResourceAsStream("/assets/bonus/largestarmy.png"))); } catch (Exception e) {}
            Label knightLabel = new Label(String.valueOf(numKnights));
            knightLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            knightBox.getChildren().addAll(knightIcon, knightLabel);

            HBox roadBox = new HBox(5);
            roadBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView roadIcon = new ImageView();
            roadIcon.setFitHeight(25);
            roadIcon.setPreserveRatio(true);
            try { roadIcon.setImage(new Image(getClass().getResourceAsStream("/assets/bonus/longestroad.png"))); } catch (Exception e) {}
            Label roadLabel = new Label(String.valueOf(longestRoad));
            roadLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            roadBox.getChildren().addAll(roadIcon, roadLabel);

            statsBox.getChildren().addAll(resBox, devBox, knightBox, roadBox);
            infoBox.getChildren().addAll(nameLabel, statsBox);
            playerBox.getChildren().addAll(settlementBox, infoBox);
            rightSidebar.getChildren().add(playerBox);
        }
    }

    private void applyTradeUpdate(com.example.network.protocol.TradeStatusDTO trade) {
        if (trade == null) return;

        String myName = gameClient != null ? gameClient.getPlayerName() : null;
        boolean iAmProposer = trade.getProposerName().equals(myName);
        boolean tradeActive = trade.isActive();

        // Limpa a sidebar atual e reconstrói conforme o papel deste cliente
        rightSidebar.getChildren().clear();

        if (!tradeActive) {
            // Troca encerrada — mostra resultado no log e volta ao estado normal
            String msg2 = trade.getResolvedWithPlayer() != null
                ? "✅ Troca realizada entre " + trade.getProposerName()
                    + " e " + trade.getResolvedWithPlayer()
                : "❌ Troca cancelada por " + trade.getProposerName();
            logArea.appendText(msg2 + "\n");
            updateSidebar();
            return;
        }

        if (iAmProposer) {
            // Mostra para o proponente quem aceitou, recusou ou está pendente
            buildTradeProposerWaitingSidebar(trade);
        } else if (trade.getResponses().containsKey(myName)) {
            // Mostra para os outros o painel de aceitar/recusar
            buildTradeResponderSidebar(trade, myName);
        }
    }

    private void buildTradeProposerWaitingSidebar(
            com.example.network.protocol.TradeStatusDTO trade) {

        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 14; -fx-background-color: #1a2538;");

        Label title = new Label("Aguardando respostas...");
        title.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        // Resumo da oferta
        Label offerLabel = new Label("Você oferece: " + trade.getGive()
            + "\nVocê quer: " + trade.getWant());
        offerLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px; -fx-wrap-text: true;");
        box.getChildren().add(offerLabel);

        // Status de cada jogador + botão para confirmar quem aceitou
        for (Map.Entry<String, String> entry : trade.getResponses().entrySet()) {
            String name = entry.getKey();
            String status = entry.getValue();

            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 6 10; -fx-background-color: #1e2d3e;" +
                         "-fx-background-radius: 6;");

            String icon = switch (status) {
                case "ACCEPTED" -> "✅";
                case "DECLINED" -> "❌";
                default         -> "⏳";
            };
            Label statusLabel = new Label(icon + "  " + name);
            statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
            row.getChildren().add(statusLabel);

            if ("ACCEPTED".equals(status)) {
                javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
                HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
                Button confirmBtn = new Button("Fechar");
                confirmBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;" +
                                    "-fx-font-weight: bold; -fx-cursor: hand;");
                confirmBtn.setOnAction(e ->
                    gameClient.sendIntent("CONFIRM_TRADE", name));
                row.getChildren().addAll(sp, confirmBtn);
            }
            box.getChildren().add(row);
        }

        // Botão cancelar
        Button cancelBtn = new Button("Cancelar proposta");
        cancelBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;" +
                           "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 14;");
        cancelBtn.setOnAction(e -> gameClient.sendIntent("CANCEL_TRADE", null));
        box.getChildren().add(cancelBtn);

        rightSidebar.getChildren().add(box);
    }

    private void buildTradeResponderSidebar(
            com.example.network.protocol.TradeStatusDTO trade, String myName) {

        String myStatus = trade.getResponses().getOrDefault(myName, "PENDING");

        VBox box = new VBox(10);
        box.setStyle("-fx-padding: 14; -fx-background-color: #1a2538;");

        Label title = new Label(trade.getProposerName() + " propôs uma troca:");
        title.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label offerLabel = new Label("Ele oferece: " + trade.getGive()
            + "\nEle quer: " + trade.getWant());
        offerLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;" +
                            "-fx-wrap-text: true;");

        box.getChildren().addAll(title, offerLabel);

        if ("PENDING".equals(myStatus)) {
            HBox btns = new HBox(10);
            btns.setAlignment(javafx.geometry.Pos.CENTER);

            Button acceptBtn = new Button("✅ Aceitar");
            acceptBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;" +
                               "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");
            acceptBtn.setOnAction(e -> gameClient.sendIntent("TRADE_RESPONSE", "true"));

            Button declineBtn = new Button("❌ Recusar");
            declineBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;" +
                                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16;");
            declineBtn.setOnAction(e -> gameClient.sendIntent("TRADE_RESPONSE", "false"));

            btns.getChildren().addAll(acceptBtn, declineBtn);
            box.getChildren().add(btns);
        } else {
            Label replied = new Label("ACCEPTED".equals(myStatus)
                ? "✅ Você aceitou — aguardando o proponente confirmar"
                : "❌ Você recusou");
            replied.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            box.getChildren().add(replied);
        }

        rightSidebar.getChildren().add(box);
    }

    private void buildDiscardSidebar(Player player, WaitingDiscardState state) {
        int requiredAmount = player.getWallet().getTotalCards() / 2;
        Map<ResourceType, Integer> selection = new HashMap<>();

        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) selection.put(type, 0);
        }

        Label titleLabel = new Label("Descarte de " + player.getName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label countLabel = new Label("Selecione " + requiredAmount + " cartas\n(0 selecionadas)");
        countLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-text-alignment: center;");

        VBox resourcesBox = new VBox(10);
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            int maxOwned = player.getWallet().getResourceAmount(type);
            if (maxOwned == 0) continue;

            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER);

            Label resLabel = new Label(type.name() + " (" + maxOwned + "): ");
            resLabel.setStyle("-fx-text-fill: white; -fx-pref-width: 120px;");

            Label amtLabel = new Label("0");
            amtLabel.setStyle("-fx-text-fill: yellow; -fx-font-weight: bold; -fx-pref-width: 20px;");

            Button btnMinus = new Button("-");
            Button btnPlus = new Button("+");

            Runnable updateCount = () -> {
                int totalSelected = selection.values().stream().mapToInt(Integer::intValue).sum();
                countLabel.setText("Selecione " + requiredAmount + " cartas\n(" + totalSelected + " selecionadas)");
            };

            btnMinus.setOnAction(e -> {
                int current = selection.get(type);
                if (current > 0) {
                    selection.put(type, current - 1);
                    amtLabel.setText(String.valueOf(current - 1));
                    updateCount.run();
                }
            });

            btnPlus.setOnAction(e -> {
                int current = selection.get(type);
                int totalSelected = selection.values().stream().mapToInt(Integer::intValue).sum();
                if (current < maxOwned && totalSelected < requiredAmount) {
                    selection.put(type, current + 1);
                    amtLabel.setText(String.valueOf(current + 1));
                    updateCount.run();
                }
            });

            row.getChildren().addAll(resLabel, btnMinus, amtLabel, btnPlus);
            resourcesBox.getChildren().add(row);
        }

        Button confirmBtn = new Button("Confirmar Descarte");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");
        confirmBtn.setOnAction(e -> {
            int totalSelected = selection.values().stream().mapToInt(Integer::intValue).sum();
            if (totalSelected == requiredAmount) {
                if (online) {
                    // Monta o JSON dos recursos a descartar e envia ao servidor
                    try {
                        Map<String, Integer> rawMap = new HashMap<>();
                        for (Map.Entry<ResourceType, Integer> ent : selection.entrySet()) {
                            rawMap.put(ent.getKey().name(), ent.getValue());
                        }
                        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(rawMap);
                        gameClient.sendIntent("SUBMIT_DISCARD", json);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    state.submitDiscard(player, selection, gameManager.getCurrentTurn());
                }
                updateSidebar();
            }
        });

        rightSidebar.getChildren().addAll(titleLabel, countLabel, resourcesBox, confirmBtn);
    }

    private void buildStealVictimSidebar(List<Player> victims, MoveRobberState state, Runnable updateActionUI) {
        rightSidebar.getChildren().clear();
        Label titleLabel = new Label("Escolha sua vítima:");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        rightSidebar.getChildren().add(titleLabel);

        for (Player victim : victims) {
            Button btn = new Button("Roubar de " + victim.getName());
            btn.setStyle("-fx-font-weight: bold; -fx-min-width: 200px;");
            btn.setOnAction(e -> {
                if (online) {
                    gameClient.sendIntent("STEAL_FROM", victim.getName());
                } else {
                    state.executeSteal(victim, gameManager.getCurrentTurn());
                    updateActionUI.run();
                }
            });
            rightSidebar.getChildren().add(btn);
        }
    }

    private double computeCenterX(Vertex[] vertices) {
        double sum = 0; for (Vertex v : vertices) sum += v.getX(); return sum / vertices.length;
    }
    private double computeCenterY(Vertex[] vertices) {
        double sum = 0; for (Vertex v : vertices) sum += v.getY(); return sum / vertices.length;
    }

    private double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0 && dy == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    public static void main(String[] args) {
        launch(args);
    }
}