package com.skyplusplus.minesolver.game;


import com.skyplusplus.minesolver.core.PlayerState;
import com.skyplusplus.minesolver.core.SquareState;
import javafx.util.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;

/* Class that implements the logic of minesweeper */
public class MineSweeper {

    private int totalMines;
    private int numFlags;
    private int numProbed;
    private boolean initializeOnProbe;
    private boolean isMine[][];
    private PlayerState playerState;
    private GameState _gameState = GameState.IN_PROGRESS;

    public MineSweeper(int width, int height, int totalMines) {
        if (width * height < totalMines) {
            throw new IllegalArgumentException("Total mines is higher than the number of squares");
        }
        if (totalMines < 0) {
            throw new IllegalArgumentException("Total mines must be >= 0");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Both width and height must be greater than 0");
        }
        playerState = new PlayerState(width, height);
        this.totalMines = totalMines;
        this.isMine = new boolean[width][height];
        initializeOnProbe = true;
    }

    /***
     * Initializes a game with custom mine locations.
     * @param width
     * @param height
     * @param mines
     */
    public MineSweeper(int width, int height, ArrayList<MineLocation> mines) {
        this(width, height, 0);
        for (MineLocation mine: mines) {
            if (!isMine[mine.getX()][mine.getY()]) {
                totalMines ++;
                isMine[mine.getX()][mine.getY()] = true;
            }
        }
        initializeOnProbe = false;
    }

    public MineSweeper(String... repr) {
        this(repr[0].length(), repr.length, 0);
        for (int y = 0; y < repr.length; y++) {
            for (int x = 0; x < repr[y].length(); x++) {
                if (repr[y].charAt(x) != ' ') {
                    totalMines += 1;
                    isMine[x][y] = true;
                }
            }
        }
        initializeOnProbe = false;
    }

    /***
     * Effect a probe on an unknown square. Does nothing on a flag or already uncovered square. Loses the game on a
     * mine. No effect when the game has ended.
     * @param x
     * @param y
     * @return
     */
    public ProbeResult probe(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (initializeOnProbe) {
            initializeMines(x, y);
            initializeOnProbe = false;
        }

        if (playerState.getBoardState(x, y) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        } else if (isMine[x][y]) {
            playerState.setBoard(x, y, SquareState.MINE);
            loseGame();
            return ProbeResult.LOSE;
        } else if (playerState.getBoardState(x, y) == SquareState.FLAGGED){
            return ProbeResult.NOP;
        } else {
            return cascade(x, y);
        }
    }

    /*
     * Internal probe-expand function, and assumes (x,y) is not a mine.
     */
    private ProbeResult cascade(int x, int y) {
        if (playerState.getBoardState(x, y) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        }

        int squareState = nMinesAround(x, y);
        numProbed ++;

        if (squareState == 0) {
            playerState.setBoard(x, y, 0);
            for (int nx = Math.max(0, x - 1); nx <= Math.min(x + 1, playerState.getWidth() - 1); nx++) {
                for (int ny = Math.max(0, y - 1); ny <= Math.min(y + 1, playerState.getHeight() - 1); ny++) {
                    cascade(nx, ny);
                }
            }
            return ProbeResult.CASCADE;
        } else {
            playerState.setBoard(x, y, squareState);
            return ProbeResult.SINGLE;
        }
    }

    /***
     * Sweeps the perimeter when a user double clicks an uncovered square with the right number of flags around it.
     * Will lose the game if it happens that the flags are wrong, and a mine is exploded. No effect if the game has
     * ended.
     * @param x
     * @param y
     * @return
     */
    public SweepResult sweep(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return SweepResult.NOP;
        }

        if (playerState.getProbedSquare(x, y) != nFlagsAround(x, y)) {
            return SweepResult.NOP;
        }

