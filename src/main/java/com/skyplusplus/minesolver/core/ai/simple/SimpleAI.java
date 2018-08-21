package com.skyplusplus.minesolver.core.ai.simple;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.util.*;

public class SimpleAI extends MineSweeperAI {
    private final boolean shouldGuess;

    public SimpleAI(boolean shouldGuess) {
        this.shouldGuess = shouldGuess;
    }

    public SimpleAI() {
        this.shouldGuess = true;
    }

    @Override
    public Move calculate(PlayerView view) {
        Set<BoardCoord> toHit = new HashSet<>();
        Set<BoardCoord> toFlag = new HashSet<>();

        List<BoardCoord> canHit = naivelyFindMoves(view, toHit, toFlag);

        if (shouldGuess) {
            if (canHit.size() > 0 && toHit.isEmpty() && toFlag.isEmpty()) {
                toHit.add(getRandomMove(canHit));
            }
        }

        try {
            this.reportProgress(() -> new BoardUpdate(null, "Simple AI hitting " + toHit.size() + " squares"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Move(new ArrayList<>(toHit), new ArrayList<>(toFlag));
    }

    private static BoardCoord getRandomMove(List<BoardCoord> possibleMoves) {
        Collections.shuffle(possibleMoves);
        return possibleMoves.get(0);
    }

    /**
     * Naively finds moves that are certainly successful by counting neighbours.
     * @return all possible places to hit.
     */
    private static List<BoardCoord> naivelyFindMoves(
            PlayerView view,
            Set<BoardCoord> toHit,
            Set<BoardCoord> toFlag
    ) {
        List<BoardCoord> canHit = new ArrayList<>();
        for (BoardCoord coord: view.getAllSquares()) {
            if (view.getSquareState(coord) == SquareState.PROBED) {
                // Naively flag all neighbours of saturated numbers.
                int numMines = view.getSquareMineCount(coord);
                List<BoardCoord> flaggedSquares = view.getNeighbours(coord, SquareState.FLAGGED);
                List<BoardCoord> unknownSquares = view.getNeighbours(coord, SquareState.UNKNOWN);

                if (flaggedSquares.size() == numMines) {
                    toHit.addAll(unknownSquares);
                } else if (unknownSquares.size() + flaggedSquares.size() == numMines) {
                    toFlag.addAll(unknownSquares);
                }
            } else if (view.getSquareState(coord) == SquareState.UNKNOWN) {
                canHit.add(coord);
            }
        }
        return canHit;
    }

    @Override
    public String toString() {
        return "Simple AI";
    }
}
