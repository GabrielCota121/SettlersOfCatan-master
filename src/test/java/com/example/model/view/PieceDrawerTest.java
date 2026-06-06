package com.example.model.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PieceDrawerTest {

    @Test
    void testConstructorAndStaticAccess() {
        // Cobre o construtor da classe (estava em 0%)
        assertDoesNotThrow(PieceDrawer::new);
    }

    @Test
    void drawSettlement_withImage_calculatesCenter() {
        GraphicsContext gc = mock(GraphicsContext.class);
        Image img = mock(Image.class);
        when(img.getWidth()).thenReturn(20.0);
        when(img.getHeight()).thenReturn(20.0);

        // Executa o branch (img != null)
        PieceDrawer.drawSettlement(gc, 100, 100, img);
        
        // Verifica se desenhou centralizado: 100 - (20/2) = 90
        verify(gc).drawImage(img, 90.0, 90.0);
    }

    @Test
    void drawCity_withImage_calculatesCenter() {
        GraphicsContext gc = mock(GraphicsContext.class);
        Image img = mock(Image.class);
        when(img.getWidth()).thenReturn(40.0);
        when(img.getHeight()).thenReturn(40.0);

        PieceDrawer.drawCity(gc, 200, 200, img);
        
        verify(gc).drawImage(img, 180.0, 180.0);
    }

    @Test
    void drawRoad_fullFlow() {
        GraphicsContext gc = mock(GraphicsContext.class);
        Image img = mock(Image.class);
        when(img.getWidth()).thenReturn(30.0);
        when(img.getHeight()).thenReturn(10.0);

        // Testa estrada horizontal (0 graus + 90 de ajuste)
        PieceDrawer.drawRoad(gc, 0, 0, 100, 0, img);

        verify(gc).save();
        verify(gc).translate(50.0, 0.0);
        verify(gc).rotate(90.0);
        verify(gc).restore();
        verify(gc).drawImage(eq(img), anyDouble(), anyDouble());
    }

    @Test
    void nullGuards_allMethods() {
        // Cobre os branches de 'return' quando img é null
        assertDoesNotThrow(() -> PieceDrawer.drawSettlement(null, 0, 0, null));
        assertDoesNotThrow(() -> PieceDrawer.drawCity(null, 0, 0, null));
        assertDoesNotThrow(() -> PieceDrawer.drawRoad(null, 0, 0, 0, 0, null));
    }
}