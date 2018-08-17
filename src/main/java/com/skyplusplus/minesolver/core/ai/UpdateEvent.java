package com.skyplusplus.minesolver.core.ai;

import java.util.List;

public class UpdateEvent {
    private List<UpdateEventEntry> entries;
    private String message;

    public List<UpdateEventEntry> getEntries() {
        return entries;
    }

    public String getMessage() {
        return message;
    }

    public UpdateEvent(List<UpdateEventEntry> entries, String message) {
        this.entries = entries;
        this.message = message;
    }
}
