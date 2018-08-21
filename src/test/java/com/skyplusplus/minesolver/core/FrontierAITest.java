package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.frontier.FrontierAI;

@SuppressWarnings("WeakerAccess")
public class FrontierAITest extends BackTrackComboAITest {

    @Override
    protected FrontierAI getAI() {
        return new FrontierAI();
    }
}
