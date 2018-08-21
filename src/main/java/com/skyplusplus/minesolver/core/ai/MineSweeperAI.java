package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;


public abstract class MineSweeperAI extends IncrementalWorker<MineLocation> {
    public abstract Move calculate(PlayerState state);

    public final Move calculate(PlayerState state, UpdateHandler handler) {
        this.handler = handler;
        return calculate(state);
    }

}
