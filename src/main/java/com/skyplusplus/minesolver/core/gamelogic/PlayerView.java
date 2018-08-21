package com.skyplusplus.minesolver.core.gamelogic;

/* Compact class representing what the player can see */

import java.util.*;
import java.util.stream.Collectors;

public class PlayerView {
    private final int width;
    private final int height;
    private final int boardProbedSquares[][];
    private final SquareState boardSquareStates[][];
    private final int totalMines;
    private List<BoardCoord> allSquares;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @SuppressWarnings("WeakerAccess")
    public int getTotalMines() {
        return totalMines;
    }

    public int getSquareMineCount(BoardCoord coord) {
        return boardProbedSquares[coord.getX()][coord.getY()];
    }

    public void setBoard(BoardCoord coord, int number) {
        this.boardProbedSquares[coord.getX()][coord.getY()] = number;
        this.boardSquareStates[coord.getX()][coord.getY()] = SquareState.PROBED;
    }

    public void setBoard(BoardCoord coord, SquareState state) {
        this.boardSquareStates[coord.getX()][coord.getY()] = state;
    }

    public PlayerView(int width, int height, int totalMines) {
        this.width = width;
        this.height = height;
        this.totalMines = totalMines;
        this.boardProbedSquares = new int[width][height];
        this.boardSquareStates = new SquareState[width][height];
        for (int i = 0; i < width; i++) {
            Arrays.fill(this.boardSquareStates[i], SquareState.UNKNOWN);
        }
    }

    public PlayerView copy() {
        PlayerView ret = new PlayerView(width, height, totalMines);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                ret.boardProbedSquares[i][j] = this.boardProbedSquares[i][j];
                ret.boardSquareStates[i][j] = this.boardSquareStates[i][j];
            }
        }
        return ret;
    }

    public List<BoardCoord> getNeighbours(BoardCoord coord) {
        return coord.getNeighbours(width, height);
    }

    public List<BoardCoord> getNeighbours(BoardCoord coord, SquareState state) {
        List<BoardCoord> retVal = coord.getNeighbours(width, height);
        retVal.removeIf(loc -> boardSquareStates[loc.getX()][loc.getY()] != state);
        return retVal;
    }

    public SquareState getSquareState(BoardCoord coord) {
        return boardSquareStates[coord.getX()][coord.getY()];
    }

    private void ensureAllSquaresList() {
        if (allSquares == null) {
            List<BoardCoord> retVal = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    retVal.add(BoardCoord.ofValue(x, y));
                }
            }
            allSquares = Collections.unmodifiableList(retVal);
        }
    }

    public List<BoardCoord> getAllSquares() {
        ensureAllSquaresList();
        return allSquares;
    }

    public List<BoardCoord> getAllSquares(SquareState state) {
        ensureAllSquaresList();
        return allSquares.stream().filter(coord -> getSquareState(coord) == state).collect(Collectors.toList());
    }
}
