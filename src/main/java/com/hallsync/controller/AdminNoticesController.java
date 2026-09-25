package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.Notice;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for admin_notices.fxml — the Admin notice publish page.
 */
public class AdminNoticesController implements PageController {

    @FXML private TextField titleField;
    @FXML private TextArea bodyArea;
    @FXML private VBox noticesContainer;

    private User user;
    private DashboardController dashboard;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.user = user;
        this.dashboard = dashboard;
        loadNotices();
    }

    private void loadNotices() {
        noticesContainer.getChildren().clear();
        for (Notice n : Database.notices()) {
            noticesContainer.getChildren().add(View.card(n.title(),
                new Label(n.body()),
                new Label("Posted by " + n.postedBy() + " • " + n.date())));
        }
    }

    @FXML
    private void handlePublish() {
        if (titleField.getText().isBlank() || bodyArea.getText().isBlank()) {
            View.showError("Missing data", "Enter both title and notice details.");
            return;
        }
        Database.addNotice(titleField.getText().trim(), bodyArea.getText().trim(), user.fullName);
        titleField.clear();
        bodyArea.clear();
        loadNotices();
    }
}
