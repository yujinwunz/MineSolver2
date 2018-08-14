package com.skyplusplus.minesolver.core.ai;

import java.util.List;

public interface UpdateHandler {
    void handleUpdate(List<UpdateEventEntry> updateEvent);
}
