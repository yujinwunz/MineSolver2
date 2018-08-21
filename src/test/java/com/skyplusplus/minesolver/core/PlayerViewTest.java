package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import com.skyplusplus.minesolver.core.gamelogic.SquareState;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PlayerViewTest {
    @Test
    public void shouldInitiallyBeUnknown() {
        PlayerView playerView = new PlayerView(600, 600, 100);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) {
                assertEquals(SquareState.UNKNOWN, playerView.getSquareState(BoardCoord.ofValue(x, y)));
            }
        }
    }

    @Test
    public void shouldStoreStateAndProbes() {
        PlayerView playerView = new PlayerView(40, 20, 10);

        playerView.setBoard(BoardCoord.ofValue(0, 10), SquareState.FLAGGED);
        playerView.setBoard(BoardCoord.ofValue(30, 0), SquareState.MINE);
        playerView.setBoard(BoardCoord.ofValue(7, 8), 0);
        playerView.setBoard(BoardCoord.ofValue(39, 19), 3);

        assertEquals(SquareState.FLAGGED, playerView.getSquareState(BoardCoord.ofValue(0, 10)));
        assertEquals(SquareState.MINE, playerView.getSquareState(BoardCoord.ofValue(30, 0)));
        assertEquals(SquareState.UNKNOWN, playerView.getSquareState(BoardCoord.ofValue(1, 2)));
        assertEquals(0, playerView.getSquareMineCount(BoardCoord.ofValue(7, 8)));
        assertEquals(3, playerView.getSquareMineCount(BoardCoord.ofValue(39, 19)));

        playerView.setBoard(BoardCoord.ofValue(0, 10), SquareState.UNKNOWN);
        assertEquals(SquareState.UNKNOWN, playerView.getSquareState(BoardCoord.ofValue(0, 10)));
    }


    @Test
    public void shouldSetStateToProbedAfterStoringNumber() {
        PlayerView playerView = new PlayerView(10, 20, 0);

        playerView.setBoard(BoardCoord.ofValue(9, 19), 3);

        assertEquals(SquareState.PROBED, playerView.getSquareState(BoardCoord.ofValue(9, 19)));
    }

    @Test
    public void copyShouldNotAffectOriginal() {
        PlayerView playerView = new PlayerView(10, 20, 10);

        playerView.setBoard(BoardCoord.ofValue(5, 10), 4);
        playerView.setBoard(BoardCoord.ofValue(5, 11), SquareState.FLAGGED);

        PlayerView copy = playerView.copy();

        copy.setBoard(BoardCoord.ofValue(5, 12), SquareState.FLAGGED);
        copy.setBoard(BoardCoord.ofValue(5, 11), SquareState.UNKNOWN);
        copy.setBoard(BoardCoord.ofValue(5, 9), 3);

        assertEquals(4, playerView.getSquareMineCount(BoardCoord.ofValue(5, 10)));
        assertEquals(SquareState.FLAGGED, playerView.getSquareState(BoardCoord.ofValue(5, 11)));
        assertEquals(SquareState.FLAGGED, copy.getSquareState(BoardCoord.ofValue(5, 12)));
        assertEquals(SquareState.UNKNOWN, copy.getSquareState(BoardCoord.ofValue(5, 11)));
        assertEquals(SquareState.PROBED, copy.getSquareState(BoardCoord.ofValue(5, 10)));
        assertEquals(SquareState.PROBED, copy.getSquareState(BoardCoord.ofValue(5, 9)));
        assertEquals(4, copy.getSquareMineCount(BoardCoord.ofValue(5, 10)));
        assertEquals(3, copy.getSquareMineCount(BoardCoord.ofValue(5, 9)));
    }

    @Test
    public void shouldFindNeighbours() {
        PlayerView playerView = new PlayerView(10, 10, 10);
        List<BoardCoord> neighbours = playerView.getNeighbours(BoardCoord.ofValue(4, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(3, 4),
                        BoardCoord.ofValue(3, 5),
                        BoardCoord.ofValue(3, 6),
                        BoardCoord.ofValue(4, 6),
                        BoardCoord.ofValue(5, 6),
                        BoardCoord.ofValue(5, 5),
                        BoardCoord.ofValue(5, 4),
                        BoardCoord.ofValue(4, 4)
                ))
        );
    }

    @Test
    public void shouldFindNeighboursOfEdge() {
        PlayerView playerView = new PlayerView(10, 10, 10);

        // Left edge
        List<BoardCoord> neighbours = playerView.getNeighbours(BoardCoord.ofValue(0, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(0, 4),
                        BoardCoord.ofValue(0, 6),
                        BoardCoord.ofValue(1, 4),
                        BoardCoord.ofValue(1, 5),
                        BoardCoord.ofValue(1, 6)
                ))
        );

        // Right edge
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(9, 5));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(9, 4),
                        BoardCoord.ofValue(9, 6),
                        BoardCoord.ofValue(8, 4),
                        BoardCoord.ofValue(8, 5),
                        BoardCoord.ofValue(8, 6)
                ))
        );

        // Top edge
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(5, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(4, 0),
                        BoardCoord.ofValue(6, 0),
                        BoardCoord.ofValue(4, 1),
                        BoardCoord.ofValue(5, 1),
                        BoardCoord.ofValue(6, 1)
                ))
        );

        // Bottom edge
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(5, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(4, 9),
                        BoardCoord.ofValue(6, 9),
                        BoardCoord.ofValue(4, 8),
                        BoardCoord.ofValue(5, 8),
                        BoardCoord.ofValue(6, 8)
                ))
        );
    }

    @Test
    public void shouldFindNeighboursOfCorner() {
        PlayerView playerView = new PlayerView(10, 10, 10);

        // Top Left
        List<BoardCoord> neighbours = playerView.getNeighbours(BoardCoord.ofValue(0, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(0, 1),
                        BoardCoord.ofValue(1, 0),
                        BoardCoord.ofValue(1, 1)
                ))
        );

        // Top Right
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(9, 0));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(9, 1),
                        BoardCoord.ofValue(8, 0),
                        BoardCoord.ofValue(8, 1)
                ))
        );

        // Bottom Left
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(0, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(0, 8),
                        BoardCoord.ofValue(1, 9),
                        BoardCoord.ofValue(1, 8)
                ))
        );

        // Bottom Right
        neighbours = playerView.getNeighbours(BoardCoord.ofValue(9, 9));
        assertEquals(
                new HashSet<>(neighbours),
                new HashSet<>(Arrays.asList(
                        BoardCoord.ofValue(9, 8),
                        BoardCoord.ofValue(8, 9),
                        BoardCoord.ofValue(8, 8)
                ))
        );
    }
}
