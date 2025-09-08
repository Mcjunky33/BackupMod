package de.mcjunky33.backupmod.lang;

import com.google.gson.Gson;
import java.util.Map;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class LangManager {
    private static Map<String, String> translations = new HashMap<>();
    private static Map<String, String> fallbackTranslations = new HashMap<>();

    public static void load() {
        try {
            InputStream fallbackStream = LangManager.class.getResourceAsStream("/assets/backupmod/lang/en_us.json");
            if (fallbackStream != null) {
                InputStreamReader fallbackReader = new InputStreamReader(fallbackStream);
                fallbackTranslations = new Gson().fromJson(fallbackReader, Map.class);
                fallbackReader.close();
            } else {
                fallbackTranslations = new HashMap<>();
            }

            String lang = de.mcjunky33.backupmod.config.BackupConfig.lang;
            String file = "/assets/backupmod/lang/" + lang + ".json";
            InputStream stream = LangManager.class.getResourceAsStream(file);
            if (stream != null) {
                InputStreamReader reader = new InputStreamReader(stream);
                translations = new Gson().fromJson(reader, Map.class);
                reader.close();
            } else {
                translations = new HashMap<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            translations = new HashMap<>();
            fallbackTranslations = new HashMap<>();
        }
    }

    public static String tr(String key, Object... args) {
        String s = translations.getOrDefault(key, fallbackTranslations.getOrDefault(key, key));
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", args[i].toString());
        }
        return s;
    }
}