package com.hallsync.controller;

import com.hallsync.database.Database;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class AdminBillsController implements PageController {

    @FXML private ComboBox<String> monthSelector;
    @FXML private TextField customMonthField;
    @FXML private Label currentScopeLabel;
    @FXML private TextField sectorField;
    @FXML private TextField amountField;
    @FXML private VBox scopesContainer;

    private String currentTarget = "DEFAULT";

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        Database.createBillScopesTable();
        
        monthSelector.getItems().addAll("Default Setup", "Custom Upcoming Month...");
        monthSelector.getSelectionModel().selectFirst();
        
        monthSelector.setOnAction(e -> {
            boolean isCustom = monthSelector.getValue().equals("Custom Upcoming Month...");
            customMonthField.setVisible(isCustom);
            customMonthField.setManaged(isCustom);
        });

        loadScopesForTarget("DEFAULT");
    }

    @FXML
    private void handleLoadScopes() {
        String selection = monthSelector.getValue();
        if (selection.equals("Default Setup")) {
            loadScopesForTarget("DEFAULT");
        } else {
            String custom = customMonthField.getText().trim();
            if (custom.isBlank()) {
                View.showError("Error", "Enter an upcoming month (YYYY-MM).");
                return;
            }
            try {
                YearMonth ym = YearMonth.parse(custom);
                YearMonth now = YearMonth.now();
                if (!ym.isAfter(now)) {
                    View.showError("Error", "You can only edit bill scopes for upcoming months (after " + now + ").");
                    return;
                }
                loadScopesForTarget(custom);
            } catch (DateTimeParseException ex) {
                View.showError("Error", "Invalid format. Use YYYY-MM.");
            }
        }
    }

    private void loadScopesForTarget(String targetMonth) {
        this.currentTarget = targetMonth;
        currentScopeLabel.setText("Editing: " + (targetMonth.equals("DEFAULT") ? "Default Setup" : targetMonth));
        scopesContainer.getChildren().clear();
        
        try (Connection c = Database.connect(); 
             PreparedStatement p = c.prepareStatement("SELECT id, sector, amount FROM bill_scopes WHERE month=? ORDER BY id")) {
            p.setString(1, targetMonth);
            ResultSet r = p.executeQuery();
            while (r.next()) {
                int id = r.getInt("id");
                String sector = r.getString("sector");
                double amount = r.getDouble("amount");

                HBox row = new HBox(15);
                row.getChildren().addAll(
                    new Label(sector),
                    new Label(String.valueOf(amount))
                );

                Button del = new Button("Delete");
                del.setOnAction(e -> {
                    deleteScope(id);
                });
                row.getChildren().add(del);
                
                scopesContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteScope(int id) {
        try (Connection c = Database.connect(); 
             PreparedStatement p = c.prepareStatement("DELETE FROM bill_scopes WHERE id=?")) {
            p.setInt(1, id);
            p.executeUpdate();
            loadScopesForTarget(currentTarget);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddScope() {
        String sector = sectorField.getText();
        String amountText = amountField.getText();

        if (sector.isBlank() || amountText.isBlank()) {
            View.showError("Error", "Sector and amount are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            try (Connection c = Database.connect(); 
                 PreparedStatement p = c.prepareStatement("INSERT INTO bill_scopes(month, sector, amount) VALUES(?, ?, ?)")) {
                p.setString(1, currentTarget);
                p.setString(2, sector);
                p.setDouble(3, amount);
                p.executeUpdate();
                
                sectorField.clear();
                amountField.clear();
                loadScopesForTarget(currentTarget);
            }
        } catch (NumberFormatException e) {
            View.showError("Error", "Amount must be a valid number.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
