package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.MineLocation;
import javafx.util.Pair;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Move {
    private List<MineLocation> toProbe;
    private List<MineLocation> toFlag;

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
