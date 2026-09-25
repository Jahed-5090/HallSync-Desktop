package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.CommitteeMember;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for committee.fxml — the Student Hall Committee page.
 */
public class CommitteeController implements PageController {

    @FXML private VBox membersContainer;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        for (CommitteeMember m : Database.committee()) {
            Button profile = View.button("Open public profile");
            profile.setOnAction(e -> View.showInfo("Public Profile",
                m.name() + "\n\nPosition: " + m.position() +
                "\nDepartment: " + m.department() +
                "\nRoom: " + m.room() +
                "\nContact: " + m.contact()));
            membersContainer.getChildren().add(View.card(m.name(),
                new Label("Position: " + m.position()),
                new Label("Department: " + m.department()),
                new Label("Room: " + m.room()),
                profile));
        }
    }
}
