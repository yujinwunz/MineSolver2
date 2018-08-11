package com.skyplusplus.minesolver.game;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class MineGameController {

    private void redraw() {

        gameCanvas.getGraphicsContext2D().setFill(Color.BLUE);
        gameCanvas.getGraphicsContext2D().fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
    }


    @FXML
    public void initialize() {
        gameCanvas.widthProperty().bind(wrapperPane.widthProperty());
        gameCanvas.heightProperty().bind(wrapperPane.heightProperty());


        gameCanvas.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                redraw();
            }
        });
        gameCanvas.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                redraw();
            }
        });
    }

    @FXML
    private Canvas gameCanvas;

    @FXML
    private Pane wrapperPane;
}
