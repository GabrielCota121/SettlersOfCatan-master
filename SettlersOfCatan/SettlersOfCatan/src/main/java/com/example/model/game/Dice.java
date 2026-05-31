package com.example.model.game;
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
    /** Define o resultado diretamente — usado para reconciliar o cliente com o
     *  resultado autoritativo rolado no servidor. */
    public void setResult(int result) {
        this.result = result;
    }
}