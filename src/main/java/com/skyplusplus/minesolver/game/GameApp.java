package com.skyplusplus.minesolver.game;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


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
