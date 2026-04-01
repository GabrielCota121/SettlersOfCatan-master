package com.catan;

import com.catan.model.Board;
import com.catan.model.BoardFactory;
import com.catan.model.Edge;
import com.catan.model.ResourceType;
import com.catan.model.Tile;
import com.catan.model.Vertex;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

    private Vertex selectedVertex = null;
    private Edge selectedEdge = null;

    @Override
    public void start(Stage primaryStage) {
        int WIDTH = 1200;
        int HEIGHT = 900;

        Board board = BoardFactory.createStandardBoard();

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        drawBoard(gc, board);
        canvas.setOnMouseClicked(event -> {
            double mx = event.getX();
            double my = event.getY();
            selectedVertex = null;
            selectedEdge = null;

            for (Vertex v : board.getVertices()) {
                if (Math.hypot(v.getX() - mx, v.getY() - my) <= 12.0) {
                    selectedVertex = v;
                    break;
                }
            }

            if (selectedVertex == null) {
                for (Edge e : board.getEdges()) {
                    double dist = pointToLineDistance(mx, my, e.getV1().getX(), e.getV1().getY(), e.getV2().getX(), e.getV2().getY());
                    if (dist <= 6.0) {
                        selectedEdge = e;
                        break;
                    }
                }
            }
            gc.clearRect(0, 0, WIDTH, HEIGHT);
            drawBoard(gc, board);
        });

        StackPane root = new StackPane();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.LIGHTBLUE);

        primaryStage.setTitle("Catan Toca do Cota");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void drawBoard(GraphicsContext gc, Board board) {

        for (Tile tile : board.getTiles()) {
            Vertex[] vertices = tile.getVertices();
            double[] xPoints = new double[6];
            double[] yPoints = new double[6];

            double centerX = 0;
            double centerY = 0;

            for (int i = 0; i < 6; i++) {
                xPoints[i] = vertices[i].getX();
                yPoints[i] = vertices[i].getY();
                centerX += xPoints[i];
                centerY += yPoints[i];
            }
            centerX /= 6;
            centerY /= 6;

            gc.setFill(getColorForResource(tile.getResource()));
            gc.fillPolygon(xPoints, yPoints, 6);

            if (tile.getResource() != ResourceType.DESERT) {
                gc.setFill(Color.NAVAJOWHITE);
                gc.fillOval(centerX - 15, centerY - 15, 30, 30);
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeOval(centerX - 15, centerY - 15, 30, 30);

                int token = tile.getNumberToken();
                if (token == 6 || token == 8) {
                    gc.setFill(Color.RED);
                } else {
                    gc.setFill(Color.BLACK);
                }

                gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                double textOffsetX = token > 9 ? 8 : 4;
                gc.fillText(String.valueOf(token), centerX - textOffsetX, centerY + 5);
            }
        }

        for (Edge edge : board.getEdges()) {
            double x1 = edge.getV1().getX();
            double y1 = edge.getV1().getY();
            double x2 = edge.getV2().getX();
            double y2 = edge.getV2().getY();

            boolean isHighlighted = false;


            if (selectedVertex != null && selectedVertex.getAdjacentEdges().contains(edge)) {
                isHighlighted = true;
            }

            else if (selectedEdge != null) {
                if (edge == selectedEdge ||
                        selectedEdge.getV1().getAdjacentEdges().contains(edge) ||
                        selectedEdge.getV2().getAdjacentEdges().contains(edge)) {
                    isHighlighted = true;
                }
            }

            if (isHighlighted) {
                gc.setStroke(Color.RED);
                gc.setLineWidth(6);
            } else {
                gc.setStroke(Color.DARKSLATEGRAY);
                gc.setLineWidth(4);
            }
            gc.strokeLine(x1, y1, x2, y2);
        }

        double vertexRadius = 6.0;
        for (Vertex vertex : board.getVertices()) {
            double vx = vertex.getX();
            double vy = vertex.getY();

            boolean isHighlighted = false;

            if (selectedVertex != null) {
                if (vertex == selectedVertex || selectedVertex.getAdjacentVertices().contains(vertex)) {
                    isHighlighted = true;
                }
            }
            else if (selectedEdge != null) {
                if (vertex == selectedEdge.getV1() || vertex == selectedEdge.getV2()) {
                    isHighlighted = true;
                }
            }

            if (isHighlighted) {
                gc.setFill(Color.RED);
                gc.fillOval(vx - (vertexRadius + 3), vy - (vertexRadius + 3), (vertexRadius + 3) * 2, (vertexRadius + 3) * 2);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillOval(vx - vertexRadius, vy - vertexRadius, vertexRadius * 2, vertexRadius * 2);
            }

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            if (isHighlighted) {
                gc.strokeOval(vx - (vertexRadius + 3), vy - (vertexRadius + 3), (vertexRadius + 3) * 2, (vertexRadius + 3) * 2);
            } else {
                gc.strokeOval(vx - vertexRadius, vy - vertexRadius, vertexRadius * 2, vertexRadius * 2);
            }
        }
    }

    private Color getColorForResource(ResourceType resource) {
        switch (resource) {
            case WOOD: return Color.FORESTGREEN;
            case BRICK: return Color.FIREBRICK;
            case WOOL: return Color.LIGHTGREEN;
            case WHEAT: return Color.GOLD;
            case ORE: return Color.DIMGRAY;
            case DESERT: return Color.SANDYBROWN;
            default: return Color.WHITE;
        }
    }

    private double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;

        if (len_sq != 0) {
            param = dot / len_sq;
        }

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static void main(String[] args) {
        launch(args);
    }
}