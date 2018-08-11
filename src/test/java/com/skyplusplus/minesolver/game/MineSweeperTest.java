package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.SquareState;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class MineSweeperTest {
    @Test
    public void shouldInitializeWithCorrectMineCount() {

    }

    @Test
    public void shouldCascadeWithFirstProbe() {

    }

    @Test
    public void shouldWinWhenAllSafeSquaresAreProbed() {

    }

    @Test
    public void shouldNotWinWhileSafeSquaresAreUnprobed() {

    }

    @Test
    public void shouldSweepAndCascadeCornersAndEdges() {

    }

    @Test
    public void shouldOnlySweepReadySquares() {

    }

    @Test
    public void shouldNotProbeSweepOrCascadeFlags() {

    }

    @Test
    public void shouldLoseAfterProbingOrSweepingMine() {

    }

    @Test
    public void shouldOnlyProbeSweepOrFlagWhileGameIsInProgress() {

    }

    private void assertBoardState(String[] repr, MineSweeper target, GameState gameState) {
        Assertions.assertEquals(repr.length, target.getHeight());
        Assertions.assertEquals(repr[0].length(), target.getHeight());

        Assertions.assertEquals(gameState, target.getGameState());

        for (int x = 0; x < repr[0].length(); x++) {
            for (int y = 0; y < repr.length; y++) {
                switch (repr[y].charAt(x)) {
                    case '?':
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
