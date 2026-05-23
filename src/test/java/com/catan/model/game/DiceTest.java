package com.catan.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class DiceTest {

    private Dice dice;

    @BeforeEach
    void setUp() {
        dice = new Dice();
    }

    @RepeatedTest(100)
    void rollReturnsValidNumber() {
        dice.roll();
        int result = dice.getResult();
        assertTrue(result >= 1 && result <= 6);
    }
}