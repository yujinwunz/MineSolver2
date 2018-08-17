package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateEvent;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * So this one's supposed to do grouping, and then correct combinatorics on the resulting groups
 * to take into account the effect of remaining mines.
 */

public class BackTrackComboAI extends BackTrackGroupAI {
    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        prepareStatsHandler(handler);

        List<List<MineLocation>> candidateGroups = getNeighboursOfVisibleNumbersGroups(state);

        List<MineLocation> unconstrainedSquares = state.getAllSquares(SquareState.UNKNOWN);
        int availableMines = state.getTotalMines() - state.getAllSquares(SquareState.FLAGGED).size();
        candidateGroups.forEach(unconstrainedSquares::removeAll);

        // Generate the move according to the probability table.
        // 1. flag anything that's 100%.
        // 2. click anything that is safe, ie. 0%.
        // 3. click square with least probability if no safe squares are available.
        // TODO: 3. is very incomplete. Next stage of the AI is to find a better method of picking an uncertain square.
        List<MineLocation> toFlag = new ArrayList<>();
        List<MineLocation> toProbe = new ArrayList<>();

        List<GroupResult> groupResults;
        try {
            groupResults = processGroups(state, candidateGroups);
        } catch (InterruptedException e) {
            return new Move(null, null);
        }


        // For each group, we have to decide. How are its probabilities affected by its number of mines?
        // To answer that question, we have to think. With (t) total mines and (n) mines used in this group, how
        // many ways can the remaining (t-n) be distributed validly among all other groups *and* the empty space?
        // Compare that for each (n) value in the group, and you have weights to apply to each solution. Take the
        // weighted average for each square and you have accurate probabilities.
        int numUnconstrained = unconstrainedSquares.size();

        double[][] probIsMine = calculateSquareProbabilities(groupResults, state.getWidth(), state.getHeight(),
                numUnconstrained, availableMines, toFlag::add, toProbe::add);

        BigInteger combosWithMine = getCombosByMineCount(groupResults, -1, numUnconstrained - 1,
                availableMines - 1, null);
        BigInteger combosWithoutMine = getCombosByMineCount(groupResults, -1, numUnconstrained - 1,
                availableMines, null);

        if (combosWithMine.equals(BigInteger.ZERO) && combosWithoutMine.compareTo(BigInteger.ZERO) > 0) {
            toProbe.addAll(unconstrainedSquares);
        }

        if (!toProbe.isEmpty()) {
            return new Move(toProbe, toFlag);
        }

        // No sure-fire squares. Naively use least likely square instead, starting with any unconstrained square.
        double bestScore = 1.0;
        if (false) System.out.println("Prob of any other being a mine: " + bestScore);

        MineLocation bestLocation = null;

        if (combosWithMine.compareTo(BigInteger.ZERO) > 0) {
            bestScore = new BigDecimal(combosWithMine)
                    .divide(new BigDecimal(combosWithoutMine.add(combosWithMine)), 50, BigDecimal.ROUND_FLOOR)
                    .doubleValue();
            bestLocation = unconstrainedSquares.get(new Random().nextInt(unconstrainedSquares.size()));
        }

        List<MineLocation> allCandidates =
                candidateGroups.stream().flatMap(Collection::stream).collect(Collectors.toList());

