package com.hallsync.controller;

import com.hallsync.config.JsonConfig;
import com.hallsync.database.Database;
import com.hallsync.model.Bill;
import com.hallsync.model.User;
import com.hallsync.view.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller for bills.fxml — the Fees & Dues page.
 * Currency formatting is driven by app-config.json.
 */
public class BillsController implements PageController {

    @FXML private VBox billsList;

    private Stage stage;
    private User user;
    private List<Bill> bills;

    @Override
    public void init(Stage stage, User user, DashboardController dashboard) {
        this.stage = stage;
        this.user = user;
        this.bills = Database.bills();
        loadBills();
    }

    private void loadBills() {
        for (Bill b : bills) {
            GridPane g = new GridPane();
            g.setHgap(25);
            g.setVgap(7);
            g.add(new Label("Rent"), 0, 0);       g.add(new Label(money(b.rent())), 1, 0);
            g.add(new Label("Meals"), 0, 1);       g.add(new Label(money(b.meals())), 1, 1);
            g.add(new Label("Electricity"), 0, 2); g.add(new Label(money(b.electricity())), 1, 2);
            double extraTotal = 0.0;
            try (java.sql.Connection c = Database.connect(); 
                 java.sql.PreparedStatement pMonth = c.prepareStatement("SELECT sector, amount FROM bill_scopes WHERE month=?");
                 java.sql.PreparedStatement pDefault = c.prepareStatement("SELECT sector, amount FROM bill_scopes WHERE month='DEFAULT'")) {
                
                pMonth.setString(1, b.month());
                
                java.util.List<String[]> currentScopes = new java.util.ArrayList<>();
                
                try (java.sql.ResultSet r = pMonth.executeQuery()) {
                    while (r.next()) {
                        currentScopes.add(new String[]{r.getString("sector"), String.valueOf(r.getDouble("amount"))});
                    }
                }
                
                if (currentScopes.isEmpty()) {
                    try (java.sql.ResultSet r = pDefault.executeQuery()) {
                        while (r.next()) {
                            currentScopes.add(new String[]{r.getString("sector"), String.valueOf(r.getDouble("amount"))});
                        }
                    }
                }

                int row = 4;
                for (String[] scope : currentScopes) {
                    double amount = Double.parseDouble(scope[1]);
                    g.add(new Label(scope[0]), 0, row);
                    g.add(new Label(money(amount)), 1, row);
                    extraTotal += amount;
                    row++;
                }
                g.add(new Label("Total"), 0, row);       
                g.add(new Label(money(b.total() + extraTotal)), 1, row);
                row++;
                g.add(new Label("Paid"), 0, row);        
                g.add(new Label(money(b.paid())), 1, row);
                
            } catch (Exception e) {
                e.printStackTrace();
            }

            double grandTotal = b.total() + extraTotal;
            double actualDue = Math.max(0, grandTotal - b.paid());

            Label due = new Label("Past due / current due: " + money(actualDue));
            due.getStyleClass().add(actualDue > 0 ? "due" : "paid");

            billsList.getChildren().add(View.card(b.month(), g, due));
        }
    }

    @FXML
    private void handleExportPdf() {
        if (bills == null || bills.isEmpty()) {
            View.showError("No bill", "No bill record exists.");
            return;
        }
        Bill b = bills.get(0);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save bill PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("HallSync_Bill.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            double extraTotal = 0.0;
            java.util.List<String> dynamicLines = new java.util.ArrayList<>();
            try (java.sql.Connection c = Database.connect(); 
                 java.sql.PreparedStatement pMonth = c.prepareStatement("SELECT sector, amount FROM bill_scopes WHERE month=?");
                 java.sql.PreparedStatement pDefault = c.prepareStatement("SELECT sector, amount FROM bill_scopes WHERE month='DEFAULT'")) {
                 
                pMonth.setString(1, b.month());
                java.util.List<String[]> currentScopes = new java.util.ArrayList<>();
                
                try (java.sql.ResultSet r = pMonth.executeQuery()) {
                    while (r.next()) {
                        currentScopes.add(new String[]{r.getString("sector"), String.valueOf(r.getDouble("amount"))});
                    }
                }
                
                if (currentScopes.isEmpty()) {
                    try (java.sql.ResultSet r = pDefault.executeQuery()) {
                        while (r.next()) {
                            currentScopes.add(new String[]{r.getString("sector"), String.valueOf(r.getDouble("amount"))});
                        }
                    }
                }
                
                for (String[] scope : currentScopes) {
                    double amount = Double.parseDouble(scope[1]);
                    dynamicLines.add(scope[0] + ": " + money(amount));
                    extraTotal += amount;
                }
            }
            
            double grandTotal = b.total() + extraTotal;
            double actualDue = Math.max(0, grandTotal - b.paid());
            
            java.util.List<String> lines = new java.util.ArrayList<>();
            lines.add("Student: " + user.fullName);
            lines.add("Rent: " + money(b.rent()));
            lines.add("Meals: " + money(b.meals()));
            lines.add("Electricity: " + money(b.electricity()));
            lines.addAll(dynamicLines);
            lines.add("Total: " + money(grandTotal));
            lines.add("Paid: " + money(b.paid()));
            lines.add("Due: " + money(actualDue));
            
            writePdf(file, JsonConfig.getAppName() + " - " + b.month(), lines.toArray(new String[0]));
            View.showInfo("PDF created", "Bill exported successfully.");
        } catch (Exception ex) {
            View.showError("PDF error", ex.getMessage());
        }
    }

    private String money(double x) {
        return String.format(JsonConfig.getCurrencyFormat(), x);
    }

    /** Minimal PDF writer – same logic as the original AppController.SimplePdf */
    private void writePdf(File file, String title, String[] lines) throws Exception {
        String currencySymbol = JsonConfig.getCurrencySymbol();
        String currencyFallback = JsonConfig.getCurrencyFallback();

        StringBuilder body = new StringBuilder();
        body.append("BT\n/F1 18 Tf\n50 750 Td\n").append(pdfText(title, currencySymbol, currencyFallback)).append(" Tj\n/F1 11 Tf\n0 -30 Td\n");
        for (String line : lines) {
            body.append(pdfText(line, currencySymbol, currencyFallback)).append(" Tj\n0 -22 Td\n");
        }
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
            for (int i = 0; i < objects.length; i++) {
                offsets[i + 1] = out.getChannel().position();
                String obj = (i + 1) + " 0 obj\n" + objects[i] + "\nendobj\n";
                out.write(obj.getBytes(StandardCharsets.UTF_8));
            }
            long xref = out.getChannel().position();
            out.write(("xref\n0 " + (objects.length + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (int i = 1; i <= objects.length; i++) {
                out.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
            }
            out.write(("trailer\n<< /Size " + (objects.length + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF")
                    .getBytes(StandardCharsets.US_ASCII));
        }
    }

    private String pdfText(String s, String symbol, String fallback) {
        return "(" + s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace(symbol, fallback + " ") + ")";
    }
}
