package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.User;
import com.hallsync.util.AppExecutor;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for meals.fxml.
 *
 * Features:
 * 1. Month-by-month organized calendar with previous/next month navigation.
 * 2. Daily meal turn ON/OFF by tapping/clicking that specific date card.
 * 3. Role-restricted: Meal toggling is strictly for STUDENT accounts.
 * 4. 24-hour advance lockout rule enforcement.
 * 5. Multithreaded database operations via AppExecutor thread pool.
 */
public class MealsController implements PageController {

    @FXML private HBox roleAlertBanner;
    @FXML private Label roleAlertLabel;
    @FXML private Label statusLabel;

    @FXML private VBox calendarCard;
    @FXML private Button btnPrevMonth;
    @FXML private Label monthTitleLabel;
    @FXML private Button btnNextMonth;
    @FXML private Button btnCurrentMonth;

    @FXML private Label statActiveLabel;
    @FXML private Label statOffLabel;

    @FXML private GridPane weekdayHeaderGrid;
    @FXML private GridPane calendarGrid;

    private User user;
    private YearMonth currentYearMonth = YearMonth.now();
    private boolean isStudent = false;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.user = user;
        this.isStudent = (user != null && "STUDENT".equalsIgnoreCase(user.role));

        // Enforce role restriction: only students can manage meals
        if (!isStudent) {
            roleAlertBanner.setVisible(true);
            roleAlertBanner.setManaged(true);
            roleAlertLabel.setText("🔒 Access Restricted: Meal on/off management is only available to student accounts. Logged in as: "
                    + (user != null ? user.role : "Unknown"));
        } else {
            roleAlertBanner.setVisible(false);
            roleAlertBanner.setManaged(false);
        }

