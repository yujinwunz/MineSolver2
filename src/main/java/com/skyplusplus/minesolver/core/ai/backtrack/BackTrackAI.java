package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.*;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.util.*;
import java.util.function.Consumer;

/**
 * A standard backtracking AI. Will not be extended, instead will be used for benchmarking
 * against the full AI.
 */
public class BackTrackAI implements MineSweeperAI {

    protected UpdateHandler updateHandler;
    private static final int REPORT_MIN_TIME_MS = 100;

    @Override
    public Move calculate(PlayerState state) {
        return calculate(state, null);
    }

    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        prepareStatsHandler(handler);

        List<MineLocation> candidates = getNeighboursOfVisibleNumbers(state);

        // Build probability table, indicating the number of solutions with (x, y) being a mine.
        int[][] numWaysToBeMine = new int[state.getWidth()][state.getHeight()];

        int totalSolutions;
        try {
            totalSolutions = findCombinationsOfMines(state, candidates, (isMine) -> {
                for (int i = 0; i < candidates.size(); i++) {
                    int x = candidates.get(i).getX();
                    int y = candidates.get(i).getY();
                    if (isMine[i]) {
                        numWaysToBeMine[x][y] ++;
                    }
                }
            });
        } catch (InterruptedException ex) {
            return new Move(null, null);
        }

        if (totalSolutions == 0) {
            return new Move(null, null);
        }

        // Generate the move according to the probability table.
        // 1. flag anything that's 100%.
        // 2. click anything that is safe, ie. 0%.
        // 3. click square with least probability if no safe squares are available.
        // TODO: 3. is very incomplete. Next stage of the AI is to find a better method of picking an uncertain square.
        List<MineLocation> toFlag = squaresWithNumWays(numWaysToBeMine, totalSolutions);
        List<MineLocation> toProbe = squaresWithNumWays(numWaysToBeMine, 0);
        toProbe.retainAll(candidates);

