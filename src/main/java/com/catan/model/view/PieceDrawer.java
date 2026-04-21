package com.catan.model.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class PieceDrawer {

    public static void drawSettlement(GraphicsContext gc, double x, double y, Image img) {
        if (img == null) return;
        double width = img.getWidth();
        double height = img.getHeight();
        gc.drawImage(img, x - (width / 2), y - (height / 2));
    }

    public static void drawCity(GraphicsContext gc, double x, double y, Image img) {
        if (img == null) return;
        double width = img.getWidth();
        double height = img.getHeight();
        gc.drawImage(img, x - (width / 2), y - (height / 2));
    }

    public static void drawRoad(GraphicsContext gc, double x1, double y1, double x2, double y2, Image img) {
        if (img == null) return;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double anguloGraus = Math.toDegrees(Math.atan2(dy, dx));

        double larguraImagem = img.getWidth();
        double alturaImagem = img.getHeight();

        gc.save();
        gc.translate((x1 + x2) / 2, (y1 + y2) / 2);
        gc.rotate(anguloGraus + 90);
        gc.drawImage(img, -(larguraImagem / 2), -(alturaImagem / 2));

        gc.restore();
    }
}