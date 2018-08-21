package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackAI;
import org.junit.jupiter.api.BeforeEach;

public class BackTrackAITest extends DeterministicAITest<BackTrackAI> {

    @Override
    protected BackTrackAI getAI() {
        return new BackTrackAI();
    }
}
