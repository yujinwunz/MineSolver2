package com.skyplusplus.minesolver.core.ai;

import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;

public class BoardUpdateEntry {
    private final BoardCoord boardCoord;
    private final UpdateColor color;
    private final String tag;

    public BoardCoord getBoardCoord() {
        return boardCoord;
    }

    public UpdateColor getColor() {
        return color;
    }

    public String getTag() {
        return tag;
    }

    public BoardUpdateEntry(BoardCoord boardCoord, UpdateColor color, String tag) {
        this.boardCoord = boardCoord;
        this.color = color;
        this.tag = tag;
    }
}
