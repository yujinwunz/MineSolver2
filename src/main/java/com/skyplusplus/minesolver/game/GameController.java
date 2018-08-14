package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.UpdateEventEntry;
import com.skyplusplus.minesolver.core.backtrackai.BackTrackAI;
import com.skyplusplus.minesolver.core.gamelogic.*;
import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Collections;
import java.util.List;


public class GameController {

    private static final int SQUARE_PADDING = 2;

    private MouseEvent lastMouseEvent;
    private MouseEvent lastPressedEvent;

    private MineSweeper mineSweeper;
    private RunAIService aiService;

    private double boardOffsetX;
    private double boardOffsetY;
    private double squareWidth;

    private GameAppState currentAppState;

    @SuppressWarnings("CanBeFinal")
    private MineSweeperAI mineSweeperAI = new BackTrackAI();

    private List<UpdateEventEntry> lastAiUpdate = null;

    @FXML
    protected TextField minesRemainingTextField;
    @FXML
    protected TextField colsTextField;
    @FXML
    protected TextField rowsTextField;
    @FXML
    protected TextField minesTextField;
    @FXML
    protected Canvas gameCanvas;
    @FXML
    protected Button useAiBtn;
    @FXML
    protected Pane wrapperPane;

    private void redraw() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        recalculateBoardParameters();

        redrawBackground(gc);

        boolean isMouseDown = lastMouseEvent != null && lastMouseEvent.isPrimaryButtonDown();
        MineLocation lastHoveredSquare = null;
        MineLocation thisHoveredSquare = null;
        if (lastPressedEvent != null) {
            lastHoveredSquare = getBoardLocation(lastPressedEvent);
        }
        if (lastMouseEvent != null) {
            thisHoveredSquare = getBoardLocation(lastMouseEvent);
        }


