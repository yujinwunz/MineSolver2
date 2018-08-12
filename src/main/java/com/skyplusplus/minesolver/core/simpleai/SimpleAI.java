package com.skyplusplus.minesolver.core.simpleai;

import com.skyplusplus.minesolver.core.MineLocation;
import com.skyplusplus.minesolver.core.PlayerState;
import com.skyplusplus.minesolver.core.SquareState;
import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SimpleAI implements MineSweeperAI {
    private UpdateHandler handler = null;

    @Override
    public Move calculate(PlayerState state) {
        return calculate(state, null);
    }

    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        this.handler = handler;
        Random random = new Random();
        Set<MineLocation> toHit = new HashSet<>();
        Set<MineLocation> toFlag = new HashSet<>();
        int numCanHit = 0;

        for (int x = 0; x < state.getWidth(); x++) {
            for (int y = 0; y < state.getHeight(); y++) {
                if (state.getSquareState(x, y) == SquareState.PROBED) {
                    // Naively flag all neighbours of saturated numbers.
                    int numMines = state.getSquareMineCount(x, y);
                    int numFlags = 0;
                    int numUnknown = 0;

                    ArrayList<MineLocation> unflagged = new ArrayList<>();
                    for (int nx = Math.max(0, x-1); nx <= Math.min(state.getWidth()-1, x+1); nx++) {
                        for (int ny = Math.max(0, y-1); ny <= Math.min(state.getHeight()-1, y+1); ny++) {
                            if (state.getSquareState(nx, ny) == SquareState.FLAGGED) {
                                numFlags ++;
                                numUnknown ++;
                            } else if (state.getSquareState(nx, ny) == SquareState.UNKNOWN) {
                                numUnknown ++;
                                unflagged.add(new MineLocation(nx, ny));
                            }
                        }
                    }

                    if (numFlags == numMines) {
                        toHit.addAll(unflagged);
                    }
                    if (numUnknown == numMines) {
                        toFlag.addAll(unflagged);
                    }

                } else if (state.getSquareState(x, y) == SquareState.UNKNOWN) {
                    numCanHit ++;
                }
            }
        }

        while (toHit.isEmpty() && toFlag.isEmpty() && numCanHit > 0) {
            int x = random.nextInt(state.getWidth());
            int y = random.nextInt(state.getHeight());
            if (state.getSquareState(x, y) == SquareState.UNKNOWN) {
                toHit.add(new MineLocation(x, y));
            }
        }

        return new Move(new ArrayList<>(toHit), new ArrayList<>(toFlag));
    }
}
