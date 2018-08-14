package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PlayerStateTest {
    @Test
    public void shouldInitiallyBeUnknown() {
        PlayerState playerState = new PlayerState(600, 600, 100);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) {
                assertEquals(SquareState.UNKNOWN, playerState.getSquareState(MineLocation.ofValue(x, y)));
            }
        }
    }

    @Test
    public void shouldStoreStateAndProbes() {
        PlayerState playerState = new PlayerState(40, 20, 10);

        playerState.setBoard(MineLocation.ofValue(0, 10), SquareState.FLAGGED);
        playerState.setBoard(MineLocation.ofValue(30, 0), SquareState.MINE);
        playerState.setBoard(MineLocation.ofValue(7, 8), 0);
        playerState.setBoard(MineLocation.ofValue(39, 19), 3);

        assertEquals(SquareState.FLAGGED, playerState.getSquareState(MineLocation.ofValue(0, 10)));
        assertEquals(SquareState.MINE, playerState.getSquareState(MineLocation.ofValue(30, 0)));
        assertEquals(SquareState.UNKNOWN, playerState.getSquareState(MineLocation.ofValue(1, 2)));
        assertEquals(0, playerState.getSquareMineCount(MineLocation.ofValue(7, 8)));
        assertEquals(3, playerState.getSquareMineCount(MineLocation.ofValue(39, 19)));

        playerState.setBoard(MineLocation.ofValue(0, 10), SquareState.UNKNOWN);
        assertEquals(SquareState.UNKNOWN, playerState.getSquareState(MineLocation.ofValue(0, 10)));
    }


    @Test
    public void shouldSetStateToProbedAfterStoringNumber() {
        PlayerState playerState = new PlayerState(10, 20, 0);

        playerState.setBoard(MineLocation.ofValue(9, 19), 3);

        assertEquals(SquareState.PROBED, playerState.getSquareState(MineLocation.ofValue(9, 19)));
    }

    @Test
    public void copyShouldNotAffectOriginal() {
        PlayerState playerState = new PlayerState(10, 20, 10);

        playerState.setBoard(MineLocation.ofValue(5, 10), 4);
        playerState.setBoard(MineLocation.ofValue(5, 11), SquareState.FLAGGED);

        PlayerState copy = playerState.copy();

        copy.setBoard(MineLocation.ofValue(5, 12), SquareState.FLAGGED);
        copy.setBoard(MineLocation.ofValue(5, 11), SquareState.UNKNOWN);
        copy.setBoard(MineLocation.ofValue(5, 9), 3);

        assertEquals(4, playerState.getSquareMineCount(MineLocation.ofValue(5, 10)));
        assertEquals(SquareState.FLAGGED, playerState.getSquareState(MineLocation.ofValue(5, 11)));
        assertEquals(SquareState.FLAGGED, copy.getSquareState(MineLocation.ofValue(5, 12)));
        assertEquals(SquareState.UNKNOWN, copy.getSquareState(MineLocation.ofValue(5, 11)));
        assertEquals(SquareState.PROBED, copy.getSquareState(MineLocation.ofValue(5, 10)));
        assertEquals(SquareState.PROBED, copy.getSquareState(MineLocation.ofValue(5, 9)));
        assertEquals(4, copy.getSquareMineCount(MineLocation.ofValue(5, 10)));
        assertEquals(3, copy.getSquareMineCount(MineLocation.ofValue(5, 9)));
    }

    @Test
    public void shouldFindNeighbours() {
        PlayerState playerState = new PlayerState(10, 10, 10);
        List<MineLocation> neighbours = playerState.getNeighbours(MineLocation.ofValue(4, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(3, 4),
                        MineLocation.ofValue(3, 5),
                        MineLocation.ofValue(3, 6),
                        MineLocation.ofValue(4, 6),
                        MineLocation.ofValue(5, 6),
                        MineLocation.ofValue(5, 5),
                        MineLocation.ofValue(5, 4),
                        MineLocation.ofValue(4, 4)
                ))
        );
    }

    @Test
    public void shouldFindNeighboursOfEdge() {
        PlayerState playerState = new PlayerState(10, 10, 10);

        // Left edge
        List<MineLocation> neighbours = playerState.getNeighbours(MineLocation.ofValue(0, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(0, 4),
                        MineLocation.ofValue(0, 6),
                        MineLocation.ofValue(1, 4),
                        MineLocation.ofValue(1, 5),
                        MineLocation.ofValue(1, 6)
                ))
        );

        // Right edge
        neighbours = playerState.getNeighbours(MineLocation.ofValue(9, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(9, 4),
                        MineLocation.ofValue(9, 6),
                        MineLocation.ofValue(8, 4),
                        MineLocation.ofValue(8, 5),
                        MineLocation.ofValue(8, 6)
                ))
        );

        // Top edge
        neighbours = playerState.getNeighbours(MineLocation.ofValue(5, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(4, 0),
                        MineLocation.ofValue(6, 0),
                        MineLocation.ofValue(4, 1),
                        MineLocation.ofValue(5, 1),
                        MineLocation.ofValue(6, 1)
                ))
        );

        // Bottom edge
        neighbours = playerState.getNeighbours(MineLocation.ofValue(5, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(4, 9),
                        MineLocation.ofValue(6, 9),
                        MineLocation.ofValue(4, 8),
                        MineLocation.ofValue(5, 8),
                        MineLocation.ofValue(6, 8)
                ))
        );
    }

    @Test
    public void shouldFindNeighboursOfCorner() {
        PlayerState playerState = new PlayerState(10, 10, 10);

        // Top Left
        List<MineLocation> neighbours = playerState.getNeighbours(MineLocation.ofValue(0, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(0, 1),
                        MineLocation.ofValue(1, 0),
                        MineLocation.ofValue(1, 1)
                ))
        );

        // Top Right
        neighbours = playerState.getNeighbours(MineLocation.ofValue(9, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(9, 1),
                        MineLocation.ofValue(8, 0),
                        MineLocation.ofValue(8, 1)
                ))
        );

        // Bottom Left
        neighbours = playerState.getNeighbours(MineLocation.ofValue(0, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(0, 8),
                        MineLocation.ofValue(1, 9),
                        MineLocation.ofValue(1, 8)
                ))
        );

        // Bottom Right
        neighbours = playerState.getNeighbours(MineLocation.ofValue(9, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        MineLocation.ofValue(9, 8),
                        MineLocation.ofValue(8, 9),
                        MineLocation.ofValue(8, 8)
                ))
        );
    }
}
