package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;

public class MultiAI implements MineSweeperAI {
    @Override
    public Move calculate(PlayerState state) {
        return calculate(state, null);
    }

    MineSweeperAI goodAI = new BackTrackComboAI();
    SimpleAI simpleAI = new SimpleAI(false);

    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        Move firstTry = simpleAI.calculate(state, handler);
        if (firstTry.getToFlag().isEmpty() && firstTry.getToProbe().isEmpty()) {
            return goodAI.calculate(state, handler);
        }
        return firstTry;
    }
}