        boolean hasLost = false;
        for (int nx = Math.max(0, x - 1); nx <= Math.min(x + 1, playerState.getWidth() - 1); nx++) {
            for (int ny = Math.max(0, y - 1); ny <= Math.min(y + 1, playerState.getHeight() - 1); ny++) {
                if (probe(nx, ny) == ProbeResult.LOSE) {
                    hasLost = true;
                }
            }
        }
        return hasLost ? SweepResult.LOSE : SweepResult.OK;
    }

    /***
     * Flags a square, if unknown.
     * @param x
     * @param y
     */
    public FlagResult flag(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getBoardState(x, y) == SquareState.UNKNOWN) {
            playerState.setBoard(x, y, SquareState.FLAGGED);
            numFlags ++;
            return FlagResult.FLAGGED;
        } else {
            return FlagResult.NOP;
        }
    }

    /***
     * Unflags a square, if flagged.
     * @param x
     * @param y
     */
    public FlagResult unflag(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getBoardState(x, y) == SquareState.FLAGGED) {
            playerState.setBoard(x, y, SquareState.UNKNOWN);
            numFlags --;
            return FlagResult.UNFLAGGED;
        } else {
            return FlagResult.NOP;
        }
    }

    /**
     * Unflags a flag, or flags an unknown square.
     * @param x
     * @param y
     * @return
     */
    public FlagResult toggleFlag(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getBoardState(x, y) == SquareState.FLAGGED) {
            return unflag(x, y);
        } else if (playerState.getBoardState(x, y) == SquareState.UNKNOWN) {
            return flag(x, y);
        } else {
            return FlagResult.NOP;
        }
    }

    public int getHeight() {
        return playerState.getHeight();
    }

    public int getWidth() {
        return playerState.getWidth();
    }

    public int getTotalMines() {
        return totalMines;
    }

    public int getUnflaggedMines() {
        return totalMines - numFlags;
    }

    public PlayerState clonePlayerState() {
        return playerState.copy();
    }

    public GameState getGameState() {
        if (numProbed == getWidth() * getHeight() - totalMines) {
            _gameState = GameState.WIN;
        }
        return _gameState;
    }

    public SquareState getPlayerSquareState(int x, int y) {
        return playerState.getBoardState(x, y);
    }

    public int getProbedSquare(int x, int y) {
        if (playerState.getBoardState(x, y) == SquareState.PROBED) {
            return playerState.getProbedSquare(x, y);
        } else {
            throw new IllegalArgumentException("Square is not probed yet");
        }
    }

    public String[] toStringList() {
        String[] ret = new String[getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            ret[y] = "";
            for (int x = 0; x < getWidth(); x++) {
                switch (playerState.getBoardState(x, y)) {
                    case UNKNOWN:
                        ret[y] += ' ';
                        break;
                    case FLAGGED:
                        ret[y] += 'X';
                        break;
                    case MINE:
                        ret[y] += '*';
                        break;
                    case PROBED:
                        ret[y] += (char)((int)'0' + playerState.getProbedSquare(x, y));
                        break;
                }
            }
        }
        return ret;
    }

    private int nMinesAround(int x, int y) {
        int nMines = 0;
        for (int nx = Math.max(0, x-1); nx <= Math.min(x+1, playerState.getWidth()-1); nx++) {
            for (int ny = Math.max(0, y-1); ny <= Math.min(y+1, playerState.getHeight()-1); ny++) {
                if (isMine[nx][ny]) {
                    nMines ++;
                }
            }
        }
        return nMines;
    }

    private int nFlagsAround(int x, int y) {
        int nFlags = 0;
        for (int nx = Math.max(0, x-1); nx <= Math.min(x+1, playerState.getWidth()-1); nx++) {
            for (int ny = Math.max(0, y-1); ny <= Math.min(y+1, playerState.getHeight()-1); ny++) {
                if (playerState.getBoardState(nx,ny) == SquareState.FLAGGED) {
                    nFlags ++;
                }
            }
        }
        return nFlags;
    }


    /*
     * Creates random mines and allows (x, y) to cascade, if possible.
     */
    private void initializeMines(int x, int y) {
        int setMines = 0;

        ArrayList<Pair<Integer, Integer>> warPlan = new ArrayList<>();

        // Create a list of candidate mines, and randomize them to create the minefield.
        for (int nx = 0; nx < getWidth(); nx++) {
            for (int ny = 0; ny < getHeight(); ny++) {
                if (nx < x-1 || nx > x+1 || ny < y-1 || ny > y+1) {
                    warPlan.add(new Pair<>(nx, ny));
                }
            }
        }
        Collections.shuffle(warPlan);

        ArrayList<Pair<Integer, Integer>> warPlanBackup = new ArrayList<>();
        // This would only be used on a minefield so full that inital cascading is impossible.
        for (int nx = Math.max(0, x-1); nx < Math.min(getWidth()-1, x+1); nx++) {
            for (int ny = Math.max(0, y-1); ny < Math.min(getHeight()-1, y+1); ny++){
                if (nx != x || ny != y) {
                    warPlanBackup.add(new Pair<>(nx, ny));
                }
            }
        }
        Collections.shuffle(warPlanBackup);

        warPlan.addAll(warPlanBackup);
        warPlan.add(new Pair<>(x, y)); // This would only be used on a completely full minefield

        for (Pair<Integer, Integer> location: warPlan.subList(0, totalMines)) {
            isMine[location.getKey()][location.getValue()] = true;
        }
    }

    private void loseGame() {
        _gameState = GameState.LOSE;
    }

    public static class MineLocation {
        int x;
        int y;
        public MineLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
