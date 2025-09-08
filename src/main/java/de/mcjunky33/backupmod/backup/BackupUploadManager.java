package de.mcjunky33.backupmod.backup;

import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupUploadManager {

    public static String uploadBackupFromUrl(String urlStr, String customName) throws IOException {
        String normalizedUrl = normalizeUrl(urlStr);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String fileNameBase = (customName != null && !customName.isEmpty()) ? customName : "backup";
        String backupFileName = fileNameBase + "-" + timestamp + ".zip";
        File backupFile = new File("backups/" + backupFileName);

        try (BufferedInputStream in = new BufferedInputStream(new URL(normalizedUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(backupFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        return backupFileName;
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.matches("^(https?://).*")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    public static String ensureBackupNameConformity(File file) {
        String name = file.getName();
        if (isStandardZip(name)) return name;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(file.lastModified()));
        String base = getBaseName(name);
        String newName = base + "-" + timestamp + ".zip";
        File renamed = new File(file.getParent(), newName);
        if (file.renameTo(renamed)) {
            return renamed.getName();
        }
        return name;
    }

    private static String getBaseName(String name) {
        int idx = name.indexOf('-');
        if (idx > 0) return name.substring(0, idx);
        if (name.endsWith(".zip")) return name.substring(0, name.length() - 4);
        return name;
    }

    private static boolean isStandardZip(String name) {
        return name.matches("^[^-]+-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.zip$");
    }
}