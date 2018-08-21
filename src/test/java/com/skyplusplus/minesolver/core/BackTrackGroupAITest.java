package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackGroupAI;
import org.junit.jupiter.api.BeforeEach;

public class BackTrackGroupAITest extends DeterministicAITest<BackTrackGroupAI> {
    @Override
    protected BackTrackGroupAI getAI() {
        return new BackTrackGroupAI();
    }
}
