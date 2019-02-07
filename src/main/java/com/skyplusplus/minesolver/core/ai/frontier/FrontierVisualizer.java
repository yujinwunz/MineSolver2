package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.ai.BoardUpdateEntry;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateColor;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class FrontierVisualizer extends FrontierAI {

    public static final int MIN_FRAME_DELAY_MS = 500;
    public static final int MIN_TIME_PER_ANIMATION_MS = 1000;
    public static final int MAX_FRAME_DELAY_MS = 500;
    private final boolean shouldPlay;

    public FrontierVisualizer(boolean shouldPlay) {
        this.shouldPlay = shouldPlay;
    }

    public FrontierVisualizer() {
        this.shouldPlay = false;
    }

    @Override
    public Move calculate(PlayerView view) {
        List<List<BoardCoord>> candidateGroups = getGroupsOfBorders(view);

        List<BoardCoord> unconstrainedSquares = view.getAllSquares(SquareState.UNKNOWN);
        candidateGroups.forEach(unconstrainedSquares::removeAll);

        try {
            doAnimation(view, candidateGroups);
        } catch (InterruptedException e) {
            return new Move(null, null);
        }

        if (shouldPlay) {
            return super.calculate(view);
        } else {
            return new Move(null, null);
        }
    }

    public void doAnimation(
            PlayerView view,
            List<List<BoardCoord>> groups
    ) throws InterruptedException {


        int frames = 0;
        List<BoardCoord> doing = new ArrayList<>();
        Set<BoardCoord> numbers = new HashSet<>();
        for (List<BoardCoord> group: groups) {
            frames += group.size();
            doing.addAll(group);
            for (BoardCoord l: group) {
                numbers.addAll(view.getNeighbours(l, SquareState.PROBED));
            }
        }

        if (frames == 0) return;

        int frameDelay = Math.max(MIN_TIME_PER_ANIMATION_MS / frames, MIN_FRAME_DELAY_MS);
        frameDelay = Math.min(frameDelay, MAX_FRAME_DELAY_MS);

        int _i = 1;
        for (List<BoardCoord> group : groups) {
            final int _fi = _i++;
            CSPSolver solver = new CSPSolver(group.size(), updateEvent -> {});

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

            List<List<Integer>> animation = solver.getFrontierPath();


            for (List<Integer> frame: animation) {
                List<BoardUpdateEntry> toDisplayA = new ArrayList<>();
                List<BoardUpdateEntry> toDisplayB = new ArrayList<>();
                for (BoardCoord bc: doing) {
                    toDisplayA.add(new BoardUpdateEntry(bc, UpdateColor.RED, ""));
                    toDisplayB.add(new BoardUpdateEntry(bc, UpdateColor.RED, ""));
                }

                for (BoardCoord bc: varToBoardCoord) {
                    if (doing.contains(bc)) {
                        toDisplayA.add(new BoardUpdateEntry(bc, UpdateColor.GRAY, ""));
                        toDisplayB.add(new BoardUpdateEntry(bc, UpdateColor.GRAY, ""));
                    }
                }

                for (BoardCoord bc: numbers) {
                    if (!Collections.disjoint(view.getNeighbours(bc, SquareState.UNKNOWN), doing)) {
                        toDisplayA.add(new BoardUpdateEntry(bc, UpdateColor.RED, ""));
                    }
                }

                for (Integer bci: frame) {
                    toDisplayA.add(new BoardUpdateEntry(varToBoardCoord.get(bci), UpdateColor.SOLIDGREEN, ""));
                    toDisplayB.add(new BoardUpdateEntry(varToBoardCoord.get(bci), UpdateColor.SOLIDGREEN, ""));
                    if (doing.contains(varToBoardCoord.get(bci))) {
                        doing.remove(varToBoardCoord.get(bci));
                    }
                }

                for (BoardCoord bc: numbers) {
                    if (!Collections.disjoint(view.getNeighbours(bc, SquareState.UNKNOWN), doing)) {
                        toDisplayB.add(new BoardUpdateEntry(bc, UpdateColor.RED, ""));
                    }
                }


                Thread.sleep(frameDelay);
                reportProgressImmediate(new BoardUpdate(toDisplayA, "Animating group " + _i + " of " + groups.size()));

                Thread.sleep(frameDelay);
                reportProgressImmediate(new BoardUpdate(toDisplayB, "Animating group " + _i + " of " + groups.size()));
            }
        }
    }

    @Override
    public String toString() {
        return "Frontier Sweep Visualizer" + (shouldPlay ? " + Play" : "");
    }
}
