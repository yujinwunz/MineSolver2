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

            for (MineLocation mineLocation: move.getToFlag()) {
                logger.info(String.format("Flag: %d %d\n", mineLocation.getX(), mineLocation.getY()));
                if (mineSweeper.flag(mineLocation) == FlagResult.FLAGGED) {
                    didMove = true;
                }
            }
            for (MineLocation mineLocation: move.getToProbe()) {
                logger.info(String.format("Probe: %d %d\n", mineLocation.getX(), mineLocation.getY()));
                if (mineSweeper.probe(mineLocation) == ProbeResult.OK) {
                    didMove = true;
                }
            }
            assertNotEquals(GameState.LOSE, mineSweeper.getGameState());
            assertTrue(didMove); // Should do something at every iteration
        }
        assertEquals(GameState.WIN, mineSweeper.getGameState());
    }
}
