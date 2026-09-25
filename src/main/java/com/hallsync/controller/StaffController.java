package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.StaffMember;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for staff.fxml — the Hall Staff Directory page.
 * Displays all hall staff members with their name, phone, location, and role.
 */
public class StaffController implements PageController {

    @FXML private VBox staffContainer;

    private DashboardController dashboard;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.dashboard = dashboard;
        loadStaff();
    }

    private void loadStaff() {
        for (StaffMember s : Database.hallStaff()) {
            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(6);
            grid.setPadding(new Insets(4, 0, 4, 0));

            Label nameLabel = new Label(s.name());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label roleTag = new Label(s.workRole());
            roleTag.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");

            grid.add(new Label("Phone:"), 0, 0);
            grid.add(new Label(s.phone()), 1, 0);
            grid.add(new Label("Location:"), 0, 1);
            grid.add(new Label(s.location()), 1, 1);

            VBox card = View.card(null, nameLabel, roleTag, new Separator(), grid);
            staffContainer.getChildren().add(card);
        }

        if (Database.hallStaff().isEmpty()) {
            staffContainer.getChildren().add(new Label("No staff members found."));
        }
    }

    @FXML
    private void goBack() {
        dashboard.navigateTo("Home");
    }
}
