package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.gamelogic.*;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public abstract class AITest <T extends MineSweeperAI> {
    protected T mineSweeperAI;

    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    protected void assertCanWinGame(MineSweeper mineSweeper) {
        while (mineSweeper.getGameState() == GameState.IN_PROGRESS) {
            boolean didMove = false;

            logger.info("\n" + String.join("\n", mineSweeper.toStringArray()));
            Move move = mineSweeperAI.calculate(mineSweeper.clonePlayerState());

            for (BoardCoord boardCoord : move.getToFlag()) {
                logger.info(String.format("Flag: %d %d\n", boardCoord.getX(), boardCoord.getY()));
                if (mineSweeper.flag(boardCoord) == FlagResult.FLAGGED) {
                    didMove = true;
                }
            }
            for (BoardCoord boardCoord : move.getToProbe()) {
                logger.info(String.format("Probe: %d %d\n", boardCoord.getX(), boardCoord.getY()));
                if (mineSweeper.probe(boardCoord) == ProbeResult.OK) {
                    didMove = true;
                }
            }
            assertNotEquals(GameState.LOSE, mineSweeper.getGameState());
            assertTrue(didMove); // Should do something at every iteration
        }
        assertEquals(GameState.WIN, mineSweeper.getGameState());
    }
}
