package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controller for meals.fxml — the Meal Management Calendar page.
 * Calendar weeks and lockout hours are driven by app-config.json.
 */
public class MealsController implements PageController {

    @FXML private VBox calendarContainer;

    private User user;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.user = user;
        buildCalendar();
    }

    private void buildCalendar() {
        LocalDate start = LocalDate.now();
        int weeks = JsonConfig.getMealCalendarWeeks();
        for (int week = 0; week < weeks; week++) {
            HBox row = new HBox(8);
            for (int day = 0; day < 7; day++) {
                LocalDate date = start.plusDays(week * 7L + day);
                Button b = new Button(date.getDayOfMonth() + "\n" + date.getMonthValue());
                b.getStyleClass().add("meal-cell");
                updateMealButton(b, date);

                if (JsonConfig.hasPermission(user.role, "canSubmitComplaints")) {
                    // Students can toggle meals
                    b.setOnAction(e -> toggleMeal(date, b));
                } else if (JsonConfig.hasPermission(user.role, "canSetHallOff")) {
                    // Admins can set hall-off
                    b.setOnAction(e -> hallOff(date, b));
                }
                row.getChildren().add(b);
            }
            calendarContainer.getChildren().add(row);
        }
    }

    private void updateMealButton(Button b, LocalDate date) {
        String state = Database.mealState(date);
        b.getStyleClass().removeAll("meal-on", "meal-off", "meal-hall-off");
        if ("HALL_OFF".equals(state)) {
            b.getStyleClass().add("meal-hall-off");
            b.setText(date.getDayOfMonth() + "\nHALL OFF");
        } else if ("OFF".equals(state)) {
            b.getStyleClass().add("meal-off");
            b.setText(date.getDayOfMonth() + "\nOFF");
        } else {
            b.getStyleClass().add("meal-on");
            b.setText(date.getDayOfMonth() + "\nON");
        }
    }

    private void toggleMeal(LocalDate date, Button b) {
        int lockoutHours = JsonConfig.getMealLockoutHours();
        if (!date.atStartOfDay().isAfter(LocalDateTime.now().plusHours(lockoutHours))) {
            View.showError(lockoutHours + "-hour rule",
                "Meals can only be changed at least " + lockoutHours + " hours in advance.");
            return;
        }
        String state = Database.mealState(date);
        if ("HALL_OFF".equals(state)) return;
        Database.setMealState(date, "ON".equals(state) ? "OFF" : "ON");
        updateMealButton(b, date);
    }

    private void hallOff(LocalDate date, Button b) {
        Database.setHallOff(date);
        updateMealButton(b, date);
    }
}
