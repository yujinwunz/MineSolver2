package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.MineLocation;

import java.util.Collections;
import java.util.List;

public class Move {
    private final List<MineLocation> toProbe;
    private final List<MineLocation> toFlag;

    public Move(List<MineLocation> toProbe, List<MineLocation> toFlag) {
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

    public List<MineLocation> getToProbe() {
        return toProbe;
    }

    public List<MineLocation> getToFlag() {
        return toFlag;
    }
}
