package com.hallsync.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for hallinfo.fxml — the Hall Information page.
 * Achievements and former provosts are loaded from hall-info.json.
 */
public class HallInfoController implements PageController {

    @FXML private Label provostLabel;
    @FXML private Label diningManagerLabel;
    @FXML private Label diningContactLabel;
    @FXML private VBox achievementsContainer;
    @FXML private VBox formerProvostsContainer;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        // Load current team from the database
        String[] info = Database.hallInfo();
        provostLabel.setText("Provost: " + info[2]);
        diningManagerLabel.setText("Dining Manager: " + info[0]);
        diningContactLabel.setText("Dining Manager Contact: " + info[1]);

        // Load achievements and former provosts from hall-info.json
        JsonObject hallInfo = JsonConfig.loadJsonObject(JsonConfig.hallInfoPath());

        // Achievements
        JsonArray achievements = hallInfo.getAsJsonArray("achievements");
        StringBuilder achText = new StringBuilder();
        for (JsonElement el : achievements) {
            if (achText.length() > 0) achText.append("\n");
            achText.append("• ").append(el.getAsString());
        }
        achievementsContainer.getChildren().add(createWrappedLabel(achText.toString()));

        // Former provosts
        JsonArray provosts = hallInfo.getAsJsonArray("formerProvosts");
        StringBuilder provText = new StringBuilder();
        for (JsonElement el : provosts) {
            JsonObject p = el.getAsJsonObject();
            if (provText.length() > 0) provText.append("\n");
            provText.append(p.get("year").getAsString()).append(" — ").append(p.get("name").getAsString());
        }
        formerProvostsContainer.getChildren().add(createWrappedLabel(provText.toString()));
    }

    private Label createWrappedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}
