package com.skyplusplus.minesolver.core.ai.simple;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateEvent;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;

import java.util.*;

public class SimpleAI extends MineSweeperAI {
    private boolean shouldGuess;

    public SimpleAI(boolean shouldGuess) {
        this.shouldGuess = shouldGuess;
    }

    public SimpleAI() {
        this.shouldGuess = true;
    }

    @Override
    public Move calculate(PlayerState state) {
        Set<MineLocation> toHit = new HashSet<>();
        Set<MineLocation> toFlag = new HashSet<>();

        List<MineLocation> canHit = naivelyFindMoves(state, toHit, toFlag);

        if (shouldGuess) {
            if (canHit.size() > 0 && toHit.isEmpty() && toFlag.isEmpty()) {
                toHit.add(getRandomMove(canHit));
            }
        }

        try {
            this.reportProgress(() -> new UpdateEvent(null, "Simple AI hitting " + toHit.size() + " squares"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Move(new ArrayList<>(toHit), new ArrayList<>(toFlag));
    }

    private static MineLocation getRandomMove(List<MineLocation> possibleMoves) {
        Collections.shuffle(possibleMoves);
        return possibleMoves.get(0);
    }

    /**
     * Naively finds moves that are certainly successful by counting neighbours.
     * @return all possible places to hit.
     */
    private static List<MineLocation> naivelyFindMoves(
            PlayerState state,
            Set<MineLocation> toHit,
            Set<MineLocation> toFlag
    ) {
        List<MineLocation> canHit = new ArrayList<>();
        for (MineLocation location: state.getAllSquares()) {
            if (state.getSquareState(location) == SquareState.PROBED) {
                // Naively flag all neighbours of saturated numbers.
                int numMines = state.getSquareMineCount(location);
                List<MineLocation> flaggedSquares = state.getNeighbours(location, SquareState.FLAGGED);
                List<MineLocation> unknownSquares = state.getNeighbours(location, SquareState.UNKNOWN);

                if (flaggedSquares.size() == numMines) {
                    toHit.addAll(unknownSquares);
                } else if (unknownSquares.size() + flaggedSquares.size() == numMines) {
                    toFlag.addAll(unknownSquares);
                }
            } else if (state.getSquareState(location) == SquareState.UNKNOWN) {
                canHit.add(location);
            }
        }
        return canHit;
    }

    @Override
    public String toString() {
        return "Simple AI";
    }
}
