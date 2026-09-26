package com.hallsync.view;

import com.hallsync.config.JsonConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;


import java.util.function.Consumer;

public class View {
    public static Label title(String text) { Label l = new Label(text); l.getStyleClass().add("page-title"); return l; }
    public static Label subtitle(String text) { Label l = new Label(text); l.getStyleClass().add("subtitle"); l.setWrapText(true); return l; }
    public static VBox card(String heading, Node... nodes) {
        VBox box = new VBox(10); box.getStyleClass().add("card");
        if (heading != null && !heading.isBlank()) { Label h = new Label(heading); h.getStyleClass().add("card-title"); box.getChildren().add(h); }
        box.getChildren().addAll(nodes); return box;
    }
    public static Button button(String text) { Button b = new Button(text); b.getStyleClass().add("action-button"); return b; }
    public static void showInfo(String title, String message) { Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK); a.setTitle(title); a.setHeaderText(null); a.showAndWait(); }
    public static void showError(String title, String message) { Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK); a.setTitle(title); a.setHeaderText(null); a.showAndWait(); }
    public static BorderPane shell(String name, String role, Consumer<String> nav, Runnable logout) {
        BorderPane root = new BorderPane(); root.getStyleClass().add("root");
        VBox sidebar = new VBox(8); sidebar.getStyleClass().add("sidebar"); sidebar.setPadding(new Insets(20)); sidebar.setPrefWidth(JsonConfig.getSidebarWidth());
        Label brand = new Label(JsonConfig.getBrandText()); brand.getStyleClass().add("brand");
        Label user = new Label(name + "\n" + role); user.getStyleClass().add("user-label");
        sidebar.getChildren().addAll(brand, user, new Separator());
        // Menu items loaded from navigation.json
        String[] items = JsonConfig.getMenuItems(role);
        for (String item : items) { Button b = new Button(item); b.getStyleClass().add("nav-button"); b.setMaxWidth(Double.MAX_VALUE); b.setOnAction(e -> nav.accept(item)); sidebar.getChildren().add(b); }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS); Button out = new Button("Log out"); out.getStyleClass().add("nav-button"); out.setMaxWidth(Double.MAX_VALUE); out.setOnAction(e -> logout.run()); sidebar.getChildren().addAll(spacer, out);
        root.setLeft(sidebar);
        return root;
    }
    public static HBox row(Node... nodes) { HBox h = new HBox(10, nodes); h.setAlignment(Pos.CENTER_LEFT); return h; }
    public static void stylePlaceholder(TextField t, String text) { t.setPromptText(text); t.setMaxWidth(Double.MAX_VALUE); }
}
