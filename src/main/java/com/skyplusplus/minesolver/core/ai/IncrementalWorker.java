package com.skyplusplus.minesolver.core.ai;

import java.util.function.Supplier;

public abstract class IncrementalWorker<T> {

    private static final long UPDATE_DELAY_MS = 100;
    protected UpdateHandler<T> handler;
    private long lastUpdate = 0;

    protected final void reportProgress(Supplier<UpdateEvent<T>> supplier) throws InterruptedException {
        if (handler != null) {
            if (System.currentTimeMillis() - lastUpdate > UPDATE_DELAY_MS) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                handler.handleUpdate(supplier.get());
                lastUpdate = System.currentTimeMillis();
            }
        }
    }
}
