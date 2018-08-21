package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.PlayerView;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.function.Consumer;

class RunAIService extends Service<Void> {

    private MineSweeperAI ai;
    private final UpdateHandler<BoardUpdate> updateHandler;
    private final Consumer<Move> onMakeMove;

    private PlayerView view;

    private int currentAINumber = 0;

    RunAIService(UpdateHandler<BoardUpdate> updateHandler, Consumer<Move> onMakeMove) {
        this.updateHandler = updateHandler;
        this.onMakeMove = onMakeMove;
        this.setOnFailed(value -> value.getSource().getException().printStackTrace());
    }

    void calculate(MineSweeperAI ai, PlayerView view) {
        this.ai = ai;
        this.view = view;
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
                if (view != null) {
                    Move result = ai.calculate(view, event ->
                                Platform.runLater(() -> {
                                    if (currentAINumber == thisAINumber) {
                                        updateHandler.handleUpdate(event);
                                    }
                                })
                            );
                    Platform.runLater(() -> {
                        if (currentAINumber == thisAINumber) {
                            onMakeMove.accept(result);
                        }
                    });
                }
                return null;
            }
        };
    }
}
