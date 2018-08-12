package com.skyplusplus.minesolver.core;

/* Compact class representing what the player can see */

import java.util.Arrays;

public class PlayerState {
    private int width;
    private int height;
    private int boardProbedSquares[][];
    private SquareState boardSquareStates[][];
    private int totalMines;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTotalMines() {
        return totalMines;
    }

    public boolean isFlagged(int x, int y) {
        return boardSquareStates[x][y] == SquareState.FLAGGED;
    }

    public int getSquareMineCount(int x, int y) {
        return boardProbedSquares[x][y];
    }

    /**
     * Automatically sets state to "PROBED"
     * @param x
     * @param y
     * @param number
     */
    public void setBoard(int x, int y, int number) {
        this.boardProbedSquares[x][y] = number;
        this.boardSquareStates[x][y] = SquareState.PROBED;
    }

    public void setBoard(int x, int y, SquareState state) {
        this.boardSquareStates[x][y] = state;
    }


    public PlayerState(int width, int height, int totalMines) {
        this.width = width;
        this.height = height;
        this.totalMines = totalMines;
        this.boardProbedSquares = new int[width][height];
        this.boardSquareStates = new SquareState[width][height];
        for (int i = 0; i < width; i++) {
            Arrays.fill(this.boardSquareStates[i], SquareState.UNKNOWN);
        }
    }

    public PlayerState copy() {
        PlayerState ret = new PlayerState(width, height, totalMines);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                ret.boardProbedSquares[i][j] = this.boardProbedSquares[i][j];
                ret.boardSquareStates[i][j] = this.boardSquareStates[i][j];
            }
        }
        return ret;
    }

    public SquareState getSquareState(int x, int y) {
        return boardSquareStates[x][y];
    }


}
