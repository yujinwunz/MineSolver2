package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;

public class FrankensteinAI extends MineSweeperAI {

    MineSweeperAI goodAI = new BackTrackComboAI();
    SimpleAI simpleAI = new SimpleAI(false);

    @Override
    public Move calculate(PlayerState state) {
        Move firstTry = simpleAI.calculate(state, this.handler);
        if (firstTry.getToFlag().isEmpty() && firstTry.getToProbe().isEmpty()) {
            return goodAI.calculate(state, this.handler);
        }
        return firstTry;
    }

    @Override
    public String toString() {
        return "FrankensteinAI AI";
    }
}