        setupWeekdayHeaders();
        loadMonth();
    }

    // ────────────────── Weekday Header Setup ──────────────────

    private void setupWeekdayHeaders() {
        weekdayHeaderGrid.getChildren().clear();
        weekdayHeaderGrid.getColumnConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            weekdayHeaderGrid.getColumnConstraints().add(col);

            Label header = new Label(WEEKDAYS[i]);
            header.getStyleClass().add("weekday-header-cell");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            weekdayHeaderGrid.add(header, i, 0);
        }
    }

    // ────────────────── Month Navigation & Loading ──────────────────

    @FXML
    private void handlePrevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        loadMonth();
    }

    @FXML
    private void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        loadMonth();
    }

    @FXML
    private void handleCurrentMonth() {
        currentYearMonth = YearMonth.now();
        loadMonth();
    }

    /**
     * Loads the entire month's meal states in a single background thread query,
     * then populates the calendar grid on the JavaFX thread.
     */
    private void loadMonth() {
        monthTitleLabel.setText(currentYearMonth.format(MONTH_FORMATTER));
        String username = (user != null) ? user.username : null;
        YearMonth targetYm = currentYearMonth;

        AppExecutor.runAsync(
            () -> Database.getMonthlyMealStates(targetYm, username),
            states -> renderCalendar(targetYm, states),
            error -> {
                error.printStackTrace();
                View.showError("Database Error", "Failed to load meal calendar: " + error.getMessage());
            }
        );
    }

    /**
     * Renders the 7-column calendar grid for the specified month and meal states.
     */
    private void renderCalendar(YearMonth ym, Map<LocalDate, String> states) {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(col);
        }

        LocalDate firstDay = ym.atDay(1);
        int daysInMonth = ym.lengthOfMonth();
        LocalDate today = LocalDate.now();

        // Mon = 1, Sun = 7 -> col index 0..6
        int startCol = firstDay.getDayOfWeek().getValue() - 1;

        int row = 0;
        int col = 0;

        // Leading empty cells
        for (int i = 0; i < startCol; i++) {
            Region empty = new Region();
            empty.getStyleClass().add("meal-cell-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            calendarGrid.add(empty, col++, row);
        }

        int activeCount = 0;
        int offCount = 0;

        // Day cells
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            String state = states.getOrDefault(date, "ON");

            if ("OFF".equalsIgnoreCase(state)) {
                offCount++;
            } else if ("ON".equalsIgnoreCase(state)) {
                activeCount++;
            }

            VBox cell = buildDayCell(date, state, today);
            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        // Trailing empty cells to complete the last row
        if (col > 0) {
            for (int i = col; i < 7; i++) {
                Region empty = new Region();
                empty.getStyleClass().add("meal-cell-empty");
                empty.setMaxWidth(Double.MAX_VALUE);
                calendarGrid.add(empty, i, row);
            }
        }

        // Update statistics badges
        statActiveLabel.setText("🟢 Meals ON: " + activeCount + " days");
        statOffLabel.setText("⚪ Meals OFF: " + offCount + " days");
    }

    private VBox buildDayCell(LocalDate date, String state, LocalDate today) {
        VBox cell = new VBox(6);
        cell.getStyleClass().add("meal-day-cell");
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean isToday = date.equals(today);
        boolean isLocked = isDateLocked(date);

        // State classes
        if ("HALL_OFF".equalsIgnoreCase(state)) {
            cell.getStyleClass().add("meal-cell-hall-off");
        } else if ("OFF".equalsIgnoreCase(state)) {
            cell.getStyleClass().add("meal-cell-off");
        } else {
            cell.getStyleClass().add("meal-cell-on");
        }

        if (isToday) {
            cell.getStyleClass().add("meal-cell-today");
        }
        if (isLocked) {
            cell.getStyleClass().add("meal-cell-past");
        }

        // Top line: Day number + Today indicator
        HBox topLine = new HBox(6);
        topLine.setAlignment(Pos.CENTER_LEFT);
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.getStyleClass().add("day-number-label");
        topLine.getChildren().add(dayNum);

        if (isToday) {
            Label todayTag = new Label("Today");
            todayTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #2563eb; -fx-font-weight: bold;");
            topLine.getChildren().add(todayTag);
        }

        // Status Badge
        Label badge = new Label();
        badge.getStyleClass().add("day-status-badge");
        if ("HALL_OFF".equalsIgnoreCase(state)) {
            badge.setText("✕ HALL OFF");
            badge.getStyleClass().add("badge-hall-off");
        } else if ("OFF".equalsIgnoreCase(state)) {
            badge.setText("○ OFF");
            badge.getStyleClass().add("badge-off");
        } else {
            badge.setText("● ON");
            badge.getStyleClass().add("badge-on");
        }

        cell.getChildren().addAll(topLine, badge);

        // Tap/Click handler: Toggle daily meal (only for students)
        if (isStudent) {
            cell.setOnMouseClicked(e -> handleDayClick(date, state, isLocked));
        }

        return cell;
    }

    /**
     * Daily toggle handler: Tapping that date toggles between ON and OFF.
     */
    private void handleDayClick(LocalDate date, String currentState, boolean isLocked) {
        if (!isStudent) {
            View.showError("Access Denied", "Meal management is only available to student accounts.");
            return;
        }

        if ("HALL_OFF".equalsIgnoreCase(currentState)) {
            View.showError("Hall Closed", "Dining hall is closed by administration on " + date + ".");
            return;
        }

        if (isLocked) {
            int lockoutHours = JsonConfig.getMealLockoutHours();
            View.showError(lockoutHours + "-Hour Advance Rule",
                    "Meals can only be changed at least " + lockoutHours + " hours in advance.\n"
                    + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + " cannot be modified.");
            return;
        }

        // Toggle state: ON <-> OFF
        String newState = "ON".equalsIgnoreCase(currentState) ? "OFF" : "ON";
        String username = user != null ? user.username : null;

        AppExecutor.runAsync(
            () -> {
                Database.setMealState(date, username, newState);
                return null;
            },
            res -> {
                if (statusLabel != null) {
                    statusLabel.setText("✅ " + date.format(DateTimeFormatter.ofPattern("dd MMMM")) + " set to MEAL " + newState);
                }
                loadMonth(); // Reload to refresh counters and cell styles
            },
            error -> View.showError("Update Error", "Failed to update meal state: " + error.getMessage())
        );
    }

    // ────────────────── Lockout Helper ──────────────────

    private boolean isDateLocked(LocalDate date) {
        int lockoutHours = JsonConfig.getMealLockoutHours();
        return !date.atStartOfDay().isAfter(LocalDateTime.now().plusHours(lockoutHours));
    }
}
