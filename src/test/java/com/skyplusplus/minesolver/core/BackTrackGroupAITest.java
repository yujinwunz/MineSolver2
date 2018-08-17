package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackGroupAI;
import org.junit.jupiter.api.BeforeEach;

public class BackTrackGroupAITest extends BackTrackAITest {
    @BeforeEach
    @Override
    public void setup() {
        this.mineSweeperAI = new BackTrackGroupAI();
    }
}
