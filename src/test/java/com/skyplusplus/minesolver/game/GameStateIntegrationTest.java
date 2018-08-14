package com.skyplusplus.minesolver.game;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.LabeledMatchers;

import static org.junit.jupiter.api.Assertions.fail;
import static org.testfx.api.FxAssert.verifyThat;

;

@SuppressWarnings("WeakerAccess")
@ExtendWith(ApplicationExtension.class)
public class GameStateIntegrationTest {
    Scene scene;

    @Start
    public void onStart(Stage stage) {
        stage.setTitle("Menu layout");
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("/mine_game.fxml"));
            scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            fail("Could not load the game");
        }
    }

    @Test
    public void shouldSwichStates(FxRobot robot) {
        verifyThat("#useAIBtn", NodeMatchers.isEnabled());
        verifyThat("#useAIBtn", LabeledMatchers.hasText("Use AI"));
        

        Button useAIBtn = (Button)scene.lookup("#useAIBtn");
        robot.clickOn("#useAIBtn");
    }

    @Test
    public void shouldNotSwitchStates() {
    }
}
