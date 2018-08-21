package com.skyplusplus.minesolver.core.ai;

public class UpdateEventEntry<T> {
    private final T object;
    private final String label;
    private final int value;

    public T getObject() {
        return object;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }

    public UpdateEventEntry(T object, String label, int value) {
        this.object = object;
        this.label = label;
        this.value = value;
    }
}
