package com.hallsync.controller;

import com.hallsync.model.User;
import javafx.stage.Stage;

/**
 * Common interface for page controllers that need the stage, user, and
 * parent dashboard context.
 * Implement this in any FXML page controller that needs to navigate or
 * access the logged-in user.
 */
public interface PageController {
    /**
     * Called by DashboardController after the page FXML is loaded.
     *
     * @param stage     the primary stage
     * @param user      the currently logged-in user
     * @param dashboard the parent dashboard controller (for navigation)
     */
    void init(Stage stage, User user, DashboardController dashboard);
}
