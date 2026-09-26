package com.hallsync.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hallsync.config.JsonConfig;
import com.hallsync.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, full_name TEXT, role TEXT, email TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS notices (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, body TEXT, posted_by TEXT, posted_at TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bills (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, month TEXT, rent REAL, meals REAL, electricity REAL, paid REAL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS meals (date TEXT PRIMARY KEY, state TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS student_meals (username TEXT NOT NULL, date TEXT NOT NULL, state TEXT NOT NULL, PRIMARY KEY (username, date))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS complaints (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, subject TEXT, message TEXT, status TEXT, posted_at TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT, department TEXT, roll TEXT, session TEXT, room TEXT, block TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS hall_info (id INTEGER PRIMARY KEY CHECK(id=1), dining_manager TEXT, dining_contact TEXT, provost TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS committee (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, position TEXT, department TEXT, room TEXT, contact TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS hall_staff (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, location TEXT, work_role TEXT)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bill_scopes (id INTEGER PRIMARY KEY AUTOINCREMENT, month TEXT, sector TEXT, amount REAL)");
            // Add email column to users table if it doesn't exist (safe migration)
            try { s.executeUpdate("ALTER TABLE users ADD COLUMN email TEXT"); } catch (SQLException ignored) {}

            seedUsers(c);
            seedNotices(c);
            seedBills(c);
            seedMeals(c);
            seedStudents(c);
            seedHallInfo(c);
            seedCommittee(c);
            seedHallStaff(c);
        } catch (SQLException e) {
            throw new RuntimeException("Database setup failed", e);
        }
    }

    // ────────────────── Seed Methods (from JSON via Jackson) ──────────────────

    private static void seedUsers(Connection c) throws SQLException {
        ArrayNode users = JsonConfig.loadJsonArray(JsonConfig.seedUsersPath());
        try (PreparedStatement p = c.prepareStatement(
                "INSERT OR IGNORE INTO users(username,password,full_name,role,email) VALUES(?,?,?,?,?)")) {
            for (JsonNode u : users) {
                p.setString(1, u.get("username").asText());
                p.setString(2, u.get("password").asText());
                p.setString(3, u.get("fullName").asText());
                p.setString(4, u.get("role").asText());
                p.setString(5, u.has("email") ? u.get("email").asText() : "");
                p.executeUpdate();
            }
        }
    }

    private static void seedNotices(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM notices")) {
            if (r.next() && r.getInt(1) == 0) {
                ArrayNode notices = JsonConfig.loadJsonArray(JsonConfig.seedNoticesPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO notices(title,body,posted_by,posted_at) VALUES(?,?,?,datetime('now'))")) {
                    for (JsonNode n : notices) {
                        p.setString(1, n.get("title").asText());
                        p.setString(2, n.get("body").asText());
                        p.setString(3, n.get("postedBy").asText());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedBills(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM bills")) {
            if (r.next() && r.getInt(1) == 0) {
                ArrayNode bills = JsonConfig.loadJsonArray(JsonConfig.seedBillsPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO bills(student_id,month,rent,meals,electricity,paid) VALUES(?,?,?,?,?,?)")) {
                    for (JsonNode b : bills) {
                        p.setInt(1, b.get("studentId").asInt());
                        p.setString(2, b.get("month").asText());
                        p.setDouble(3, b.get("rent").asDouble());
                        p.setDouble(4, b.get("meals").asDouble());
                        p.setDouble(5, b.get("electricity").asDouble());
                        p.setDouble(6, b.get("paid").asDouble());
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
                ArrayNode students = JsonConfig.loadJsonArray(JsonConfig.seedStudentsPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO students(full_name,department,roll,session,room,block) VALUES(?,?,?,?,?,?)")) {
                    for (JsonNode st : students) {
                        p.setString(1, st.get("fullName").asText());
                        p.setString(2, st.get("department").asText());
                        p.setString(3, st.get("roll").asText());
                        p.setString(4, st.get("session").asText());
                        p.setString(5, st.get("room").asText());
                        p.setString(6, st.get("block").asText());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedHallInfo(Connection c) throws SQLException {
        ObjectNode hallInfo = JsonConfig.loadJsonObject(JsonConfig.hallInfoPath());
        JsonNode team = hallInfo.get("currentTeam");
        try (PreparedStatement p = c.prepareStatement(
                "INSERT OR IGNORE INTO hall_info(id,dining_manager,dining_contact,provost) VALUES(1,?,?,?)")) {
            p.setString(1, team.get("diningManager").asText());
            p.setString(2, team.get("diningContact").asText());
            p.setString(3, team.get("provost").asText());
            p.executeUpdate();
        }
    }

    private static void seedCommittee(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM committee")) {
            if (r.next() && r.getInt(1) == 0) {
                ArrayNode members = JsonConfig.loadJsonArray(JsonConfig.seedCommitteePath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO committee(name,position,department,room,contact) VALUES(?,?,?,?,?)")) {
                    for (JsonNode m : members) {
                        p.setString(1, m.get("name").asText());
                        p.setString(2, m.get("position").asText());
                        p.setString(3, m.get("department").asText());
                        p.setString(4, m.get("room").asText());
                        p.setString(5, m.get("contact").asText());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    // ────────────────── Query Methods ──────────────────

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

    public static void createBillScopesTable() {
        try (Connection c = connect(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bill_scopes (id INTEGER PRIMARY KEY AUTOINCREMENT, month TEXT, sector TEXT, amount REAL)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static String mealState(LocalDate date) {
        return mealState(date, null);
    }

    public static String mealState(LocalDate date, String username) {
        if (username != null && !username.isBlank()) {
            try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT state FROM student_meals WHERE username=? AND date=?")) {
                p.setString(1, username);
                p.setString(2, date.toString());
                try (ResultSet r = p.executeQuery()) {
                    if (r.next()) return r.getString(1);
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        // Fallback to global meals table
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("SELECT state FROM meals WHERE date=?")) {
            p.setString(1, date.toString());
            try (ResultSet r = p.executeQuery()) { if (r.next()) return r.getString(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return JsonConfig.getDefaultMealState();
    }

    public static Map<LocalDate, String> getMonthlyMealStates(YearMonth ym, String username) {
        Map<LocalDate, String> states = new HashMap<>();
        int days = ym.lengthOfMonth();
        String defaultState = JsonConfig.getDefaultMealState();
        for (int d = 1; d <= days; d++) {
            states.put(ym.atDay(d), defaultState);
        }

        String prefix = ym.toString() + "-%";
        try (Connection c = connect()) {
            // 1. Global meal states (e.g. HALL_OFF)
            try (PreparedStatement p = c.prepareStatement("SELECT date, state FROM meals WHERE date LIKE ?")) {
                p.setString(1, prefix);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        try {
                            states.put(LocalDate.parse(r.getString(1)), r.getString(2));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 2. Student-specific states (override global if set)
            if (username != null && !username.isBlank()) {
                try (PreparedStatement p = c.prepareStatement("SELECT date, state FROM student_meals WHERE username=? AND date LIKE ?")) {
                    p.setString(1, username);
                    p.setString(2, prefix);
                    try (ResultSet r = p.executeQuery()) {
                        while (r.next()) {
                            try {
                                states.put(LocalDate.parse(r.getString(1)), r.getString(2));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return states;
    }

    public static void setMealState(LocalDate date, String state) {
        setMealState(date, null, state);
    }

    public static void setMealState(LocalDate date, String username, String state) {
        if (username != null && !username.isBlank()) {
            try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                    "INSERT INTO student_meals(username,date,state) VALUES(?,?,?) ON CONFLICT(username,date) DO UPDATE SET state=excluded.state")) {
                p.setString(1, username);
                p.setString(2, date.toString());
                p.setString(3, state);
                p.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                    "INSERT INTO meals(date,state) VALUES(?,?) ON CONFLICT(date) DO UPDATE SET state=excluded.state")) {
                p.setString(1, date.toString());
                p.setString(2, state);
                p.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public static int setMealStateRange(LocalDate from, LocalDate to, String username, String state) {
        if (from.isAfter(to)) return 0;
        int count = 0;
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            String sql = (username != null && !username.isBlank())
                ? "INSERT INTO student_meals(username,date,state) VALUES(?,?,?) ON CONFLICT(username,date) DO UPDATE SET state=excluded.state"
                : "INSERT INTO meals(date,state) VALUES(?,?) ON CONFLICT(date) DO UPDATE SET state=excluded.state";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                LocalDate curr = from;
                while (!curr.isAfter(to)) {
                    if (username != null && !username.isBlank()) {
                        p.setString(1, username);
                        p.setString(2, curr.toString());
                        p.setString(3, state);
                    } else {
                        p.setString(1, curr.toString());
                        p.setString(2, state);
                    }
                    p.addBatch();
                    count++;
                    curr = curr.plusDays(1);
                }
                p.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public static void setHallOff(LocalDate date) { setMealState(date, null, "HALL_OFF"); }

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

    // ────────────────── Students (CRUD) ──────────────────

    public static List<Student> searchStudents(String term) {
        List<Student> list = new ArrayList<>();
        String q = "%" + term.toLowerCase() + "%";
        String sql = "SELECT id,full_name,department,roll,session,room,block FROM students WHERE lower(full_name) LIKE ? OR lower(department) LIKE ? OR lower(roll) LIKE ? OR lower(room) LIKE ? OR lower(block) LIKE ? ORDER BY full_name";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) p.setString(i, q);
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Student(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<Student> allStudents() {
        List<Student> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT id,full_name,department,roll,session,room,block FROM students ORDER BY full_name")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new Student(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6), r.getString(7)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void addStudent(String name, String dept, String roll, String session, String room, String block) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO students(full_name,department,roll,session,room,block) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, name); p.setString(2, dept); p.setString(3, roll);
            p.setString(4, session); p.setString(5, room); p.setString(6, block);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void updateStudent(int id, String name, String dept, String roll, String session, String room, String block) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "UPDATE students SET full_name=?,department=?,roll=?,session=?,room=?,block=? WHERE id=?")) {
            p.setString(1, name); p.setString(2, dept); p.setString(3, roll);
            p.setString(4, session); p.setString(5, room); p.setString(6, block);
            p.setInt(7, id);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void deleteStudent(int id) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM students WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ────────────────── User Account Management (Admin) ──────────────────

    /** Returns all user accounts (excluding ADMIN itself for safety). */
    public static List<UserAccount> allUserAccounts() {
        List<UserAccount> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT id,username,password,full_name,role,COALESCE(email,'') FROM users ORDER BY role,full_name")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new UserAccount(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void updateUserAccount(int id, String username, String password, String fullName, String email) {
        String sql = password.isBlank()
            ? "UPDATE users SET username=?,full_name=?,email=? WHERE id=?"
            : "UPDATE users SET username=?,password=?,full_name=?,email=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            if (password.isBlank()) {
                p.setString(1, username); p.setString(2, fullName); p.setString(3, email); p.setInt(4, id);
            } else {
                p.setString(1, username); p.setString(2, password); p.setString(3, fullName); p.setString(4, email); p.setInt(5, id);
            }
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ────────────────── Hall Staff (CRUD) ──────────────────

    private static void seedHallStaff(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM hall_staff")) {
            if (r.next() && r.getInt(1) == 0) {
                ArrayNode staff = JsonConfig.loadJsonArray(JsonConfig.seedStaffPath());
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO hall_staff(name,phone,location,work_role) VALUES(?,?,?,?)")) {
                    for (JsonNode m : staff) {
                        p.setString(1, m.get("name").asText());
                        p.setString(2, m.get("phone").asText());
                        p.setString(3, m.get("location").asText());
                        p.setString(4, m.get("workRole").asText());
                        p.executeUpdate();
                    }
                }
            }
        }
    }

    public static List<StaffMember> hallStaff() {
        List<StaffMember> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT id,name,phone,location,work_role FROM hall_staff ORDER BY id")) {
            ResultSet r = p.executeQuery();
            while (r.next()) list.add(new StaffMember(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5)));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static void addStaff(String name, String phone, String location, String workRole) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO hall_staff(name,phone,location,work_role) VALUES(?,?,?,?)")) {
            p.setString(1, name); p.setString(2, phone); p.setString(3, location); p.setString(4, workRole);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void updateStaff(int id, String name, String phone, String location, String workRole) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "UPDATE hall_staff SET name=?,phone=?,location=?,work_role=? WHERE id=?")) {
            p.setString(1, name); p.setString(2, phone); p.setString(3, location); p.setString(4, workRole); p.setInt(5, id);
            p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void deleteStaff(int id) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement("DELETE FROM hall_staff WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ────────────────── Hall Info ──────────────────

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
