package com.skyplusplus.minesolver.core.ai;

import java.util.List;

public class BoardUpdate {
    private final List<BoardUpdateEntry> entries;
    private final String message;

    public List<BoardUpdateEntry> getEntries() {
        return entries;
    }

    public String getMessage() {
        return message;
    }

    public BoardUpdate(List<BoardUpdateEntry> entries, String message) {
        this.entries = entries;
        this.message = message;
    }
}
