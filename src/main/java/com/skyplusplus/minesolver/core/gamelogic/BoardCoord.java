package com.skyplusplus.minesolver.core.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class BoardCoord {
    private static final int CACHE_MAX_X = 300;
    private static final int CACHE_MAX_Y = 300;
    private static final BoardCoord[][] cache = new BoardCoord[CACHE_MAX_X][CACHE_MAX_Y];

    private final int x;
    private final int y;

    public static BoardCoord ofValue(int x, int y) {
        if (x >= 0 && x < CACHE_MAX_X && y >= 0 && y < CACHE_MAX_Y) {
            if (cache[x][y] == null) {
                cache[x][y] = new BoardCoord(x, y);
            }
            return cache[x][y];
        }
        return new BoardCoord(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private BoardCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BoardCoord) {
            return this.x == ((BoardCoord) other).x && this.y == ((BoardCoord) other).y;
        } else {
            return false;
        }
    }

    public List<BoardCoord> getNeighbours(int width, int height) {
        List<BoardCoord> retVal = new ArrayList<>();
        for (int x = Math.max(0, this.x-1); x <= Math.min(width-1, this.x+1); x++) {
            for (int y = Math.max(0, this.y-1); y <= Math.min(height-1, this.y+1); y++) {
                if (x == this.x && y == this.y) continue;
                retVal.add(BoardCoord.ofValue(x, y));
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
