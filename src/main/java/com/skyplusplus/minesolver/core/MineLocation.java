package com.skyplusplus.minesolver.core;

public class MineLocation {
    private int x;
    private int y;
    public MineLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MineLocation) {
            return this.x == ((MineLocation) other).x && this.y == ((MineLocation) other).y;
        } else {
            return false;
        }
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
