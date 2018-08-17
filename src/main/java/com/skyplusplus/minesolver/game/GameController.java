package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.UpdateEvent;
import com.skyplusplus.minesolver.core.ai.UpdateEventEntry;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.ai.backtrack.MultiAI;
import com.skyplusplus.minesolver.core.gamelogic.*;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;


public class GameController {

    private static final int SQUARE_PADDING = 2;

    private MouseEvent lastMouseEvent;
    private MouseEvent lastPressedEvent;

    private MineSweeper mineSweeper;
    private RunAIService aiService;

    private double boardOffsetX;
    private double boardOffsetY;
    private double squareWidth;

    private GameUIState currentUIState;

    private Statistics statistics = new Statistics();

    @SuppressWarnings("CanBeFinal")
    private MineSweeperAI mineSweeperAI = new BackTrackComboAI();

    private UpdateEvent lastAiUpdate = null;

    @FXML
    protected Text minesRemainingTextField;
    @FXML
    protected TextField colsTextField;
    @FXML
    protected TextField rowsTextField;
    @FXML
    protected TextField minesTextField;
    @FXML
    protected Canvas gameCanvas;
    @FXML
    protected Button useAIBtn;
    @FXML
    protected Pane wrapperPane;
    @FXML
    protected CheckBox autoMove;
    @FXML
    public Text winLossText;
    @FXML
    public CheckBox autoRestart;
    @FXML
    public TextField AIMsg;

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


        for (MineLocation thisLocation: mineSweeper.getAllSquares()) {

            SquareControlState controlState =
                    getSquareControlState(thisLocation, thisHoveredSquare, lastHoveredSquare, isMouseDown);
            if (currentUIState != GameUIState.GAME_IN_PROGRESS) {
                controlState = SquareControlState.NEUTRAL;
            }

            Color squareColor = getSquareColor(mineSweeper.getPlayerSquareState(thisLocation), controlState);
            drawBoardRect(gc, thisLocation.getX(), thisLocation.getY(), squareColor);

            if (mineSweeper.getPlayerSquareState(thisLocation) == SquareState.PROBED
                    && mineSweeper.getProbedSquare(thisLocation) > 0) {

                Color textColor = getSquareNumberColor(mineSweeper.getProbedSquare(thisLocation));
                drawTextToSquare(
                        gc,
                        Integer.toString(mineSweeper.getProbedSquare(thisLocation)),
                        thisLocation.getX(),
                        thisLocation.getY(),
                        textColor,
                        0.8
                );
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
                return Color.rgb(255, 0, 0, 0.3);
            case "Safe":
                return Color.rgb(0, 255, 0, 0.3);
            default:
                return Color.rgb(255, 255, 0, 0.3);
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
        if (lastAiUpdate != null && lastAiUpdate.getEntries() != null) {
            for (UpdateEventEntry entry : lastAiUpdate.getEntries()) {
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

        autoMove.selectedProperty().addListener((observable, oldValue, newValue) -> {
            autoRestart.setDisable(!newValue);
            if (!newValue) {
                autoRestart.setSelected(false);
            }
        });

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
                        if (currentUIState == GameUIState.GAME_IN_PROGRESS && autoMove.isSelected()) {
                            onUseAI(null);
                        }
                    }
                }
        );

        setUpHooks();

        currentUIState = GameUIState.GAME_IN_PROGRESS;
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
            if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
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
        if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
            if (mineSweeper.getGameState() == GameState.WIN) {
                enterUIWinState();
                statistics.wins ++;
                statistics.totalGames ++;

                if (autoRestart.isSelected()) {
                    onNewGame(null);
                }
            } else if (mineSweeper.getGameState() == GameState.LOSE) {
                enterUILoseState();
                statistics.totalGames ++;
                if (autoRestart.isSelected()) {
                    onNewGame(null);
                }
            }
        }
        redrawStatistics(statistics);
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

    private void updateAIProgress(UpdateEvent event) {
        lastAiUpdate = event;
        if (event != null) {
            AIMsg.setText(event.getMessage());
        }
        redraw();
    }

    private void stopAI() {
        updateAIProgress(null);
        aiService.cancel();
    }

    @FXML
    protected void onNewGame(@SuppressWarnings("unused") ActionEvent actionEvent) {

        startNewGame();
    }

    @FXML
    protected void onUseAI(@SuppressWarnings("unused") ActionEvent actionEvent) {
        if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
            aiService.updateState(mineSweeper.clonePlayerState());
            enterAIInProgressState();
        } else if (currentUIState == GameUIState.AI_IN_PROGRESS) {
            stopAI();
            enterUIGameInProgressState();
        }
    }

    private void enterUIGameInProgressState() {
        switch (currentUIState) {
            case AI_IN_PROGRESS:
                useAIBtn.setText("Use AI");
            case GAME_WON:
            case GAME_LOST:
                useAIBtn.disableProperty().setValue(false);
            case GAME_IN_PROGRESS:
                currentUIState = GameUIState.GAME_IN_PROGRESS;
                break;
        }
    }
    private void enterUIWinState() {
        switch (currentUIState) {
            case GAME_LOST:
            case AI_IN_PROGRESS:
                throw new IllegalStateException("Cannot enter WIN GAME state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                if (!autoRestart.isSelected()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "You have won!",
                            ButtonType.OK);
                    alert.showAndWait();
                }
                useAIBtn.disableProperty().setValue(true);
                currentUIState = GameUIState.GAME_WON;
            case GAME_WON:
                break;
        }
    }

    private void enterUILoseState() {
        switch (currentUIState) {
            case AI_IN_PROGRESS:
            case GAME_WON:
                throw new IllegalStateException("Cannot enter GAME_LOST state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                if (!autoRestart.isSelected()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "You lost.",
                            ButtonType.OK);
                    alert.showAndWait();
                }
                useAIBtn.disableProperty().setValue(true);
                currentUIState = GameUIState.GAME_LOST;
            case GAME_LOST:
                break;
        }
    }

    private void enterAIInProgressState() {
        switch (currentUIState) {
            case GAME_WON:
            case GAME_LOST:
                throw new IllegalStateException("Cannot enter AI state while not GAME_IN_PROGRESS.");
            case GAME_IN_PROGRESS:
                useAIBtn.setText("Stop AI");
                currentUIState = GameUIState.AI_IN_PROGRESS;
            case AI_IN_PROGRESS:
                break;
        }
    }

    public void resetWinLoss(ActionEvent actionEvent) {
        statistics.reset();
        redrawStatistics(statistics);
    }

    public void redrawStatistics(Statistics statistics) {
        winLossText.setText("" + statistics.wins + "/" + statistics.totalGames);
    }
}
