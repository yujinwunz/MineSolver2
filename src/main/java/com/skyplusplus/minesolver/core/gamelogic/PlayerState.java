package com.skyplusplus.minesolver.core.gamelogic;

/* Compact class representing what the player can see */

import java.util.*;
import java.util.stream.Collectors;

public class PlayerState {
    private final int width;
    private final int height;
    private final int boardProbedSquares[][];
    private final SquareState boardSquareStates[][];
    private final int totalMines;
    private List<MineLocation> allSquares;

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

    public int getSquareMineCount(MineLocation location) {
        return boardProbedSquares[location.getX()][location.getY()];
    }

    public void setBoard(MineLocation location, int number) {
        this.boardProbedSquares[location.getX()][location.getY()] = number;
        this.boardSquareStates[location.getX()][location.getY()] = SquareState.PROBED;
    }

    public void setBoard(MineLocation location, SquareState state) {
        this.boardSquareStates[location.getX()][location.getY()] = state;
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

    public List<MineLocation> getNeighbours(MineLocation location) {
        return location.getNeighbours(width, height);
    }

    public List<MineLocation> getNeighbours(MineLocation location, SquareState state) {
        List<MineLocation> retVal = location.getNeighbours(width, height);
        retVal.removeIf(loc -> boardSquareStates[loc.getX()][loc.getY()] != state);
        return retVal;
    }

    public SquareState getSquareState(MineLocation location) {
        return boardSquareStates[location.getX()][location.getY()];
    }

    private void ensureAllSquaresList() {
        if (allSquares == null) {
            List<MineLocation> retVal = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    retVal.add(MineLocation.ofValue(x, y));
                }
            }
            allSquares = Collections.unmodifiableList(retVal);
        }
    }

    public List<MineLocation> getAllSquares() {
        ensureAllSquaresList();
        return allSquares;
    }

    public List<MineLocation> getAllSquares(SquareState state) {
        ensureAllSquaresList();
        return allSquares.stream().filter(location -> getSquareState(location) == state).collect(Collectors.toList());
    }
}
