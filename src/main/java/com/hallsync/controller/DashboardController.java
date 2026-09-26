package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

/**
 * Controller for dashboard.fxml.
 * Manages the sidebar navigation and loads page FXML files into the content area.
 * Navigation items and page routes are driven by navigation.json.
 */
public class DashboardController {

    @FXML private Label userLabel;
    @FXML private VBox navContainer;
    @FXML private VBox contentArea;
    @FXML private Label quoteLabel;

    private Stage stage;
    private User user;

    /**
     * Called after the FXML is loaded to inject the stage and user context.
     * Populates the sidebar navigation buttons from navigation.json.
     */
    public void init(Stage stage, User user) {
        this.stage = stage;
        this.user = user;
        userLabel.setText(user.fullName + "\n" + user.role);
        buildNavigation();
        navigateTo(JsonConfig.getDefaultPage());
        fetchQuoteOfTheDay();
    }

    /**
     * Networking Requirement: Fetches a random quote from an external API via HTTP GET
     * and parses the JSON response using Jackson.
     */
    private void fetchQuoteOfTheDay() {
        com.hallsync.util.AppExecutor.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://dummyjson.com/quotes/random"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(response.body());
                    String quote = rootNode.get("quote").asText();
                    String author = rootNode.get("author").asText();
                    
                    Platform.runLater(() -> {
                        if (quoteLabel != null) {
                            quoteLabel.setText("\"" + quote + "\" — " + author);
                        }
                    });
                } else {
                    Platform.runLater(() -> quoteLabel.setText("Could not load quote today."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> quoteLabel.setText("Stay inspired!"));
            }
            return null;
        }, res -> {});
    }

    private void buildNavigation() {
        // Menu items loaded from navigation.json based on user role
        String[] items = JsonConfig.getMenuItems(user.role);
        for (String item : items) {
            Button btn = new Button(item);
            btn.getStyleClass().add("nav-button");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> navigateTo(item));
            navContainer.getChildren().add(btn);
        }
    }

    /**
     * Loads the appropriate page FXML into the dashboard content area.
     * The page-to-FXML mapping is driven by navigation.json pageRoutes.
     */
    public void navigateTo(String page) {
        // Page route resolved from navigation.json
        String fxmlFile = JsonConfig.getPageRoute(page);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hallsync/fxml/" + fxmlFile));
            Parent pageRoot = loader.load();

            // Pass context to page controllers that need it
            Object controller = loader.getController();
            if (controller instanceof PageController) {
                ((PageController) controller).init(stage, user, this);
            }

            contentArea.getChildren().setAll(pageRoot);
        } catch (IOException e) {
            e.printStackTrace();
            com.hallsync.view.View.showError("Navigation Error", "Failed to load page: " + page);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hallsync/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController loginCtrl = loader.getController();
            loginCtrl.setStage(stage);
            stage.setScene(new Scene(root, JsonConfig.getLoginWidth(), JsonConfig.getLoginHeight()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
