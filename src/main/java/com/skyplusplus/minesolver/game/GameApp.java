package com.skyplusplus.minesolver.game;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javax.annotation.Resources;
import java.awt.*;
import java.net.URL;


/* A simple minesweeper game which also helps visualize the AI.
* Author: yujinwunz@gmail.com
* Date: 10/08/18
*/

public class GameApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Menu layout");
        Parent root = FXMLLoader.load(getClass().getResource("/mine_game.fxml"));

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);

        stage.show();
    }
}
