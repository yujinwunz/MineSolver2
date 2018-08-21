package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.PlayerView;


public abstract class MineSweeperAI extends IncrementalWorker<BoardUpdate> {
    public abstract Move calculate(PlayerView view);

    public final Move calculate(PlayerView view, UpdateHandler<BoardUpdate> handler) {
        this.handler = handler;
        return calculate(view);
    }

}
