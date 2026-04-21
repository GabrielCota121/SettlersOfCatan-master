package com.catan.model.view;

import com.catan.model.game.ResourceType;
import com.catan.model.player.Player;
import com.catan.model.player.ResourceWallet;
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
        update(player.getWallet());
    }

    public void update(ResourceWallet wallet) {
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
}