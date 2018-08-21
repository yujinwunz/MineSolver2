package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.MineSweeper;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackAI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public abstract class DeterministicAITest<T extends MineSweeperAI> extends AITest<T> {

    @BeforeEach
    public void setup() {
        mineSweeperAI = getAI();
    }

    protected abstract T getAI();

    @Test
    public void shouldFindCandidateSquares() {
        MineSweeper mineSweeper = new MineSweeper(
                " *100000",
                "* 201110",
                " *101*10",
                "  212 21",
                "   *   *"
        );
        assertEquals(
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(1, 0),
                        BoardCoord.ofValue(1, 1),
                        BoardCoord.ofValue(1, 2),
                        BoardCoord.ofValue(1, 3),
                        BoardCoord.ofValue(1, 4),
                        BoardCoord.ofValue(2, 4),
                        BoardCoord.ofValue(3, 4),
                        BoardCoord.ofValue(4, 4),
                        BoardCoord.ofValue(5, 4),
                        BoardCoord.ofValue(6, 4),
                        BoardCoord.ofValue(7, 4),
                        BoardCoord.ofValue(5, 2),
                        BoardCoord.ofValue(5, 3)
                )),
                new HashSet<>(BackTrackAI.getNeighboursOfVisibleNumbers(mineSweeper.clonePlayerState()))
        );
    }

    @Test
    public void shouldSolveSimpleGame() {
        MineSweeper mineSweeper = new MineSweeper(
                "     ",
                "     ",
                "     ",
                "     "
        );

        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                " *100000",
                "* 201110",
                " *101*10",
                "  212 21",
                "   *   *"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(50, 50, 2499);
        assertCanWinGame(mineSweeper);
    }

    @RepeatedTest(10)
    public void shouldMakeAdvancedMove() {
        MineSweeper mineSweeper = new MineSweeper(
                " * **",
                "11 **",
                "   **",
                "*****"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "   **",
                "11 **",
                " * **",
                "*****"
        );
        assertCanWinGame(mineSweeper);
    }

    @RepeatedTest(10)
    public void shouldWinAdvancedGame() {
        MineSweeper mineSweeper = new MineSweeper(
                "12 *",
                "1X**",
                "13 *",
                "24**",
                "XX**"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "1X**",
                "13  ",
                "1X  ",
                "23  ",
                " ***"
        );
        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(
                "1X *",
                "13**",
                "1X  ",
                "23**",
                " * *"
        );
        assertCanWinGame(mineSweeper);
    }
}
