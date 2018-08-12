package com.skyplusplus.minesolver.core.ai;

/**
 * Contains some graphical info for displaying AI progress.
 */
public class UpdateEvent {
    private int value[][];
    private String label[][];
    private String message;

    public UpdateEvent(int width, int height, String message) {
        value = new int[width][height];
        label = new String[width][height];
        this.message = message;
    }

    public void setLabel(int x, int y, String label) {
        this.label[x][y] = label;
    }

    public void setValue(int x, int y, int value) {
        this.value[x][y] = value;
    }

    public String getLabel(int x, int y) {
        return label[x][y];
    }

    public int getValue(int x, int y) {
        return value[x][y];
    }

    public String getMessage() {
        return message;
    }
}
