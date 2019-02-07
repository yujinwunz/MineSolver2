package com.skyplusplus.minesolver.core.ai.backtrack;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.frontier.FrontierAI;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;

public class FrankensteinAI extends MineSweeperAI {

    private final MineSweeperAI goodAI = new FrontierAI();
    private final SimpleAI simpleAI = new SimpleAI(false);
    private static final int MAX_SIMPLES_IN_A_ROW = 3;

    private int simplesInARow = 0;
    @Override
    public Move calculate(PlayerView view) {
        if (simplesInARow == MAX_SIMPLES_IN_A_ROW) {
            simplesInARow = 0;
            return goodAI.calculate(view, this.handler);
        } else {
            Move firstTry = simpleAI.calculate(view, this.handler);
            if (firstTry.getToFlag().isEmpty() && firstTry.getToProbe().isEmpty()) {
                simplesInARow = 0;
                return goodAI.calculate(view, this.handler);
            } else {
                simplesInARow++;
                return firstTry;
            }
        }
    }

    @Override
    public String toString() {
        return "Frontier & Simple Hybrid";
    }
}
