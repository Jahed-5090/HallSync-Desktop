package com.hallsync.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.User;

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
        ObjectNode hallInfo = JsonConfig.loadJsonObject(JsonConfig.hallInfoPath());

        // Achievements
        ArrayNode achievements = (ArrayNode) hallInfo.get("achievements");
        StringBuilder achText = new StringBuilder();
        for (JsonNode el : achievements) {
            if (achText.length() > 0) achText.append("\n");
            achText.append("• ").append(el.asText());
        }
        achievementsContainer.getChildren().add(createWrappedLabel(achText.toString()));

        // Former provosts
        ArrayNode provosts = (ArrayNode) hallInfo.get("formerProvosts");
        StringBuilder provText = new StringBuilder();
        for (JsonNode p : provosts) {
            if (provText.length() > 0) provText.append("\n");
            provText.append(p.get("year").asText()).append(" — ").append(p.get("name").asText());
        }
        formerProvostsContainer.getChildren().add(createWrappedLabel(provText.toString()));
    }

    private Label createWrappedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}
