package com.hallsync.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hallsync.config.JsonConfig;
import com.hallsync.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL;

    static {
        URL = JsonConfig.getDatabaseUrl();
        init();
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private static void init() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("PRAGMA foreign_keys = ON");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, full_name TEXT, role TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS notices (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, body TEXT, posted_by TEXT, posted_at TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bills (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, month TEXT, rent REAL, meals REAL, electricity REAL, paid REAL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS meals (date TEXT PRIMARY KEY, state TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS complaints (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, subject TEXT, message TEXT, status TEXT, posted_at TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT, department TEXT, roll TEXT, session TEXT, room TEXT, block TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS hall_info (id INTEGER PRIMARY KEY CHECK(id=1), dining_manager TEXT, dining_contact TEXT, provost TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS committee (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, position TEXT, department TEXT, room TEXT, contact TEXT)");

            seedUsers(c);
            seedNotices(c);
            seedBills(c);
            seedMeals(c);
            seedStudents(c);
            seedHallInfo(c);
            seedCommittee(c);
        } catch (SQLException e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }

    // ────────────────── Seed Methods (from JSON) ──────────────────

    private static void seedUsers(Connection c) throws SQLException {
        JsonArray users = JsonConfig.loadJsonArray(JsonConfig.seedUsersPath());
        try (PreparedStatement p = c.prepareStatement(
                "INSERT OR IGNORE INTO users(username,password,full_name,role) VALUES(?,?,?,?)")) {
            for (JsonElement el : users) {
                JsonObject u = el.getAsJsonObject();
                p.setString(1, u.get("username").getAsString());
                p.setString(2, u.get("password").getAsString());
                p.setString(3, u.get("fullName").getAsString());
                p.setString(4, u.get("role").getAsString());
                p.executeUpdate();
            }
        }
    }

    private static void seedNotices(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM notices")) {
            if (r.next() && r.getInt(1) == 0) {
                JsonArray notices = JsonConfig.loadJsonArray(JsonConfig.seedNoticesPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO notices(title,body,posted_by,posted_at) VALUES(?,?,?,datetime('now'))")) {
                    for (JsonElement el : notices) {
                        JsonObject n = el.getAsJsonObject();
                        p.setString(1, n.get("title").getAsString());
                        p.setString(2, n.get("body").getAsString());
                        p.setString(3, n.get("postedBy").getAsString());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedBills(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM bills")) {
            if (r.next() && r.getInt(1) == 0) {
                JsonArray bills = JsonConfig.loadJsonArray(JsonConfig.seedBillsPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO bills(student_id,month,rent,meals,electricity,paid) VALUES(?,?,?,?,?,?)")) {
                    for (JsonElement el : bills) {
                        JsonObject b = el.getAsJsonObject();
                        p.setInt(1, b.get("studentId").getAsInt());
                        p.setString(2, b.get("month").getAsString());
                        p.setDouble(3, b.get("rent").getAsDouble());
                        p.setDouble(4, b.get("meals").getAsDouble());
                        p.setDouble(5, b.get("electricity").getAsDouble());
                        p.setDouble(6, b.get("paid").getAsDouble());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedMeals(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM meals")) {
            if (r.next() && r.getInt(1) == 0) {
                int weeks = JsonConfig.getMealCalendarWeeks();
                String defaultState = JsonConfig.getDefaultMealState();
                try (PreparedStatement p = c.prepareStatement("INSERT INTO meals(date,state) VALUES(?,?)")) {
                    for (int i = 0; i < weeks * 7; i++) {
                        p.setString(1, LocalDate.now().plusDays(i).toString());
                        p.setString(2, defaultState);
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedStudents(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM students")) {
            if (r.next() && r.getInt(1) == 0) {
                JsonArray students = JsonConfig.loadJsonArray(JsonConfig.seedStudentsPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO students(full_name,department,roll,session,room,block) VALUES(?,?,?,?,?,?)")) {
                    for (JsonElement el : students) {
                        JsonObject st = el.getAsJsonObject();
                        p.setString(1, st.get("fullName").getAsString());
                        p.setString(2, st.get("department").getAsString());
                        p.setString(3, st.get("roll").getAsString());
                        p.setString(4, st.get("session").getAsString());
                        p.setString(5, st.get("room").getAsString());
                        p.setString(6, st.get("block").getAsString());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedHallInfo(Connection c) throws SQLException {
        JsonObject hallInfo = JsonConfig.loadJsonObject(JsonConfig.hallInfoPath());
        JsonObject team = hallInfo.getAsJsonObject("currentTeam");
        try (PreparedStatement p = c.prepareStatement(
                "INSERT OR IGNORE INTO hall_info(id,dining_manager,dining_contact,provost) VALUES(1,?,?,?)")) {
            p.setString(1, team.get("diningManager").getAsString());
            p.setString(2, team.get("diningContact").getAsString());
            p.setString(3, team.get("provost").getAsString());
            p.executeUpdate();
        }
    }

    private static void seedCommittee(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM committee")) {
            if (r.next() && r.getInt(1) == 0) {
                JsonArray members = JsonConfig.loadJsonArray(JsonConfig.seedCommitteePath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO committee(name,position,department,room,contact) VALUES(?,?,?,?,?)")) {
                    for (JsonElement el : members) {
                        JsonObject m = el.getAsJsonObject();
                        p.setString(1, m.get("name").getAsString());
                        p.setString(2, m.get("position").getAsString());
                        p.setString(3, m.get("department").getAsString());
                        p.setString(4, m.get("room").getAsString());
                        p.setString(5, m.get("contact").getAsString());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    // ────────────────── Query Methods (unchanged) ──────────────────

    public static User login(String username, String password) {
        String sql = "SELECT id,username,full_name,role FROM users WHERE username=? AND password=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username); p.setString(2, password);
            try (ResultSet r = p.executeQuery()) { if (r.next()) return new User(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static List<Notice> notices() {
        List<Notice> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT title,body,posted_by,posted_at FROM notices ORDER BY id DESC")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Notice(r.getString(1), r.getString(2), r.getString(3), r.getString(4)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void addNotice(String title, String body, String postedBy) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("INSERT INTO notices(title,body,posted_by,posted_at) VALUES(?,?,?,datetime('now'))")) {
            p.setString(1, title); p.setString(2, body); p.setString(3, postedBy); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<Bill> bills() {
        List<Bill> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT month,rent,meals,electricity,paid FROM bills ORDER BY id DESC")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Bill(r.getString(1), r.getDouble(2), r.getDouble(3), r.getDouble(4), r.getDouble(5)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static String mealState(LocalDate date) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT state FROM meals WHERE date=?")) {
            p.setString(1, date.toString());
            try (ResultSet r = p.executeQuery()) { if (r.next()) return r.getString(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return JsonConfig.getDefaultMealState();
    }

    public static void setMealState(LocalDate date, String state) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("INSERT INTO meals(date,state) VALUES(?,?) ON CONFLICT(date) DO UPDATE SET state=excluded.state")) {
            p.setString(1, date.toString()); p.setString(2, state); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void setHallOff(LocalDate date) { setMealState(date, "HALL_OFF"); }

    public static void addComplaint(String user, String subject, String message) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("INSERT INTO complaints(username,subject,message,status,posted_at) VALUES(?,?,?,'OPEN',datetime('now'))")) {
            p.setString(1, user); p.setString(2, subject); p.setString(3, message); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<Complaint> complaints() {
        List<Complaint> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT id,username,subject,message,status,posted_at FROM complaints ORDER BY id DESC")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Complaint(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void updateComplaint(int id, String status) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("UPDATE complaints SET status=? WHERE id=?")) {
            p.setString(1, status); p.setInt(2, id); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<Student> searchStudents(String term) {
        List<Student> list = new ArrayList<>();
        String q = "%" + term.toLowerCase() + "%";
        String sql = "SELECT full_name,department,roll,session,room,block FROM students WHERE lower(full_name) LIKE ? OR lower(department) LIKE ? OR lower(roll) LIKE ? OR lower(room) LIKE ? OR lower(block) LIKE ? ORDER BY full_name";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) p.setString(i, q);
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Student(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static String[] hallInfo() {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT dining_manager,dining_contact,provost FROM hall_info WHERE id=1"); ResultSet r = p.executeQuery()) {
            if (r.next()) return new String[]{r.getString(1), r.getString(2), r.getString(3)};
        } catch (SQLException e) { e.printStackTrace(); }
        return new String[]{"Not set", "", "Not set"};
    }

    public static List<CommitteeMember> committee() {
        List<CommitteeMember> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT name,position,department,room,contact FROM committee ORDER BY id")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new CommitteeMember(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
