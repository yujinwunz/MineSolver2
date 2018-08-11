package com.skyplusplus.minesolver.game;


import com.skyplusplus.minesolver.core.PlayerState;
import com.skyplusplus.minesolver.core.SquareState;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;

/* Class that implements the logic of minesweeper */
public class MineSweeper {

    private int totalMines;
    private boolean initializeOnProbe;
    private boolean isMine[][];
    private PlayerState playerState;
    private GameState gameState = GameState.IN_PROGRESS;

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
        if (gameState != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (playerState.getBoardState(x, y) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        } else if (isMine[x][y]) {
            playerState.setBoard(x, y, SquareState.MINE);
            gameState = GameState.LOSE;
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
        if (gameState != GameState.IN_PROGRESS) {
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
        if (gameState != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getBoardState(x, y) == SquareState.UNKNOWN) {
            playerState.setBoard(x, y, SquareState.FLAGGED);
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
        if (gameState != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getBoardState(x, y) == SquareState.FLAGGED) {
            playerState.setBoard(x, y, SquareState.UNKNOWN);
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
        if (gameState != GameState.IN_PROGRESS) {
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
        throw new NotImplementedException();
    }

    public PlayerState clonePlayerState() {
        return playerState.copy();
    }

    public GameState getGameState() {
        return gameState;
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

    public String getBoardRepr() {
        StringBuilder builder = new StringBuilder();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                switch (playerState.getBoardState(x, y)) {
                    case PROBED:
                        builder.append('0' + playerState.getProbedSquare(x, y));
                        break;
                    case MINE:
                        builder.append('*');
                        break;
                    case FLAGGED:
                        builder.append('X');
                        break;
                    case UNKNOWN:
                        builder.append(' ');
                    default:
                        break;
                }
            }
            builder.append('\n');
        }
        return builder.toString();
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
