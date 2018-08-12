package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.MineLocation;
import com.skyplusplus.minesolver.core.MineSweeper;
import com.skyplusplus.minesolver.core.SquareState;
import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.simpleai.SimpleAI;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;


public class GameController {

    private static final int SQUARE_PADDING = 2;

    private MouseEvent lastMouseEvent;
    private MouseEvent lastPressedEvent;

    private MineSweeper mineSweeper;

    private double boardOffsetX;
    private double boardOffsetY;
    private double squareWidth;

    private MineSweeperAI mineSweeperAI = new SimpleAI();

    @FXML
    private TextField minesRemainingTextField;
    @FXML
    protected TextField colsTextField;
    @FXML
    protected TextField rowsTextField;
    @FXML
    protected TextField minesTextField;
    @FXML
    private Canvas gameCanvas;

    private void redraw() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFill(Color.GRAY);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        this.squareWidth = (int)Math.min(
                gameCanvas.getWidth() / mineSweeper.getWidth(),
                gameCanvas.getHeight() / mineSweeper.getHeight()
        );

        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, squareWidth * 0.8));

        this.boardOffsetY = (gameCanvas.getHeight() - squareWidth * mineSweeper.getHeight()) / 2;
        this.boardOffsetX = (gameCanvas.getWidth() - squareWidth * mineSweeper.getWidth()) / 2;

        boolean isMouseDown = lastMouseEvent != null && lastMouseEvent.isPrimaryButtonDown();
        Coordinate lastPressedBox = null;
        Coordinate thisHoveredBox = null;
        if (lastPressedEvent != null) {
            lastPressedBox = getBoardCoordinate(lastPressedEvent);
        }
        if (lastMouseEvent != null) {
            thisHoveredBox = getBoardCoordinate(lastMouseEvent);
        }


        for (int x = 0; x < mineSweeper.getWidth(); x++) {
            for (int y = 0; y < mineSweeper.getHeight(); y++) {

                if (thisHoveredBox != null && thisHoveredBox.getX() == x && thisHoveredBox.getY() == y) {
                    if (isMouseDown && thisHoveredBox.equals(lastPressedBox)) {
                        gc.setFill(Color.DARKGRAY);
                    } else {
                        gc.setFill(Color.BLUE);
                    }
                } else {
                    gc.setFill(Color.DARKBLUE);
                }

                if (mineSweeper.getPlayerSquareState(x, y) == SquareState.PROBED) {
                    gc.setFill(Color.LIGHTGRAY);
                } else if (mineSweeper.getPlayerSquareState(x, y) == SquareState.FLAGGED) {
                    gc.setFill(Color.RED);
                } else if (mineSweeper.getPlayerSquareState(x, y) == SquareState.MINE) {
                    gc.setFill(Color.BLACK);
                }

                gc.fillRect(
                        squareWidth * x + SQUARE_PADDING + boardOffsetX,
                        squareWidth * y + SQUARE_PADDING + boardOffsetY,
                        squareWidth * (x + 1) - squareWidth * x - SQUARE_PADDING,
                        squareWidth * (y + 1) - squareWidth * y - SQUARE_PADDING
                );

                if (mineSweeper.getPlayerSquareState(x, y) == SquareState.PROBED
                        && mineSweeper.getProbedSquare(x, y) > 0) {

                    Color textColor = getSquareNumberColor(mineSweeper.getProbedSquare(x, y));
                    gc.setFill(textColor);

                    gc.fillText(
                            Integer.toString(mineSweeper.getProbedSquare(x, y)),
                            squareWidth * x + squareWidth / 2 + boardOffsetX,
                            squareWidth * y + squareWidth / 2 + boardOffsetY
                    );
                }
            }
        }

    }

    private Color getSquareNumberColor(int probedSquare) {
        switch (probedSquare) {
            case 1:
                return Color.BLUE;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.RED;
            case 4:
                return Color.DARKBLUE;
            case 5:
                return Color.DARKRED;
            case 6:
                return Color.CYAN;
            case 7:
                return Color.BLACK;
            case 8:
                return Color.PINK;
            default:
                throw new IllegalArgumentException();
        }
    }

    @FXML
    public void initialize() {
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

        gameCanvas.setOnMouseEntered(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseExited(mouseEvent -> registerMouseEvent(null));
        gameCanvas.setOnMouseReleased(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMousePressed(mouseEvent -> registerMouseEvent(mouseEvent, mouseEvent));
        gameCanvas.setOnMouseMoved(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseDragged(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseClicked(mouseEvent -> onClick(mouseEvent));

        startNewGame();
    }

    private void registerMouseEvent(MouseEvent lastMouseEvent) {
        this.lastMouseEvent = lastMouseEvent;
        redraw();
    }

    private void registerMouseEvent(MouseEvent lastMouseEvent, MouseEvent lastPressedEvent) {
        this.lastPressedEvent = lastPressedEvent;
        registerMouseEvent(lastMouseEvent);
    }

    private void onClick(MouseEvent mouseEvent) {
        this.lastMouseEvent = mouseEvent;
        Coordinate coordinate = getBoardCoordinate(mouseEvent);

        ProbeResult result = null;
        if (coordinate.getX() >= 0
                && coordinate.getX() < mineSweeper.getWidth()
                && coordinate.getY() >= 0
                && coordinate.getY() < mineSweeper.getHeight()
        ) {

            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                if (mouseEvent.getClickCount() > 1) {
                    // Sweep
                    result = mineSweeper.sweep(coordinate.getX(), coordinate.getY());
                } else {
                    // Probe
                    result = mineSweeper.probe(coordinate.getX(), coordinate.getY());
                }
            } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                mineSweeper.toggleFlag(coordinate.getX(), coordinate.getY());
            } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
                // Only sweeping
                result = mineSweeper.sweep(coordinate.getX(), coordinate.getY());
            }
        }

        redraw();
        if (result != null && result != ProbeResult.NOP) {
            checkGameState();
        }
    }

    private void checkGameState() {
        if (mineSweeper.getGameState() == GameState.WIN) {
            Alert alert = new  Alert(Alert.AlertType.CONFIRMATION,
                    "You have won!",
                    ButtonType.OK);
            alert.showAndWait();
        } else if (mineSweeper.getGameState() == GameState.LOSE) {
            Alert alert = new  Alert(Alert.AlertType.WARNING,
                    "You lost.",
                    ButtonType.OK);
            alert.showAndWait();
        }
        minesRemainingTextField.setText(Integer.toString(mineSweeper.getTotalMinesMinusFlags()));
    }


    private Coordinate getBoardCoordinate(MouseEvent mouseEvent) {
        return new Coordinate(
                (int) ((mouseEvent.getX() - boardOffsetX) / squareWidth),
                (int) ((mouseEvent.getY() - boardOffsetY) / squareWidth)
        );
    }

    private void startNewGame() {
        try {
            mineSweeper = new MineSweeper(
                    Integer.parseInt(colsTextField.getText()),
                    Integer.parseInt(rowsTextField.getText()),
                    Integer.parseInt(minesTextField.getText())
            );
            checkGameState();
        } catch (NumberFormatException ex) {
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Columns, rows and #mines must be numbers",
                    ButtonType.OK);
            alert.showAndWait();
        } catch (MineSweeper.TooManyMinesException ex) {
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Too many mines",
                    ButtonType.OK);
            alert.showAndWait();
        } catch (IllegalArgumentException ex) {
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Columns and rows must be > 0. Mines must be >= 0.",
                    ButtonType.OK);
            alert.showAndWait();
        }
        redraw();
    }

    @FXML
    protected void onNewGame(ActionEvent actionEvent) {
       startNewGame();
    }

    @FXML
    protected void onUseAI(ActionEvent actionEvent) {
        Move move = mineSweeperAI.calculate(mineSweeper.clonePlayerState());
        boolean didSomething = false;
        for (MineLocation location: move.getToFlag()) {
            if (mineSweeper.flag(location.getX(), location.getY()) != FlagResult.NOP) {
                didSomething = true;
            }
        }
        for (MineLocation location: move.getToProbe()) {
            if (mineSweeper.probe(location.getX(), location.getY()) != ProbeResult.NOP) {
                didSomething = true;
            };
        }
        if (didSomething) {
            redraw();
            checkGameState();
        }
    }

    private static class Coordinate {
        private int x;
        private int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Coordinate) {
                Coordinate coordinate = (Coordinate) other;
                return this.x == coordinate.x && this.y == coordinate.y;
            }
            return false;
        }
    }
}
