package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.Notice;
import com.hallsync.model.User;
import com.hallsync.util.AppExecutor;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for home.fxml — Notice Board / Home page.
 * Loads notices and hall info in parallel using AppExecutor thread pool.
 */
public class HomeController implements PageController {

    @FXML private VBox noticesContainer;
    @FXML private GridPane infoGrid;
    @FXML private HBox quickActions;

    private Stage stage;
    private User user;
    private DashboardController dashboard;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.stage = stage;
        this.user = user;
        this.dashboard = dashboard;

        // Show loading placeholder immediately
        noticesContainer.getChildren().add(new Label("Loading notices..."));

        // Load notices and hall info in parallel on the thread pool
        loadNoticesAsync();
        loadInfoCardsAsync();

        // Show quick actions based on JSON permission
        if (JsonConfig.hasPermission(user.role, "showQuickActions")) {
            quickActions.setVisible(true);
            quickActions.setManaged(true);
        }
    }

    /**
     * Fetches notices via thread pool and renders cards on the UI thread.
     */
    private void loadNoticesAsync() {
        AppExecutor.runAsync(
            () -> Database.notices(),
            notices -> {
                noticesContainer.getChildren().clear();
                if (notices.isEmpty()) {
                    noticesContainer.getChildren().add(new Label("No notices posted yet."));
                } else {
                    int count = 0;
                    for (Notice n : notices) {
                        if (count >= 4) break;
                        Label h = new Label(n.title());
                        h.getStyleClass().add("notice-title");
                        Label b = new Label(n.body() + "\n\nPosted by: " + n.postedBy() + " • " + n.date());
                        b.getStyleClass().add("notice-body");
                        b.setWrapText(true);
                        noticesContainer.getChildren().add(View.card(null, h, b));
                        count++;
                    }
                }
            },
            error -> {
                noticesContainer.getChildren().clear();
                noticesContainer.getChildren().add(new Label("Failed to load notices."));
            }
        );
    }

    /**
     * Fetches hall info via thread pool and populates the cards on the UI thread.
     */
    private void loadInfoCardsAsync() {
        AppExecutor.runAsync(
            () -> Database.hallInfo(),
            info -> {
                infoGrid.add(View.card("Dining Manager", new Label(info[0]), new Label(info[1])), 0, 0);
                infoGrid.add(View.card("Active Provost", new Label(info[2])), 1, 0);
                infoGrid.add(View.card("Role", new Label(user.role), new Label("Your menu is role-based.")), 2, 0);
            },
            error -> {
                infoGrid.add(new Label("Failed to load hall info."), 0, 0);
            }
        );
    }

    @FXML private void goToBills()      { dashboard.navigateTo("Bills"); }
    @FXML private void goToMeals()      { dashboard.navigateTo("Meals"); }
    @FXML private void goToComplaints() { dashboard.navigateTo("Complaints"); }
    @FXML private void goToStaff()      { dashboard.navigateTo("Staff"); }
}