        for (int x = 0; x < mineSweeper.getWidth(); x++) {
            for (int y = 0; y < mineSweeper.getHeight(); y++) {
                MineLocation thisLocation = MineLocation.ofValue(x, y);

                SquareControlState controlState =
                        getSquareControlState(thisLocation, thisHoveredSquare, lastHoveredSquare, isMouseDown);
                if (currentAppState != GameAppState.GAME_IN_PROGRESS) {
                    controlState = SquareControlState.NEUTRAL;
                }

                Color squareColor = getSquareColor(mineSweeper.getPlayerSquareState(thisLocation), controlState);
                drawBoardRect(gc, x, y, squareColor);

                if (mineSweeper.getPlayerSquareState(thisLocation) == SquareState.PROBED
                        && mineSweeper.getProbedSquare(thisLocation) > 0) {

                    Color textColor = getSquareNumberColor(mineSweeper.getProbedSquare(thisLocation));
                    drawTextToSquare(
                            gc,
                            Integer.toString(mineSweeper.getProbedSquare(thisLocation)),
                            x,
                            y,
                            textColor,
                            0.8
                    );
                }
            }
        }
        redrawAIInfo(gc);
    }

    private static Color getSquareNumberColor(int probedSquare) {
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
                return Color.HOTPINK;
            case 8:
                return Color.DEEPPINK;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static SquareControlState getSquareControlState(
            MineLocation thisSquare,
            MineLocation thisHoveredSquare,
            MineLocation lastHoveredSquare,
            boolean isMouseDown
    ) {
        if (thisHoveredSquare != null && thisHoveredSquare.equals(thisSquare)) {
            if (isMouseDown && thisHoveredSquare.equals(lastHoveredSquare)) {
                return SquareControlState.PRESSED;
            } else {
                return SquareControlState.HOVERED;
            }
        } else {
            return SquareControlState.NEUTRAL;
        }
    }

    private static Color getSquareColor(SquareState state, SquareControlState controlState) {
        switch (state) {
            case UNKNOWN:
                switch (controlState) {
                    case NEUTRAL:
                        return Color.DARKBLUE;
                    case HOVERED:
                        return Color.BLUE;
                    case PRESSED:
                        return Color.DARKGRAY;
                    default:
                        throw new IllegalArgumentException();
                }
            case FLAGGED:
                return Color.RED;
            case MINE:
                return Color.BLACK;
            case PROBED:
                return Color.LIGHTGRAY;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static Color getAIInfoSquareColor(UpdateEventEntry entry) {
        switch (entry.getLabel()) {
            case "Mine":
                return Color.rgb(255, 0, 0, 0.4);
            case "Not mine":
                return Color.rgb(0, 255, 0, 0.4);
            default:
                return Color.rgb(255, 255, 0, 0.4);
        }
    }

    private MineLocation getBoardLocation(MouseEvent mouseEvent) {
        return MineLocation.ofValue(
                (int) ((mouseEvent.getX() - boardOffsetX) / squareWidth),
                (int) ((mouseEvent.getY() - boardOffsetY) / squareWidth)
        );
    }

    private void recalculateBoardParameters() {
        this.squareWidth = (int)Math.min(
                gameCanvas.getWidth() / mineSweeper.getWidth(),
                gameCanvas.getHeight() / mineSweeper.getHeight()
        );

        this.boardOffsetY = (gameCanvas.getHeight() - squareWidth * mineSweeper.getHeight()) / 2;
        this.boardOffsetX = (gameCanvas.getWidth() - squareWidth * mineSweeper.getWidth()) / 2;
    }

    private void redrawBackground(GraphicsContext gc) {
        gc.setFill(Color.GRAY);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
    }

    private void redrawAIInfo(GraphicsContext gc) {
        if (lastAiUpdate != null) {
            for (UpdateEventEntry entry : lastAiUpdate) {
                int x = entry.getMineLocation().getX();
                int y = entry.getMineLocation().getY();

                drawBoardRect(gc, x, y, getAIInfoSquareColor(entry));

                drawTextToSquare(gc, Integer.toString(entry.getValue()), x + 0.2, y - 0.25, Color.BLACK, 0.3);
            }
        }
    }

    private void drawTextToSquare(GraphicsContext gc, String text, double x, double y, Paint paint, double ratio) {
        gc.setFill(paint);
        gc.setFont(Font.font("Ariel", FontWeight.BOLD, squareWidth * ratio));
        gc.fillText(
                text,
                squareWidth * x + squareWidth / 2 + boardOffsetX,
                squareWidth * y + squareWidth / 2 + boardOffsetY
        );
    }

    private void drawBoardRect(GraphicsContext gc, int x, int y, Paint paint) {
        gc.setFill(paint);
        gc.fillRect(
                squareWidth * x + SQUARE_PADDING + boardOffsetX,
                squareWidth * y + SQUARE_PADDING + boardOffsetY,
                squareWidth * (x + 1) - squareWidth * x - SQUARE_PADDING,
                squareWidth * (y + 1) - squareWidth * y - SQUARE_PADDING
        );
    }

    @FXML
    public void initialize() {
        gameCanvas.widthProperty().addListener(
                (ObservableValue<? extends Number> observableValue, Number number, Number t1) ->
                redraw()
        );
        gameCanvas.heightProperty().addListener(
                (ObservableValue<? extends Number> observableValue, Number number, Number t1) ->
                redraw()
        );

        aiService = new RunAIService(
                mineSweeperAI,
                this::updateAIProgress,
                move -> {
                    boolean didSomething = false;
                    for (MineLocation location: move.getToFlag()) {
                        if (mineSweeper.flag(location) != FlagResult.NOP) {
                            didSomething = true;
                        }
                    }
                    for (MineLocation location: move.getToProbe()) {
                        if (mineSweeper.probe(location) != ProbeResult.NOP) {
                            didSomething = true;
                        }
                    }
                    final boolean finalDidSomething = didSomething;
                    stopAI();
                    enterUIGameInProgressState();
                    redraw();
                    if (finalDidSomething) {
                        checkGameState();
                    }
                }
        );

        setUpHooks();

        currentAppState = GameAppState.GAME_IN_PROGRESS;
        startNewGame();
    }

    @SuppressWarnings("Convert2MethodRef")
    private void setUpHooks() {
        gameCanvas.setOnMouseEntered(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseExited(mouseEvent -> registerMouseEvent(null));
        gameCanvas.setOnMouseReleased(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMousePressed(mouseEvent -> registerMouseEvent(mouseEvent, mouseEvent));
        gameCanvas.setOnMouseMoved(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseDragged(mouseEvent -> registerMouseEvent(mouseEvent));
        gameCanvas.setOnMouseClicked(mouseEvent -> onClick(mouseEvent));
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
        MineLocation location = getBoardLocation(mouseEvent);

        ProbeResult result = null;
        if (location.getX() >= 0
                && location.getX() < mineSweeper.getWidth()
                && location.getY() >= 0
                && location.getY() < mineSweeper.getHeight()
        ) {
            if (currentAppState == GameAppState.GAME_IN_PROGRESS) {
                if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                    if (mouseEvent.getClickCount() > 1) {
                        // Sweep
                        result = mineSweeper.sweep(location);
                    } else {
                        // Probe
                        result = mineSweeper.probe(location);
                    }
                } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    mineSweeper.toggleFlag(location);
                } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
                    // Only sweeping
                    result = mineSweeper.sweep(location);
                }
            }
        }

        redraw();
        if (result != null && result != ProbeResult.NOP) {
            checkGameState();
        }
    }

    private void checkGameState() {
        if (mineSweeper.getGameState() == GameState.WIN) {
            enterUIWinState();
        } else if (mineSweeper.getGameState() == GameState.LOSE) {
            enterUILoseState();
        }
        minesRemainingTextField.setText(Integer.toString(mineSweeper.getTotalMinesMinusFlags()));
    }

    private void startNewGame() {
        try {
            mineSweeper = new MineSweeper(
                    Integer.parseInt(colsTextField.getText()),
                    Integer.parseInt(rowsTextField.getText()),
                    Integer.parseInt(minesTextField.getText())
            );
            stopAI();
            enterUIGameInProgressState();
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

    private void updateAIProgress(List<UpdateEventEntry> events) {
        lastAiUpdate = events;
        redraw();
    }

    private void stopAI() {
        updateAIProgress(Collections.emptyList());
        aiService.cancel();
    }

    @FXML
    protected void onNewGame(@SuppressWarnings("unused") ActionEvent actionEvent) {

        startNewGame();
    }

    @FXML
    protected void onUseAI(@SuppressWarnings("unused") ActionEvent actionEvent) {
        if (currentAppState == GameAppState.GAME_IN_PROGRESS) {
            aiService.updateState(mineSweeper.clonePlayerState());
            enterUIAIState();
        } else if (currentAppState == GameAppState.AI_IN_PROGRESS) {
            stopAI();
            enterUIGameInProgressState();
        }
    }

    /**
     * UI state management.
     */

    private void enterUIGameInProgressState() {
        switch (currentAppState) {
            case AI_IN_PROGRESS:
                useAiBtn.setText("Use AI");
            case GAME_WON:
            case GAME_LOST:
                useAiBtn.disableProperty().setValue(false);
            case GAME_IN_PROGRESS:
                currentAppState = GameAppState.GAME_IN_PROGRESS;
                break;
        }
    }

    private void enterUIWinState() {
        switch (currentAppState) {
            case GAME_LOST:
            case AI_IN_PROGRESS:
                throw new IllegalStateException("Cannot enter WIN GAME state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                Alert alert = new  Alert(Alert.AlertType.CONFIRMATION,
                        "You have won!",
                        ButtonType.OK);
                alert.showAndWait();
                useAiBtn.disableProperty().setValue(true);
                currentAppState = GameAppState.GAME_WON;
            case GAME_WON:
                break;
        }
    }

    private void enterUILoseState() {
        switch (currentAppState) {
            case AI_IN_PROGRESS:
            case GAME_WON:
                throw new IllegalStateException("Cannot enter GAME_LOST state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                Alert alert = new  Alert(Alert.AlertType.WARNING,
                        "You lost.",
                        ButtonType.OK);
                alert.showAndWait();
                useAiBtn.disableProperty().setValue(true);
                currentAppState = GameAppState.GAME_LOST;
            case GAME_LOST:
                break;
        }
    }

    private void enterUIAIState() {
        switch (currentAppState) {
            case GAME_WON:
            case GAME_LOST:
                throw new IllegalStateException("Cannot enter AI state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                useAiBtn.setText("Stop AI");
                currentAppState = GameAppState.AI_IN_PROGRESS;
            case AI_IN_PROGRESS:
                break;
        }
    }
}
