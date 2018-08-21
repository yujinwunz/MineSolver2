package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrontierAI extends BackTrackComboAI {

    @Override
    public List<GroupResult> processGroups(PlayerState state, List<List<MineLocation>> groups) throws InterruptedException {
        List<GroupResult> retVal = new ArrayList<>();

        for (List<MineLocation> group: groups) {
            CSPSolver solver = new CSPSolver(group.size(), updateEvent -> {

            });

            Set<MineLocation> seenProbed = new HashSet<>();
            List<MineLocation> varToMineLocation = new ArrayList<>();

            for (MineLocation l : group) {
                for (MineLocation probed : state.getNeighbours(l, SquareState.PROBED)) {
                    if (!seenProbed.contains(probed)) {
                        seenProbed.add(probed);

                        List<MineLocation> variables = state.getNeighbours(probed, SquareState.UNKNOWN);
                        int[] varIdParam = new int[variables.size()];

                        for (int i = 0; i < variables.size(); i++) {
                            int id = varToMineLocation.indexOf(variables.get(i));
                            if (id == -1) {
                                id = varToMineLocation.size();
                                varToMineLocation.add(variables.get(i));
                            }
                            varIdParam[i] = id;
                        }

                        solver.addRule(state.getSquareMineCount(probed) - state.getNeighbours(probed, SquareState.FLAGGED).size(), varIdParam);
                    }
                }
            }

            BigInteger[][] solution = new BigInteger[group.size()+1][group.size()];
            BigInteger[] numSolutions = solver.solve(solution);

            GroupResult toAdd = new GroupResult(group.size());
            BigInteger totalSols = BigInteger.ZERO;
            for (int numMines = 0; numMines <= group.size(); numMines++) {
                totalSols = totalSols.add(numSolutions[numMines]);
                GroupResultEntry entry = new GroupResultEntry(numSolutions[numMines]);
                for (int j = 0; j < group.size(); j++) {
                    entry.squareResults.add(new SquareResult(varToMineLocation.get(j), solution[numMines][j]));
                }
                toAdd.groupResults.add(entry);
            }
            retVal.add(toAdd);
            if (totalSols.equals(BigInteger.ZERO)) {
                System.out.println("Group: " + group + " has no solutions.");
            }
        }

        return retVal;
    }

    @Override
    public String toString() {
        return "Frontier AI";
    }
}
