package com.skyplusplus.minesolver.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlayerStateTest {
    @Test
    public void shouldInitiallyBeUnknown() {
        PlayerState playerState = new PlayerState(600, 600);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) {
                Assertions.assertEquals(SquareState.UNKNOWN, playerState.getBoardState(x, y));
            }
        }
    }

    @Test
    public void shouldStoreStateAndProbes() {
        PlayerState playerState = new PlayerState(40, 20);

        playerState.setBoard(0, 10, SquareState.FLAGGED);
        playerState.setBoard(39, 0, SquareState.MINE);
        playerState.setBoard(7, 8, 0);
        playerState.setBoard(39, 19, 3);

        Assertions.assertEquals(SquareState.FLAGGED, playerState.getBoardState(0, 10));
        Assertions.assertEquals(SquareState.MINE, playerState.getBoardState(39, 0));
        Assertions.assertEquals(SquareState.UNKNOWN, playerState.getBoardState(1, 2));
        Assertions.assertEquals(0, playerState.getProbedSquare(7, 8));
        Assertions.assertEquals(3, playerState.getProbedSquare(39, 19));

        playerState.setBoard(0, 10, SquareState.UNKNOWN);
        Assertions.assertEquals(SquareState.UNKNOWN, playerState.getBoardState(0, 10));
    }


    @Test
    public void shouldSetStateToProbedAfterStoringNumber() {
        PlayerState playerState = new PlayerState(10, 20);

        playerState.setBoard(9, 19, 3);

        Assertions.assertEquals(SquareState.PROBED, playerState.getBoardState(9, 19));
    }

    @Test
    public void copyShouldNotAffectOriginal() {
        PlayerState playerState = new PlayerState(10, 20);

        playerState.setBoard(5, 10, 4);
        playerState.setBoard(5, 11, SquareState.FLAGGED);

        PlayerState copy = playerState.copy();

        copy.setBoard(5, 12, SquareState.FLAGGED);
        copy.setBoard(5, 11, SquareState.UNKNOWN);
        copy.setBoard(5, 9, 3);

        Assertions.assertEquals(4, playerState.getProbedSquare(5, 10));
        Assertions.assertEquals(SquareState.FLAGGED, playerState.getBoardState(5, 11));
        Assertions.assertEquals(SquareState.FLAGGED, copy.getBoardState(5, 12));
        Assertions.assertEquals(SquareState.UNKNOWN, copy.getBoardState(5, 11));
        Assertions.assertEquals(SquareState.PROBED, copy.getBoardState(5, 10));
        Assertions.assertEquals(SquareState.PROBED, copy.getBoardState(5, 9));
        Assertions.assertEquals(4, copy.getProbedSquare(5, 10));
        Assertions.assertEquals(3, copy.getProbedSquare(5, 9));
    }
}
