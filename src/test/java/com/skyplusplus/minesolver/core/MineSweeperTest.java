package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class MineSweeperTest {
    @Test
    public void shouldInitializeWithCorrectMineCount() {
        MineSweeper mineSweeper = new MineSweeper(30, 30, 100);
        assertEquals(100, mineSweeper.getTotalMines());
        assertEquals(100, mineSweeper.getTotalMinesMinusFlags());

        mineSweeper = new MineSweeper(30, 30, 900);
        assertEquals(900, mineSweeper.getTotalMines());
        assertEquals(900, mineSweeper.getTotalMinesMinusFlags());


        mineSweeper = new MineSweeper(30, 30, 0);
        assertEquals(0, mineSweeper.getTotalMines());
        assertEquals(0, mineSweeper.getTotalMinesMinusFlags());

        mineSweeper = new MineSweeper(
                "*   *  ",
                "   *   ",
                "  *  * ",
                "*  *** ",
                "     * "
        );
        assertEquals(10, mineSweeper.getTotalMines());
    }

    @Test
    public void shouldNotInitializeWithTooManyMines() {
        assertThrows(
                MineSweeper.TooManyMinesException.class,
                () -> new MineSweeper(10, 10, 101)
        );
    }

    @Test
    public void shouldCascadeWithFirstProbe() {
        assertTimeout(Duration.ofSeconds(1), () -> {
                    for (int i = 0; i < 100; i++) { // Make sure chance of lucky pass is very very small
                        MineSweeper mineSweeper = new MineSweeper(40, 16, 99);
                        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(10, 10)));
                        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(10, 10)));
                        assertTrue(mineSweeper.getNumSquaresExposed() >= 9);
                    }

                    for (int i = 0; i < 50; i++) { // Make sure chance of lucky pass is very very small
                        MineSweeper mineSweeper = new MineSweeper(100, 50, 4991);
                        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(10, 10)));
                        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(10, 10)));
                        assertTrue(mineSweeper.getNumSquaresExposed() >= 9);
                    }
                }
        );

        // Should still cascade on a corner with limited space
        MineSweeper mineSweeper = new MineSweeper(10, 10, 96);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(9, 0)));
        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(9, 0)));
        assertTrue(mineSweeper.getNumSquaresExposed() >= 4);

        // Should still cascade on a corner with limited space
        mineSweeper = new MineSweeper(10, 10, 92);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(9, 9)));
        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(9, 9)));
        assertTrue(mineSweeper.getNumSquaresExposed() >= 4);

        // Should still cascade on an edge with limited space
        mineSweeper = new MineSweeper(10, 10, 94);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(9, 5)));
        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(9, 5)));
        assertTrue(mineSweeper.getNumSquaresExposed() >= 6);
        // Should still cascade on an edge with limited space
        mineSweeper = new MineSweeper(10, 10, 92);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(9, 5)));
        assertEquals(0, mineSweeper.getProbedSquare(MineLocation.ofValue(9, 5)));
        assertTrue(mineSweeper.getNumSquaresExposed() >= 6);

        // Shouldn't cascade when impossible, but should at least continue the game
        mineSweeper = new MineSweeper(10, 10, 92);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(5, 5)));
        assertEquals(1, mineSweeper.getNumSquaresExposed());
        mineSweeper = new MineSweeper(10, 10, 97);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(0, 9)));
        assertEquals(1, mineSweeper.getNumSquaresExposed());
        mineSweeper = new MineSweeper(10, 10, 95);
        assertEquals(ProbeResult.OK, mineSweeper.probe(MineLocation.ofValue(0, 5)));
        assertEquals(1, mineSweeper.getNumSquaresExposed());
    }

    @Test
    public void shouldCascadeCornersAndEdges() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "*** ",
                "**  ",
                "**  "
        );

        mineSweeper.probe(MineLocation.ofValue(3, 3));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "  41",
                "  20");


        mineSweeper = new MineSweeper(
                "*  **",
                "     ",
                "     ",
                "     ",
                "**   "
        );

        mineSweeper.probe(MineLocation.ofValue(4, 4));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "     ",
                "11122",
                "00000",
                "22100",
                "  100");


        mineSweeper = new MineSweeper(
                "     ",
                " **  ",
                "  *  ",
                "     ",
                "     "
        );

        mineSweeper.probe(MineLocation.ofValue(2, 4));

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "   10",
                "   20",
                "13 20",
                "01110",
                "00000");

        mineSweeper = new MineSweeper(
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   ",
                "                                   "
        );

        mineSweeper.probe(MineLocation.ofValue(0, 5));
        assertBoardState(mineSweeper, GameState.WIN,
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000",
                "00000000000000000000000000000000000"
        );
    }

    @Test
    public void shouldSweepCentersCornersAndEdges() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "*** ",
                "**  ",
                "**  "
        );

        mineSweeper.probe(MineLocation.ofValue(3, 3));
        mineSweeper.flag(MineLocation.ofValue(2, 1));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "  X ",
                "  41",
                "  20");

        mineSweeper.sweep(MineLocation.ofValue(3, 2));
        assertBoardState(mineSweeper, GameState.WIN,
                "    ",
                "  X3",
                "  41",
                "  20");


        mineSweeper = new MineSweeper(
                "    ",
                "   *",
                " *  ",
                "  * "
        );

        mineSweeper.probe(MineLocation.ofValue(0, 3));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "    ",
                "1   ");

        mineSweeper.flag(MineLocation.ofValue(1, 2));
        mineSweeper.sweep(MineLocation.ofValue(0, 3));

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "1X  ",
                "12  ");

        mineSweeper.probe(MineLocation.ofValue(0, 1));
        mineSweeper.sweep(MineLocation.ofValue(0, 1));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "001 ",
                "112 ",
                "1X  ",
                "12  ");

        mineSweeper.flag(MineLocation.ofValue(3, 1));
        mineSweeper.sweep(MineLocation.ofValue(2, 1));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "0011",
                "112X",
                "1X32",
                "12  ");
    }

    @Test
    public void shouldWinOnlyWhenAllSafeSquaresAreProbed() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "****",
                "****",
                "****"
        );
        assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(4, 4, 15);

        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(3, 3));
        assertEquals(GameState.WIN, mineSweeper.getGameState());


        mineSweeper = new MineSweeper(
                "****",
                "****",
                "**  ",
                "**  "
        );

        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(3, 3));
        assertEquals(GameState.WIN, mineSweeper.getGameState());


        mineSweeper = new MineSweeper(
                "*  *",
                " * *",
                "* * ",
                "* * "
        );

        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(3, 3));
        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(1, 3));
        mineSweeper.probe(MineLocation.ofValue(1, 2));
        mineSweeper.probe(MineLocation.ofValue(3, 2));
        mineSweeper.probe(MineLocation.ofValue(0, 1));
        mineSweeper.probe(MineLocation.ofValue(2, 1));
        mineSweeper.probe(MineLocation.ofValue(1, 0));
        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(2, 0));
        assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "    ",
                "    ",
                "    ",
                "    "
        );

        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(1, 2));
        assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "*   ",
                "    ",
                "    ",
                "    "
        );

        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.flag(MineLocation.ofValue(1, 0));
        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(MineLocation.ofValue(1, 3));
        assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.unflag(MineLocation.ofValue(1, 0));
        mineSweeper.probe(MineLocation.ofValue(1, 0));
        assertEquals(GameState.WIN, mineSweeper.getGameState());
    }

    @Test
    public void shouldOnlySweepReadySquares() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "*** ",
                "**  ",
                "**  "
        );

        mineSweeper.probe(MineLocation.ofValue(3, 3));
        mineSweeper.flag(MineLocation.ofValue(2, 1));
        assertEquals(ProbeResult.NOP, mineSweeper.sweep(MineLocation.ofValue(2, 2)));
        assertEquals(ProbeResult.NOP, mineSweeper.sweep(MineLocation.ofValue(0, 0)));
        assertEquals(ProbeResult.OK, mineSweeper.sweep(MineLocation.ofValue(3, 2)));
        assertBoardState(mineSweeper, GameState.WIN,
                "    ",
                "  X3",
                "  41",
                "  20");

        mineSweeper = new MineSweeper(
                "    ",
                "  * ",
                " *  ",
                "    "
        );

        mineSweeper.probe(MineLocation.ofValue(2, 2));
        mineSweeper.flag(MineLocation.ofValue(2, 1));
        mineSweeper.flag(MineLocation.ofValue(1, 2));
        mineSweeper.flag(MineLocation.ofValue(1, 1));

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                " XX ",
                " X2 ",
                "    ");
        assertEquals(ProbeResult.NOP, mineSweeper.sweep(MineLocation.ofValue(2, 2)));

        mineSweeper.unflag(MineLocation.ofValue(1, 1));
        assertEquals(ProbeResult.OK, mineSweeper.sweep(MineLocation.ofValue(2, 2)));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                " 2X1",
                " X21",
                " 110");
    }

    @Test
    public void shouldNotProbeSweepOrCascadeFlags() {
        MineSweeper mineSweeper = new MineSweeper(
                "     ",
                "     ",
                "     ",
                "     ",
                "     "
        );

        mineSweeper.flag(MineLocation.ofValue(2, 0));
        mineSweeper.flag(MineLocation.ofValue(2, 1));
        mineSweeper.flag(MineLocation.ofValue(2, 2));
        mineSweeper.flag(MineLocation.ofValue(2, 3));

        mineSweeper.flag(MineLocation.ofValue(0, 3));
        mineSweeper.flag(MineLocation.ofValue(1, 3));
        mineSweeper.flag(MineLocation.ofValue(0, 1));
        mineSweeper.flag(MineLocation.ofValue(1, 2));

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "  X  ",
                "X X  ",
                " XX  ",
                "XXX  ",
                "     "
        );

        mineSweeper.probe(MineLocation.ofValue(0, 0));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "00X  ",
                "X0X  ",
                "0XX  ",
                "XXX  ",
                "     "
        );

        assertEquals(ProbeResult.NOP, mineSweeper.sweep(MineLocation.ofValue(2, 2)));
        assertEquals(ProbeResult.NOP, mineSweeper.probe(MineLocation.ofValue(2, 2)));
    }

    @Test
    public void shouldOnlyFlagOrUnflagUnknownSquares() {
        MineSweeper mineSweeper = new MineSweeper(
                "     ",
                "  *  ",
                "* * ",
                "     ",
                "     "
        );
        mineSweeper.probe(MineLocation.ofValue(0, 0));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01   ",
                "13   ",
                "     ",
                "     ",
                "     "
        );

        assertEquals(FlagResult.FLAGGED, mineSweeper.flag(MineLocation.ofValue(2, 0)));
        assertEquals(FlagResult.NOP, mineSweeper.unflag(MineLocation.ofValue(2, 1)));
        assertEquals(FlagResult.NOP, mineSweeper.unflag(MineLocation.ofValue(2, 2)));
        assertEquals(FlagResult.FLAGGED, mineSweeper.flag(MineLocation.ofValue(2, 1)));
        assertEquals(FlagResult.FLAGGED, mineSweeper.flag(MineLocation.ofValue(2, 2)));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "13X  ",
                "  X  ",
                "     ",
                "     "
        );

        assertEquals(FlagResult.UNFLAGGED, mineSweeper.unflag(MineLocation.ofValue(2, 1)));
        assertEquals(FlagResult.UNFLAGGED, mineSweeper.unflag(MineLocation.ofValue(2, 2)));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "13   ",
                "     ",
                "     ",
                "     "
        );


        assertEquals(FlagResult.NOP, mineSweeper.unflag(MineLocation.ofValue(1, 1)));
        assertEquals(FlagResult.NOP, mineSweeper.flag(MineLocation.ofValue(1, 1)));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "13   ",
                "     ",
                "     ",
                "     "
        );
    }

    @Test
    public void shouldLoseAfterProbingOrSweepingMine() {

        MineSweeper mineSweeper = new MineSweeper(
                "    ",
                "   *",
                " *  ",
                "  * "
        );

        mineSweeper.probe(MineLocation.ofValue(0, 3));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "    ",
                "1   ");
        mineSweeper.flag(MineLocation.ofValue(0, 2));
        assertEquals(ProbeResult.LOSE, mineSweeper.sweep(MineLocation.ofValue(0, 3)));
        assertEquals(GameState.LOSE, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "    ",
                "   *",
                " *  ",
                "  * "
        );

        assertEquals(ProbeResult.LOSE, mineSweeper.probe(MineLocation.ofValue(1, 2)));
        assertEquals(GameState.LOSE, mineSweeper.getGameState());
    }

    @Test
    public void cascadeShouldNotRatifyFlags() {
        MineSweeper mineSweeper = new MineSweeper(
                "       ",
                "  *    ",
                "       ",
                "  *    ",
                "       "
        );

        mineSweeper.flag(MineLocation.ofValue(2, 1));
        mineSweeper.flag(MineLocation.ofValue(2, 3));
        mineSweeper.probe(MineLocation.ofValue(4, 2));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "   1000",
                "  X1000",
                "   2000",
                "  X1000",
                "   1000"
        );

    }

    @Test
    public void shouldOnlyProbeSweepOrFlagWhileGameIsInProgress() {
        MineSweeper mineSweeper = new MineSweeper(
                "    ",
                "*   ",
                "*   ",
                "    "
        );
        mineSweeper.probe(MineLocation.ofValue(1, 0));
        mineSweeper.flag(MineLocation.ofValue(0, 1));
        assertEquals(ProbeResult.LOSE, mineSweeper.probe(MineLocation.ofValue(0, 2)));
        assertEquals(GameState.LOSE, mineSweeper.getGameState());

        assertEquals(ProbeResult.NOP, mineSweeper.probe(MineLocation.ofValue(3, 3)));
        assertEquals(ProbeResult.NOP, mineSweeper.sweep(MineLocation.ofValue(1, 0)));
        assertEquals(FlagResult.NOP, mineSweeper.flag(MineLocation.ofValue(0, 2)));
        assertEquals(FlagResult.NOP, mineSweeper.unflag(MineLocation.ofValue(0, 1)));
    }

    @Test
    public void shouldSerializeWithoutHiddenMines() {

        MineSweeper state = new MineSweeper(
                " x100",
                " X200",
                "**410",
                " *#10",
                "   10"
        );

        assertBoardState(state, GameState.LOSE,
                " X100",
                " X200",
                "  410",
                "  *10",
                "   10"
        );

        assertEquals(5, state.getTotalMines());
    }

    private void assertBoardState(MineSweeper target, GameState gameState, String... repr) {
        assertEquals(repr.length, target.getHeight());
        assertEquals(repr[0].length(), target.getWidth());

        assertEquals(String.join("\n", repr), String.join("\n", target.toStringArray()));

        assertEquals(gameState, target.getGameState());
    }
}
