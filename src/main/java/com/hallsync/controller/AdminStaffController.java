package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.StaffMember;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Controller for admin_staff.fxml
 * Provides full CRUD for hall staff members.
 */
public class AdminStaffController implements PageController {

    @FXML private VBox staffContainer;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        loadStaff();
    }

    private void loadStaff() {
        staffContainer.getChildren().clear();
        var staffList = Database.hallStaff();
        if (staffList.isEmpty()) {
            staffContainer.getChildren().add(new Label("No staff members yet. Use 'Add Staff Member' to add one."));
            return;
        }
        for (StaffMember s : staffList) {
            staffContainer.getChildren().add(buildCard(s));
        }
    }

    private VBox buildCard(StaffMember s) {
        Label roleTag = new Label(s.workRole());
        roleTag.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(6);
        grid.setPadding(new Insets(6, 0, 6, 0));
        grid.add(new Label("Phone:"),    0, 0); grid.add(new Label(s.phone()),    1, 0);
        grid.add(new Label("Location:"), 0, 1); grid.add(new Label(s.location()), 1, 1);

        Button edit   = View.button("✏ Edit");
        Button delete = new Button("🗑 Delete"); delete.getStyleClass().add("danger-button");

        edit.setOnAction(e -> showDialog(s));
        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete staff member \"" + s.name() + "\"?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.YES) {
                Database.deleteStaff(s.id());
                loadStaff();
            }
        });

        HBox actions = new HBox(8, edit, delete);
        return View.card(null,
            new Label(s.name()) {{ setStyle("-fx-font-size:16px;-fx-font-weight:bold;"); }},
            roleTag, new Separator(), grid, actions);
    }

    @FXML
    private void handleAdd() {
        showDialog(null);
    }

    private void showDialog(StaffMember s) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(s == null ? "Add Staff Member" : "Edit Staff Member");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        form.setPadding(new Insets(18));

        TextField name     = field("Full Name",        s != null ? s.name()     : "");
        TextField workRole = field("Role/Position",    s != null ? s.workRole() : "");
        TextField phone    = field("Phone Number",     s != null ? s.phone()    : "");
        TextField location = field("Current Location", s != null ? s.location() : "");

        form.add(new Label("Full Name"),     0, 0); form.add(name,     1, 0);
        form.add(new Label("Role/Position"), 0, 1); form.add(workRole, 1, 1);
        form.add(new Label("Phone"),         0, 2); form.add(phone,    1, 2);
        form.add(new Label("Location"),      0, 3); form.add(location, 1, 3);

        dialog.getDialogPane().setContent(form);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (name.getText().isBlank()) { View.showError("Validation", "Name is required."); return; }
            if (s == null) {
                Database.addStaff(name.getText(), phone.getText(), location.getText(), workRole.getText());
            } else {
                Database.updateStaff(s.id(), name.getText(), phone.getText(), location.getText(), workRole.getText());
            }
            loadStaff();
        }
    }

    private TextField field(String prompt, String value) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setPrefWidth(240);
        return tf;
    }
}
