package com.skyplusplus.minesolver.core;


import com.skyplusplus.minesolver.core.MineLocation;
import com.skyplusplus.minesolver.core.PlayerState;
import com.skyplusplus.minesolver.core.SquareState;
import com.skyplusplus.minesolver.game.FlagResult;
import com.skyplusplus.minesolver.game.GameState;
import com.skyplusplus.minesolver.game.ProbeResult;
import javafx.util.Pair;

import java.util.*;

/* Class that implements the logic of the minesweeper game */
public class MineSweeper {

    private int numFlags;
    private int numProbed;
    private boolean initializeOnProbe;
    private boolean isMine[][];
    private PlayerState playerState;
    private GameState _gameState = GameState.IN_PROGRESS;

    public static class TooManyMinesException extends IllegalArgumentException {}

    public MineSweeper(int width, int height, int totalMines) {
        this(width, height);
        if (width * height < totalMines) {
            throw new TooManyMinesException();
        }
        if (totalMines < 0) {
            throw new IllegalArgumentException("Total mines must be >= 0");
        }
        playerState = new PlayerState(width, height, totalMines);
        initializeOnProbe = true;
    }

    private MineSweeper(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Both width and height must be greater than 0");
        }
        this.isMine = new boolean[width][height];
    }


    /***
     * Initializes a game with custom mine locations.
     * @param width
     * @param height
     * @param mines
     */
    public MineSweeper(int width, int height, ArrayList<MineLocation> mines) {
        this(width, height);

        int totalMines = 0;
        for (MineLocation mine: mines) {
            if (!isMine[mine.getX()][mine.getY()]) {
                totalMines ++;
                isMine[mine.getX()][mine.getY()] = true;
            }
        }
        this.playerState = new PlayerState(width, height, totalMines);
    }

    public MineSweeper(String... repr) {
        this(repr[0].length(), repr.length);

        int totalMines = 0;
        boolean shouldLose = false;
        for (int y = 0; y < repr.length; y++) {
            for (int x = 0; x < repr[y].length(); x++) {
                switch (repr[y].charAt(x)) {
                    case 'x': // Wrong flag
                        playerState.setBoard(x, y, SquareState.FLAGGED);
                        break;
                    case 'X':
                        playerState.setBoard(x, y, SquareState.FLAGGED);
                        totalMines += 1;
                        isMine[x][y] = true;
                        break;
                    case '#': // busted
                        playerState.setBoard(x, y, SquareState.MINE);
                        totalMines += 1;
                        isMine[x][y] = true;
                        shouldLose = true;
                        break;
                    case '*':
                        totalMines += 1;
                        isMine[x][y] = true;
                        break;
                    case ' ':
                        playerState.setBoard(x, y, SquareState.UNKNOWN);
                        break;
                    default:
                        if (repr[y].charAt(x) >= '0' && repr[y].charAt(x) <= '8') {
                            playerState.setBoard(x, y, repr[y].charAt(x) - '0');
                            numProbed ++;
                        } else {
                            throw new IllegalArgumentException("Invalid character: " + repr[y].charAt(x));
                        }
                }
            }
        }
        this.playerState = new PlayerState(repr[0].length(), repr.length, totalMines);

        if (shouldLose) {
            loseGame();
        }
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

        if (playerState.getSquareState(x, y) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        } else if (isMine[x][y]) {
            playerState.setBoard(x, y, SquareState.MINE);
            loseGame();
            return ProbeResult.LOSE;
        } else if (playerState.getSquareState(x, y) == SquareState.FLAGGED){
            return ProbeResult.NOP;
        } else {
            return cascade(x, y);
        }
    }

    /*
     * Internal probe-expand function, and assumes (x,y) is not a mine.
     */
    private ProbeResult cascade(int startX, int startY) {
        if (playerState.getSquareState(startX, startY) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        }

        Queue<MineLocation> dfs = new ArrayDeque<MineLocation>();
        dfs.add(new MineLocation(startX, startY));
        while (!dfs.isEmpty()) {
            int x = dfs.peek().getX();
            int y = dfs.peek().getY();
            dfs.remove();

            if (playerState.getSquareState(x, y) == SquareState.UNKNOWN) {
                int squareState = nMinesAround(x, y);
                numProbed++;
                playerState.setBoard(x, y, squareState);

                if (squareState == 0) {
                    for (int nx = Math.max(0, x - 1); nx <= Math.min(x + 1, playerState.getWidth() - 1); nx++) {
                        for (int ny = Math.max(0, y - 1); ny <= Math.min(y + 1, playerState.getHeight() - 1); ny++) {
                            dfs.add(new MineLocation(nx, ny));
                        }
                    }
                }
            }
        }
        return ProbeResult.OK;
    }

    /***
     * Sweeps the perimeter when a user double clicks an uncovered square with the right number of flags around it.
     * Will lose the game if it happens that the flags are wrong, and a mine is exploded. No effect if the game has
     * ended.
     * @param x
     * @param y
     * @return
     */
    public ProbeResult sweep(int x, int y) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (playerState.getSquareState(x, y) != SquareState.PROBED) {
            return ProbeResult.NOP;
        }

        if (playerState.getSquareMineCount(x, y) != nFlagsAround(x, y)) {
            return ProbeResult.NOP;
        }

        boolean hasLost = false;
        for (int nx = Math.max(0, x - 1); nx <= Math.min(x + 1, playerState.getWidth() - 1); nx++) {
            for (int ny = Math.max(0, y - 1); ny <= Math.min(y + 1, playerState.getHeight() - 1); ny++) {
                if (probe(nx, ny) == ProbeResult.LOSE) {
                    hasLost = true;
                }
            }
        }
        return hasLost ? ProbeResult.LOSE : ProbeResult.OK;
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
        if (playerState.getSquareState(x, y) == SquareState.UNKNOWN) {
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
        if (playerState.getSquareState(x, y) == SquareState.FLAGGED) {
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
        if (playerState.getSquareState(x, y) == SquareState.FLAGGED) {
            return unflag(x, y);
        } else if (playerState.getSquareState(x, y) == SquareState.UNKNOWN) {
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
        return playerState.getTotalMines();
    }

    public int getTotalMinesMinusFlags() {
        return playerState.getTotalMines() - numFlags;
    }

    public PlayerState clonePlayerState() {
        return playerState.copy();
    }

    public GameState getGameState() {
        if (numProbed == getWidth() * getHeight() - playerState.getTotalMines()) {
            _gameState = GameState.WIN;
        }
        return _gameState;
    }

    public SquareState getPlayerSquareState(int x, int y) {
        return playerState.getSquareState(x, y);
    }

    public int getProbedSquare(int x, int y) {
        if (playerState.getSquareState(x, y) == SquareState.PROBED) {
            return playerState.getSquareMineCount(x, y);
        } else {
            throw new IllegalArgumentException("Square is not probed yet");
        }
    }

    public String[] toStringArray() {
        String[] ret = new String[getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            ret[y] = "";
            for (int x = 0; x < getWidth(); x++) {
                switch (playerState.getSquareState(x, y)) {
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
                        ret[y] += (char)((int)'0' + playerState.getSquareMineCount(x, y));
                        break;
                }
            }
        }
        return ret;
    }

    public int getNumProbed() {
        return numProbed;
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
                if (playerState.getSquareState(nx,ny) == SquareState.FLAGGED) {
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

        ArrayList<MineLocation> warPlan = new ArrayList<>();

        // Create a list of candidate mines, and randomize them to create the minefield.
        for (int nx = 0; nx < getWidth(); nx++) {
            for (int ny = 0; ny < getHeight(); ny++) {
                if (nx < x-1 || nx > x+1 || ny < y-1 || ny > y+1) {
                    warPlan.add(new MineLocation(nx, ny));
                }
            }
        }
        Collections.shuffle(warPlan);

        ArrayList<MineLocation> warPlanBackup = new ArrayList<>();
        // This would only be used on a minefield so full that inital cascading is impossible.
        for (int nx = Math.max(0, x-1); nx <= Math.min(getWidth()-1, x+1); nx++) {
            for (int ny = Math.max(0, y-1); ny <= Math.min(getHeight()-1, y+1); ny++){
                if (nx != x || ny != y) {
                    warPlanBackup.add(new MineLocation(nx, ny));
                }
            }
        }
        Collections.shuffle(warPlanBackup);

        warPlan.addAll(warPlanBackup);
        warPlan.add(new MineLocation(x, y)); // This would only be used on a completely full minefield

        for (MineLocation location: warPlan.subList(0, playerState.getTotalMines())) {
            isMine[location.getX()][location.getY()] = true;
        }
    }

    private void loseGame() {
        _gameState = GameState.LOSE;
    }
}
