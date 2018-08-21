package com.skyplusplus.minesolver.core.ai;

import java.util.List;

public interface UpdateHandler<T> {
    void handleUpdate(UpdateEvent<T> updateEvent);
}
