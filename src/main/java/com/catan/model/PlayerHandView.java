package com.catan.model;

import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineJoin;

import java.util.HashMap;
import java.util.Map;

public class PlayerHandView extends HBox {
    private final Map<ResourceType, Image> cardImages = new HashMap<>();
    private final Map<ResourceType, Label> quantityLabels = new HashMap<>();

    public PlayerHandView() {
        this.setSpacing(10);
        this.setAlignment(Pos.CENTER_LEFT);
        loadImages();
        setupView();
    }

    private void loadImages() {
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            String name = type.name().toLowerCase() + ".png";
            try {
                cardImages.put(type, new Image(getClass().getResourceAsStream("/assets/resources/" + name)));
            } catch (Exception e) {
                System.err.println("Cade a iamgem da carta?? " + name);
            }
        }
    }

    private void setupView() {
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            StackPane cardPane = new StackPane();

            ImageView imageView = new ImageView();
            Image img = cardImages.get(type);
            if (img != null) {
                imageView.setImage(img);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
            }

            Label qtyLabel = new Label("0");
            qtyLabel.setStyle(
                    "-fx-background-color: #e74c3c; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 14px; " +
                            "-fx-padding: 2 8 2 8; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: white; " +
                            "-fx-border-radius: 12; " +
                            "-fx-border-width: 2;"
            );

            StackPane.setAlignment(qtyLabel, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(qtyLabel, new javafx.geometry.Insets(0, 0, 15, 0));

            quantityLabels.put(type, qtyLabel);
            cardPane.getChildren().addAll(imageView, qtyLabel);

            this.getChildren().add(cardPane);
        }
    }

    public void update(Player player) {
        ResourceWallet wallet = player.getWallet();
        quantityLabels.forEach((type, label) -> {
            int amount = wallet.getResourceAmount(type);
            label.setText(String.valueOf(amount));
            if (amount == 0) {
                label.getParent().setOpacity(0.5);
            } else {
                label.getParent().setOpacity(1.0);
            }
        });
    }

    public static class AssetDrawer {

        public static void drawSettlement(GraphicsContext gc, double x, double y, Color playerColor) {
            double baseSize = 20;
            double heightRoof = 12.0;

            gc.setFill(playerColor);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.0);
            gc.setLineJoin(StrokeLineJoin.ROUND);

            double[] xPoints = {
                    x - baseSize / 2,
                    x + baseSize / 2,
                    x + baseSize / 2,
                    x,
                    x - baseSize / 2
            };
            double[] yPoints = {
                    y + baseSize / 2,
                    y + baseSize / 2,
                    y - baseSize / 2 + heightRoof,
                    y - baseSize / 2,
                    y - baseSize / 2 + heightRoof
            };

            gc.fillPolygon(xPoints, yPoints, 5);
            gc.strokePolygon(xPoints, yPoints, 5);
        }

        public static void drawCity(GraphicsContext gc, double x, double y, Color playerColor) {
            double w = 36.0;
            double h = 18.0;

            double triSide = w * 0.4;
            double triHeight = Math.sqrt(3) / 2 * triSide;

            gc.setFill(playerColor);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.2);
            gc.setLineJoin(StrokeLineJoin.ROUND);

            double left = x - w / 2;
            double right = x + w / 2;
            double top = y - h / 2;
            double bottom = y + h / 2;

            double triLeft = left;
            double triRight = left + triSide;
            double triPeakX = (triLeft + triRight) / 2;
            double triPeakY = top - triHeight;

            double[] xPoints = {
                    left,
                    right,
                    right,
                    triRight,
                    triPeakX,
                    triLeft
            };

            double[] yPoints = {
                    bottom,
                    bottom,
                    top,
                    top,
                    triPeakY,
                    top
            };

            gc.fillPolygon(xPoints, yPoints, xPoints.length);
            gc.strokePolygon(xPoints, yPoints, xPoints.length);
        }

        public static void drawRoad(GraphicsContext gc, double x1, double y1, double x2, double y2, Color playerColor) {
            double roadWidth = 6.0;

            double dx = x2 - x1;
            double dy = y2 - y1;
            double edgeLength = Math.hypot(dx, dy);
            double angleRad = Math.atan2(dy, dx);

            double setback = 8.0;
            double roadLength = Math.max(0, edgeLength - setback * 2);

            gc.save();
            gc.translate((x1 + x2) / 2, (y1 + y2) / 2);
            gc.rotate(Math.toDegrees(angleRad));

            gc.setFill(playerColor);
            gc.fillRect(-roadLength / 2, -roadWidth / 2, roadLength, roadWidth);

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(0.5);
            gc.strokeRect(-roadLength / 2, -roadWidth / 2, roadLength, roadWidth);

            gc.restore();
        }
    }
}