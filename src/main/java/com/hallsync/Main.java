package com.hallsync;

import com.hallsync.config.JsonConfig;
import com.hallsync.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hallsync/fxml/login.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setStage(stage);
        stage.setScene(new Scene(root, JsonConfig.getLoginWidth(), JsonConfig.getLoginHeight()));
        stage.setTitle(JsonConfig.getAppTitle());
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
