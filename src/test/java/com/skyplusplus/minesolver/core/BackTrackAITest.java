package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackAI;

@SuppressWarnings("WeakerAccess")
public class BackTrackAITest extends DeterministicAITest<BackTrackAI> {

    @Override
    protected BackTrackAI getAI() {
        return new BackTrackAI();
    }
}
