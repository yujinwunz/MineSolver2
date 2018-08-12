package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.simpleai.SimpleAI;
import com.skyplusplus.minesolver.game.FlagResult;
import com.skyplusplus.minesolver.game.GameState;

import static org.junit.jupiter.api.Assertions.*;

import com.skyplusplus.minesolver.game.ProbeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

public class SimpleAITest {

    MineSweeperAI simpleAI;

    @BeforeEach
    public void setup() {
        simpleAI = new SimpleAI();
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

        Move move = simpleAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                new MineLocation(1, 1),
                new MineLocation(1, 2),
                new MineLocation(1, 3),
                new MineLocation(2, 3)
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

        Move move = simpleAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                new MineLocation(1, 0),
                new MineLocation(2, 4)
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

        Move move = simpleAI.calculate(state.clonePlayerState());
        assertEquals(new HashSet<>(Arrays.asList(
                new MineLocation(1, 1),
                new MineLocation(1, 2),
                new MineLocation(1, 3),
                new MineLocation(1, 4),
                new MineLocation(0, 3)
        )), new HashSet<>(move.getToFlag()));

        assertEquals(new HashSet<>(Arrays.asList(
                new MineLocation(3, 0),
                new MineLocation(3, 1),
                new MineLocation(3, 2),
                new MineLocation(3, 3),
                new MineLocation(4, 3)
        )), new HashSet<>(move.getToProbe()));
    }

    @Test
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

    private void assertCanWinGame(MineSweeper mineSweeper) {
        while (mineSweeper.getGameState() == GameState.IN_PROGRESS) {
            boolean didMove = false;
            Move move = simpleAI.calculate(mineSweeper.clonePlayerState());
            for (MineLocation mineLocation: move.getToFlag()) {
                if (mineSweeper.flag(mineLocation.getX(), mineLocation.getY()) == FlagResult.FLAGGED) {
                    didMove = true;
                }
            }
            for (MineLocation mineLocation: move.getToProbe()) {
                if (mineSweeper.probe(mineLocation.getX(), mineLocation.getY()) == ProbeResult.OK) {
                    didMove = true;
                }
            }
            assertNotEquals(GameState.LOSE, mineSweeper.getGameState());
            assertTrue(didMove); // Should do something at every iteration
        }
        assertEquals(GameState.WIN, mineSweeper.getGameState());
    }
}
