package com.skyplusplus.minesolver.core.ai;

import java.util.List;

public class UpdateEvent<T> {
    private List<UpdateEventEntry<T>> entries;
    private String message;

    public List<UpdateEventEntry<T>> getEntries() {
        return entries;
    }

    public String getMessage() {
        return message;
    }

    public UpdateEvent(List<UpdateEventEntry<T>> entries, String message) {
        this.entries = entries;
        this.message = message;
    }
}
