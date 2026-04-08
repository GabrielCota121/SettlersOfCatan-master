package com.catan;

import com.catan.model.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class Main extends Application {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;
    private static final double TILE_SCALE = 0.97;
    private static final double SAND_BORDER_WIDTH = 18.0;
    private static final double SAND_HEX_SCALE = 1.03;

    private static final double BASE_VERTEX_RADIUS = 4.0;
    private static final double HIGHLIGHT_VERTEX_EXTRA = 3.0;

    private static final Color OCEAN_COLOR = Color.web("#78b7e8");
    private static final Color SAND_COLOR = Color.web("#e0c08c");
    private static final Color TILE_OUTLINE_COLOR = Color.rgb(0, 0, 0, 0.16);
    private static final Color EDGE_COLOR = Color.rgb(44, 62, 80, 0.55);
    private static final Color EDGE_HIGHLIGHT_COLOR = Color.WHITE;

    private Vertex selectedVertex = null;
    private Edge selectedEdge = null;

    private final Map<ResourceType, ImagePattern> resourceTextures = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        loadAssets();

        Board board = BoardFactory.createStandardBoard();

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawBoard(gc, board);

        canvas.setOnMouseClicked(event -> {
            double mx = event.getX();
            double my = event.getY();

            updateSelection(board, mx, my);

            gc.clearRect(0, 0, WIDTH, HEIGHT);
            drawBoard(gc, board);
        });

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(OCEAN_COLOR);

        primaryStage.setTitle("Catan Toca do Cota");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadAssets() {
        double zoom = 1.4;
        double offset = -(zoom - 1.0) / 2.0;

        for (ResourceType type : ResourceType.values()) {
            String fileName = type.name().toLowerCase() + ".jpeg";
            try {
                String path = getClass().getResource("/assets/" + fileName).toExternalForm();
                Image img = new Image(path);
                resourceTextures.put(type, new ImagePattern(img, offset, offset, zoom, zoom, true));
            } catch (Exception e) {
                System.err.println("Erro ao carregar textura: " + fileName);
            }
        }
    }

    private void updateSelection(Board board, double mx, double my) {
        selectedVertex = null;
        selectedEdge = null;
        for (Vertex v : board.getVertices()) {
            if (Math.hypot(v.getX() - mx, v.getY() - my) <= 12.0) {
                selectedVertex = v;
                return;
            }
        }

        for (Edge e : board.getEdges()) {
            double dist = pointToLineDistance(
                    mx, my,
                    e.getV1().getX(), e.getV1().getY(),
                    e.getV2().getX(), e.getV2().getY()
            );

            if (dist <= 6.0) {
                selectedEdge = e;
                return;
            }
        }
    }

    private void drawBoard(GraphicsContext gc, Board board) {
        drawSandBackplates(gc, board);
        drawTiles(gc, board);
        drawEdges(gc, board);
        drawVertices(gc, board);
    }

    private void drawSandBackplates(GraphicsContext gc, Board board) {
        gc.setLineJoin(StrokeLineJoin.ROUND);

        for (Tile tile : board.getTiles()) {
            Vertex[] vertices = tile.getVertices();
            double centerX = computeCenterX(vertices);
            double centerY = computeCenterY(vertices);
            double[] xPoints = scaleXPoints(vertices, centerX, SAND_HEX_SCALE);
            double[] yPoints = scaleYPoints(vertices, centerY, SAND_HEX_SCALE);
            gc.setFill(SAND_COLOR);
            gc.fillPolygon(xPoints, yPoints, 6);
            gc.setStroke(SAND_COLOR);
            gc.setLineWidth(SAND_BORDER_WIDTH);
            gc.strokePolygon(xPoints, yPoints, 6);
        }
    }

    private void drawTiles(GraphicsContext gc, Board board) {
        gc.setLineJoin(StrokeLineJoin.ROUND);

        for (Tile tile : board.getTiles()) {
            Vertex[] vertices = tile.getVertices();

            double centerX = computeCenterX(vertices);
            double centerY = computeCenterY(vertices);

            double[] xPoints = scaleXPoints(vertices, centerX, TILE_SCALE);
            double[] yPoints = scaleYPoints(vertices, centerY, TILE_SCALE);

            ImagePattern pattern = resourceTextures.get(tile.getResource());
            if (pattern != null) {
                gc.setFill(pattern);
            } else {
                gc.setFill(Color.MAGENTA);
            }
            gc.fillPolygon(xPoints, yPoints, 6);
            gc.setStroke(TILE_OUTLINE_COLOR);
            gc.setLineWidth(1.0);
            gc.strokePolygon(xPoints, yPoints, 6);

            drawNumberToken(gc, tile, centerX, centerY);
        }
    }

    private void drawNumberToken(GraphicsContext gc, Tile tile, double centerX, double centerY) {
        if (tile.getResource() == ResourceType.DESERT) {
            return;
        }

        gc.setFill(Color.web("#f2e6d9"));
        gc.fillOval(centerX - 16, centerY - 16, 32, 32);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.strokeOval(centerX - 16, centerY - 16, 32, 32);

        int token = tile.getNumberToken();

        gc.setFill((token == 6 || token == 8) ? Color.RED : Color.BLACK);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 15));

        double textOffset = token > 9 ? 10 : 5;
        gc.fillText(String.valueOf(token), centerX - textOffset, centerY + 6);
    }

    private void drawEdges(GraphicsContext gc, Board board) {
        for (Edge edge : board.getEdges()) {
            boolean isHighlighted = isEdgeHighlighted(edge);

            gc.setStroke(isHighlighted ? EDGE_HIGHLIGHT_COLOR : EDGE_COLOR);
            gc.setLineWidth(isHighlighted ? 4.0 : 1.8);

            gc.strokeLine(
                    edge.getV1().getX(), edge.getV1().getY(),
                    edge.getV2().getX(), edge.getV2().getY()
            );
        }
    }

    private void drawVertices(GraphicsContext gc, Board board) {
        for (Vertex vertex : board.getVertices()) {
            boolean isHighlighted = isVertexHighlighted(vertex);

            double radius = isHighlighted
                    ? BASE_VERTEX_RADIUS + HIGHLIGHT_VERTEX_EXTRA
                    : BASE_VERTEX_RADIUS;

            gc.setFill(isHighlighted ? Color.WHITE : Color.BLACK);
            gc.fillOval(
                    vertex.getX() - radius,
                    vertex.getY() - radius,
                    radius * 2,
                    radius * 2
            );

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.1);
            gc.strokeOval(
                    vertex.getX() - radius,
                    vertex.getY() - radius,
                    radius * 2,
                    radius * 2
            );
        }
    }

    private boolean isEdgeHighlighted(Edge edge) {
        if (selectedVertex != null) {
            return selectedVertex.getAdjacentEdges().contains(edge);
        }

        if (selectedEdge != null) {
            return edge == selectedEdge
                    || selectedEdge.getV1().getAdjacentEdges().contains(edge)
                    || selectedEdge.getV2().getAdjacentEdges().contains(edge);
        }

        return false;
    }

    private boolean isVertexHighlighted(Vertex vertex) {
        if (selectedVertex != null) {
            return vertex == selectedVertex
                    || selectedVertex.getAdjacentVertices().contains(vertex);
        }

        if (selectedEdge != null) {
            return vertex == selectedEdge.getV1()
                    || vertex == selectedEdge.getV2();
        }

        return false;
    }

    private double computeCenterX(Vertex[] vertices) {
        double sum = 0;
        for (Vertex v : vertices) {
            sum += v.getX();
        }
        return sum / vertices.length;
    }

    private double computeCenterY(Vertex[] vertices) {
        double sum = 0;
        for (Vertex v : vertices) {
            sum += v.getY();
        }
        return sum / vertices.length;
    }

    private double[] scaleXPoints(Vertex[] vertices, double centerX, double scale) {
        double[] xPoints = new double[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            xPoints[i] = centerX + (vertices[i].getX() - centerX) * scale;
        }
        return xPoints;
    }

    private double[] scaleYPoints(Vertex[] vertices, double centerY, double scale) {
        double[] yPoints = new double[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            yPoints[i] = centerY + (vertices[i].getY() - centerY) * scale;
        }
        return yPoints;
    }

    private double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            return Math.hypot(px - x1, py - y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        return Math.hypot(px - closestX, py - closestY);
    }

    public static void main(String[] args) {
        launch(args);
    }
}