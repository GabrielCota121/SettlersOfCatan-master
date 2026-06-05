package com.example.model.view;

import com.example.model.game.ResourceType;
import com.example.model.player.Player;
import com.example.model.player.ResourceWallet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

public class PlayerHandView extends HBox {
    private final Map<ResourceType, Image> cardImages = new HashMap<>();
    private final Map<ResourceType, Label> quantityLabels = new HashMap<>();
    private final double cardHeight;

    public PlayerHandView() {
        this(80);
    }

    public PlayerHandView(double cardHeight) {
        this.cardHeight = cardHeight;
        this.setSpacing(6);
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
                System.err.println("Imagem de recurso ausente: " + name);
            }
        }
    }

    private void setupView() {
        int fontSize = Math.max(10, (int)(cardHeight * 0.14));
        for (ResourceType type : ResourceType.values()) {
            if (type == ResourceType.DESERT) continue;

            ImageView imageView = new ImageView();
            Image img = cardImages.get(type);
            if (img != null) {
                imageView.setImage(img);
                imageView.setFitHeight(cardHeight);
                imageView.setPreserveRatio(true);
            }

            Label qtyLabel = new Label("0");
            qtyLabel.setStyle(
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;" +
                    "-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 8;" +
                    "-fx-padding: 1 6 1 6;");

            StackPane cardPane = new StackPane(imageView, qtyLabel);
            StackPane.setAlignment(qtyLabel, Pos.TOP_CENTER);
            StackPane.setMargin(qtyLabel, new Insets(4, 0, 0, 0));

            quantityLabels.put(type, qtyLabel);
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
            label.getParent().setOpacity(amount == 0 ? 0.5 : 1.0);
        });
    }
}
