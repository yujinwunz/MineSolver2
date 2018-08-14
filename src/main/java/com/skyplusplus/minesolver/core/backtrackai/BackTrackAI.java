package com.skyplusplus.minesolver.core.backtrackai;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateEventEntry;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigInteger;
import java.util.*;

/**
 * A standard backtracking AI. Will not be extended, instead will be used for benchmarking
 * against the full AI.
 */
public class BackTrackAI implements MineSweeperAI {

    private UpdateHandler updateHandler;

    @Override
    public Move calculate(PlayerState state) {
        return calculate(state, null);
    }

    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        this.updateHandler = handler;

        List<MineLocation> candidates = getNeighboursOfVisibleNumbers(state);

        // Build probability table, indicating the number of solutions with (x, y) being a mine.
        BigInteger[][] solution = new BigInteger[state.getWidth()][state.getHeight()];
        for (BigInteger[] array: solution) {
            Arrays.fill(array, BigInteger.ZERO);
        }

        BigInteger totalSolutions;
        try {
            totalSolutions = findCombinationsOfMines(state, candidates, solution);
        } catch (InterruptedException ex) {
            return new Move(null, null);
        }

        if (totalSolutions.equals(BigInteger.ZERO)) {
            return new Move(null, null);
        }

        // Generate the move according to the probability table.
        // 1. flag anything that's 100%.
        // 2. click anything that is safe, ie. 0%.
        // 3. click square with least probability if no safe squares are available.
        // TODO: 3. is very incomplete. Next stage of the AI is to find a better method of picking an uncertain square.
        List<MineLocation> toFlag = squaresWithNumWays(solution, totalSolutions);
        List<MineLocation> toProbe = squaresWithNumWays(solution, BigInteger.ZERO);
        toProbe.retainAll(candidates);

