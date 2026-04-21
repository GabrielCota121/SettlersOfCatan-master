package com.catan;

import com.catan.model.board.*;
import com.catan.model.building.Settlement;
import com.catan.model.game.CatanGameManager;
import com.catan.model.game.ResourceType;
import com.catan.model.game.Turn;
import com.catan.model.game.Bank;
import com.catan.model.logging.ConsoleLogger;
import com.catan.model.logging.IGameLogger;
import com.catan.model.player.Player;
import com.catan.model.state.ITurnState;
import com.catan.model.state.MoveRobberState;
import com.catan.model.state.SetupState;
import com.catan.model.state.WaitingDiscardState;
import com.catan.model.view.PieceDrawer;
import com.catan.model.view.PlayerHandView;
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

    private static final Color OCEAN_COLOR = Color.web("#78b7e8");
    private static final Color EDGE_COLOR = Color.rgb(44, 62, 80, 0.55);

    private CatanGameManager gameManager;

    private final Map<ResourceType, Image> tileImages = new HashMap<>();
    private final Map<Integer, Image> numberImages = new HashMap<>();
    private final Map<String, Image> settlementImages = new HashMap<>();
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
        players.add(new Player(1, "Lucas", "PURPLE"));
        players.add(new Player(2, "Gabriel", "LIGHTBLUE"));
        players.add(new Player(3, "Juliana", "BRONZE"));
        players.add(new Player(4, "Cauã", "BLACK"));

        IGameLogger logger = new ConsoleLogger();
        gameManager = new CatanGameManager(board, players, logger);
        gameManager.getCurrentTurn().setState(new SetupState(false));
        logger.log("Fase atual: " + gameManager.getCurrentTurn().getState().getName());
        logger.log("Vez de " + gameManager.getCurrentTurn().getCurrentPlayer().getName());

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

        for (Player p : players) {
            p.getWallet().setOnWalletChangedListener(() -> {
                handView.update(gameManager.getCurrentTurn().getCurrentPlayer());
            });
        }

        PlayerHandView bankHandView = new PlayerHandView();

        VBox bankInfoBox = new VBox(5);
        bankInfoBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label bankNameLabel = new Label("BANCO");
        bankNameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        ImageView bankIconView = new ImageView();
        bankIconView.setFitHeight(45);
        bankIconView.setPreserveRatio(true);

        try {
            Image bankIcon = new Image(getClass().getResourceAsStream("/assets/bank/bankicon.png"));
            bankIconView.setImage(bankIcon);
        } catch (Exception e) {
            System.out.println("CadÊ o ícone do Bank??");
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
        Image[] diceImgs = new Image[7];

        try {
            passTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/passturn.png"));
            cantPassTurnImg = new Image(getClass().getResourceAsStream("/assets/passturn/cantpassturn.png"));

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

        VBox turnControlsBox = new VBox(15, diceBox, passTurnView);
        turnControlsBox.setAlignment(javafx.geometry.Pos.CENTER);

        Image finalPassTurnImg = passTurnImg;
        Image finalCantPassTurnImg = cantPassTurnImg;
        Runnable updateActionUI = () -> {
            ITurnState state = gameManager.getCurrentTurn().getState();

            passTurnView.setImage(state.canEndTurn() ? finalPassTurnImg : finalCantPassTurnImg);
            passTurnView.setCursor(state.canEndTurn() ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);

            diceBox.setCursor(state.canRollDice() ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.DEFAULT);
            diceBox.setOpacity(state.canRollDice() ? 1.0 : 0.3);

            int d1 = gameManager.getDice1().getResult();
            int d2 = gameManager.getDice2().getResult();

            if (d1 > 0 && d1 <= 6) dice1View.setImage(diceImgs[d1]);
            if (d2 > 0 && d2 <= 6) dice2View.setImage(diceImgs[d2]);

            updateSidebar();
        };

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
                gameManager.getLogger().log("Ação inválida: Você não pode passar o turno agora!");
            }
            updateActionUI.run();
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
                        buildStealVictimSidebar(victims, robberState, updateActionUI); // Passando o runnable aqui
                    }
                }
                render(gc, board, true);
                return;
            }

            Player currentPlayer = gameManager.getCurrentTurn().getCurrentPlayer();
            String playerName = currentPlayer.getName();

            Vertex v = findVertexAt(board, worldX, worldY);
            if (v != null) {
                boolean builtSettlement = currentState.buildSettlement(v, gameManager.getCurrentTurn());

                if (builtSettlement) {
                    gameManager.getLogger().log(playerName + " construiu um Settlement!");
                    updateActionUI.run();
                } else {
                    boolean builtCity = currentState.buildCity(v, gameManager.getCurrentTurn());

                    if (builtCity) {
                        gameManager.getLogger().log(playerName + " construiu uma City!");
                        updateActionUI.run();
                    }
                }
            } else {
                Edge e = findEdgeAt(board, worldX, worldY);
                if (e != null) {
                    boolean builtRoad = currentState.buildRoad(e, gameManager.getCurrentTurn());
                    if (builtRoad) {
                        gameManager.getLogger().log(playerName + " construiu uma Road!");
                        updateActionUI.run();
                    }
                }
            }
            render(gc, board, true);
        });

        bindPlayerToUI.run();
        gameManager.setOnTurnChangedListener(bindPlayerToUI);

        HBox bottomMenu = new HBox(20,
                playerInfoBox,
                handView,
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

        rightSidebar = new VBox(15);
        rightSidebar.setPrefWidth(350);
        rightSidebar.setStyle("-fx-background-color: #34495e; -fx-padding: 20;");
        rightSidebar.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setRight(rightSidebar);

        render(gc, board, true);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
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
            } else {
                gc.setStroke(EDGE_COLOR);
                gc.setLineWidth(1.5);
                gc.strokeLine(edge.getV1().getX(), edge.getV1().getY(), edge.getV2().getX(), edge.getV2().getY());
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

    private void updateSidebar() {
        if (rightSidebar == null) return;

        rightSidebar.getChildren().clear();
        ITurnState state = gameManager.getCurrentTurn().getState();

        if (state instanceof WaitingDiscardState discardState) {
            List<Player> pending = discardState.getPendingPlayers();

            if (!pending.isEmpty()) {
                buildDiscardSidebar(pending.get(0), discardState);
            }
        } else {
            Label infoLabel = new Label("");
            infoLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            rightSidebar.getChildren().add(infoLabel);
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