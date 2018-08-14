package com.skyplusplus.minesolver.core.gamelogic;


import java.util.*;

/* Class that implements the logic of the minesweeper game */
public class MineSweeper {

    private int numFlags;
    private int numSquaresExposed;
    private boolean waitingOnProbeToInitialize;
    private boolean isMine[][];
    private PlayerState playerState;
    private GameState _gameState = GameState.IN_PROGRESS;

    public static class TooManyMinesException extends IllegalArgumentException {}

    /**
     * Initializes a minesweeper game which will create @param totalMines mines upon the first probe,
     * and tries to guarantee that the probe will cascade.
     * @param width number of columns
     * @param height number of rows
     * @param totalMines number of mines
     */
    public MineSweeper(int width, int height, int totalMines) {
        this(width, height);
        if (width * height < totalMines) {
            throw new TooManyMinesException();
        }
        if (totalMines < 0) {
            throw new IllegalArgumentException("Total mines must be >= 0");
        }
        playerState = new PlayerState(width, height, totalMines);
        waitingOnProbeToInitialize = true;
    }

    private MineSweeper(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Both width and height must be greater than 0");
        }
        this.isMine = new boolean[width][height];
    }

    /**
     * Creates a minefield specified by the graphical string. Note that invalid minefields will have undefined
     * behavior. Enter the minefield, as strings of equal length, one row at a time. The legend should be:
     *
     * ' ': unknown
     * '*': hidden mine
     * 'X': flag with mine underneath
     * 'x': flag without a mine
     * '#': Exploded mine (Game will be over immediately)
     * '0' to '8': An exposed minesweeper square.
     *
     * @param repr The array of strings representing the minefield.
     */
    public MineSweeper(String... repr) {
        this(repr[0].length(), repr.length);

        int totalMines = 0;
        boolean shouldLose = false;

        for (String s : repr) {
            for (int i = 0; i < s.length(); i++) {
                if ("X#*".contains(String.valueOf(s.charAt(i)))) {
                    totalMines ++;
                }
            }

        }
        this.playerState = new PlayerState(repr[0].length(), repr.length, totalMines);

        for (int y = 0; y < repr.length; y++) {
            for (int x = 0; x < repr[y].length(); x++) {
                switch (repr[y].charAt(x)) {
                    case 'x': // Wrong flag
                        playerState.setBoard(MineLocation.ofValue(x, y), SquareState.FLAGGED);
                        break;
                    case 'X':
                        playerState.setBoard(MineLocation.ofValue(x, y), SquareState.FLAGGED);
                        isMine[x][y] = true;
                        break;
                    case '#': // busted
                        playerState.setBoard(MineLocation.ofValue(x, y), SquareState.MINE);
                        isMine[x][y] = true;
                        shouldLose = true;
                        break;
                    case '*':
                        isMine[x][y] = true;
                        break;
                    case ' ':
                        playerState.setBoard(MineLocation.ofValue(x, y), SquareState.UNKNOWN);
                        break;
                    default:
                        if (repr[y].charAt(x) >= '0' && repr[y].charAt(x) <= '8') {
                            playerState.setBoard(MineLocation.ofValue(x, y), repr[y].charAt(x) - '0');
                            numSquaresExposed++;
                        } else {
                            throw new IllegalArgumentException("Invalid character: " + repr[y].charAt(x));
                        }
                }
            }
        }

        if (shouldLose) {
            loseGame();
        }
    }

    /***
     * Effect a probe on an unknown square. Does nothing on a flag or already uncovered square. Loses the game on a
     * mine. No effect when the game has ended.
     */
    public ProbeResult probe(MineLocation location) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (waitingOnProbeToInitialize) {
            initializeMines(location);
            waitingOnProbeToInitialize = false;
        }

        if (playerState.getSquareState(location) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        } else if (isMine[location.getX()][location.getY()]) {
            playerState.setBoard(location, SquareState.MINE);
            loseGame();
            return ProbeResult.LOSE;
        } else if (playerState.getSquareState(location) == SquareState.FLAGGED){
            return ProbeResult.NOP;
        } else {
            return cascade(location);
        }
    }

    /*
     * Internal probe-expand function, and assumes (x,y) is not a mine.
     */
    private ProbeResult cascade(MineLocation location) {
        if (playerState.getSquareState(location) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        }

        Queue<MineLocation> dfs = new ArrayDeque<>();
        dfs.add(location);
        while (!dfs.isEmpty()) {
            MineLocation thisLocation = dfs.remove();

            if (playerState.getSquareState(thisLocation) == SquareState.UNKNOWN) {
                int squareNum = nMinesNeighbouring(thisLocation);
                numSquaresExposed++;
                playerState.setBoard(thisLocation, squareNum);

                if (squareNum == 0) {
                    dfs.addAll(playerState.getNeighbours(thisLocation));
                }
            }
        }
        return ProbeResult.OK;
    }

    /***
     * Sweeps the perimeter when a user double clicks an uncovered square with the right number of flags around it.
     * Will lose the game if it happens that the flags are wrong, and a mine is exploded. No effect if the game has
     * ended.
     * @return result after probing all non-flagged squares around the target square
     */
    public ProbeResult sweep(MineLocation location) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (playerState.getSquareState(location) != SquareState.PROBED) {
            return ProbeResult.NOP;
        }

        if (playerState.getSquareMineCount(location) != nFlagsNeighbouring(location)) {
            return ProbeResult.NOP;
        }

        boolean hasLost = false;
        for (MineLocation l: playerState.getNeighbours(location)) {
            if (probe(l) == ProbeResult.LOSE) {
                hasLost = true;
            }
        }
        return hasLost ? ProbeResult.LOSE : ProbeResult.OK;
    }

    /***
     * Flags a square, if unknown.
     */
    public FlagResult flag(MineLocation location) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getSquareState(location) == SquareState.UNKNOWN) {
            playerState.setBoard(location, SquareState.FLAGGED);
            numFlags ++;
            return FlagResult.FLAGGED;
        } else {
            return FlagResult.NOP;
        }
    }

    /***
     * Unflags a square, if flagged.
     */
    public FlagResult unflag(MineLocation location) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getSquareState(location) == SquareState.FLAGGED) {
            playerState.setBoard(location, SquareState.UNKNOWN);
            numFlags --;
            return FlagResult.UNFLAGGED;
        } else {
            return FlagResult.NOP;
        }
    }

    /**
     * Unflags a flag, or flags an unknown square.
     */
    @SuppressWarnings("UnusedReturnValue")
    public FlagResult toggleFlag(MineLocation location) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerState.getSquareState(location) == SquareState.FLAGGED) {
            return unflag(location);
        } else if (playerState.getSquareState(location) == SquareState.UNKNOWN) {
            return flag(location);
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

    /**
     * Note this does not check if the flags actually correspond to actual mines.
     */
    public int getTotalMinesMinusFlags() {
        return playerState.getTotalMines() - numFlags;
    }

    public PlayerState clonePlayerState() {
        return playerState.copy();
    }

    public GameState getGameState() {
        if (numSquaresExposed == getWidth() * getHeight() - playerState.getTotalMines()
                && _gameState != GameState.LOSE) {
            _gameState = GameState.WIN;
        }
        return _gameState;
    }

    public SquareState getPlayerSquareState(MineLocation location) {
        return playerState.getSquareState(location);
    }

    public int getProbedSquare(MineLocation location) {
        if (playerState.getSquareState(location) == SquareState.PROBED) {
            return playerState.getSquareMineCount(location);
        } else {
            throw new IllegalArgumentException("Square is not probed yet");
        }
    }

    public String[] toStringArray() {
        String[] ret = new String[getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            ret[y] = "";
            for (int x = 0; x < getWidth(); x++) {
                MineLocation location = MineLocation.ofValue(x, y);
                switch (playerState.getSquareState(location)) {
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
                        ret[y] += (char)((int)'0' + playerState.getSquareMineCount(location));
                        break;
                }
            }
        }
        return ret;
    }

    public int getNumSquaresExposed() {
        return numSquaresExposed;
    }

    private int nMinesNeighbouring(MineLocation location) {
        int nMines = 0;
        for (MineLocation l: playerState.getNeighbours(location)) {
            if (isMine[l.getX()][l.getY()]) {
                nMines ++;
            }
        }
        return nMines;
    }

    private int nFlagsNeighbouring(MineLocation location) {
        return playerState.getNeighbours(location, SquareState.FLAGGED).size();
    }


    /*
     * Creates random mines after the initial click and allows (x, y) to cascade, if possible.
     */
    private void initializeMines(MineLocation startLocation) {
        ArrayList<MineLocation> warPlan = new ArrayList<>();

        // Create a list of candidate mines, and randomize them to create the minefield.
        for (int nx = 0; nx < getWidth(); nx++) {
            for (int ny = 0; ny < getHeight(); ny++) {
                warPlan.add(MineLocation.ofValue(nx, ny));
            }
        }
        warPlan.removeAll(playerState.getNeighbours(startLocation));
        warPlan.remove(startLocation);
        Collections.shuffle(warPlan);

        // If there are a lot of mines, we might have to use direct neighbours of the click as mines.
        List<MineLocation> warPlanBackup = playerState.getNeighbours(startLocation);
        Collections.shuffle(warPlanBackup);
        warPlan.addAll(warPlanBackup);

        // User even this location itself if the minefield is completely full of mines.
        warPlan.add(startLocation);

        for (MineLocation l: warPlan.subList(0, playerState.getTotalMines())) {
            isMine[l.getX()][l.getY()] = true;
        }
    }

    private void loseGame() {
        _gameState = GameState.LOSE;
    }
}
