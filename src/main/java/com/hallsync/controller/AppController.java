package com.hallsync.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.*;
import com.hallsync.view.View;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AppController {
    private final Stage stage;
    private User user;
    private BorderPane shell;
    private VBox content;

    public AppController(Stage stage) { this.stage = stage; }

    public void showLogin() {
        VBox box = new VBox(15); box.setMaxWidth(380); box.getStyleClass().add("login-box");
        Label title = View.title("HallSync");
        Label sub = View.subtitle("University Residential Hall Management");
        TextField username = new TextField(); username.setPromptText("Username");
        PasswordField password = new PasswordField(); password.setPromptText("Password");
        Button login = View.button("Login");
        Label hint = new Label("Demo accounts: admin/admin · provost/provost · student/student"); hint.getStyleClass().add("hint"); hint.setWrapText(true);
        login.setOnAction(e -> {
            User found = Database.login(username.getText().trim(), password.getText());
            if (found == null) View.showError("Login failed", "Incorrect username or password."); else { user = found; showDashboard("Home"); }
        });
        box.getChildren().addAll(title, sub, username, password, login, hint);
        StackPane root = new StackPane(box); root.getStyleClass().add("login-root");
        stage.setScene(new Scene(root, JsonConfig.getLoginWidth(), JsonConfig.getLoginHeight())); stage.setTitle(JsonConfig.getAppTitle()); stage.show();
    }

    private void showDashboard(String page) {
        shell = View.shell(user.fullName, user.role, this::showDashboard, this::showLogin);
        content = new VBox(18); content.getStyleClass().add("page"); content.setPadding(new Insets(26));
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true); scroll.getStyleClass().add("scroll"); shell.setCenter(scroll);
        render(page);
        stage.setScene(new Scene(shell, JsonConfig.getDashboardWidth(), JsonConfig.getDashboardHeight()));
        stage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    }

    private void render(String page) {
        content.getChildren().clear();
        if (user.role.equals("ADMIN") && page.equals("Notices")) adminNoticePage();
        else if (page.equals("Home")) homePage();
        else if (page.equals("Bills")) billsPage();
        else if (page.equals("Meals")) mealsPage();
        else if (page.equals("Complaints")) complaintsPage();
        else if (page.equals("Directory")) directoryPage();
        else if (page.equals("Hall Info")) hallInfoPage();
        else if (page.equals("Committee")) committeePage();
        else if (page.equals("Staff")) staffPage();
        else homePage();
    }

    private void homePage() {
        content.getChildren().add(View.title("Notice Board"));
        content.getChildren().add(View.subtitle("Important hall updates appear first on every dashboard."));
        VBox notices = new VBox(12);
        for (Notice n : Database.notices()) {
            Label h = new Label(n.title()); h.getStyleClass().add("notice-title");
            Label b = new Label(n.body() + "\n\nPosted by: " + n.postedBy() + " • " + n.date()); b.getStyleClass().add("notice-body"); b.setWrapText(true);
            notices.getChildren().add(View.card(null, h, b));
        }
        content.getChildren().add(notices);

        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(16);
        String[] info = Database.hallInfo();
        grid.add(View.card("Dining Manager", new Label(info[0]), new Label(info[1])), 0, 0);
        grid.add(View.card("Active Provost", new Label(info[2])), 1, 0);
        grid.add(View.card("Role", new Label(user.role), new Label("Your menu is role-based.")), 2, 0);
        content.getChildren().add(grid);
        Button staffBtn = View.button("👥  View Hall Staff"); staffBtn.setOnAction(e -> showDashboard("Staff")); content.getChildren().add(staffBtn);
        if (user.role.equals("STUDENT")) content.getChildren().add(studentQuickActions());
    }

    private HBox studentQuickActions() {
        Button bill = View.button("View Bills"); bill.setOnAction(e -> showDashboard("Bills"));
        Button meal = View.button("Manage Meals"); meal.setOnAction(e -> showDashboard("Meals"));
        Button complaint = View.button("Submit Complaint"); complaint.setOnAction(e -> showDashboard("Complaints"));
        return View.row(bill, meal, complaint);
    }

    private void adminNoticePage() {
        content.getChildren().add(View.title("Publish Notice"));
        TextField title = new TextField(); title.setPromptText("Notice title");
        TextArea body = new TextArea(); body.setPromptText("Notice details"); body.setPrefRowCount(5);
        Button post = View.button("Publish"); post.setOnAction(e -> {
            if (title.getText().isBlank() || body.getText().isBlank()) { View.showError("Missing data", "Enter both title and notice details."); return; }
            Database.addNotice(title.getText().trim(), body.getText().trim(), user.fullName); title.clear(); body.clear(); showDashboard("Notices");
        });
        content.getChildren().add(View.card("Admin notice editor", title, body, post));
        content.getChildren().add(View.title("Current Notices"));
        for (Notice n : Database.notices()) content.getChildren().add(View.card(n.title(), new Label(n.body()), new Label("Posted by " + n.postedBy() + " • " + n.date())));
    }

    private void billsPage() {
        content.getChildren().add(View.title("Fees & Dues"));
        List<Bill> bills = Database.bills();
        VBox list = new VBox(12);
        for (Bill b : bills) {
            GridPane g = new GridPane(); g.setHgap(25); g.setVgap(7);
            g.add(new Label("Rent"), 0, 0); g.add(new Label(money(b.rent())), 1, 0);
            g.add(new Label("Meals"), 0, 1); g.add(new Label(money(b.meals())), 1, 1);
            g.add(new Label("Electricity"), 0, 2); g.add(new Label(money(b.electricity())), 1, 2);
            g.add(new Label("Total"), 0, 3); g.add(new Label(money(b.total())), 1, 3);
            g.add(new Label("Paid"), 0, 4); g.add(new Label(money(b.paid())), 1, 4);
            Label due = new Label("Past due / current due: " + money(b.due())); due.getStyleClass().add(b.due() > 0 ? "due" : "paid");
            VBox card = View.card(b.month(), g, due);
            list.getChildren().add(card);
        }
        Button export = View.button("Export Bill to PDF"); export.setOnAction(e -> exportBill(bills.isEmpty() ? null : bills.get(0)));
        content.getChildren().addAll(list, export);
    }

    private String money(double x) { return String.format(JsonConfig.getCurrencyFormat(), x); }

    private void exportBill(Bill b) {
        if (b == null) { View.showError("No bill", "No bill record exists."); return; }
        FileChooser chooser = new FileChooser(); chooser.setTitle("Save bill PDF"); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf")); chooser.setInitialFileName("HallSync_Bill.pdf");
        File file = chooser.showSaveDialog(stage); if (file == null) return;
        try { SimplePdf.write(file, "HallSync - " + b.month(), new String[]{
                "Student: " + user.fullName,
                "Rent: " + money(b.rent()),
                "Meals: " + money(b.meals()),
                "Electricity: " + money(b.electricity()),
                "Total: " + money(b.total()),
                "Paid: " + money(b.paid()),
                "Due: " + money(b.due())
        }); View.showInfo("PDF created", "Bill exported successfully."); }
        catch (Exception ex) { View.showError("PDF error", ex.getMessage()); }
    }

    private void mealsPage() {
        content.getChildren().add(View.title("Meal Management Calendar"));
        content.getChildren().add(View.subtitle("Green = active, Grey = student OFF, Red = hall OFF. Student changes are allowed only 24+ hours ahead."));
        VBox calendar = new VBox(8);
        LocalDate start = LocalDate.now();
        for (int week = 0; week < JsonConfig.getMealCalendarWeeks(); week++) {
            HBox row = new HBox(8);
            for (int day = 0; day < 7; day++) {
                LocalDate date = start.plusDays(week * 7L + day);
                Button b = new Button(date.getDayOfMonth() + "\n" + date.getMonthValue()); b.getStyleClass().add("meal-cell");
                updateMealButton(b, date);
                if (user.role.equals("STUDENT")) b.setOnAction(e -> toggleMeal(date, b));
                else if (user.role.equals("ADMIN")) b.setOnAction(e -> hallOff(date, b));
                row.getChildren().add(b);
            }
            calendar.getChildren().add(row);
        }
        content.getChildren().add(calendar);
        Label legend = new Label("Today and the next 23 hours are protected from student changes."); legend.getStyleClass().add("hint"); content.getChildren().add(legend);
    }

    private void updateMealButton(Button b, LocalDate date) {
        String state = Database.mealState(date); b.getStyleClass().removeAll("meal-on", "meal-off", "meal-hall-off");
        if (state.equals("HALL_OFF")) { b.getStyleClass().add("meal-hall-off"); b.setText(date.getDayOfMonth() + "\nHALL OFF"); }
        else if (state.equals("OFF")) { b.getStyleClass().add("meal-off"); b.setText(date.getDayOfMonth() + "\nOFF"); }
        else { b.getStyleClass().add("meal-on"); b.setText(date.getDayOfMonth() + "\nON"); }
    }

    private void toggleMeal(LocalDate date, Button b) {
        int lockout = JsonConfig.getMealLockoutHours();
        if (!date.atStartOfDay().isAfter(LocalDateTime.now().plusHours(lockout))) { View.showError(lockout + "-hour rule", "Meals can only be changed at least " + lockout + " hours in advance."); return; }
        String state = Database.mealState(date); if (state.equals("HALL_OFF")) return;
        Database.setMealState(date, state.equals("ON") ? "OFF" : "ON"); updateMealButton(b, date);
    }

    private void hallOff(LocalDate date, Button b) {
        Database.setHallOff(date); updateMealButton(b, date);
    }

    private void complaintsPage() {
        content.getChildren().add(View.title("Complaints"));
        if (user.role.equals("STUDENT")) {
            TextField subject = new TextField(); subject.setPromptText("Subject"); TextArea message = new TextArea(); message.setPromptText("Describe the service issue"); message.setPrefRowCount(5);
            Button submit = View.button("Submit Complaint"); submit.setOnAction(e -> {
                if (subject.getText().isBlank() || message.getText().isBlank()) { View.showError("Missing data", "Enter a subject and message."); return; }
                Database.addComplaint(user.username, subject.getText(), message.getText()); subject.clear(); message.clear(); View.showInfo("Submitted", "Your complaint was sent to the Hall Office."); render("Complaints");
            });
            content.getChildren().add(View.card("New complaint", subject, message, submit));
        }
        content.getChildren().add(View.title(user.role.equals("ADMIN") ? "All Complaints" : "Complaint Portal"));
        for (Complaint c : Database.complaints()) {
            HBox top = new HBox(12, new Label("#" + c.id()), new Label(c.subject()), new Label("From: " + c.user()), new Label(c.status())); top.setAlignment(Pos.CENTER_LEFT);
            Label msg = new Label(c.message() + "\n" + c.date()); msg.setWrapText(true);
            if (user.role.equals("ADMIN")) {
                Button done = View.button("Mark Resolved"); done.setOnAction(e -> { Database.updateComplaint(c.id(), "RESOLVED"); render("Complaints"); });
                content.getChildren().add(View.card(null, top, msg, done));
            } else content.getChildren().add(View.card(null, top, msg));
        }
    }

    private void directoryPage() {
        content.getChildren().add(View.title("Universal Student Directory"));
        TextField search = new TextField(); search.setPromptText("Search by name, department, roll, room or block");
        VBox results = new VBox(10); Button find = View.button("Search");
        Runnable runSearch = () -> { results.getChildren().clear(); for (Student s : Database.searchStudents(search.getText().trim())) {
            results.getChildren().add(View.card(s.name(), new Label("Department: " + s.department()), new Label("Roll: " + s.roll()), new Label("Session: " + s.session()), new Label("Room: " + s.room()), new Label("Block: " + s.block())));
        }};
        find.setOnAction(e -> runSearch.run()); search.setOnAction(e -> runSearch.run()); runSearch.run();
        content.getChildren().addAll(View.card("Read-only search", search, find), results);
    }

    private void hallInfoPage() {
        content.getChildren().add(View.title("Hall Information"));
        String[] info = Database.hallInfo();
        content.getChildren().add(View.card("Current Hall Team", new Label("Provost: " + info[2]), new Label("Dining Manager: " + info[0]), new Label("Dining Manager Contact: " + info[1])));
        ObjectNode hallData = JsonConfig.loadJsonObject(JsonConfig.hallInfoPath());
        ArrayNode achievements = (ArrayNode) hallData.get("achievements");
        StringBuilder achText = new StringBuilder();
        for (JsonNode el : achievements) { if (achText.length() > 0) achText.append("\n"); achText.append("• ").append(el.asText()); }
        content.getChildren().add(View.card("Hall Achievements", new Label(achText.toString())));
        ArrayNode provosts = (ArrayNode) hallData.get("formerProvosts");
        StringBuilder provText = new StringBuilder();
        for (JsonNode el : provosts) { if (provText.length() > 0) provText.append("\n"); provText.append(el.get("year").asText()).append(" — ").append(el.get("name").asText()); }
        content.getChildren().add(View.card("Former Provost Archive", new Label(provText.toString())));
    }

    private void committeePage() {
        content.getChildren().add(View.title("Student Hall Committee"));
        content.getChildren().add(View.subtitle("Public student profiles are linked to the committee records below."));
        for (CommitteeMember m : Database.committee()) {
            Button profile = View.button("Open public profile"); profile.setOnAction(e -> View.showInfo("Public Profile", m.name() + "\n\nPosition: " + m.position() + "\nDepartment: " + m.department() + "\nRoom: " + m.room() + "\nContact: " + m.contact()));
            content.getChildren().add(View.card(m.name(), new Label("Position: " + m.position()), new Label("Department: " + m.department()), new Label("Room: " + m.room()), profile));
        }
    }

    private void staffPage() {
        content.getChildren().add(View.title("Hall Staff Directory"));
        content.getChildren().add(View.subtitle("All current staff members of the hall and their contact information."));
        Button back = View.button("← Back to Home"); back.setOnAction(e -> showDashboard("Home")); content.getChildren().add(back);
        for (StaffMember s : Database.hallStaff()) {
            content.getChildren().add(View.card(s.name(),
                new Label("Role: " + s.workRole()),
                new Label("Phone: " + s.phone()),
                new Label("Location: " + s.location())));
        }
    }

    private static class SimplePdf {
        static void write(File file, String title, String[] lines) throws Exception {
            StringBuilder body = new StringBuilder(); body.append("BT\n/F1 18 Tf\n50 750 Td\n").append(text(title)).append(" Tj\n/F1 11 Tf\n0 -30 Td\n");
            for (String line : lines) { body.append(text(line)).append(" Tj\n0 -22 Td\n"); }
            body.append("ET");
            byte[] stream = body.toString().getBytes(StandardCharsets.UTF_8);
            String[] objects = {
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                    "<< /Length " + stream.length + " >>\nstream\n" + body + "\nendstream"
            };
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
                long[] offsets = new long[objects.length + 1];
                for (int i = 0; i < objects.length; i++) { offsets[i + 1] = out.getChannel().position(); String obj = (i + 1) + " 0 obj\n" + objects[i] + "\nendobj\n"; out.write(obj.getBytes(StandardCharsets.UTF_8)); }
                long xref = out.getChannel().position(); out.write(("xref\n0 " + (objects.length + 1) + "\n").getBytes(StandardCharsets.US_ASCII)); out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
                for (int i = 1; i <= objects.length; i++) out.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
                out.write(("trailer\n<< /Size " + (objects.length + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
            }
        }
        private static String text(String s) { return "(" + s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace(JsonConfig.getCurrencySymbol(), JsonConfig.getCurrencyFallback() + " ") + ")"; }
    }
}
