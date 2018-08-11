package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.PlayerState;
import com.skyplusplus.minesolver.core.SquareState;
import com.sun.tracing.Probe;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class MineSweeperTest {
    @Test
    public void shouldInitializeWithCorrectMineCount() {
        MineSweeper mineSweeper = new MineSweeper(30, 30, 100);
        Assertions.assertEquals(100, mineSweeper.getTotalMines(), mineSweeper.getUnflaggedMines());

        mineSweeper = new MineSweeper(30, 30, 900);
        Assertions.assertEquals(900, mineSweeper.getTotalMines(), mineSweeper.getUnflaggedMines());


        mineSweeper = new MineSweeper(30, 30, 0);
        Assertions.assertEquals(0, mineSweeper.getTotalMines(), mineSweeper.getUnflaggedMines());

        mineSweeper = new MineSweeper(
                "*   *  ",
                "   *   ",
                "  *  * ",
                "*  *** ",
                "     * "
        );
        Assertions.assertEquals(10, mineSweeper.getTotalMines());
    }

    @Test(expected = Exception.class)
    public void shouldNotInitializeWithTooManyMines() {
        MineSweeper mineSweeper = new MineSweeper(10, 10, 101);
    }

    @Test(timeout = 1000)
    public void shouldCascadeWithFirstProbe() {
        for (int i = 0; i < 100; i++) { // Make sure chance of lucky pass is very very small
            MineSweeper mineSweeper = new MineSweeper(40, 16, 99);
            Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(10, 10));
            Assertions.assertEquals(0, mineSweeper.getProbedSquare(10, 10));
        }

        for (int i = 0; i < 50; i++) { // Make sure chance of lucky pass is very very small
            MineSweeper mineSweeper = new MineSweeper(100, 50, 4991);
            Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(10, 10));
            Assertions.assertEquals(0, mineSweeper.getProbedSquare(10, 10));
        }

        // Should still cascade on a corner with limited space
        MineSweeper mineSweeper = new MineSweeper(10, 10, 96);
        Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(9, 0));

        // Should still cascade on a corner with limited space
        mineSweeper = new MineSweeper(10, 10, 92);
        Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(9, 9));

        // Should still cascade on an edge with limited space
        mineSweeper = new MineSweeper(10, 10, 94);
        Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(9, 5));
        // Should still cascade on an edge with limited space
        mineSweeper = new MineSweeper(10, 10, 92);
        Assertions.assertEquals(ProbeResult.CASCADE, mineSweeper.probe(9, 5));

        // Shouldn't cascade when impossible, but should at least continue the game
        mineSweeper = new MineSweeper(10, 10, 92);
        Assertions.assertEquals(ProbeResult.SINGLE, mineSweeper.probe(5, 5));
        mineSweeper = new MineSweeper(10, 10, 97);
        Assertions.assertEquals(ProbeResult.SINGLE, mineSweeper.probe(0, 9));
        mineSweeper = new MineSweeper(10, 10, 95);
        Assertions.assertEquals(ProbeResult.SINGLE, mineSweeper.probe(0, 5));
    }

    @Test
    public void shouldCascadeCornersAndEdges() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "*** ",
                "**  ",
                "**  "
        );

        mineSweeper.probe(3, 3);
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

        mineSweeper.probe(4, 4);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "     ",
                "11122",
                "00000",
                "21100",
                "  100");


        mineSweeper = new MineSweeper(
                "     ",
                " **  ",
                "  *  ",
                "     ",
                "     "
        );

        mineSweeper.probe(2, 4);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "   10",
                "1  20",
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

        mineSweeper.probe(0, 5);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
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

        mineSweeper.probe(3, 3);
        mineSweeper.flag(2, 1);
        mineSweeper.sweep(3, 2);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "   3",
                "  41",
                "  20");


        mineSweeper = new MineSweeper(
                "    ",
                "   *",
                " *  ",
                "  * "
        );

        mineSweeper.probe(0, 3);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "    ",
                "1   ");

        mineSweeper.flag(1, 2);
        mineSweeper.sweep(0, 3);

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "1X  ",
                "11  ");

        mineSweeper.sweep(0, 2);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "001 ",
                "012 ",
                "1X  ",
                "12  ");

        mineSweeper.flag(3, 1);
        mineSweeper.sweep(2, 1);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "0011",
                "012X",
                "1X22",
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
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "****",
                "****",
                "****",
                "*** "
        );

        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(3, 3);
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());


        mineSweeper = new MineSweeper(
                "****",
                "****",
                "**  ",
                "**  "
        );

        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(3, 3);
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());


        mineSweeper = new MineSweeper(
                "*  *",
                " * *",
                "* * ",
                "* * "
        );

        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(3, 3);
        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(1, 3);
        mineSweeper.probe(1, 2);
        mineSweeper.probe(3, 2);
        mineSweeper.probe(0, 1);
        mineSweeper.probe(2, 1);
        mineSweeper.probe(1, 0);
        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(2, 0);
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "    ",
                "    ",
                "    ",
                "    "
        );

        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(1, 2);
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "*   ",
                "    ",
                "    ",
                "    "
        );

        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.flag(1, 1);
        Assertions.assertEquals(GameState.IN_PROGRESS, mineSweeper.getGameState());
        mineSweeper.probe(1, 3);
        Assertions.assertEquals(GameState.WIN, mineSweeper.getGameState());
    }

    @Test
    public void shouldOnlySweepReadySquares() {
        MineSweeper mineSweeper = new MineSweeper(
                "****",
                "*** ",
                "**  ",
                "**  "
        );

        mineSweeper.probe(3, 3);
        mineSweeper.flag(2, 1);
        Assertions.assertEquals(SweepResult.NOP, mineSweeper.sweep(2, 2));
        Assertions.assertEquals(SweepResult.OK, mineSweeper.sweep(3, 2));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
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

        mineSweeper.probe(2, 2);
        mineSweeper.flag(2, 1);
        mineSweeper.flag(1, 2);
        mineSweeper.flag(1, 1);

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                " XX ",
                " X2 ",
                "    ");
        Assertions.assertEquals(SweepResult.NOP, mineSweeper.sweep(2, 2));

        mineSweeper.unflag(1, 1);
        Assertions.assertEquals(SweepResult.OK, mineSweeper.sweep(2, 2));
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

        mineSweeper.flag(2, 0);
        mineSweeper.flag(2, 1);
        mineSweeper.flag(2, 2);
        mineSweeper.flag(2, 3);

        mineSweeper.flag(0, 3);
        mineSweeper.flag(1, 3);
        mineSweeper.flag(0, 1);
        mineSweeper.flag(1, 2);

        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "  X  ",
                "X X  ",
                " XX  ",
                "XXX  ",
                "     "
        );

        mineSweeper.probe(0, 0);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "00X  ",
                "X0X  ",
                "0XX  ",
                "XXX  ",
                "     "
        );

        Assertions.assertEquals(SweepResult.NOP, mineSweeper.sweep(2, 2));
        Assertions.assertEquals(ProbeResult.NOP, mineSweeper.probe(2, 2));
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
        mineSweeper.probe(0, 0);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01   ",
                "12   ",
                "     ",
                "     ",
                "     "
        );

        Assertions.assertEquals(FlagResult.FLAGGED, mineSweeper.flag(2, 0));
        Assertions.assertEquals(FlagResult.NOP, mineSweeper.unflag(2, 1));
        Assertions.assertEquals(FlagResult.NOP, mineSweeper.unflag(2, 2));
        Assertions.assertEquals(FlagResult.FLAGGED, mineSweeper.flag(2, 1));
        Assertions.assertEquals(FlagResult.FLAGGED, mineSweeper.flag(2, 2));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "12X  ",
                "  X  ",
                "     ",
                "     "
        );

        Assertions.assertEquals(FlagResult.UNFLAGGED, mineSweeper.unflag(2, 1));
        Assertions.assertEquals(FlagResult.UNFLAGGED, mineSweeper.unflag(2, 2));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "12   ",
                "     ",
                "     ",
                "     "
        );


        Assertions.assertEquals(FlagResult.NOP, mineSweeper.unflag(1, 1));
        Assertions.assertEquals(FlagResult.NOP, mineSweeper.flag(1, 1));
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "01X  ",
                "12   ",
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

        mineSweeper.probe(0, 3);
        assertBoardState(mineSweeper, GameState.IN_PROGRESS,
                "    ",
                "    ",
                "    ",
                "1   ");
        mineSweeper.flag(0, 2);
        Assertions.assertEquals(SweepResult.LOSE, mineSweeper.sweep(0, 3));
        Assertions.assertEquals(GameState.LOSE, mineSweeper.getGameState());

        mineSweeper = new MineSweeper(
                "    ",
                "   *",
                " *  ",
                "  * "
        );

        Assertions.assertEquals(ProbeResult.LOSE, mineSweeper.probe(1, 2));
        Assertions.assertEquals(GameState.LOSE, mineSweeper.getGameState());
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

        mineSweeper.flag(1, 1);
        mineSweeper.flag(1, 3);
        mineSweeper.probe(4, 2);
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
        mineSweeper.probe(1, 0);
        mineSweeper.flag(0, 1);
        Assertions.assertEquals(ProbeResult.LOSE, mineSweeper.probe(0, 1));
        Assertions.assertEquals(GameState.LOSE, mineSweeper.getGameState());

        Assertions.assertEquals(ProbeResult.NOP, mineSweeper.probe(3, 3));
        Assertions.assertEquals(SweepResult.NOP, mineSweeper.sweep(1, 0));
        Assertions.assertEquals(FlagResult.NOP, mineSweeper.flag(0, 2));
        Assertions.assertEquals(FlagResult.NOP, mineSweeper.unflag(0, 1));
    }

    private void assertBoardState(MineSweeper target, GameState gameState, String... repr) {
        Assertions.assertEquals(repr.length, target.getHeight());
        Assertions.assertEquals(repr[0].length(), target.getHeight());

        Assertions.assertEquals(gameState, target.getGameState());

        for (int x = 0; x < repr[0].length(); x++) {
            for (int y = 0; y < repr.length; y++) {
                switch (repr[y].charAt(x)) {
                    case ' ':
                        Assertions.assertEquals(SquareState.UNKNOWN, target.getPlayerSquareState(x, y));
                        break;
                    case 'X':
                        Assertions.assertEquals(SquareState.FLAGGED, target.getPlayerSquareState(x, y));
                        break;
                    case '*':
                        Assertions.assertEquals(SquareState.MINE, target.getPlayerSquareState(x, y));
                        break;
                    default:
                        Assertions.assertEquals(SquareState.PROBED, target.getPlayerSquareState(x, y));
                        Assertions.assertEquals(repr[y].charAt(x) - '0', target.getProbedSquare(x, y));
                }
            }
        }
    }
}
