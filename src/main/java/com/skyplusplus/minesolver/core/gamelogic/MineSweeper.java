package com.skyplusplus.minesolver.core.gamelogic;


import java.util.*;

/* Class that implements the logic of the minesweeper game */
public class MineSweeper {

    private int numFlags;
    private int numSquaresExposed;
    private boolean waitingOnProbeToInitialize;
    private boolean isMine[][];
    private PlayerView playerView;
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
        playerView = new PlayerView(width, height, totalMines);
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
        this.playerView = new PlayerView(repr[0].length(), repr.length, totalMines);

        for (int y = 0; y < repr.length; y++) {
            for (int x = 0; x < repr[y].length(); x++) {
                switch (repr[y].charAt(x)) {
                    case 'x': // Wrong flag
                        playerView.setBoard(BoardCoord.ofValue(x, y), SquareState.FLAGGED);
                        break;
                    case 'X':
                        playerView.setBoard(BoardCoord.ofValue(x, y), SquareState.FLAGGED);
                        isMine[x][y] = true;
                        break;
                    case '#': // busted
                        playerView.setBoard(BoardCoord.ofValue(x, y), SquareState.MINE);
                        isMine[x][y] = true;
                        shouldLose = true;
                        break;
                    case '*':
                        isMine[x][y] = true;
                        break;
                    case ' ':
                        playerView.setBoard(BoardCoord.ofValue(x, y), SquareState.UNKNOWN);
                        break;
                    default:
                        if (repr[y].charAt(x) >= '0' && repr[y].charAt(x) <= '8') {
                            playerView.setBoard(BoardCoord.ofValue(x, y), repr[y].charAt(x) - '0');
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
    public ProbeResult probe(BoardCoord coord) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (waitingOnProbeToInitialize) {
            initializeMines(coord);
            waitingOnProbeToInitialize = false;
        }

        if (playerView.getSquareState(coord) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        } else if (isMine[coord.getX()][coord.getY()]) {
            playerView.setBoard(coord, SquareState.MINE);
            loseGame();
            return ProbeResult.LOSE;
        } else if (playerView.getSquareState(coord) == SquareState.FLAGGED){
            return ProbeResult.NOP;
        } else {
            return cascade(coord);
        }
    }

    /*
     * Internal probe-expand function, and assumes (x,y) is not a mine.
     */
    private ProbeResult cascade(BoardCoord coord) {
        if (playerView.getSquareState(coord) != SquareState.UNKNOWN) {
            return ProbeResult.NOP;
        }

        Queue<BoardCoord> dfs = new ArrayDeque<>();
        dfs.add(coord);
        while (!dfs.isEmpty()) {
            BoardCoord thisCoord = dfs.remove();

            if (playerView.getSquareState(thisCoord) == SquareState.UNKNOWN) {
                int squareNum = nMinesNeighbouring(thisCoord);
                numSquaresExposed++;
                playerView.setBoard(thisCoord, squareNum);

                if (squareNum == 0) {
                    dfs.addAll(playerView.getNeighbours(thisCoord));
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
    public ProbeResult sweep(BoardCoord coord) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return ProbeResult.NOP;
        }

        if (playerView.getSquareState(coord) != SquareState.PROBED) {
            return ProbeResult.NOP;
        }

        if (playerView.getSquareMineCount(coord) != nFlagsNeighbouring(coord)) {
            return ProbeResult.NOP;
        }

        boolean hasLost = false;
        for (BoardCoord l: playerView.getNeighbours(coord)) {
            if (probe(l) == ProbeResult.LOSE) {
                hasLost = true;
            }
        }
        return hasLost ? ProbeResult.LOSE : ProbeResult.OK;
    }

    /***
     * Flags a square, if unknown.
     */
    public FlagResult flag(BoardCoord coord) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerView.getSquareState(coord) == SquareState.UNKNOWN) {
            playerView.setBoard(coord, SquareState.FLAGGED);
            numFlags ++;
            return FlagResult.FLAGGED;
        } else {
            return FlagResult.NOP;
        }
    }

    /***
     * Unflags a square, if flagged.
     */
    public FlagResult unflag(BoardCoord coord) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerView.getSquareState(coord) == SquareState.FLAGGED) {
            playerView.setBoard(coord, SquareState.UNKNOWN);
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
    public FlagResult toggleFlag(BoardCoord coord) {
        if (getGameState() != GameState.IN_PROGRESS) {
            return FlagResult.NOP;
        }
        if (playerView.getSquareState(coord) == SquareState.FLAGGED) {
            return unflag(coord);
        } else if (playerView.getSquareState(coord) == SquareState.UNKNOWN) {
            return flag(coord);
        } else {
            return FlagResult.NOP;
        }
    }

    public int getHeight() {
        return playerView.getHeight();
    }

    public int getWidth() {
        return playerView.getWidth();
    }

    public int getTotalMines() {
        return playerView.getTotalMines();
    }

    /**
     * Note this does not check if the flags actually correspond to actual mines.
     */
    public int getTotalMinesMinusFlags() {
        return playerView.getTotalMines() - numFlags;
    }

    public PlayerView clonePlayerState() {
        return playerView.copy();
    }

    public List<BoardCoord> getAllSquares() {
        return playerView.getAllSquares();
    }

    public GameState getGameState() {
        if (numSquaresExposed == getWidth() * getHeight() - playerView.getTotalMines()
                && _gameState != GameState.LOSE) {
            _gameState = GameState.WIN;
        }
        return _gameState;
    }

    public SquareState getPlayerSquareState(BoardCoord coord) {
        return playerView.getSquareState(coord);
    }

    public int getProbedSquare(BoardCoord coord) {
        if (playerView.getSquareState(coord) == SquareState.PROBED) {
            return playerView.getSquareMineCount(coord);
        } else {
            throw new IllegalArgumentException("Square is not probed yet");
        }
    }

    public String[] toStringArray() {
        String[] ret = new String[getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            ret[y] = "";
            for (int x = 0; x < getWidth(); x++) {
                BoardCoord coord = BoardCoord.ofValue(x, y);
                switch (playerView.getSquareState(coord)) {
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
                        ret[y] += (char)((int)'0' + playerView.getSquareMineCount(coord));
                        break;
                }
            }
        }
        return ret;
    }

    public int getNumSquaresExposed() {
        return numSquaresExposed;
    }

    private int nMinesNeighbouring(BoardCoord coord) {
        int nMines = 0;
        for (BoardCoord l: playerView.getNeighbours(coord)) {
            if (isMine[l.getX()][l.getY()]) {
                nMines ++;
            }
        }
        return nMines;
    }

    private int nFlagsNeighbouring(BoardCoord coord) {
        return playerView.getNeighbours(coord, SquareState.FLAGGED).size();
    }


    /*
     * Creates random mines after the initial click and allows (x, y) to cascade, if possible.
     */
    private void initializeMines(BoardCoord startCoord) {
        ArrayList<BoardCoord> warPlan = new ArrayList<>();

        // Create a list of candidate mines, and randomize them to create the minefield.
        for (int nx = 0; nx < getWidth(); nx++) {
            for (int ny = 0; ny < getHeight(); ny++) {
                warPlan.add(BoardCoord.ofValue(nx, ny));
            }
        }
        warPlan.removeAll(playerView.getNeighbours(startCoord));
        warPlan.remove(startCoord);
        Collections.shuffle(warPlan);

        // If there are a lot of mines, we might have to use direct neighbours of the click as mines.
        List<BoardCoord> warPlanBackup = playerView.getNeighbours(startCoord);
        Collections.shuffle(warPlanBackup);
        warPlan.addAll(warPlanBackup);

        // User even this location itself if the minefield is completely full of mines.
        warPlan.add(startCoord);

        for (BoardCoord l: warPlan.subList(0, playerView.getTotalMines())) {
            isMine[l.getX()][l.getY()] = true;
        }
    }

    private void loseGame() {
        _gameState = GameState.LOSE;
    }
}
