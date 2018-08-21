package com.skyplusplus.minesolver.core.ai;

public interface UpdateHandler<T> {
    void handleUpdate(T updateEvent);
}
