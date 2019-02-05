package com.skyplusplus.minesolver.game;

import com.skyplusplus.minesolver.core.ai.BoardUpdate;
import com.skyplusplus.minesolver.core.ai.BoardUpdateEntry;
import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackAI;
import com.skyplusplus.minesolver.core.ai.backtrack.BackTrackComboAI;
import com.skyplusplus.minesolver.core.ai.backtrack.FrankensteinAI;
import com.skyplusplus.minesolver.core.ai.frontier.FrontierAI;
import com.skyplusplus.minesolver.core.ai.simple.SimpleAI;
import com.skyplusplus.minesolver.core.gamelogic.*;
import com.sun.javafx.collections.ObservableListWrapper;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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

import java.util.Arrays;
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

    private GameUIState currentUIState;

    private final Statistics statistics = new Statistics();

    private MineSweeperAI selectedAI;

    private static final List<MineSweeperAI> availableAIs = Arrays.asList(
            new SimpleAI(),
            new BackTrackComboAI(),
            new FrontierAI(),
            new FrankensteinAI()
    );

    private static final int defaultAI = 2;

    private BoardUpdate lastAiUpdate = null;

    private boolean invalidated = true;

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
    protected Text winLossText;
    @FXML
    protected TextField AIMsg;
    @FXML
    protected ComboBox<MineSweeperAI> aiCbbx;

    private void redraw() {
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        recalculateBoardParameters();

        redrawBackground(gc);

        boolean isMouseDown = lastMouseEvent != null && lastMouseEvent.isPrimaryButtonDown();
        BoardCoord lastHoveredSquare = null;
        BoardCoord thisHoveredSquare = null;
        if (lastPressedEvent != null) {
            lastHoveredSquare = getBoardCoord(lastPressedEvent);
        }
        if (lastMouseEvent != null) {
            thisHoveredSquare = getBoardCoord(lastMouseEvent);
        }


        for (BoardCoord thisCoord : mineSweeper.getAllSquares()) {

            SquareControlState controlState =
                    getSquareControlState(thisCoord, thisHoveredSquare, lastHoveredSquare, isMouseDown);
            if (currentUIState != GameUIState.GAME_IN_PROGRESS) {
                controlState = SquareControlState.NEUTRAL;
            }

            Color squareColor = getSquareColor(mineSweeper.getPlayerSquareState(thisCoord), controlState);
            drawBoardRect(gc, thisCoord.getX(), thisCoord.getY(), squareColor);

            if (mineSweeper.getPlayerSquareState(thisCoord) == SquareState.PROBED
                    && mineSweeper.getProbedSquare(thisCoord) > 0) {

                Color textColor = getSquareNumberColor(mineSweeper.getProbedSquare(thisCoord));
                drawTextToSquare(
                        gc,
                        Integer.toString(mineSweeper.getProbedSquare(thisCoord)),
                        thisCoord.getX(),
                        thisCoord.getY(),
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
            BoardCoord thisSquare,
            BoardCoord thisHoveredSquare,
            BoardCoord lastHoveredSquare,
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

    private static Color getAIInfoSquareColor(BoardUpdateEntry entry) {
        switch (entry.getColor()) {
            case GREEN:
                return Color.rgb(0, 255, 0, 0.3);
            case YELLOW:
                return Color.rgb(255, 255, 0, 0.3);
            case RED:
                return Color.rgb(255, 0, 0, 0.3);
            case BLUE:
                return Color.rgb(0, 0, 255, 0.3);
            case WHITE:
                return Color.rgb(0, 0, 0, 0.3);
            case ORANGE:
                return Color.rgb(255, 165, 0, 0.3);
            case GRAY:
            default:
                return Color.rgb(100, 100, 100, 0.3);
        }
    }

    private BoardCoord getBoardCoord(MouseEvent mouseEvent) {
        return BoardCoord.ofValue(
                (int) ((mouseEvent.getX() - boardOffsetX) / squareWidth),
                (int) ((mouseEvent.getY() - boardOffsetY) / squareWidth)
        );
    }

    private void recalculateBoardParameters() {
        this.squareWidth = (int) Math.min(
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
            for (BoardUpdateEntry entry : lastAiUpdate.getEntries()) {
                int x = entry.getBoardCoord().getX();
                int y = entry.getBoardCoord().getY();

                drawBoardRect(gc, x, y, getAIInfoSquareColor(entry));

                drawTextToSquare(gc, entry.getTag(), x + 0.2, y - 0.25, Color.BLACK, 0.3);
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
                        invalidate()
        );
        gameCanvas.heightProperty().addListener(
                (ObservableValue<? extends Number> observableValue, Number number, Number t1) ->
                        invalidate()
        );

        aiService = new RunAIService(
                this::updateAIProgress,
                move -> {
                    boolean didSomething = false;
                    for (BoardCoord coord : move.getToFlag()) {
                        if (mineSweeper.flag(coord) != FlagResult.NOP) {
                            didSomething = true;
                        }
                    }
                    for (BoardCoord coord : move.getToProbe()) {
                        if (mineSweeper.probe(coord) != ProbeResult.NOP) {
                            didSomething = true;
                        }
                    }
                    final boolean finalDidSomething = didSomething;
                    stopAI();
                    enterUIGameInProgressState();
                    invalidate();
                    if (finalDidSomething) {
                        checkGameState();
                        if (currentUIState == GameUIState.GAME_IN_PROGRESS && autoMove.isSelected()) {
                            onUseAI();
                        }
                    }
                }
        );

        ObservableList<MineSweeperAI> checkboxItems = new ObservableListWrapper<>(availableAIs);
        aiCbbx.setItems(checkboxItems);
        aiCbbx.getSelectionModel()
              .selectedItemProperty()
              .addListener((observable, oldValue, newValue) -> selectedAI = newValue);
        aiCbbx.getSelectionModel().select(availableAIs.get(defaultAI));

        setUpHooks();

        currentUIState = GameUIState.GAME_IN_PROGRESS;
        startNewGame();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (invalidated) {
                    redraw();
                }
                invalidated = false;
            }
        };
        timer.start();
    }

    private void invalidate() {
        invalidated = true;
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
        invalidate();
    }

    private void registerMouseEvent(MouseEvent lastMouseEvent, MouseEvent lastPressedEvent) {
        this.lastPressedEvent = lastPressedEvent;
        registerMouseEvent(lastMouseEvent);
    }

    private void onClick(MouseEvent mouseEvent) {
        this.lastMouseEvent = mouseEvent;
        BoardCoord coord = getBoardCoord(mouseEvent);

        ProbeResult result = null;
        if (coord.getX() >= 0
                && coord.getX() < mineSweeper.getWidth()
                && coord.getY() >= 0
                && coord.getY() < mineSweeper.getHeight()
        ) {
            if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
                if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                    if (mouseEvent.getClickCount() > 1) {
                        // Sweep
                        result = mineSweeper.sweep(coord);
                    } else {
                        // Probe
                        result = mineSweeper.probe(coord);
                    }
                } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    mineSweeper.toggleFlag(coord);
                } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
                    // Only sweeping
                    result = mineSweeper.sweep(coord);
                }
            }
        }

        invalidate();
        if (result != null && result != ProbeResult.NOP) {
            checkGameState();
        }
    }

    private void checkGameState() {
        if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
            if (mineSweeper.getGameState() == GameState.WIN) {
                enterUIWinState();
                statistics.wins++;
                statistics.totalGames++;

                if (autoMove.isSelected()) {
                    onNewGame();
                }
            } else if (mineSweeper.getGameState() == GameState.LOSE) {
                enterUILoseState();
                statistics.totalGames++;
                if (autoMove.isSelected()) {
                    onNewGame();
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
        invalidate();
    }

    @SuppressWarnings("SameParameterValue")
    private void updateAIProgress(BoardUpdate event) {
        lastAiUpdate = event;
        if (event != null) {
            AIMsg.setText(event.getMessage());
        }
        invalidate();
    }

    private void stopAI() {
        updateAIProgress(null);
        aiService.cancel();
    }

    @FXML
    protected void onNewGame() {

        startNewGame();
    }

    @FXML
    protected void onUseAI() {
        if (currentUIState == GameUIState.GAME_IN_PROGRESS) {
            aiService.calculate(selectedAI, mineSweeper.clonePlayerState());
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
                if (!autoMove.isSelected()) {
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
                if (!autoMove.isSelected()) {
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

    public void resetWinLoss() {
        statistics.reset();
        redrawStatistics(statistics);
    }

    private void redrawStatistics(Statistics statistics) {
        winLossText.setText("" + statistics.wins + "/" + statistics.totalGames);
    }
}
