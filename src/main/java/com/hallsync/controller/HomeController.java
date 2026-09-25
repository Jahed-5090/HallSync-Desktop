package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.Notice;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for home.fxml — the Notice Board / Home page.
 * Quick-action visibility is driven by navigation.json permissions.
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
        loadNotices();
        loadInfoCards();

        // Show quick actions based on JSON permission
        if (JsonConfig.hasPermission(user.role, "showQuickActions")) {
            quickActions.setVisible(true);
            quickActions.setManaged(true);
        }
    }

    private void loadNotices() {
        for (Notice n : Database.notices()) {
            Label h = new Label(n.title());
            h.getStyleClass().add("notice-title");
            Label b = new Label(n.body() + "\n\nPosted by: " + n.postedBy() + " • " + n.date());
            b.getStyleClass().add("notice-body");
            b.setWrapText(true);
            noticesContainer.getChildren().add(View.card(null, h, b));
        }
    }

    private void loadInfoCards() {
        String[] info = Database.hallInfo();
        infoGrid.add(View.card("Dining Manager", new Label(info[0]), new Label(info[1])), 0, 0);
        infoGrid.add(View.card("Active Provost", new Label(info[2])), 1, 0);
        infoGrid.add(View.card("Role", new Label(user.role), new Label("Your menu is role-based.")), 2, 0);
    }

    @FXML private void goToBills()      { dashboard.navigateTo("Bills"); }
    @FXML private void goToMeals()      { dashboard.navigateTo("Meals"); }
    @FXML private void goToComplaints() { dashboard.navigateTo("Complaints"); }
    @FXML private void goToStaff()      { dashboard.navigateTo("Staff"); }
}
