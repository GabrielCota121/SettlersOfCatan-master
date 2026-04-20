package com.catan.model;
import java.util.Random;

public class Dice {
    private final Random random;
    private int result;

    public Dice() {
        this.random = new Random();
    }

    public void roll() {
         result = random.nextInt(6) + 1;
    }
    public int getResult() {
        return result;
    }
}