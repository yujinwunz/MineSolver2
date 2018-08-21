package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.*;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.util.*;
import java.util.function.Consumer;

/**
 * A standard backtracking AI. Will not be extended, instead will be used for benchmarking
 * against the full AI.
 */
public class BackTrackAI extends MineSweeperAI {

    private int bestIndex = -1;
    private boolean[] bestIsMine;
    private int iterations;

    @Override
    public Move calculate(PlayerView view) {

        List<BoardCoord> candidates = getNeighboursOfVisibleNumbers(view);

        // Build probability table, indicating the number of solutions with (x, y) being a mine.
        int[][] numWaysToBeMine = new int[view.getWidth()][view.getHeight()];

        int totalSolutions;
        try {
            totalSolutions = findCombinationsOfMines(view, candidates, (isMine) -> {
                for (int i = 0; i < candidates.size(); i++) {
                    int x = candidates.get(i).getX();
                    int y = candidates.get(i).getY();
                    if (isMine[i]) {
                        numWaysToBeMine[x][y]++;
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
        List<BoardCoord> toFlag = squaresWithNumWays(numWaysToBeMine, totalSolutions);
        List<BoardCoord> toProbe = squaresWithNumWays(numWaysToBeMine, 0);
        toProbe.retainAll(candidates);

        if (toProbe.size() == 0 && candidates.size() > 0) {
            // Couldn't find a 100% solution. Return the next best thing instead, but only one of them.
            toProbe.add(getMinNonZeroSquare(numWaysToBeMine));
        } else if (candidates.size() == 0) {
            List<BoardCoord> unknowns = view.getAllSquares(SquareState.UNKNOWN);

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

    private static BoardCoord getMinNonZeroSquare(int[][] squareValues) {
        int minScore = Integer.MAX_VALUE;
        BoardCoord minVal = null;
        for (int x = 0; x < squareValues.length; x++) {
            for (int y = 0; y < squareValues[x].length; y++) {
                if (squareValues[x][y] < minScore) {
                    if (squareValues[x][y] != 0) {
                        minScore = squareValues[x][y];
                        minVal = BoardCoord.ofValue(x, y);
                    }
                }
            }
        }
        return minVal;
    }

    private static List<BoardCoord> squaresWithNumWays(int[][] solution, int ways) {
        ArrayList<BoardCoord> retVal = new ArrayList<>();
        for (int x = 0; x < solution.length; x++) {
            for (int y = 0; y < solution[x].length; y++) {
                if (solution[x][y] == ways) {
                    retVal.add(BoardCoord.ofValue(x, y));
                }
            }
        }
        return retVal;
    }

    int findCombinationsOfMines(
            PlayerView view,
            List<BoardCoord> variables,
            Consumer<boolean[]> onSolutionFound
    ) throws InterruptedException {

        boolean isMine[] = new boolean[variables.size()];
        int[][] numNeighbourUnknowns = new int[view.getWidth()][view.getHeight()];
        int[][] numNeighbourMines = new int[view.getWidth()][view.getHeight()];
        boolean isProbed[][] = new boolean[view.getWidth()][view.getHeight()];

        for (BoardCoord l : view.getAllSquares()) {
            if (view.getSquareState(l) == SquareState.PROBED) {
                numNeighbourMines[l.getX()][l.getY()] =
                        view.getSquareMineCount(l)
                                - view.getNeighbours(l, SquareState.FLAGGED).size();
                isProbed[l.getX()][l.getY()] = true;
            }
            numNeighbourUnknowns[l.getX()][l.getY()] = view.getNeighbours(l, SquareState.UNKNOWN).size();
        }

        return backtrackForSolutions(numNeighbourMines, numNeighbourUnknowns, isProbed, variables, isMine, 0,
                () -> onSolutionFound.accept(isMine));
    }

    private int backtrackForSolutions(
            int[][] numNeighbourMines,
            int[][] numNeighbourUnknowns,
            boolean[][] isProbed,
            List<BoardCoord> variables,
            boolean[] isMine,
            int index,
            Runnable onSolutionFound
    ) throws InterruptedException {
        reportProgress(variables, isMine, index);
        if (index == variables.size()) {
            // The DP has reached the end. If the board is in a finished state, assume the current configuration as
            // one possible solution
            for (BoardCoord candidate : variables) {
                for (BoardCoord n : candidate.getNeighbours(isProbed.length, isProbed[0].length)) {
                    if (isProbed[n.getX()][n.getY()] && numNeighbourMines[n.getX()][n.getY()] > 0) {
                        return 0;
                    }
                }
            }

            onSolutionFound.run();

            return 1;
        } else {
            int nSolutions = 0;
            BoardCoord thisSquare = variables.get(index);
            int width = numNeighbourMines.length;
            int height = numNeighbourMines[0].length;

            for (boolean thisIsMine : Arrays.asList(false, true)) {
                boolean valid = true;
                for (BoardCoord l : thisSquare.getNeighbours(width, height)) {
                    int x = l.getX();
                    int y = l.getY();
                    if (thisIsMine && isProbed[x][y]) {
                        numNeighbourMines[x][y]--;
                    } else if (!thisIsMine) {
                        numNeighbourUnknowns[x][y]--;
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
                            index + 1,
                            onSolutionFound
                    );
                }

                for (BoardCoord l : thisSquare.getNeighbours(width, height)) {
                    if (thisIsMine && isProbed[l.getX()][l.getY()]) {
                        numNeighbourMines[l.getX()][l.getY()]++;
                    } else if (!thisIsMine) {
                        numNeighbourUnknowns[l.getX()][l.getY()]++;
                    }
                }
            }

            return nSolutions;
        }
    }

    private void reportProgress(List<BoardCoord> candidates, boolean[] isMine, int index) throws InterruptedException {
        iterations++;
        if (index > bestIndex) {
            bestIsMine = Arrays.copyOf(isMine, isMine.length);
            bestIndex = index;
        }

        reportProgress(() -> {
            ArrayList<BoardUpdateEntry> updates = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                if (i < bestIndex) {
                    if (bestIsMine[i]) {
                        updates.add(new BoardUpdateEntry(candidates.get(i), UpdateColor.RED, Integer.toString(i)));
                    } else {
                        updates.add(new BoardUpdateEntry(candidates.get(i), UpdateColor.GREEN, Integer.toString(i)));
                    }
                } else {
                    updates.add(new BoardUpdateEntry(candidates.get(i), UpdateColor.GRAY, Integer.toString(i)));
                }
            }

            bestIndex = -1;
            return new BoardUpdate(updates, "Iterations: " + iterations);
        });
    }

    // Visible for testing only
    public static List<BoardCoord> getNeighboursOfVisibleNumbers(PlayerView view) {
        List<BoardCoord> candidates = new ArrayList<>();
        boolean[][] seen = new boolean[view.getWidth()][view.getHeight()];
        for (int x = 0; x < view.getWidth(); x++) {
            for (int y = 0; y < view.getHeight(); y++) {
                Stack<BoardCoord> bfs = new Stack<>();
                bfs.add(BoardCoord.ofValue(x, y));

                while (!bfs.isEmpty()) {
                    BoardCoord thisCoord = bfs.pop();
                    int nx = thisCoord.getX();
                    int ny = thisCoord.getY();

                    if (!seen[nx][ny]
                            && view.getSquareState(thisCoord) == SquareState.UNKNOWN
                            && view.getNeighbours(thisCoord, SquareState.PROBED).size() > 0
                    ) {
                        seen[nx][ny] = true;
                        candidates.add(thisCoord);
                        bfs.addAll(view.getNeighbours(thisCoord));
                    }
                }
            }
        }
        // Return the list in some kind of board-DFS order to make backtracking fast

        return candidates;
    }

    @Override
    public String toString() {
        return "Backtrack AI";
    }
}
