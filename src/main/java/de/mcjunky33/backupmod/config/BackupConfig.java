package de.mcjunky33.backupmod.config;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class BackupConfig {
    public static String lang = "en_us"; // Default ist jetzt en_us!
    public static List<String> excludePaths = new ArrayList<>();
    public static int maxBackups = Integer.MAX_VALUE;
    public static int autoBackupTimes = 0;
    public static int autoBackupEnabled = 0;

    public static boolean normallogEnabled = true;
    public static boolean permlogEnabled = true;
    public static List<PermUser> permissionUsers = new ArrayList<>();

    public static final String BACKUP_DIR = "backups";
    public static final String CONFIG_FILE = BACKUP_DIR + File.separator + "backup-config.json";

    public static class PermUser {
        public String uuid;
        public String name;
        public PermUser(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public static void load() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("Could not create backup folder!");
                return;
            }
        }
        File f = new File(CONFIG_FILE);
        if (!f.exists() || f.length() == 0) {
            saveDefaults();
            return;
        }
        try (Reader reader = new FileReader(f)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            lang = json.has("lang") ? json.get("lang").getAsString() : "en_us";
            excludePaths = new ArrayList<>();
            if (json.has("excludePaths") && json.get("excludePaths").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("excludePaths")) {
                    excludePaths.add(el.getAsString());
                }
            }
            maxBackups = json.has("maxBackups") ? json.get("maxBackups").getAsInt() : Integer.MAX_VALUE;
            autoBackupTimes = json.has("autoBackupTimes") ? json.get("autoBackupTimes").getAsInt() : 0;
            autoBackupEnabled = json.has("autoBackupEnabled") ? json.get("autoBackupEnabled").getAsInt() : 0;
            normallogEnabled = json.has("normallogEnabled") ? json.get("normallogEnabled").getAsBoolean() : true;
            permlogEnabled = json.has("permlogEnabled") ? json.get("permlogEnabled").getAsBoolean() : true;
            permissionUsers = new ArrayList<>();
            if (json.has("permissionUsers") && json.get("permissionUsers").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("permissionUsers")) {
                    JsonObject obj = el.getAsJsonObject();
                    String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
                    String name = obj.has("name") ? obj.get("name").getAsString() : "";
                    if (!uuid.isEmpty() && !name.isEmpty()) {
                        permissionUsers.add(new PermUser(uuid, name));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("BackupConfig error loading: " + e.getMessage());
            saveDefaults();
        }
    }

    public static void saveDefaults() {
        lang = "en_us";
        excludePaths = new ArrayList<>();
        maxBackups = Integer.MAX_VALUE;
        autoBackupTimes = 0;
        autoBackupEnabled = 0;
        normallogEnabled = true;
        permlogEnabled = true;
        permissionUsers = new ArrayList<>();
        saveConfig();
    }

    public static void saveConfig() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println(" could not create backup folder!");
                return;
            }
        }
        File f = new File(CONFIG_FILE);
        try (Writer writer = new FileWriter(f)) {
            JsonObject json = new JsonObject();

            json.addProperty("lang", lang);

            JsonArray excludes = new JsonArray();
            for (String s : excludePaths != null ? excludePaths : Collections.<String>emptyList()) {
                excludes.add(new JsonPrimitive(s));
            }
            json.add("excludePaths", excludes);

            json.addProperty("maxBackups", maxBackups);
            json.addProperty("autoBackupTimes", autoBackupTimes);
            json.addProperty("autoBackupEnabled", autoBackupEnabled);
            json.addProperty("normallogEnabled", normallogEnabled);
            json.addProperty("permlogEnabled", permlogEnabled);

            JsonArray perms = new JsonArray();
            for (PermUser user : permissionUsers) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", user.uuid);
                obj.addProperty("name", user.name);
                perms.add(obj);
            }
            json.add("permissionUsers", perms);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(json));
        } catch (Exception e) {
            System.err.println("BackupConfig failed to save: " + e.getMessage());
        }
    }

    public static boolean hasBackupPerm(String uuid, String name) {
        if (uuid == null || name == null) return false;
        for (PermUser u : permissionUsers) {
            if (u.uuid.equals(uuid) || u.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static void addPermUser(String uuid, String name) {
        if (!hasBackupPerm(uuid, name)) {
            permissionUsers.add(new PermUser(uuid, name));
            saveConfig();
        }
    }

    public static void removePermUser(String uuid, String name) {
        permissionUsers.removeIf(u -> u.uuid.equals(uuid) || u.name.equalsIgnoreCase(name));
        saveConfig();
    }
}