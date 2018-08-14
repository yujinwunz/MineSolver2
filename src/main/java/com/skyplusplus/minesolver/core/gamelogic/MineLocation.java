package com.skyplusplus.minesolver.core.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class MineLocation {
    private static final int CACHE_MAX_X = 300;
    private static final int CACHE_MAX_Y = 300;
    private static final MineLocation[][] cache = new MineLocation[CACHE_MAX_X][CACHE_MAX_Y];

    private final int x;
    private final int y;

    public static MineLocation ofValue(int x, int y) {
        if (x >= 0 && x < CACHE_MAX_X && y >= 0 && y < CACHE_MAX_Y) {
            if (cache[x][y] == null) {
                cache[x][y] = new MineLocation(x, y);
            }
            return cache[x][y];
        }
        return new MineLocation(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private MineLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MineLocation) {
            return this.x == ((MineLocation) other).x && this.y == ((MineLocation) other).y;
        } else {
            return false;
        }
    }

    public List<MineLocation> getNeighbours(int width, int height) {
        List<MineLocation> retVal = new ArrayList<>();
        for (int x = Math.max(0, this.x-1); x <= Math.min(width-1, this.x+1); x++) {
            for (int y = Math.max(0, this.y-1); y <= Math.min(height-1, this.y+1); y++) {
                if (x == this.x && y == this.y) continue;
                retVal.add(new MineLocation(x, y));
            }
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        return this.x * 100000 + this.y;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", this.x, this.y);
    }
}
