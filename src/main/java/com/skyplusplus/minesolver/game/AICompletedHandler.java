package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.Move;

interface AICompletedHandler {
    void onAiComplete(Move move);
}
