package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrontierAI extends BackTrackComboAI {

    @Override
    public List<GroupResult> processGroups(
            PlayerView view,
            List<List<BoardCoord>> groups
    ) throws InterruptedException {
        List<GroupResult> retVal = new ArrayList<>();

        int _i = 1;
        for (List<BoardCoord> group : groups) {
            final int _fi = _i++;
            CSPSolver solver = new CSPSolver(group.size(), updateEvent -> reportProgressImmediate(
                    new BoardUpdate(null, "Group #" + _fi + ": " + updateEvent.getMessage())));

            Set<BoardCoord> seenProbed = new HashSet<>();
            List<BoardCoord> varToBoardCoord = new ArrayList<>();

            for (BoardCoord l : group) {
                for (BoardCoord probed : view.getNeighbours(l, SquareState.PROBED)) {
                    if (!seenProbed.contains(probed)) {
                        seenProbed.add(probed);

                        List<BoardCoord> variables = view.getNeighbours(probed, SquareState.UNKNOWN);
                        int[] varIdParam = new int[variables.size()];

                        for (int i = 0; i < variables.size(); i++) {
                            int id = varToBoardCoord.indexOf(variables.get(i));
                            if (id == -1) {
                                id = varToBoardCoord.size();
                                varToBoardCoord.add(variables.get(i));
                            }
                            varIdParam[i] = id;
                        }

                        solver.addRule(
                                view.getSquareMineCount(probed) - view.getNeighbours(probed, SquareState.FLAGGED)
                                                                      .size(), varIdParam);
                    }
                }
            }

            BigDecimal[][] solution = new BigDecimal[group.size() + 1][group.size()];
            BigDecimal[] numSolutions = solver.solveApproximate(solution);

            GroupResult toAdd = new GroupResult(group);
            BigDecimal totalSols = BigDecimal.ZERO;
            for (int numMines = 0; numMines <= group.size(); numMines++) {
                totalSols = totalSols.add(numSolutions[numMines]);
                GroupResultEntry entry = new GroupResultEntry(numSolutions[numMines]);
                for (BoardCoord g : group) {
                    int index = varToBoardCoord.indexOf(g);
                    entry.squareResults.add(solution[numMines][index]);
                }
                toAdd.addGroupResultEntry(entry);
            }
            retVal.add(toAdd);
            if (totalSols.equals(BigDecimal.ZERO)) {
                reportProgressImmediate(new BoardUpdate(null, "Group has no solutions: " + group));
                throw new IllegalStateException("Group has no solutions: " + group);
            }
        }

        return retVal;
    }

    @Override
    public String toString() {
        return "Frontier DP";
    }
}