        if (toProbe.size() == 0 && candidates.size() > 0) {
            // Couldn't find a 100% solution. Return the next best thing instead, but only one of them.
            toProbe.add(getMinNonZeroSquare(solution));
        } else if (candidates.size() == 0) {
            // We are either just starting, or the game is won. Just return the center either way.
            return new Move(
                    Collections.singletonList(MineLocation.ofValue(state.getWidth()/2, state.getHeight()/2)),
                    null
            );
        }
        return new Move(toProbe, toFlag);
    }

    private static MineLocation getMinNonZeroSquare(BigInteger[][] squareValues) {
        BigInteger minScore = null;
        MineLocation minVal = null;
        for (int x = 0; x < squareValues.length; x++) {
            for (int y = 0; y < squareValues[x].length; y++) {
                if (minScore == null || squareValues[x][y].compareTo(minScore) < 0) {
                    if (!squareValues[x][y].equals(BigInteger.ZERO)) {
                        minScore = squareValues[x][y];
                        minVal = MineLocation.ofValue(x, y);
                    }
                }
            }
        }
        return minVal;
    }

    private static List<MineLocation> squaresWithNumWays(BigInteger[][] solution, BigInteger ways) {
        ArrayList<MineLocation> retVal = new ArrayList<>();
        for (int x = 0; x < solution.length; x++) {
            for (int y = 0; y < solution[x].length; y++) {
                if (solution[x][y].equals(ways)) {
                    retVal.add(MineLocation.ofValue(x, y));
                }
            }
        }
        return retVal;
    }

    private BigInteger findCombinationsOfMines(
            PlayerState state,
            List<MineLocation> candidates,
            BigInteger[][] solution
    ) throws InterruptedException {

        boolean isMine[] = new boolean[candidates.size()];
        int[][] numNeighbouringUnknowns = new int[state.getWidth()][state.getHeight()];
        int[][] numNeighbouringMines = new int[state.getWidth()][state.getHeight()];
        boolean isProbed[][] = new boolean[state.getWidth()][state.getHeight()];

        for (int x = 0; x < state.getWidth(); x++) {
            for (int y = 0; y < state.getHeight(); y++) {
                MineLocation location = MineLocation.ofValue(x, y);
                if (state.getSquareState(location) == SquareState.PROBED) {
                    numNeighbouringMines[x][y] =
                            state.getSquareMineCount(location)
                            - state.getNeighbours(location, SquareState.FLAGGED).size();
                    isProbed[x][y] = true;
                }
                numNeighbouringUnknowns[x][y] = state.getNeighbours(location, SquareState.UNKNOWN).size();
            }
        }

        return _findCombo(numNeighbouringMines, numNeighbouringUnknowns, isProbed, candidates, isMine, solution, 0);
    }

    private BigInteger _findCombo(
            int[][] numNeighbouringMines,
            int[][] numNeighbouringUnknowns,
            boolean[][] isProbed,
            List<MineLocation> candidates,
            boolean[] isMine, BigInteger[][] solution,
            int index
    ) throws InterruptedException {
        reportProgress(candidates, isMine, index);
        if (index == candidates.size()) {
            // The DP has reached the end. If the board is in a finished state, assume the current configuration as
            // one possible solution
            for (int x = 0; x < isProbed.length; x++) {
                for (int y = 0; y < isProbed[x].length; y++) {
                    if (isProbed[x][y] && numNeighbouringMines[x][y] > 0) {
                        return BigInteger.ZERO;
                    }
                }
            }

            // Checks out. record this as one of the solutions.
            for (int i = 0; i < candidates.size(); i++) {
                int x = candidates.get(i).getX();
                int y = candidates.get(i).getY();
                if (isMine[i]) {
                    solution[x][y] = solution[x][y].add(BigInteger.ONE);
                }
            }
            return BigInteger.ONE;
        } else {
            BigInteger nSolutions = BigInteger.ZERO;
            MineLocation thisSquare = candidates.get(index);
            int width = numNeighbouringMines.length;
            int height = numNeighbouringMines[0].length;

            for (boolean thisIsMine: Arrays.asList(false, true)) {
                boolean valid = true;
                for (MineLocation l: thisSquare.getNeighbours(width, height)) {
                    int x = l.getX();
                    int y = l.getY();
                    if (thisIsMine && isProbed[x][y]) {
                        numNeighbouringMines[x][y]--;
                    } else if (!thisIsMine){
                        numNeighbouringUnknowns[x][y] --;
                    }
                    if ((numNeighbouringMines[x][y] < 0 && isProbed[x][y])
                            || numNeighbouringUnknowns[x][y] < numNeighbouringMines[x][y]) {
                        valid = false;
                    }
                }

                isMine[index] = thisIsMine;
                if (valid) {
                    nSolutions = nSolutions.add(_findCombo(
                            numNeighbouringMines,
                            numNeighbouringUnknowns,
                            isProbed, candidates,
                            isMine, solution,
                            index+1)
                    );
                }

                for (MineLocation l: thisSquare.getNeighbours(width, height)) {
                    if (thisIsMine && isProbed[l.getX()][l.getY()]) {
                        numNeighbouringMines[l.getX()][l.getY()] ++;
                    } else if (!thisIsMine) {
                        numNeighbouringUnknowns[l.getX()][l.getY()] ++;
                    }
                }
            }

            return nSolutions;
        }
    }

    private long lastProgressReport = 0;
    private int bestIndex = -1;
    private boolean[] bestIsMine;
    /**
     * Rate limits the output of backtracking progress.
     * Shows the furthest progress since last time.
     * @param candidates list of candidate squares for exploration
     * @param isMine array of assumptions we have made for each square
     * @param index how far into the candidate list we have made assumptions for
     */
    private void reportProgress(List<MineLocation> candidates, boolean[] isMine, int index) throws InterruptedException {
        if (updateHandler == null) return;
        if (index > bestIndex) {
            bestIsMine = Arrays.copyOf(isMine, isMine.length);
            bestIndex = index;
        }
        if (System.currentTimeMillis() - lastProgressReport < 30) return;
        lastProgressReport = System.currentTimeMillis();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        ArrayList<UpdateEventEntry> updates = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            if (i < bestIndex) {
                if (bestIsMine[i]) {
                    updates.add(new UpdateEventEntry(candidates.get(i), "Mine", i));
                } else {
                    updates.add(new UpdateEventEntry(candidates.get(i), "Safe", i));
                }
            } else {
                updates.add(new UpdateEventEntry(candidates.get(i), "Candidate", i));
            }
        }

        bestIndex = -1;
        updateHandler.handleUpdate(updates);
    }

    // Visible for testing only
    public static List<MineLocation> getNeighboursOfVisibleNumbers(PlayerState state) {
        List<MineLocation> candidates = new ArrayList<>();
        boolean[][] seen = new boolean[state.getWidth()][state.getHeight()];
        for (int x = 0; x < state.getWidth(); x++) {
            for (int y = 0; y < state.getHeight(); y++) {
                Stack<MineLocation> bfs = new Stack<>();
                bfs.add(MineLocation.ofValue(x, y));

                while (!bfs.isEmpty()) {
                    MineLocation thisLocation = bfs.pop();
                    int nx = thisLocation.getX();
                    int ny = thisLocation.getY();

                    if (!seen[nx][ny]
                            && state.getSquareState(thisLocation) == SquareState.UNKNOWN
                            && state.getNeighbours(thisLocation, SquareState.PROBED).size() > 0
                    ) {
                        seen[nx][ny] = true;
                        candidates.add(thisLocation);
                        bfs.addAll(state.getNeighbours(thisLocation));
                    }
                }
            }
        }
        // Return the list in some kind of board-DFS order to make backtracking fast

        return candidates;
    }
}
