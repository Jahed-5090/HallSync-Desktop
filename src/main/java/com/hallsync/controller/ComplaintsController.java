package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.Complaint;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for complaints.fxml — the Complaints page.
 * Role permissions are driven by navigation.json.
 */
public class ComplaintsController implements PageController {

    @FXML private VBox submitCard;
    @FXML private TextField subjectField;
    @FXML private TextArea messageArea;
    @FXML private Label listTitle;
    @FXML private VBox complaintsContainer;

    private User user;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.user = user;

        // Show submission form based on JSON permission
        if (JsonConfig.hasPermission(user.role, "canSubmitComplaints")) {
            submitCard.setVisible(true);
            submitCard.setManaged(true);
        }

        // Set list heading based on JSON permission
        if (JsonConfig.hasPermission(user.role, "canResolveComplaints")) {
            listTitle.setText("All Complaints");
        }

        loadComplaints();
    }

    private void loadComplaints() {
        complaintsContainer.getChildren().clear();
        for (Complaint c : Database.complaints()) {
            HBox top = new HBox(12,
                new Label("#" + c.id()),
                new Label(c.subject()),
                new Label("From: " + c.user()),
                new Label(c.status()));
            top.setAlignment(Pos.CENTER_LEFT);

            Label msg = new Label(c.message() + "\n" + c.date());
            msg.setWrapText(true);

            if (JsonConfig.hasPermission(user.role, "canResolveComplaints")) {
                Button done = View.button("Mark Resolved");
                done.setOnAction(e -> {
                    Database.updateComplaint(c.id(), "RESOLVED");
                    loadComplaints();
                });
                complaintsContainer.getChildren().add(View.card(null, top, msg, done));
            } else {
                complaintsContainer.getChildren().add(View.card(null, top, msg));
            }
        }
    }

    @FXML
    private void handleSubmit() {
        if (subjectField.getText().isBlank() || messageArea.getText().isBlank()) {
            View.showError("Missing data", "Enter a subject and message.");
            return;
        }
        Database.addComplaint(user.username, subjectField.getText(), messageArea.getText());
        subjectField.clear();
        messageArea.clear();
        View.showInfo("Submitted", "Your complaint was sent to the Hall Office.");
        loadComplaints();
    }
}
