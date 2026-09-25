package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for login.fxml.
 * Handles user authentication and navigates to the dashboard on success.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    /** The stage is injected by the Main class after loading the FXML. */
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        User user = Database.login(username, password);
        if (user == null) {
            com.hallsync.view.View.showError("Login failed", "Incorrect username or password.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hallsync/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController dashboard = loader.getController();
            dashboard.init(stage, user);
            stage.setScene(new Scene(root, JsonConfig.getDashboardWidth(), JsonConfig.getDashboardHeight()));
        } catch (Exception e) {
            e.printStackTrace();
            com.hallsync.view.View.showError("Error", "Failed to load dashboard: " + e.getMessage());
        }
    }
}