        if (toProbe.size() == 0 && candidates.size() > 0) {
            // Couldn't find a 100% solution. Return the next best thing instead, but only one of them.
            toProbe.add(getMinNonZeroSquare(numWaysToBeMine));
        } else if (candidates.size() == 0) {
            List<MineLocation> unknowns = state.getAllSquares(SquareState.UNKNOWN);

            if (!unknowns.isEmpty()) {
                // We are either just starting, or all unknowns squares have only flag neighbours.
                // Just return a blank square.
                return new Move(
                        Collections.singletonList(unknowns.get(new Random().nextInt(unknowns.size()))),
                        null
                );
            }
        }
        return new Move(toProbe, toFlag);
    }

    protected void prepareStatsHandler(UpdateHandler handler) {
        this.updateHandler = handler;
        this.iterations = 0;
    }

    protected static MineLocation getMinNonZeroSquare(int[][] squareValues) {
        int minScore = Integer.MAX_VALUE;
        MineLocation minVal = null;
        for (int x = 0; x < squareValues.length; x++) {
            for (int y = 0; y < squareValues[x].length; y++) {
                if (squareValues[x][y] < minScore) {
                    if (squareValues[x][y] != 0) {
                        minScore = squareValues[x][y];
                        minVal = MineLocation.ofValue(x, y);
                    }
                }
            }
        }
        return minVal;
    }

    protected static List<MineLocation> squaresWithNumWays(int[][] solution, int ways) {
        ArrayList<MineLocation> retVal = new ArrayList<>();
        for (int x = 0; x < solution.length; x++) {
            for (int y = 0; y < solution[x].length; y++) {
                if (solution[x][y] == ways) {
                    retVal.add(MineLocation.ofValue(x, y));
                }
            }
        }
        return retVal;
    }

    protected int findCombinationsOfMines(
            PlayerState state,
            List<MineLocation> variables,
            Consumer<boolean[]> onSolutionFound
    ) throws InterruptedException {

        boolean isMine[] = new boolean[variables.size()];
        int[][] numNeighbourUnknowns = new int[state.getWidth()][state.getHeight()];
        int[][] numNeighbourMines = new int[state.getWidth()][state.getHeight()];
        boolean isProbed[][] = new boolean[state.getWidth()][state.getHeight()];

        for (MineLocation l: state.getAllSquares()) {
            if (state.getSquareState(l) == SquareState.PROBED) {
                numNeighbourMines[l.getX()][l.getY()] =
                        state.getSquareMineCount(l)
                                - state.getNeighbours(l, SquareState.FLAGGED).size();
                isProbed[l.getX()][l.getY()] = true;
            }
            numNeighbourUnknowns[l.getX()][l.getY()] = state.getNeighbours(l, SquareState.UNKNOWN).size();
        }

        return backtrackForSolutions(numNeighbourMines, numNeighbourUnknowns, isProbed, variables, isMine, 0, () -> {
            onSolutionFound.accept(isMine);
        });
    }

    protected int backtrackForSolutions(
            int[][] numNeighbourMines,
            int[][] numNeighbourUnknowns,
            boolean[][] isProbed,
            List<MineLocation> variables,
            boolean[] isMine,
            int index,
            Runnable onSolutionFound
    ) throws InterruptedException {
        reportProgress(variables, isMine, index);
        if (index == variables.size()) {
            // The DP has reached the end. If the board is in a finished state, assume the current configuration as
            // one possible solution
            for (MineLocation candidate: variables) {
                for (MineLocation n: candidate.getNeighbours(isProbed.length, isProbed[0].length)) {
                    if (isProbed[n.getX()][n.getY()] && numNeighbourMines[n.getX()][n.getY()] > 0) {
                        return 0;
                    }
                }
            }

            onSolutionFound.run();

            return 1;
        } else {
            int nSolutions = 0;
            MineLocation thisSquare = variables.get(index);
            int width = numNeighbourMines.length;
            int height = numNeighbourMines[0].length;

            for (boolean thisIsMine: Arrays.asList(false, true)) {
                boolean valid = true;
                for (MineLocation l: thisSquare.getNeighbours(width, height)) {
                    int x = l.getX();
                    int y = l.getY();
                    if (thisIsMine && isProbed[x][y]) {
                        numNeighbourMines[x][y]--;
                    } else if (!thisIsMine){
                        numNeighbourUnknowns[x][y] --;
                    }
                    if ((numNeighbourMines[x][y] < 0 && isProbed[x][y])
                            || numNeighbourUnknowns[x][y] < numNeighbourMines[x][y]) {
                        valid = false;
                    }
                }

                isMine[index] = thisIsMine;
                if (valid) {
                    nSolutions += backtrackForSolutions(
                            numNeighbourMines,
                            numNeighbourUnknowns,
                            isProbed, variables,
                            isMine,
                            index+1,
                            onSolutionFound
                    );
                }

                for (MineLocation l: thisSquare.getNeighbours(width, height)) {
                    if (thisIsMine && isProbed[l.getX()][l.getY()]) {
                        numNeighbourMines[l.getX()][l.getY()] ++;
                    } else if (!thisIsMine) {
                        numNeighbourUnknowns[l.getX()][l.getY()] ++;
                    }
                }
            }

            return nSolutions;
        }
    }

    private long lastProgressReport = 0;
    private int bestIndex = -1;
    private boolean[] bestIsMine;
    private int iterations;
    /**
     * Rate limits the output of backtracking progress.
     * Shows the furthest progress since last time.
     * @param candidates list of candidate squares for exploration
     * @param isMine array of assumptions we have made for each square
     * @param index how far into the candidate list we have made assumptions for
     */
    protected void reportProgress(List<MineLocation> candidates, boolean[] isMine, int index) throws InterruptedException {
        iterations ++;
        if (updateHandler == null) return;
        if (index > bestIndex) {
            bestIsMine = Arrays.copyOf(isMine, isMine.length);
            bestIndex = index;
        }
        if (System.currentTimeMillis() - lastProgressReport < REPORT_MIN_TIME_MS) return;
        lastProgressReport = System.currentTimeMillis();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        ArrayList<UpdateEventEntry> updates = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
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
        updateHandler.handleUpdate(new UpdateEvent(updates, "Iterations: " + iterations));
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
