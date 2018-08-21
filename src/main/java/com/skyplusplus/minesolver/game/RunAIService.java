package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

class RunAIService extends Service<Void> {

    private MineSweeperAI ai;
    private final UpdateHandler updateHandler;
    private final AICompletedHandler completedHandler;

    private PlayerState state;

    private int currentAINumber = 0;

    RunAIService(UpdateHandler updateHandler, AICompletedHandler completedHandler) {
        this.updateHandler = updateHandler;
        this.completedHandler = completedHandler;
        this.setOnFailed(value -> {
            value.getSource().getException().printStackTrace();
        });
    }

    void calculate(MineSweeperAI ai, PlayerState state) {
        this.ai = ai;
        this.state = state;
        this.currentAINumber ++;
        this.restart();
    }

    @Override
    public boolean cancel() {
        currentAINumber++;
        return super.cancel();
    }

    @Override
    protected Task<Void> createTask() {
        final int thisAINumber = currentAINumber;
        return new Task<Void>() {
            @Override
            protected Void call() {
                if (state != null) {
                    Move result = ai.calculate(state, event ->
                                Platform.runLater(() -> {
                                    if (currentAINumber == thisAINumber) {
                                        updateHandler.handleUpdate(event);
                                    }
                                })
                            );
                    Platform.runLater(() -> {
                        if (currentAINumber == thisAINumber) {
                            completedHandler.onAiComplete(result);
                        }
                    });
                }
                return null;
            }
        };
    }
}
