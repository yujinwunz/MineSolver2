package com.skyplusplus.minesolver.game;

import java.util.function.Consumer;
import java.util.function.Function;

public class Statistics {
    public int wins;
    public int totalGames;

    public int getLosses() {
        return totalGames - wins;
    }

    public void reset() {
        wins = 0;
        totalGames = 0;
    }
}
