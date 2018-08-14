package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.MineLocation;

public class UpdateEventEntry {
    private final MineLocation mineLocation;
    private final String label;
    private final int value;

    public MineLocation getMineLocation() {
        return mineLocation;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }

    public UpdateEventEntry(MineLocation mineLocation, String label, int value) {
        this.mineLocation = mineLocation;
        this.label = label;
        this.value = value;
    }
}
