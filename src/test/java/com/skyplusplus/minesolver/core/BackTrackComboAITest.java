package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.gamelogic.MineSweeper;
import org.junit.jupiter.api.RepeatedTest;

@SuppressWarnings("WeakerAccess")
public class BackTrackComboAITest extends DeterministicAITest<BackTrackComboAI> {
    @Override
    protected BackTrackComboAI getAI() {
        return new BackTrackComboAI();
    }

    @RepeatedTest(10)
    public void shouldConsiderTotalMines() {
        MineSweeper mineSweeper = new MineSweeper(
                "0**0",
                "0330",
                "0*00"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "*00*",
                "*33*",
                "*00*"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "00000",
                "00000",
                "00122",
                "002XX",
                "002X "
        );
        assertCanWinGame(mineSweeper);
    }

    @RepeatedTest(10)
    public void shouldConsiderGroupsAndMines() {
        MineSweeper mineSweeper = new MineSweeper(
                " * ",
                "2X2",
                "111",
                "2X2",
                " * "
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "* *",
                "2X2",
                "111",
                "2X2",
                "* *"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                " 210012 ",
                "*X1001X*",
                " 420024 ",
                "XX1001XX",
                "22100122",
                "00000000",
                "00000000",
                "22100122",
                "XX1001XX",
                " 420024 ",
                "*X1001X*",
                " 210012 "
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "*210012*",
                " X1001X ",
                "*420024*",
                "XX1001XX",
                "22100122",
                "00000000",
                "00000000",
                "22100122",
                "XX1001XX",
                "*420024*",
                " X1001X ",
                "*210012*"
        );
        assertCanWinGame(mineSweeper);
    }
}
