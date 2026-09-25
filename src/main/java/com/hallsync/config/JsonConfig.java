package com.hallsync.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Utility class for loading JSON configuration and data files
 * from the classpath (src/main/resources).
 *
 * All JSON files are loaded once and cached in memory.
 */
public class JsonConfig {

    private static final Gson GSON = new Gson();
    private static final String CONFIG_BASE = "/com/hallsync/config/";
    private static final String DATA_BASE = "/com/hallsync/data/";

    // Cached config objects
    private static JsonObject appConfig;
    private static JsonObject navigationConfig;

    // ────────────────── Raw Loaders ──────────────────

    /**
     * Loads a JSON file from the classpath and parses it as a JsonObject.
     */
    public static JsonObject loadJsonObject(String resourcePath) {
        try (InputStream is = JsonConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON: " + resourcePath, e);
        }
    }

    /**
     * Loads a JSON file from the classpath and parses it as a JsonArray.
     */
    public static JsonArray loadJsonArray(String resourcePath) {
        try (InputStream is = JsonConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonArray.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON: " + resourcePath, e);
        }
    }

    /**
     * Loads a JSON array file and deserializes it into a List of the given type.
     */
    public static <T> List<T> loadList(String resourcePath, Class<T> elementType) {
        try (InputStream is = JsonConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type listType = TypeToken.getParameterized(List.class, elementType).getType();
                return GSON.fromJson(reader, listType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON list: " + resourcePath, e);
        }
    }

    // ────────────────── App Config ──────────────────

    /** Returns the cached app-config.json as a JsonObject. */
    public static JsonObject getAppConfig() {
        if (appConfig == null) {
            appConfig = loadJsonObject(CONFIG_BASE + "app-config.json");
        }
        return appConfig;
    }

    public static String getAppName() {
        return getAppConfig().get("appName").getAsString();
    }

    public static String getAppTitle() {
        return getAppConfig().get("appTitle").getAsString();
    }

    public static String getDatabaseUrl() {
        return getAppConfig().getAsJsonObject("database").get("url").getAsString();
    }

    public static int getLoginWidth() {
        return getAppConfig().getAsJsonObject("window").getAsJsonObject("login").get("width").getAsInt();
    }

    public static int getLoginHeight() {
        return getAppConfig().getAsJsonObject("window").getAsJsonObject("login").get("height").getAsInt();
    }

    public static int getDashboardWidth() {
        return getAppConfig().getAsJsonObject("window").getAsJsonObject("dashboard").get("width").getAsInt();
    }

    public static int getDashboardHeight() {
        return getAppConfig().getAsJsonObject("window").getAsJsonObject("dashboard").get("height").getAsInt();
    }

    public static int getSidebarWidth() {
        return getAppConfig().getAsJsonObject("sidebar").get("width").getAsInt();
    }

    public static String getBrandText() {
        return getAppConfig().getAsJsonObject("sidebar").get("brandText").getAsString();
    }

    public static int getMealCalendarWeeks() {
        return getAppConfig().getAsJsonObject("meals").get("calendarWeeks").getAsInt();
    }

    public static int getMealLockoutHours() {
        return getAppConfig().getAsJsonObject("meals").get("lockoutHours").getAsInt();
    }

    public static String getDefaultMealState() {
        return getAppConfig().getAsJsonObject("meals").get("defaultState").getAsString();
    }

    public static String getCurrencyFormat() {
        return getAppConfig().getAsJsonObject("currency").get("format").getAsString();
    }

    public static String getCurrencySymbol() {
        return getAppConfig().getAsJsonObject("currency").get("symbol").getAsString();
    }

    public static String getCurrencyFallback() {
        return getAppConfig().getAsJsonObject("currency").get("fallbackSymbol").getAsString();
    }

    // ────────────────── Navigation Config ──────────────────

    /** Returns the cached navigation.json as a JsonObject. */
    public static JsonObject getNavConfig() {
        if (navigationConfig == null) {
            navigationConfig = loadJsonObject(CONFIG_BASE + "navigation.json");
        }
        return navigationConfig;
    }

    /**
     * Returns the sidebar menu items for the given role.
     */
    public static String[] getMenuItems(String role) {
        JsonObject roles = getNavConfig().getAsJsonObject("roles");
        if (roles.has(role)) {
            JsonArray items = roles.getAsJsonObject(role).getAsJsonArray("menuItems");
            String[] result = new String[items.size()];
            for (int i = 0; i < items.size(); i++) result[i] = items.get(i).getAsString();
            return result;
        }
        // Fallback to STUDENT menu
        return getMenuItems("STUDENT");
    }

    /**
     * Returns the FXML filename for a given page name.
     */
    public static String getPageRoute(String pageName) {
        JsonObject routes = getNavConfig().getAsJsonObject("pageRoutes");
        if (routes.has(pageName)) {
            return routes.get(pageName).getAsString();
        }
        return routes.get(getNavConfig().get("defaultPage").getAsString()).getAsString();
    }

    /**
     * Returns the default page name (e.g. "Home").
     */
    public static String getDefaultPage() {
        return getNavConfig().get("defaultPage").getAsString();
    }

    /**
     * Checks a boolean permission for a role in navigation.json.
     */
    public static boolean hasPermission(String role, String permission) {
        JsonObject roles = getNavConfig().getAsJsonObject("roles");
        if (roles.has(role)) {
            JsonObject roleObj = roles.getAsJsonObject(role);
            if (roleObj.has(permission)) return roleObj.get(permission).getAsBoolean();
        }
        return false;
    }

    // ────────────────── Seed Data Paths ──────────────────

    public static String seedUsersPath()     { return DATA_BASE + "seed-users.json"; }
    public static String seedNoticesPath()   { return DATA_BASE + "seed-notices.json"; }
    public static String seedBillsPath()     { return DATA_BASE + "seed-bills.json"; }
    public static String seedStudentsPath()  { return DATA_BASE + "seed-students.json"; }
    public static String seedCommitteePath() { return DATA_BASE + "seed-committee.json"; }
    public static String seedStaffPath()     { return DATA_BASE + "seed-staff.json"; }
    public static String hallInfoPath()      { return DATA_BASE + "hall-info.json"; }
}
