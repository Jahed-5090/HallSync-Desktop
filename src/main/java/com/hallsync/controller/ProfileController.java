package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileController implements PageController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private User user;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.user = user;
        loadProfile();
    }

    private void loadProfile() {
        try (Connection c = Database.connect(); 
             PreparedStatement p = c.prepareStatement("SELECT full_name, email FROM users WHERE id=?")) {
            p.setInt(1, user.id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    fullNameField.setText(r.getString("full_name"));
                    emailField.setText(r.getString("email"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveProfile() {
        String name = fullNameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();

        if (name.isBlank()) {
            View.showError("Error", "Full Name cannot be empty.");
            return;
        }

        Database.updateUserAccount(user.id, user.username, pass, name, email);
        
        if (user.role.equals("PROVOST")) {
            try (Connection c = Database.connect(); 
                 PreparedStatement p = c.prepareStatement("UPDATE hall_info SET provost=? WHERE id=1")) {
                p.setString(1, name);
                p.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (user.role.equals("STUDENT")) {
            // Optional: update students table full_name if username matches
            try (Connection c = Database.connect(); 
                 PreparedStatement p = c.prepareStatement("UPDATE students SET full_name=? WHERE roll=? OR full_name=?")) {
                p.setString(1, name);
                p.setString(2, user.username);
                p.setString(3, user.fullName);
                p.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        View.showInfo("Success", "Profile updated successfully!");
        passwordField.clear();
    }
}
