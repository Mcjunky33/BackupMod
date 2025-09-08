package de.mcjunky33.backupmod.backup;

import java.io.File;

public class BackupRenameManager {
    // Rename only the first part of the filename before first dash
    public static String renameBackupZip(File file, String newName) {
        String origName = file.getName();
        int dashIdx = origName.indexOf('-');
        if (dashIdx < 0) return origName;
        String suffix = origName.substring(dashIdx);
        String newFileName = newName + suffix;
        File newFile = new File(file.getParent(), newFileName);

        if (file.renameTo(newFile)) {
            return newFileName;
        }
        return origName;
    }

    // Only allow rename for standard and uploaded files
    public static boolean isRenameAllowed(File file) {
        String name = file.getName();
        return name.matches("^(-|[^-]+)-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.zip$")
                || name.matches("^[^-]+-uploaded-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.zip$");
    }
}