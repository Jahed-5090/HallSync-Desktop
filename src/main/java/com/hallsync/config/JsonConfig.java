package com.hallsync.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.List;

/**
 * Utility class for loading JSON configuration and data files
 * from the classpath (src/main/resources).
 *
 * Uses Jackson for JSON parsing. Config objects are loaded once and cached.
 */
public class JsonConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CONFIG_BASE = "/com/hallsync/config/";
    private static final String DATA_BASE = "/com/hallsync/data/";

    // Cached config objects
    private static ObjectNode appConfig;
    private static ObjectNode navigationConfig;

    // ────────────────── Raw Loaders ──────────────────

    /**
     * Loads a JSON file from the classpath and parses it as an ObjectNode.
     */
    public static ObjectNode loadJsonObject(String resourcePath) {
        try (InputStream is = JsonConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            return (ObjectNode) MAPPER.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON: " + resourcePath, e);
        }
    }

    /**
     * Loads a JSON file from the classpath and parses it as an ArrayNode.
     */
    public static ArrayNode loadJsonArray(String resourcePath) {
        try (InputStream is = JsonConfig.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
            return (ArrayNode) MAPPER.readTree(is);
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
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON list: " + resourcePath, e);
        }
    }

    // ────────────────── App Config ──────────────────

    /** Returns the cached app-config.json as an ObjectNode. */
    public static ObjectNode getAppConfig() {
        if (appConfig == null) {
            appConfig = loadJsonObject(CONFIG_BASE + "app-config.json");
        }
        return appConfig;
    }

    public static String getAppName() {
        return getAppConfig().get("appName").asText();
    }

    public static String getAppTitle() {
        return getAppConfig().get("appTitle").asText();
    }

    public static String getDatabaseUrl() {
        return getAppConfig().get("database").get("url").asText();
    }

    public static int getLoginWidth() {
        return getAppConfig().get("window").get("login").get("width").asInt();
    }

    public static int getLoginHeight() {
        return getAppConfig().get("window").get("login").get("height").asInt();
    }

    public static int getDashboardWidth() {
        return getAppConfig().get("window").get("dashboard").get("width").asInt();
    }

    public static int getDashboardHeight() {
        return getAppConfig().get("window").get("dashboard").get("height").asInt();
    }

    public static int getSidebarWidth() {
        return getAppConfig().get("sidebar").get("width").asInt();
    }

    public static String getBrandText() {
        return getAppConfig().get("sidebar").get("brandText").asText();
    }

    public static int getMealCalendarWeeks() {
        return getAppConfig().get("meals").get("calendarWeeks").asInt();
    }

    public static int getMealLockoutHours() {
        return getAppConfig().get("meals").get("lockoutHours").asInt();
    }

    public static String getDefaultMealState() {
        return getAppConfig().get("meals").get("defaultState").asText();
    }

    public static String getCurrencyFormat() {
        return getAppConfig().get("currency").get("format").asText();
    }

    public static String getCurrencySymbol() {
        return getAppConfig().get("currency").get("symbol").asText();
    }

    public static String getCurrencyFallback() {
        return getAppConfig().get("currency").get("fallbackSymbol").asText();
    }

    // ────────────────── Navigation Config ──────────────────

    /** Returns the cached navigation.json as an ObjectNode. */
    public static ObjectNode getNavConfig() {
        if (navigationConfig == null) {
            navigationConfig = loadJsonObject(CONFIG_BASE + "navigation.json");
        }
        return navigationConfig;
    }

    /**
     * Returns the sidebar menu items for the given role.
     */
    public static String[] getMenuItems(String role) {
        JsonNode roles = getNavConfig().get("roles");
        if (roles.has(role)) {
            ArrayNode items = (ArrayNode) roles.get(role).get("menuItems");
            String[] result = new String[items.size()];
            for (int i = 0; i < items.size(); i++) result[i] = items.get(i).asText();
            return result;
        }
        // Fallback to STUDENT menu
        return getMenuItems("STUDENT");
    }

    /**
     * Returns the FXML filename for a given page name.
     */
    public static String getPageRoute(String pageName) {
        JsonNode routes = getNavConfig().get("pageRoutes");
        if (routes.has(pageName)) {
            return routes.get(pageName).asText();
        }
        return routes.get(getNavConfig().get("defaultPage").asText()).asText();
    }

    /**
     * Returns the default page name (e.g. "Home").
     */
    public static String getDefaultPage() {
        return getNavConfig().get("defaultPage").asText();
    }

    /**
     * Checks a boolean permission for a role in navigation.json.
     */
    public static boolean hasPermission(String role, String permission) {
        JsonNode roles = getNavConfig().get("roles");
        if (roles.has(role)) {
            JsonNode roleObj = roles.get(role);
            if (roleObj.has(permission)) return roleObj.get(permission).asBoolean();
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
