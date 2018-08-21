package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.MineSweeper;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

@SuppressWarnings("WeakerAccess")
public class SimpleAITest extends AITest<SimpleAI> {

    @BeforeEach
    public void setup() {
        mineSweeperAI = new SimpleAI();
    }

    @Test
    public void shouldFlagSuffocatedSquares() {

        MineSweeper state = new MineSweeper(
                "  100",
                " *200",
                "**410",
                " **10",
                "   10"
        );

        Move move = mineSweeperAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                BoardCoord.ofValue(1, 1),
                BoardCoord.ofValue(1, 2),
                BoardCoord.ofValue(1, 3),
                BoardCoord.ofValue(2, 3)
        )), new HashSet<>(move.getToFlag()));
    }

    @Test
    public void shouldProbeSatisfiedSquares() {
        MineSweeper state = new MineSweeper(
                "  100",
                " X200",
                "*X410",
                " XX10",
                "   10"
        );

        Move move = mineSweeperAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                BoardCoord.ofValue(1, 0),
                BoardCoord.ofValue(2, 4)
        )), new HashSet<>(move.getToProbe()));
    }

    @Test
    public void shouldFlagAndProbeCornersAndEdges() {
        MineSweeper state = new MineSweeper(
                "*   1",
                "3*  X",
                "4*  1",
                "**   ",
                "3*   "
        );

        Move move = mineSweeperAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                BoardCoord.ofValue(1, 1),
                BoardCoord.ofValue(1, 2),
                BoardCoord.ofValue(1, 3),
                BoardCoord.ofValue(1, 4),
                BoardCoord.ofValue(0, 3)
        )), new HashSet<>(move.getToFlag()));

        assertEquals(new HashSet<>(Arrays.asList(
                BoardCoord.ofValue(3, 0),
                BoardCoord.ofValue(3, 1),
                BoardCoord.ofValue(3, 2),
                BoardCoord.ofValue(3, 3),
                BoardCoord.ofValue(4, 3)
        )), new HashSet<>(move.getToProbe()));
    }

    @RepeatedTest(10)
    public void shouldWinSimpleGames() {
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
                "   *   *",
                "        ",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********",
                "********"
        );

        assertCanWinGame(mineSweeper);

        mineSweeper = new MineSweeper(50, 50, 2499);
        assertCanWinGame(mineSweeper);
    }
}
