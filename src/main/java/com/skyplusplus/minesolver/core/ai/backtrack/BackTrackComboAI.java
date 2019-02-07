package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * So this one's supposed to do grouping, and then correct combinatorics on the resulting groups
 * to take into account the effect of remaining mines.
 */

public class BackTrackComboAI extends BackTrackAI {
    private static final MathContext MATH_CONTEXT = new MathContext(36, RoundingMode.HALF_DOWN);

    @Override
    public Move calculate(PlayerView view) {
        List<List<BoardCoord>> candidateGroups = getGroupsOfBorders(view);

        List<BoardCoord> unconstrainedSquares = view.getAllSquares(SquareState.UNKNOWN);
        int availableMines = view.getTotalMines() - view.getAllSquares(SquareState.FLAGGED).size();
        candidateGroups.forEach(unconstrainedSquares::removeAll);

        // If we just started, hit (2, 2) if possible.
        if (unconstrainedSquares.size() == view.getWidth() * view.getHeight() && view.getWidth() >= 5 && view.getHeight() >= 5) {
            return new Move(Collections.singletonList(BoardCoord.ofValue(2, 2)), null);
        }

        // Generate the move according to the probability table.
        // 1. flag anything that's 100%.
        // 2. click anything that is safe, ie. 0%.
        // 3. naively click square with least probability if no safe squares are available.
        List<BoardCoord> toFlag = new ArrayList<>();
        List<BoardCoord> toProbe = new ArrayList<>();

        List<GroupResult> groupResults;
        try {
            groupResults = processGroups(view, candidateGroups);
        } catch (InterruptedException e) {
            reportProgressImmediate(new BoardUpdate(Collections.emptyList(), "Processing was interrupted"));
            return new Move(null, null);
        }


        // For each group, we have to decide. How are its probabilities affected by its number of mines?
        // To answer that question, we have to think. With (t) total mines and (n) mines used in this group, how
        // many ways can the remaining (t-n) be distributed validly among all other groups *and* the empty space?
        // Compare that for each (n) value in the group, and you have weights to apply to each solution. Take the
        // weighted average for each square and you have accurate probabilities.
        double[][] probIsMine = calculateMineProbabilities(groupResults, view.getWidth(), view.getHeight(),
                unconstrainedSquares.size(), availableMines, toFlag::add, toProbe::add);

        BigDecimal combosWithMine = getCombosByMineCount(groupResults, -1, unconstrainedSquares.size() - 1,
                availableMines - 1, null);
        BigDecimal combosWithoutMine = getCombosByMineCount(groupResults, -1, unconstrainedSquares.size() - 1,
                availableMines, null);

        if (combosWithMine.equals(BigDecimal.ZERO)) {
            toProbe.addAll(unconstrainedSquares);
        }

        if (!toProbe.isEmpty()) {
            return new Move(toProbe, toFlag);
        }

        // No sure-fire squares. Naively use least likely square instead, starting with any unconstrained square.
        BoardCoord bestCoord = getLeastLikelyCoordinate(probIsMine, combosWithMine, combosWithoutMine,
                unconstrainedSquares, candidateGroups);

        return new Move(Collections.singletonList(bestCoord), toFlag);
    }

