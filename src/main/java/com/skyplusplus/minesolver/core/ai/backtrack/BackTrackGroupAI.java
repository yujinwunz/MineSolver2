package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.util.*;

public class BackTrackGroupAI extends BackTrackAI {
    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        prepareStatsHandler(handler);

        List<List<MineLocation>> candidateGroups = getNeighboursOfVisibleNumbersGroups(state);

        // Generate the move according to the probability table.
        // 1. flag anything that's 100%.
        // 2. click anything that is safe, ie. 0%.
        // 3. click square with least probability if no safe squares are available.
        // TODO: 3. is very incomplete. Next stage of the AI is to find a better method of picking an uncertain square.
        List<MineLocation> toFlag = new ArrayList<>();
        List<MineLocation> toProbe = new ArrayList<>();

        double bestProbableScore = 1;
        MineLocation bestProbableSquare = null;

        for (List<MineLocation> candidates: candidateGroups) {
            // Build probability table, indicating the number of solutions with (x, y) being a mine.
            int[][] numWaysToBeMine = new int[state.getWidth()][state.getHeight()];

            int totalSolutions;
            try {
                totalSolutions = findCombinationsOfMines(state, candidates, (isMine) -> {
                    // Checks out. record this as one of the solutions.
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

            List<MineLocation> thisToFlag = squaresWithNumWays(numWaysToBeMine, totalSolutions);
            List<MineLocation> thisToProbe = squaresWithNumWays(numWaysToBeMine, 0);

            thisToProbe.retainAll(candidates);

            if (thisToProbe.isEmpty()) {
                MineLocation trySquare = getMinNonZeroSquare(numWaysToBeMine);
                if (numWaysToBeMine[trySquare.getX()][trySquare.getY()] / (double) totalSolutions < bestProbableScore) {
                    bestProbableScore = numWaysToBeMine[trySquare.getX()][trySquare.getY()] / (double) totalSolutions;
                    bestProbableSquare = trySquare;
                }
            }

            toFlag.addAll(thisToFlag);
            toProbe.addAll(thisToProbe);
        }

        if (toProbe.size() == 0 && candidateGroups.size() > 0) {
            // Couldn't find a 100% solution. Return the next best thing instead, but only one of them.
            toProbe.add(bestProbableSquare);
        } else if (candidateGroups.size() == 0) {
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


    static List<List<MineLocation>> getNeighboursOfVisibleNumbersGroups(PlayerState state) {
        List<List<MineLocation>> candidateGroups = new ArrayList<>();
        boolean[][] seen = new boolean[state.getWidth()][state.getHeight()];

        for (MineLocation l: state.getAllSquares()) {
            List<MineLocation> candidates = new ArrayList<>();
            Stack<MineLocation> dfsProbed = new Stack<>();

            if (!seen[l.getX()][l.getY()]
                    && state.getSquareState(l) == SquareState.UNKNOWN
                    && state.getNeighbours(l, SquareState.PROBED).size() > 0
            ) {
                dfsProbed.add(l);
                candidates.add(l);
                seen[l.getX()][l.getY()] = true;
            }

            while (!dfsProbed.isEmpty()) {
                MineLocation thisLocation = dfsProbed.pop();

                for (MineLocation neighbour : state.getNeighbours(thisLocation, SquareState.PROBED)) {
                    for (MineLocation candidate : state.getNeighbours(neighbour, SquareState.UNKNOWN)) {
                        if (!seen[candidate.getX()][candidate.getY()]) {
                            candidates.add(candidate);
                            dfsProbed.add(candidate);
                        }
                        seen[candidate.getX()][candidate.getY()] = true;
                    }
                }
            }

            if (!candidates.isEmpty()) {
                candidateGroups.add(candidates);
            }
        }
        // Return the list in some kind of board-DFS order to make backtracking fast

        return candidateGroups;
    }
}
