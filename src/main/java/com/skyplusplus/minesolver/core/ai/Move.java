package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;

import java.util.Collections;
import java.util.List;

public class Move {
    private final List<BoardCoord> toProbe;
    private final List<BoardCoord> toFlag;

    public Move(List<BoardCoord> toProbe, List<BoardCoord> toFlag) {
        if (toProbe == null) {
            this.toProbe = Collections.emptyList();
        } else {
            this.toProbe = toProbe;
        }
        if (toFlag == null) {
            this.toFlag = Collections.emptyList();
        } else {
            this.toFlag = toFlag;
        }
    }

    public List<BoardCoord> getToProbe() {
        return toProbe;
    }

    public List<BoardCoord> getToFlag() {
        return toFlag;
    }
}
