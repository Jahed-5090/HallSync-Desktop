package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.Student;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for directory.fxml — the Universal Student Directory page.
 */
public class DirectoryController implements PageController {

    @FXML private TextField searchField;
    @FXML private VBox resultsContainer;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        // Load all students initially
        runSearch();
    }

    @FXML
    private void handleSearch() {
        runSearch();
    }

    private void runSearch() {
        resultsContainer.getChildren().clear();
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        for (Student s : Database.searchStudents(term)) {
            resultsContainer.getChildren().add(View.card(s.name(),
                new Label("Department: " + s.department()),
                new Label("Roll: " + s.roll()),
                new Label("Session: " + s.session()),
                new Label("Room: " + s.room()),
                new Label("Block: " + s.block())));
        }
    }
}
