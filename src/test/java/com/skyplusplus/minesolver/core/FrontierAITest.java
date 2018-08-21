package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.frontier.FrontierAI;

public class FrontierAITest extends DeterministicAITest<FrontierAI> {

    @Override
    protected FrontierAI getAI() {
        return new FrontierAI();
    }
}
