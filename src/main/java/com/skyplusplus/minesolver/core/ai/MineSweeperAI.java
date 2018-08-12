package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.PlayerState;

public interface MineSweeperAI {
     Move calculate(PlayerState state);

     Move calculate(PlayerState state, UpdateHandler handler);
}
