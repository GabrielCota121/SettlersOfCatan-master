package com.catan;

import com.catan.model.board.*;
import com.catan.model.building.BuildingCost;
import com.catan.model.building.Settlement;
import com.catan.model.cards.IDevelopmentCard;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.logging.ConsoleLogger;
import com.catan.model.logging.IGameLogger;
import com.catan.model.logging.WebSocketLogger;
import com.catan.model.player.Player;
import com.catan.model.state.ITurnState;
import com.catan.model.state.MoveRobberState;
import com.catan.model.state.SetupState;
import com.catan.model.state.WaitingDiscardState;
import com.catan.model.view.PieceDrawer;
import com.catan.model.view.PlayerHandView;
import com.catan.model.trade.TradeOffer;
import com.catan.model.state.PlayerTradeState;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.Spinner;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;

    // Controlar FPS
    private long lastRenderTime = 0;
    private final long MIN_FRAME_TIME_NS = 1_000_000_000 / 120;
    private int framesThisSecond = 0;

    private static final Color EDGE_COLOR = Color.rgb(44, 62, 80, 0.55);

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

    public void start(Stage primaryStage) {
        loadAssets();

        Board board = BoardFactory.createStandardBoard();
        List<Player> players = new ArrayList<>();
        players.add(new Player(1, "Lucas", "GREEN"));
        players.add(new Player(2, "Gabriel", "BLUE"));
        players.add(new Player(3, "Juliana", "YELLOW"));
        players.add(new Player(4, "Marcelle", "RED"));


        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-family: 'Consolas'; -fx-font-size: 14px;");
        logArea.setPrefWidth(350);

        // Mudei pra pra usar o logArea!
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

        PlayerHandView handView = new PlayerHandView();

        HBox devCardsBox = new HBox(10);
        devCardsBox.setAlignment(javafx.geometry.Pos.CENTER);

        for (Player p : players) {
            p.getWallet().setOnWalletChangedListener(() -> {
                handView.update(gameManager.getCurrentTurn().getCurrentPlayer());
            });
        }

        PlayerHandView bankHandView = new PlayerHandView();

        VBox bankInfoBox = new VBox(5);
        bankInfoBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label bankNameLabel = new Label("Bank");
        bankNameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        ImageView bankIconView = new ImageView();
        bankIconView.setFitHeight(45);
        bankIconView.setPreserveRatio(true);

        try {
            Image bankIcon = new Image(getClass().getResourceAsStream("/assets/bank/bankicon.png"));
            bankIconView.setImage(bankIcon);
        } catch (Exception e) {
            System.out.println("Cadê o ícone do Bank??");
        }

        bankInfoBox.getChildren().addAll(bankNameLabel, bankIconView);

        gameManager.getBank().getWallet().setOnWalletChangedListener(() -> {
            bankHandView.update(gameManager.getBank().getWallet());
        });

        bankHandView.update(gameManager.getBank().getWallet());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        VBox playerInfoBox = new VBox(5);
        playerInfoBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label playerNameLabel = new Label();
        playerNameLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        ImageView playerIconView = new ImageView();
        playerIconView.setFitHeight(45);
        playerIconView.setPreserveRatio(true);

        playerInfoBox.getChildren().addAll(playerNameLabel, playerIconView);

        Image passTurnImg = null;
        Image cantPassTurnImg = null;
        Image tradeImg = null;
        Image[] diceImgs = new Image[7];

        try {
            passTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/passturn.png"));
            cantPassTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/cantpassturn.png"));
            tradeImg = new Image(getClass().getResourceAsStream("/assets/trade/tradeicon.png"));
            for (int i = 1; i <= 6; i++) {
                String path = "/assets/dice/"+i;
                var stream = getClass().getResourceAsStream(path);
                if (stream == null) {
                    System.out.println("Imagem não encontrada, feelsbad " + path);
                } else {
                    diceImgs[i] = new Image(stream);
                }
            }
        } catch (Exception e) {
            System.out.println("Deu ruim ao carregar botões/dados: " + e.getMessage());
        }

        ImageView passTurnView = new ImageView(cantPassTurnImg);
        passTurnView.setFitHeight(75);
        passTurnView.setPreserveRatio(true);

        ImageView dice1View = new ImageView(diceImgs[1]);
        ImageView dice2View = new ImageView(diceImgs[6]);
        dice1View.setFitHeight(75);
        dice1View.setPreserveRatio(true);
        dice2View.setFitHeight(75);
        dice2View.setPreserveRatio(true);

        HBox diceBox = new HBox(5, dice1View, dice2View);
        diceBox.setAlignment(javafx.geometry.Pos.CENTER);

        ImageView tradeIconView = new ImageView(tradeImg);
        tradeIconView.setFitHeight(50);
        tradeIconView.setPreserveRatio(true);
        tradeIconView.setCursor(javafx.scene.Cursor.HAND);

        ImageView buyDevCardView = new ImageView(devCardDeckImage);
        buyDevCardView.setFitHeight(60);
        buyDevCardView.setPreserveRatio(true);
        buyDevCardView.setCursor(javafx.scene.Cursor.HAND);

        HBox diceAndTradeBox = new HBox(15, buyDevCardView, tradeIconView, diceBox);
        diceAndTradeBox.setAlignment(javafx.geometry.Pos.CENTER);

        VBox turnControlsBox = new VBox(15, diceAndTradeBox, passTurnView);
        turnControlsBox.setAlignment(javafx.geometry.Pos.CENTER);

        Image finalPassTurnImg = passTurnImg;
        Image finalCantPassTurnImg = cantPassTurnImg;


        Runnable[] updateActionUIRef = new Runnable[1];

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
                    cardView.setFitHeight(75);
                    cardView.setPreserveRatio(true);

                    boolean isCorrectState = state instanceof com.catan.model.state.MainState ||
                            state instanceof com.catan.model.state.WaitingRollState;
                    boolean isPlayable = playableCards != null && playableCards.contains(card);

                    boolean canPlay = isCorrectState && isPlayable;

                    cardView.setOpacity(canPlay ? 1.0 : 0.5);
                    cardView.setCursor(canPlay ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

                    String cardName = card.getName().toLowerCase().replace(" ", "");
                    try {
                        Image cardImg = new Image(getClass().getResourceAsStream("/assets/developmentcards/" + cardName + ".png"));
                        cardView.setImage(cardImg);
                    } catch (Exception e) {
                        System.out.println("Imagem da carta não encontrada: /assets/developmentcards/" + cardName + ".png");
                    }

                    cardView.setOnMouseClicked(e -> {
                        if (!canPlay) {
                            if (!isPlayable) {
                                gameManager.getLogger().log("Você não pode jogar uma carta no mesmo turno em que a comprou!");
                            } else {
                                gameManager.getLogger().log("Você não pode jogar cartas de desenvolvimento agora!");
                            }
                            return;
                        }

                        boolean success = state.playDevelopmentCard(card, gameManager.getCurrentTurn());
                        if (success) {
                            if (updateActionUIRef[0] != null) {
                                updateActionUIRef[0].run();
                            }
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

            diceBox.setCursor(state.canRollDice() ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
            diceBox.setOpacity(state.canRollDice() ? 1.0 : 0.3);

            boolean isMainState = state instanceof com.catan.model.state.MainState;
            tradeIconView.setOpacity(isMainState? 1.0 : 0.3);
            tradeIconView.setCursor(isMainState ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            //Vai checar se ele pode comrpar a devcard
            buyDevCardView.setOpacity((isMainState && currentPlayer.canAfford(BuildingCost.DEVELOPMENT_CARD)) ? 1.0 : 0.3);
            buyDevCardView.setCursor(isMainState ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            int d1 = gameManager.getDice1().getResult();
            int d2 = gameManager.getDice2().getResult();

            if (d1 > 0 && d1 <= 6) dice1View.setImage(diceImgs[d1]);
            if (d2 > 0 && d2 <= 6) dice2View.setImage(diceImgs[d2]);

            updateSidebar();
            updateDevCardsUI.run();
        };

        updateActionUIRef[0] = updateActionUI;

        Runnable bindPlayerToUI = () -> {
            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            handView.update(currentPlayer);
            playerNameLabel.setText(currentPlayer.getName());
            String colorName = currentPlayer.getColor().toLowerCase();
            String imagePath = "/assets/settlement/" + colorName + "set.png";

            try {
                Image iconImg = new Image(getClass().getResource(imagePath).toExternalForm());
                playerIconView.setImage(iconImg);
            } catch (Exception e) {
                System.out.println("Cade a imagem do settlement??? " + imagePath);
            }
            updateActionUI.run();
        };

        diceBox.setOnMouseClicked(e -> {
            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            boolean success = gameManager.rollDice(currentPlayer);

            if (!success) {
                gameManager.getLogger().log("Você não pode rolar os dados agora!");
            }
            updateActionUI.run();
        });

        passTurnView.setOnMouseClicked(e -> {
            Turn currentTurn = gameManager.getCurrentTurn();
            boolean success = currentTurn.getState().endTurn(currentTurn);

            if (!success) {
                gameManager.getLogger().log("Você não pode passar o turno agora!");
            }
            updateActionUI.run();
        });

        tradeIconView.setOnMouseClicked(e -> {
            ITurnState currentState = gameManager.getCurrentTurn().getState();
            if (!(currentState instanceof com.catan.model.state.MainState)) {
                gameManager.getLogger().log("Você só pode propor trocas na MainState!");
                return;
            }

            buildTradeOptionsSidebar();
        });

        buyDevCardView.setOnMouseClicked(e -> {
            ITurnState currentState = gameManager.getCurrentTurn().getState();
            if (currentState instanceof com.catan.model.state.MainState) {
                boolean success = currentState.buyDevelopmentCard(gameManager.getCurrentTurn());
                if (!success) {
                    gameManager.getLogger().log("Não tem recursos suficientes pra POP!");
                }
                updateActionUI.run();
            } else {
                gameManager.getLogger().log("Você só pode comprar devcard na sua Main State!");
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

            if (currentState instanceof MoveRobberState) {
                Tile clickedTile = findTileAt(board, worldX, worldY);

                if (clickedTile != null) {
                    MoveRobberState robberState = (MoveRobberState) currentState;
                    List<Player> victims = robberState.moveRobber(clickedTile, gameManager.getCurrentTurn());
                    victims.removeIf(p -> p.getWallet().getTotalCards() == 0);

                    if (victims.isEmpty()) {
                        robberState.executeSteal(null, gameManager.getCurrentTurn());
                        updateActionUI.run();

                    } else if (victims.size() == 1) {
                        robberState.executeSteal(victims.get(0), gameManager.getCurrentTurn());
                        updateActionUI.run();

                    } else {
                        buildStealVictimSidebar(victims, robberState, updateActionUI);
                    }
                }
                render(gc, board, true);
                return;
            }

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
                Edge e = findEdgeAt(board, worldX, worldY);
                if (e != null) {
                    boolean builtRoad = currentState.buildRoad(e, gameManager.getCurrentTurn());
                    if (builtRoad) {
                        updateActionUI.run();
                    }
                }
            }
            render(gc, board, true);
        });

        rightSidebar = new VBox(15);
        rightSidebar.setPrefWidth(350);
        rightSidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 20;");
        rightSidebar.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setRight(rightSidebar);

        VBox leftSidebar = new VBox(10);
        leftSidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 10;");
        Label logTitle = new Label("Histórico");
        logTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        leftSidebar.getChildren().addAll(logTitle, logArea);
        javafx.scene.layout.VBox.setVgrow(logArea, javafx.scene.layout.Priority.ALWAYS);

        root.setLeft(leftSidebar);

        bindPlayerToUI.run();
        gameManager.setOnTurnChangedListener(bindPlayerToUI);

        HBox bottomMenu = new HBox(20,
                playerInfoBox,
                handView,
                devCardsBox,
                spacer,
                bankInfoBox,
                bankHandView,
                turnControlsBox
        );
        bottomMenu.setPrefHeight(200);
        bottomMenu.setStyle("-fx-background-color: #2c3e50; -fx-padding: 15;");
        bottomMenu.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

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
                default:
                    return;
            }
            List<Player> allPlayers = gameManager.getPlayers();
            if (playerIndex >= 0 && playerIndex < allPlayers.size()) {
                Player selectedPlayer = allPlayers.get(playerIndex);

                handView.update(selectedPlayer);
                playerNameLabel.setText(selectedPlayer.getName());

                String colorName = selectedPlayer.getColor().toLowerCase();
                String imagePath = "/assets/settlement/" + colorName + "set.png";
                try {
                    Image iconImg = new Image(getClass().getResource(imagePath).toExternalForm());
                    playerIconView.setImage(iconImg);
                } catch (Exception e) {
                    System.out.println("Cade a imagem do settlement??? " + imagePath);
                }
                event.consume();
            }
        });


        primaryStage.setScene(scene);

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
        } else if (state instanceof com.catan.model.state.MonopolyState) {
            buildMonopolySidebar();
        } else if (state instanceof com.catan.model.state.YearOfPlentyState) {
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

        javafx.scene.control.ComboBox<ResourceType> resourceCombo = new javafx.scene.control.ComboBox<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) resourceCombo.getItems().add(type);
        }

        Button confirmBtn = new Button("Monopolizar Recurso!");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #9b59b6; -fx-pref-width: 150px;");

        confirmBtn.setOnAction(e -> {
            ResourceType selected = resourceCombo.getValue();
            if (selected != null) {
                ITurnState state = gameManager.getCurrentTurn().getState();
                if (state instanceof com.catan.model.state.MonopolyState) {
                    ((com.catan.model.state.MonopolyState) state).chooseResource(selected, gameManager.getCurrentTurn());

                    updateSidebar();
                }
            } else {
                gameManager.getLogger().log("Selecione um recurso primeiro, doidão!");
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

        javafx.scene.control.ComboBox<ResourceType> res1Combo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<ResourceType> res2Combo = new javafx.scene.control.ComboBox<>();

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
                ITurnState state = gameManager.getCurrentTurn().getState();
                if (state instanceof com.catan.model.state.YearOfPlentyState) {
                    ((com.catan.model.state.YearOfPlentyState) state).chooseResources(r1, r2, gameManager.getCurrentTurn());

                    updateSidebar();
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

        javafx.scene.control.Button btnBank = new javafx.scene.control.Button("Trocar com o Banco");
        btnBank.setStyle("-fx-font-weight: bold; -fx-base: #3498db; -fx-pref-width: 200px;");
        btnBank.setOnAction(e -> buildBankTradeSidebar());

        javafx.scene.control.Button btnPlayers = new javafx.scene.control.Button("Trocar com Jogadores");
        btnPlayers.setStyle("-fx-font-weight: bold; -fx-base: #9b59b6; -fx-pref-width: 200px;");
        btnPlayers.setOnAction(e -> buildPlayerTradeCreationSidebar());

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Cancelar");
        btnCancel.setStyle("-fx-font-weight: bold; -fx-base: #e74c3c; -fx-pref-width: 200px;");
        btnCancel.setOnAction(e -> updateSidebar());

        rightSidebar.getChildren().addAll(titleLabel, btnBank, btnPlayers, btnCancel);
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

            try {
                TradeOffer offer = new TradeOffer(proposer, offered, requested);

                Turn currentTurn = gameManager.getCurrentTurn();
                currentTurn.setState(new PlayerTradeState(offer, gameManager.getPlayers()));
                updateSidebar();

            } catch (IllegalArgumentException ex) {
                gameManager.getLogger().log(ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Voltar");
        cancelBtn.setStyle("-fx-font-weight: bold; -fx-base: #95a5a6;");
        cancelBtn.setOnAction(e -> buildTradeOptionsSidebar());

        HBox buttonsBox = new HBox(10, confirmBtn, cancelBtn);
        buttonsBox.setAlignment(javafx.geometry.Pos.CENTER);

        rightSidebar.getChildren().addAll(titleLabel, offerBox, requestBox, buttonsBox);
    }

    private void buildBankTradeSidebar() {
        rightSidebar.getChildren().clear();
        Player player = gameManager.getCurrentTurn().getCurrentPlayer();

        Label titleLabel = new Label("Troca Marítima");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label offerLabel = new Label("Oferecer:");
        offerLabel.setStyle("-fx-text-fill: white;");
        javafx.scene.control.ComboBox<ResourceType> offerCombo = new javafx.scene.control.ComboBox<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) offerCombo.getItems().add(type);
        }

        Label receiveLabel = new Label("Receber 1x:");
        receiveLabel.setStyle("-fx-text-fill: white;");
        javafx.scene.control.ComboBox<ResourceType> receiveCombo = new javafx.scene.control.ComboBox<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) receiveCombo.getItems().add(type);
        }

        Label infoLabel = new Label("Selecione os recursos:");
        infoLabel.setStyle("-fx-text-fill: yellow; -fx-wrap-text: true; -fx-text-alignment: center;");

        javafx.scene.control.Button confirmBtn = new javafx.scene.control.Button("ConfirmarS");
        confirmBtn.setDisable(true);
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");

        Runnable checkTradeValidity = () -> {
            ResourceType offer = offerCombo.getValue();
            ResourceType receive = receiveCombo.getValue();

            if (offer != null && receive != null) {
                if (offer == receive) {
                    infoLabel.setText("Você não pode trocar recursos iguais!");
                    infoLabel.setStyle("-fx-text-fill: #e74c3c;");
                    confirmBtn.setDisable(true);
                    return;
                }

                int rate = player.getTradeRate(offer);
                int playerHas = player.getWallet().getResourceAmount(offer);

                if (playerHas >= rate) {
                    if (gameManager.getBank().getWallet().getResourceAmount(receive) >= 1) {
                        infoLabel.setText("Taxa: " + rate + " " + offer.name() + " por 1 " + receive.name());
                        infoLabel.setStyle("-fx-text-fill: #2ecc71;");
                        confirmBtn.setDisable(false);
                    } else {
                        infoLabel.setText("O Banco não tem " + receive.name() + " suficiente!");
                        infoLabel.setStyle("-fx-text-fill: #e74c3c;");
                        confirmBtn.setDisable(true);
                    }
                } else {
                    infoLabel.setText("Você precisa de " + rate + " " + offer.name() + ", mas só tem " + playerHas + ".");
                    infoLabel.setStyle("-fx-text-fill: #e74c3c;");
                    confirmBtn.setDisable(true);
                }
            }
        };

        offerCombo.setOnAction(e -> checkTradeValidity.run());
        receiveCombo.setOnAction(e -> checkTradeValidity.run());

        confirmBtn.setOnAction(e -> {
            ResourceType offer = offerCombo.getValue();
            ResourceType receive = receiveCombo.getValue();
            int rate = player.getTradeRates().getOrDefault(offer, 4);

            player.getWallet().removeResource(offer, rate);
            gameManager.getBank().getWallet().addResource(offer, rate);

            gameManager.getBank().getWallet().removeResource(receive, 1);
            player.getWallet().addResource(receive, 1);

            gameManager.getLogger().log(player.getName() + " trocou " + rate + " " + offer + " por 1 " + receive + ".");
            updateSidebar();
        });

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Voltar");
        btnCancel.setStyle("-fx-font-weight: bold; -fx-base: #95a5a6;");
        btnCancel.setOnAction(e -> buildTradeOptionsSidebar());

        rightSidebar.getChildren().addAll(
                titleLabel,
                offerLabel, offerCombo,
                receiveLabel, receiveCombo,
                infoLabel,
                confirmBtn,
                btnCancel
        );
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
            System.err.println("Não achou os assets: " + e.getMessage() + " doidão!");
            e.printStackTrace();
        }
    }

    private String getAssetColorName(Player p) {
        return p.getColor().toLowerCase();
    }

    private Vertex findVertexAt(Board board, double worldX, double worldY) {
        double vertexTolerance = 150.0;
        for (Vertex v : board.getVertices()) {
            if (Math.hypot(v.getX() - worldX, v.getY() - worldY) <= vertexTolerance) {
                return v;
            }
        }
        return null;
    }

    private Edge findEdgeAt(Board board, double worldX, double worldY) {
        double edgeTolerance = 150.0;
        for (Edge e : board.getEdges()) {
            if (pointToLineDistance(worldX, worldY, e.getV1().getX(), e.getV1().getY(), e.getV2().getX(), e.getV2().getY()) <= edgeTolerance) {
                return e;
            }
        }
        return null;
    }

    private Tile findTileAt(Board board, double worldX, double worldY) {
        double tileTolerance = 100.0;
        for (Tile tile : board.getTiles()) {
            double cx = computeCenterX(tile.getVertices());
            double cy = computeCenterY(tile.getVertices());

            if (Math.hypot(cx - worldX, cy - worldY) <= tileTolerance) {
                return tile;
            }
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
                double w = tileImg.getWidth();
                double h = tileImg.getHeight();
                gc.drawImage(tileImg, centerX - (w / 2), centerY - (h / 2));
            }

            if (tile.getResource() != ResourceType.DESERT) {
                Image numImg = numberImages.get(tile.getNumberToken());
                if (numImg != null) {
                    double nw = numImg.getWidth();
                    double nh = numImg.getHeight();
                    gc.drawImage(numImg, centerX - (nw / 2), centerY - (nh / 2) + 80);
                }
            }
        }
    }

    private void drawEdges(GraphicsContext gc, Board board) {
        for (Edge edge : board.getEdges()) {
            if (!edge.isEmpty()) {
                Player owner = edge.getBuilding().getOwner();
                Image roadImg = roadImages.get(getAssetColorName(owner));
                PieceDrawer.drawRoad(gc, edge.getV1().getX(), edge.getV1().getY(),
                        edge.getV2().getX(), edge.getV2().getY(), roadImg);
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
                        if (v2.getAdjacentTiles().contains(t)) {
                            landTile = t;
                            break;
                        }
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
                if (length > 0) {
                    dx /= length;
                    dy /= length;
                }

                double edgeDx = v2.getX() - v1.getX();
                double edgeDy = v2.getY() - v1.getY();
                double edgeAngle = Math.toDegrees(Math.atan2(edgeDy, edgeDx));

                while (edgeAngle < 0) edgeAngle += 180;
                while (edgeAngle >= 180) edgeAngle -= 180;

                String dockKey = "0";
                if (Math.abs(edgeAngle - 90) < 15) {
                    dockKey = "0";
                } else if (Math.abs(edgeAngle - 30) < 15 || Math.abs(edgeAngle - 210) < 15) {
                    dockKey = "-30";
                } else if (Math.abs(edgeAngle - 150) < 15 || Math.abs(edgeAngle - 330) < 15) {
                    dockKey = "30";
                }

                double dockOffset = 50.0;
                double dockX = mx + dx * dockOffset;
                double dockY = my + dy * dockOffset;

                Image dImg = dockImages.get(dockKey);
                if (dImg != null) {
                    gc.drawImage(dImg, dockX - dImg.getWidth() / 2.0, dockY - dImg.getHeight() / 2.0);
                }

                double portDist = 260.0;
                double px = mx + dx * portDist;
                double py = my + dy * portDist;

                String portName = (port.getResource() == null) ? "3to1port.png" : port.getResource().name().toLowerCase() + "port.png";
                Image pImg = portImages.get(portName);

                if (pImg != null) {
                    gc.drawImage(pImg, px - pImg.getWidth() / 2.0, py - pImg.getHeight() / 2.0);
                }
            }
        }
    }

    private void drawRobber(GraphicsContext gc, Board board) {
        Robber robber = gameManager.getRobber();
        if (robber != null && robber.getCurrentTile() != null && robberImage != null) {
            Tile tile = robber.getCurrentTile();
            double centerX = computeCenterX(tile.getVertices());
            double centerY = computeCenterY(tile.getVertices());
            double w = robberImage.getWidth();
            double h = robberImage.getHeight();
            gc.drawImage(robberImage, centerX - (w / 2) - 200, centerY - (h / 2));
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
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        rightSidebar.getChildren().add(spacer);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        separator.setStyle("-fx-padding: 10 0 5 0;");
        rightSidebar.getChildren().add(separator);


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
                System.out.println("Erro ao carregar settlement no overview: " + colorName);
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
            int numKnights = p.getNumKnights();
            int longestRoad = p.getLongestRoad();

            HBox statsBox = new HBox(12);
            statsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            HBox resBox = new HBox(5);
            resBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView resIcon = new ImageView();
            resIcon.setFitHeight(25);
            resIcon.setPreserveRatio(true);
            try { resIcon.setImage(new Image(getClass().getResourceAsStream("/assets/resources/resourceback.png"))); } catch(Exception e){}
            Label resLabel = new Label(String.valueOf(numResources));
            resLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            resBox.getChildren().addAll(resIcon, resLabel);

            HBox devBox = new HBox(5);
            devBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView devIcon = new ImageView();
            devIcon.setFitHeight(25);
            devIcon.setPreserveRatio(true);
            try { devIcon.setImage(new Image(getClass().getResourceAsStream("/assets/developmentcards/development.png"))); } catch(Exception e){}
            Label devLabel = new Label(String.valueOf(numDevCards));
            devLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            devBox.getChildren().addAll(devIcon, devLabel);

            HBox knightBox = new HBox(5);
            knightBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView knightIcon = new ImageView();
            knightIcon.setFitHeight(25);
            knightIcon.setPreserveRatio(true);
            try { knightIcon.setImage(new Image(getClass().getResourceAsStream("/assets/bonus/largestarmy.png"))); } catch(Exception e){}
            Label knightLabel = new Label(String.valueOf(numKnights));
            knightLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            knightBox.getChildren().addAll(knightIcon, knightLabel);

            HBox roadBox = new HBox(5);
            roadBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView roadIcon = new ImageView();
            roadIcon.setFitHeight(25);
            roadIcon.setPreserveRatio(true);
            try { roadIcon.setImage(new Image(getClass().getResourceAsStream("/assets/bonus/longestroad.png"))); } catch(Exception e){}
            Label roadLabel = new Label(String.valueOf(longestRoad));
            roadLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-weight: bold; -fx-font-size: 20px;");
            roadBox.getChildren().addAll(roadIcon, roadLabel);

            statsBox.getChildren().addAll(resBox, devBox, knightBox, roadBox);
            infoBox.getChildren().addAll(nameLabel, statsBox);

            playerBox.getChildren().addAll(settlementBox, infoBox);

            rightSidebar.getChildren().add(playerBox);
        }
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

            javafx.scene.control.Button btnMinus = new javafx.scene.control.Button("-");
            javafx.scene.control.Button btnPlus = new javafx.scene.control.Button("+");

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

        javafx.scene.control.Button confirmBtn = new javafx.scene.control.Button("Confirmar Descarte");
        confirmBtn.setStyle("-fx-font-weight: bold; -fx-base: #2ecc71;");
        confirmBtn.setOnAction(e -> {
            int totalSelected = selection.values().stream().mapToInt(Integer::intValue).sum();
            if (totalSelected == requiredAmount) {
                state.submitDiscard(player, selection, gameManager.getCurrentTurn());
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
            javafx.scene.control.Button btn = new javafx.scene.control.Button("Roubar de " + victim.getName());
            btn.setStyle("-fx-font-weight: bold; -fx-min-width: 200px;");

            btn.setOnAction(e -> {
                state.executeSteal(victim, gameManager.getCurrentTurn());
                updateActionUI.run();
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