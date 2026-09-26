package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.CommitteeMember;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminCommitteeController implements PageController {

    @FXML private TextField nameField;
    @FXML private TextField positionField;
    @FXML private TextField deptField;
    @FXML private TextField roomField;
    @FXML private TextField contactField;
    @FXML private VBox membersContainer;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        loadMembers();
    }

    private void loadMembers() {
        membersContainer.getChildren().clear();
        for (CommitteeMember m : Database.committee()) {
            HBox row = new HBox(15);
            row.getChildren().addAll(
                new Label(m.name()),
                new Label(m.position()),
                new Label(m.department()),
                new Label(m.room()),
                new Label(m.contact())
            );

            Button del = new Button("Delete");
            del.setOnAction(e -> {
                Database.deleteCommitteeMember(m.id());
                loadMembers();
            });
            row.getChildren().add(del);
            
            membersContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleAddMember() {
        String name = nameField.getText();
        String position = positionField.getText();
        String dept = deptField.getText();
        String room = roomField.getText();
        String contact = contactField.getText();

        if (name.isBlank() || position.isBlank()) {
            View.showError("Error", "Name and Position are required.");
            return;
        }

        Database.addCommitteeMember(name, position, dept, room, contact);
        
        nameField.clear();
        positionField.clear();
        deptField.clear();
        roomField.clear();
        contactField.clear();
        
        loadMembers();
    }
}
