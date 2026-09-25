package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.Student;
import com.hallsync.model.User;
import com.hallsync.model.UserAccount;
import com.hallsync.util.AppExecutor;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Controller for admin_students.fxml
 * Provides full CRUD for student profiles, plus user account management.
 * Uses AppExecutor thread pool to query database asynchronously without freezing the UI.
 */
public class AdminStudentsController implements PageController {

    @FXML private Button tabStudents;
    @FXML private Button tabAccounts;
    @FXML private HBox searchBar;
    @FXML private TextField searchField;
    @FXML private VBox contentContainer;

    private boolean onStudentTab = true;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        showStudentTab();
    }

    // ────────────────── Tab switching ──────────────────

    @FXML
    private void showStudentTab() {
        onStudentTab = true;
        tabStudents.getStyleClass().setAll("action-button");
        tabAccounts.getStyleClass().setAll("tab-button");
        searchBar.setVisible(true);
        searchBar.setManaged(true);
        loadStudents("");
    }

    @FXML
    private void showAccountTab() {
        onStudentTab = false;
        tabStudents.getStyleClass().setAll("tab-button");
        tabAccounts.getStyleClass().setAll("action-button");
        searchBar.setVisible(false);
        searchBar.setManaged(false);
        loadUserAccounts();
    }

    // ────────────────── Student Profiles (Thread Pool async loading) ──────────────────

    @FXML
    private void handleSearch() {
        loadStudents(searchField.getText() == null ? "" : searchField.getText().trim());
    }

    /**
     * Loads students from the database asynchronously using AppExecutor.
     */
    private void loadStudents(String term) {
        contentContainer.getChildren().clear();
        contentContainer.getChildren().add(new Label("Loading students..."));

        AppExecutor.runAsync(
            () -> term.isBlank() ? Database.allStudents() : Database.searchStudents(term),
            students -> {
                contentContainer.getChildren().clear();
                if (students.isEmpty()) {
                    contentContainer.getChildren().add(new Label("No students found."));
                } else {
                    for (Student s : students) {
                        contentContainer.getChildren().add(buildStudentCard(s));
                    }
                }
            },
            error -> {
                contentContainer.getChildren().clear();
                contentContainer.getChildren().add(new Label("Failed to load students."));
            }
        );
    }

    private VBox buildStudentCard(Student s) {
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(6);
        grid.add(new Label("Department:"), 0, 0); grid.add(new Label(s.department()), 1, 0);
        grid.add(new Label("Roll:"),       0, 1); grid.add(new Label(s.roll()),       1, 1);
        grid.add(new Label("Session:"),    0, 2); grid.add(new Label(s.session()),    1, 2);
        grid.add(new Label("Room:"),       0, 3); grid.add(new Label(s.room()),       1, 3);
        grid.add(new Label("Block:"),      0, 4); grid.add(new Label(s.block()),      1, 4);

        Button edit = View.button("✏ Edit");
        Button delete = new Button("🗑 Delete"); delete.getStyleClass().add("danger-button");

        edit.setOnAction(e -> showEditStudentDialog(s));
        delete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete student \"" + s.name() + "\"?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.YES) {
                Database.deleteStudent(s.id());
                loadStudents(searchField.getText() == null ? "" : searchField.getText().trim());
            }
        });

        HBox actions = new HBox(8, edit, delete);
        return View.card(s.name(), grid, actions);
    }

    private void showEditStudentDialog(Student s) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(s == null ? "Add Student" : "Edit Student");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        form.setPadding(new Insets(18));

        TextField name    = field("Full Name",   s != null ? s.name()       : "");
        TextField dept    = field("Department",  s != null ? s.department() : "");
        TextField roll    = field("Roll Number", s != null ? s.roll()       : "");
        TextField session = field("Session",     s != null ? s.session()    : "");
        TextField room    = field("Room",        s != null ? s.room()       : "");
        TextField block   = field("Block",       s != null ? s.block()      : "");

        form.add(new Label("Full Name"),   0, 0); form.add(name,    1, 0);
        form.add(new Label("Department"),  0, 1); form.add(dept,    1, 1);
        form.add(new Label("Roll Number"), 0, 2); form.add(roll,    1, 2);
        form.add(new Label("Session"),     0, 3); form.add(session, 1, 3);
        form.add(new Label("Room"),        0, 4); form.add(room,    1, 4);
        form.add(new Label("Block"),       0, 5); form.add(block,   1, 5);

        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (name.getText().isBlank()) { View.showError("Validation", "Full name is required."); return; }
            if (s == null) {
                Database.addStudent(name.getText(), dept.getText(), roll.getText(), session.getText(), room.getText(), block.getText());
            } else {
                Database.updateStudent(s.id(), name.getText(), dept.getText(), roll.getText(), session.getText(), room.getText(), block.getText());
            }
            loadStudents(searchField.getText() == null ? "" : searchField.getText().trim());
        }
    }

    @FXML
    private void handleAdd() {
        if (onStudentTab) showEditStudentDialog(null);
    }

    // ────────────────── User Accounts (Thread Pool async loading) ──────────────────

    /**
     * Loads user accounts asynchronously using AppExecutor.
     */
    private void loadUserAccounts() {
        contentContainer.getChildren().clear();
        contentContainer.getChildren().add(new Label("Loading accounts..."));

        AppExecutor.runAsync(
            () -> Database.allUserAccounts(),
            accounts -> {
                contentContainer.getChildren().clear();
                if (accounts.isEmpty()) {
                    contentContainer.getChildren().add(new Label("No user accounts found."));
                } else {
                    for (UserAccount ua : accounts) {
                        contentContainer.getChildren().add(buildAccountCard(ua));
                    }
                }
            },
            error -> {
                contentContainer.getChildren().clear();
                contentContainer.getChildren().add(new Label("Failed to load accounts."));
            }
        );
    }

    private VBox buildAccountCard(UserAccount ua) {
        Label roleBadge = new Label(ua.role());
        roleBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(6);
        grid.add(new Label("Username:"), 0, 0); grid.add(new Label(ua.username()), 1, 0);
        grid.add(new Label("Email:"),    0, 1); grid.add(new Label(ua.email().isBlank() ? "—" : ua.email()), 1, 1);
        grid.add(new Label("Role:"),     0, 2); grid.add(roleBadge, 1, 2);

        Button edit = View.button("✏ Edit Account");
        edit.setOnAction(e -> showEditAccountDialog(ua));

        HBox titleRow = new HBox(8, new Label(ua.fullName()));
        return View.card(null, titleRow, grid, edit);
    }

    private void showEditAccountDialog(UserAccount ua) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Account — " + ua.fullName());
        dialog.setHeaderText("Leave Password blank to keep it unchanged.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        form.setPadding(new Insets(18));

        TextField username = field("Username", ua.username());
        PasswordField password = new PasswordField(); password.setPromptText("New password (leave blank to keep)");
        TextField email    = field("Email", ua.email());
        TextField fullName = field("Full Name", ua.fullName());

        form.add(new Label("Username"),  0, 0); form.add(username, 1, 0);
        form.add(new Label("Full Name"), 0, 1); form.add(fullName, 1, 1);
        form.add(new Label("Email"),     0, 2); form.add(email,    1, 2);
        form.add(new Label("Password"),  0, 3); form.add(password, 1, 3);

        dialog.getDialogPane().setContent(form);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (username.getText().isBlank() || fullName.getText().isBlank()) {
                View.showError("Validation", "Username and full name are required.");
                return;
            }
            Database.updateUserAccount(ua.id(), username.getText(), password.getText(), fullName.getText(), email.getText());
            loadUserAccounts();
        }
    }

    // ────────────────── Helpers ──────────────────

    private TextField field(String prompt, String value) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.setPrefWidth(240);
        return tf;
    }
}