        for (MineLocation location : allCandidates) {
            if (probIsMine[location.getX()][location.getY()] < bestScore) {
                bestScore = probIsMine[location.getX()][location.getY()];
                bestLocation = location;
            }
        }
        return new Move(Collections.singletonList(bestLocation), toFlag);
    }

    private double[][] calculateSquareProbabilities(
            List<GroupResult> groupResults,
            int width,
            int height,
            int numUnconstrained,
            int totalMines,
            Consumer<MineLocation> onCertainMineFound,
            Consumer<MineLocation> onCertainSafeFound
    ) {
        double[][] probIsMine = new double[width][height];

        for (int g = 0; g < groupResults.size(); g++) {
            GroupResult group = groupResults.get(g);
            BigInteger[] factorByMineCount = new BigInteger[group.maxMineCount + 1];
            BigInteger totalCombos = BigInteger.ZERO;

            BigInteger[][] dp = new BigInteger[groupResults.size()][totalMines + 1];
            for (int mineCount = 0; mineCount <= group.maxMineCount; mineCount++) {
                if (false) System.out.println(
                        "totalmines:" + totalMines + " minecount: " + mineCount + " group.maxMineCount " + group.maxMineCount);
                factorByMineCount[mineCount] =
                        getCombosByMineCount(groupResults, g, numUnconstrained, totalMines - mineCount, dp);
                if (false) System.out.println("has " + factorByMineCount[mineCount] + " combos, x " + BigInteger.valueOf(
                        group.groupResults[mineCount].totalSolutions) + " valid arrangements of " + mineCount + " mines within the group");
                totalCombos = totalCombos.add(factorByMineCount[mineCount].multiply(
                        BigInteger.valueOf(group.groupResults[mineCount].totalSolutions)));
            }

            for (int i = 0; i < group.getGroupSize(); i++) {
                BigInteger beforeNormalisation = BigInteger.ZERO;
                for (int mineCount = 0; mineCount <= group.maxMineCount; mineCount++) {
                    beforeNormalisation = beforeNormalisation.add(
                            factorByMineCount[mineCount].multiply(BigInteger.valueOf(
                                    group.groupResults[mineCount].squareResults.get(i).numWaysWithMine)));
                }

                if (beforeNormalisation.equals(totalCombos)) {
                    if (false) System.out.println("  Flagging: " + group.groupResults[0].squareResults.get(
                            i).location + " with " + totalCombos + " combos");
                    onCertainMineFound.accept(group.groupResults[0].squareResults.get(i).location);
                } else if (beforeNormalisation.equals(BigInteger.ZERO)) {
                    onCertainSafeFound.accept(group.groupResults[0].squareResults.get(i).location);
                }

                int x = group.groupResults[0].squareResults.get(i).location.getX();
                int y = group.groupResults[0].squareResults.get(i).location.getY();

                probIsMine[x][y] = new BigDecimal(beforeNormalisation)
                        .divide(new BigDecimal(totalCombos), 50, RoundingMode.HALF_DOWN)
                        .doubleValue();
            }
        }
        return probIsMine;
    }

    private List<GroupResult> processGroups(
            PlayerState state,
            List<List<MineLocation>> candidateGroups
    ) throws InterruptedException {
        List<GroupResult> retVal = new ArrayList<>();
        for (List<MineLocation> candidates : candidateGroups) {
            // Build probability table, indicating the number of solutions[x][y][z] with (x, y) being a mine, and (z)
            // mines in the group.
            int[][][] numWaysToBeMine = new int[state.getWidth()][state.getHeight()][candidates.size() + 1];

            int numSolutionsByMineCount[] = new int[candidates.size() + 1];

            int totalSolutions = findCombinationsOfMines(state, candidates, (isMine) -> {
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
                if (false) System.out.println("No solutions for group: " + candidates.toString());
                if (updateHandler != null) {
                    updateHandler.handleUpdate(new UpdateEvent(null, "No solutions for group"));
                }
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
    private BigInteger getCombosByMineCount(
            List<GroupResult> groupResults,
            int thisGroupId,
            int numUnconstrained,
            int minesRemaining,
            BigInteger[][] dp
    ) {
        if (minesRemaining < 0) {
            return BigInteger.ZERO;
        }
        if (dp == null) {
            dp = new BigInteger[groupResults.size()][minesRemaining + 1];
        }
        // TODO: dp
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i <= minesRemaining; i++) {
            if (false) System.out.println(
                    "   combo of groups with " + i + " mines remaining: " + _combosOfGroups(groupResults, thisGroupId,
                            groupResults.size() - 1, i, dp));
            if (false) System.out.println(
                    "   x with " + (minesRemaining - i) + " mines outside in " + numUnconstrained + " squares: " + _choose(
                            numUnconstrained, minesRemaining - i));
            result = result.add(
                    _combosOfGroups(groupResults, thisGroupId, groupResults.size() - 1, i, dp)
                            .multiply(_choose(numUnconstrained, minesRemaining - i))
            );
        }
        return result;
    }

    private BigInteger _combosOfGroups(
            List<GroupResult> groupResults,
            int ignoreGroupId,
            int thisGroupId,
            int minesRemaining,
            BigInteger[][] dp
    ) {
        if (minesRemaining == 0 && thisGroupId == -1) return BigInteger.ONE;
        if (thisGroupId == -1 || minesRemaining < 0) return BigInteger.ZERO;
        if (thisGroupId == ignoreGroupId) {
            return _combosOfGroups(groupResults, ignoreGroupId, thisGroupId - 1, minesRemaining, dp);
        }
        if (dp[thisGroupId][minesRemaining] == null) {
            GroupResult group = groupResults.get(thisGroupId);
            BigInteger value = BigInteger.ZERO;
            for (int mineCount = 0; mineCount <= group.maxMineCount; mineCount++) {
                value = value.add(
                        _combosOfGroups(groupResults, ignoreGroupId, thisGroupId - 1, minesRemaining - mineCount, dp)
                                .multiply(BigInteger.valueOf(group.groupResults[mineCount].totalSolutions))
                );
            }
            dp[thisGroupId][minesRemaining] = value;
        }
        return dp[thisGroupId][minesRemaining];
    }

    private static Map<Integer, BigInteger[]> chooseCache = new HashMap<>();

    private static BigInteger _choose(int n, int k) {
        if (false) System.out.println("" + n + " choose " + k);
        if (k > n) return BigInteger.ZERO;
        if (!chooseCache.containsKey(n)) {
            BigInteger[] thisCache = new BigInteger[n+1];
            thisCache[0] = BigInteger.ONE;
            for (int i = 1; i <= n; i++) {
                thisCache[i] = thisCache[i-1].multiply(BigInteger.valueOf(n - i + 1)).divide(BigInteger.valueOf(i));
            }
            chooseCache.put(n, thisCache);
        }
        return chooseCache.get(n)[k];
    }

    private GroupResult createGroupResult(
            int[][][] solution,
            List<MineLocation> candidates,
            int[] numSolutionsByMineCount
    ) {
        GroupResult retVal = new GroupResult(candidates.size());

        for (int mineCount = 0; mineCount < numSolutionsByMineCount.length; mineCount++) {
            GroupResultEntry entry = new GroupResultEntry();
            for (int i = 0; i < candidates.size(); i++) {
                entry.squareResults.add(new SquareResult(
                        candidates.get(i),
                        solution[candidates.get(i).getX()][candidates.get(i).getY()][mineCount]
                ));
            }
            entry.totalSolutions = numSolutionsByMineCount[mineCount];
            retVal.groupResults[mineCount] = entry;
        }
        return retVal;
    }

    public static class GroupResult {
        GroupResultEntry[] groupResults;
        int maxMineCount;

        GroupResult(int maxMineCount) {
            groupResults = new GroupResultEntry[maxMineCount + 1];
            this.maxMineCount = maxMineCount;
        }

        int getGroupSize() {
            return groupResults[0].squareResults.size();
        }
    }

    public static class GroupResultEntry {
        List<SquareResult> squareResults = new ArrayList<>();
        int totalSolutions = 0;
    }

    public static class SquareResult {
        MineLocation location;
        int numWaysWithMine;

        SquareResult(MineLocation location, int numWaysWithMine) {
            this.location = location;
            this.numWaysWithMine = numWaysWithMine;
        }
    }
}
