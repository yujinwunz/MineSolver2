package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;

public class FrankensteinAI extends MineSweeperAI {

    private final MineSweeperAI goodAI = new BackTrackComboAI();
    private final SimpleAI simpleAI = new SimpleAI(false);

    @Override
    public Move calculate(PlayerView view) {
        Move firstTry = simpleAI.calculate(view, this.handler);
        if (firstTry.getToFlag().isEmpty() && firstTry.getToProbe().isEmpty()) {
            return goodAI.calculate(view, this.handler);
        }
        return firstTry;
    }

    @Override
    public String toString() {
        return "FrankensteinAI AI";
    }
}