    /**
     * Gets a list of all coordinates that are unknown and neighbouring a number. Grouped into components connected
     * by each other or common neighbouring numbers, effectively "interacting" groups. Squares from different
     * groups will only interact indirectly by affecting the total number of mines on the board.
     *
     * @param view player view state
     * @return list of groups
     */
    protected static List<List<BoardCoord>> getGroupsOfBorders(PlayerView view) {

        List<List<BoardCoord>> candidateGroups = new ArrayList<>();
        boolean[][] seen = new boolean[view.getWidth()][view.getHeight()];

        for (BoardCoord l : view.getAllSquares()) {
            List<BoardCoord> candidates = new ArrayList<>();
            Stack<BoardCoord> dfsProbed = new Stack<>();

            if (!seen[l.getX()][l.getY()]
                    && view.getSquareState(l) == SquareState.UNKNOWN
                    && view.getNeighbours(l, SquareState.PROBED).size() > 0
            ) {
                dfsProbed.add(l);
                candidates.add(l);
                seen[l.getX()][l.getY()] = true;
            }

            while (!dfsProbed.isEmpty()) {
                BoardCoord thisCoord = dfsProbed.pop();

                for (BoardCoord neighbour : view.getNeighbours(thisCoord, SquareState.PROBED)) {
                    for (BoardCoord candidate : view.getNeighbours(neighbour, SquareState.UNKNOWN)) {
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

        return candidateGroups;
    }

    private BoardCoord getLeastLikelyCoordinate(
            double[][] probIsMine,
            BigDecimal combosWithMine,
            BigDecimal combosWithoutMine,
            List<BoardCoord> unconstrainedSquares,
            List<List<BoardCoord>> candidateGroups
    ) {
        BoardCoord bestCoord = null;
        double bestScore = 1.0;

        if (combosWithMine.compareTo(BigDecimal.ZERO) > 0) {
            bestScore = combosWithMine
                    .divide(combosWithoutMine.add(combosWithMine), MATH_CONTEXT)
                    .doubleValue();
            if (unconstrainedSquares.size() == 0) {
                System.out.println("Error: combos With mine positive but there are no unconstrained mines : " + combosWithMine);
                System.out.println("unconstrained mines : " + unconstrainedSquares.size());
            }
            bestCoord = unconstrainedSquares.get(new Random().nextInt(unconstrainedSquares.size()));
        }

        List<BoardCoord> allCandidates =
                candidateGroups.stream().flatMap(Collection::stream).collect(Collectors.toList());

        for (BoardCoord coord : allCandidates) {
            if (probIsMine[coord.getX()][coord.getY()] < bestScore) {
                bestScore = probIsMine[coord.getX()][coord.getY()];
                bestCoord = coord;
            }
        }

        return bestCoord;
    }

    private double[][] calculateMineProbabilities(
            List<GroupResult> groupResults,
            int width,
            int height,
            int numUnconstrained,
            int totalMines,
            Consumer<BoardCoord> onCertainMineFound,
            Consumer<BoardCoord> onCertainSafeFound
    ) {
        double[][] probIsMine = new double[width][height];

        for (int g = 0; g < groupResults.size(); g++) {
            GroupResult group = groupResults.get(g);
            BigDecimal[] factorByMineCount = new BigDecimal[group.maxMineCount + 1];
            BigDecimal totalCombos = BigDecimal.ZERO;

            BigDecimal[][] dp = new BigDecimal[groupResults.size()][totalMines + 1];
            for (int i = 0; i < group.minMineCount; i++) {
                factorByMineCount[i] = BigDecimal.ZERO;
            }
            for (int mineCount = group.minMineCount; mineCount <= group.maxMineCount; mineCount++) {
                factorByMineCount[mineCount] =
                        getCombosByMineCount(groupResults, g, numUnconstrained, totalMines - mineCount, dp);
                totalCombos = totalCombos.add(
                        factorByMineCount[mineCount].multiply(group.groupResults.get(mineCount).totalSolutions),
                        MATH_CONTEXT);
            }

            for (int i = 0; i < group.boardCoords.size(); i++) {
                BigDecimal beforeNormalisation = BigDecimal.ZERO;
                for (int mineCount = 0; mineCount <= group.maxMineCount; mineCount++) {
                    beforeNormalisation = beforeNormalisation.add(factorByMineCount[mineCount].multiply(
                            group.groupResults.get(mineCount).squareResults.get(i), MATH_CONTEXT), MATH_CONTEXT);
                }

                if (isCloseEnough(beforeNormalisation, totalCombos)) { //TODO
                    onCertainMineFound.accept(group.boardCoords.get(i));
                } else if (beforeNormalisation.equals(BigDecimal.ZERO)) {
                    onCertainSafeFound.accept(group.boardCoords.get(i));
                }

                int x = group.boardCoords.get(i).getX();
                int y = group.boardCoords.get(i).getY();

                probIsMine[x][y] = beforeNormalisation.divide(totalCombos, MATH_CONTEXT).doubleValue();
            }
        }
        return probIsMine;
    }

    private boolean isCloseEnough(BigDecimal a, BigDecimal b) {
        BigDecimal maxDiffA = a.multiply(BigDecimal.valueOf(0e-10));
        BigDecimal maxDiffB = b.multiply(BigDecimal.valueOf(0e-10));
        BigDecimal diff = a.subtract(b).abs();
        return diff.compareTo(maxDiffA) <= 0 && diff.compareTo(maxDiffB) <= 0;
    }

    protected List<GroupResult> processGroups(
            PlayerView view,
            List<List<BoardCoord>> candidateGroups
    ) throws InterruptedException {
        List<GroupResult> retVal = new ArrayList<>();
        for (List<BoardCoord> candidates : candidateGroups) {
            // Build probability table, indicating the number of solutions[x][y][z] with (x, y) being a mine, and (z)
            // mines in the group.
            int[][][] numWaysToBeMine = new int[view.getWidth()][view.getHeight()][candidates.size() + 1];

            int numSolutionsByMineCount[] = new int[candidates.size() + 1];

            int totalSolutions = findCombinationsOfMines(view, candidates, (isMine) -> {
                int mineCount = 0;
                for (boolean b : isMine) {
                    if (b) mineCount++;
                }

                for (int i = 0; i < candidates.size(); i++) {
                    int x = candidates.get(i).getX();
                    int y = candidates.get(i).getY();
                    if (isMine[i]) {
                        numWaysToBeMine[x][y][mineCount]++;
                    }
                }
                numSolutionsByMineCount[mineCount]++;
            });

            retVal.add(createGroupResult(numWaysToBeMine, candidates, numSolutionsByMineCount));

            if (totalSolutions == 0) {
                reportProgressImmediate(new BoardUpdate(null, "No solutions for group"));
                throw new IllegalStateException("No solutions for group");
            }
        }
        return retVal;
    }

    /**
     * Returns the number of ways to arrange #minesRemaining mines into the #squaresRemaining squares, while
     * considering ways to do it in the groups.
     *
     * @param groupResults     combinatorial analysis of groups independently
     * @param thisGroupId      id of a group to exclude
     * @param numUnconstrained number of unknown squares that are not part of any group
     * @param minesRemaining   number of mines to distribute to remaining groups
     * @return the total number of ways of distributing these mines among the remaining groups and unconstrained squares
     */
    private BigDecimal getCombosByMineCount(
            List<GroupResult> groupResults,
            int thisGroupId,
            int numUnconstrained,
            int minesRemaining,
            BigDecimal[][] dp
    ) {
        if (minesRemaining < 0 || numUnconstrained < 0) {
            return BigDecimal.ZERO;
        }
        if (dp == null) {
            dp = new BigDecimal[groupResults.size()][minesRemaining + 1];
        }

        BigDecimal result = BigDecimal.ZERO;
        BigDecimal choose = BigDecimal.ONE;

        int total_max_mines = 0;
        int total_min_mines = 0;

        for (int i = 0; i < groupResults.size(); i++) {
            if (i == thisGroupId) continue;
            total_max_mines += groupResults.get(i).maxMineCount;
            total_min_mines += groupResults.get(i).minMineCount;
        }

        for (int touse = minesRemaining; touse >= total_min_mines; touse--) {
            if (touse <= total_max_mines) {
                result = result.add(
                        _combosOfGroups(groupResults, thisGroupId, groupResults.size() - 1, touse, dp)
                                .multiply(choose, MATH_CONTEXT),
                        MATH_CONTEXT
                );
            }
            choose = choose.multiply(BigDecimal.valueOf(numUnconstrained - (minesRemaining - touse)), MATH_CONTEXT)
                           .divide(BigDecimal.valueOf(minesRemaining - touse + 1), MATH_CONTEXT);
        }
        return result;
    }

    private BigDecimal _combosOfGroups(
            List<GroupResult> groupResults,
            int ignoreGroupId,
            int thisGroupId,
            int minesRemaining,
            BigDecimal[][] dp
    ) {
        if (minesRemaining == 0 && thisGroupId == -1) return BigDecimal.ONE;
        if (thisGroupId == -1 || minesRemaining < 0) return BigDecimal.ZERO;
        if (thisGroupId == ignoreGroupId) {
            return _combosOfGroups(groupResults, ignoreGroupId, thisGroupId - 1, minesRemaining, dp);
        }

        if (dp[thisGroupId][minesRemaining] == null) {
            GroupResult group = groupResults.get(thisGroupId);
            BigDecimal value = BigDecimal.ZERO;
            for (int mineCount = 0; mineCount <= group.maxMineCount; mineCount++) {
                value = value.add(
                        _combosOfGroups(groupResults, ignoreGroupId, thisGroupId - 1, minesRemaining - mineCount, dp)
                                .multiply(group.groupResults.get(mineCount).totalSolutions, MATH_CONTEXT),
                        MATH_CONTEXT
                );
            }
            dp[thisGroupId][minesRemaining] = value;
        }

        return dp[thisGroupId][minesRemaining];
    }

    private GroupResult createGroupResult(
            int[][][] solution,
            List<BoardCoord> candidates,
            int[] numSolutionsByMineCount
    ) {
        GroupResult retVal = new GroupResult(candidates);

        for (int mineCount = 0; mineCount < numSolutionsByMineCount.length; mineCount++) {
            GroupResultEntry entry = new GroupResultEntry(BigDecimal.valueOf(numSolutionsByMineCount[mineCount]));
            for (BoardCoord candidate : candidates) {
                entry.squareResults.add(BigDecimal.valueOf(solution[candidate.getX()][candidate.getY()][mineCount]));
            }
            retVal.addGroupResultEntry(entry);
        }
        return retVal;
    }

    @SuppressWarnings("WeakerAccess")
    public static class GroupResult {
        public final List<GroupResultEntry> groupResults;
        public final List<BoardCoord> boardCoords;
        public int maxMineCount;
        public int minMineCount;

        public GroupResult(List<BoardCoord> boardCoords) {
            groupResults = new ArrayList<>();
            this.boardCoords = boardCoords;
            this.maxMineCount = 0;
            this.minMineCount = boardCoords.size();
        }

        public void addGroupResultEntry(GroupResultEntry entry) {
            this.groupResults.add(entry);
            if (entry.totalSolutions.compareTo(BigDecimal.ZERO) > 0) {
                this.maxMineCount = Math.max(this.maxMineCount, groupResults.size()-1);
                this.minMineCount = Math.min(this.minMineCount, groupResults.size()-1);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class GroupResultEntry {
        public final List<BigDecimal> squareResults = new ArrayList<>();
        public final BigDecimal totalSolutions;

        public GroupResultEntry(BigDecimal totalSolutions) {
            this.totalSolutions = totalSolutions;
        }
    }

    @Override
    public String toString() {
        return "Backtrack Combinatorial AI";
    }
}
