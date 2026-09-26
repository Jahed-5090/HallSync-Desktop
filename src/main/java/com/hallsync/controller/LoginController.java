package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;

import com.hallsync.util.AppExecutor;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for login.fxml.
 * Handles user authentication on a background thread pool and navigates to the dashboard on success.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            View.showError("Login failed", "Please enter both username and password.");
            return;
        }

        // 1. Disable inputs to prevent double-clicks while authenticating
        loginButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        if (statusLabel != null) statusLabel.setText("Authenticating...");

        // 2. Run DB authentication on the Thread Pool (non-blocking)
        AppExecutor.runAsync(
            () -> Database.login(username, password),
            user -> {
                if (user == null) {
                    View.showError("Login failed", "Incorrect username or password.");
                    resetLoginForm();
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
                    View.showError("Error", "Failed to load dashboard: " + e.getMessage());
                    resetLoginForm();
                }
            },
            error -> {
                View.showError("Error", "Login error: " + error.getMessage());
                resetLoginForm();
            }
        );
    }

    /** Re-enables the login form inputs after a failed attempt. */
    private void resetLoginForm() {
        loginButton.setDisable(false);
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        if (statusLabel != null) statusLabel.setText("");
    }
}
