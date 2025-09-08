package de.mcjunky33.backupmod.backup;

import de.mcjunky33.backupmod.lang.LangManager;
import de.mcjunky33.backupmod.BackupMod;
import de.mcjunky33.backupmod.config.BackupConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.function.Consumer;

public class RestoreManager {

    private static final String CONFIG_FILE_PATH = BackupConfig.CONFIG_FILE.replace("\\", "/");
    private static volatile boolean backupRunning = false;

    public static void setBackupRunning(boolean running) {
        backupRunning = running;
    }
    public static boolean isBackupRunning() {
        return backupRunning;
    }

    public static boolean restoreBackup(String backupFileName, MinecraftServer server) {
        // Anfang: Prüfen, ob Datei vorhanden
        File backupFile = new File("backups/" + backupFileName);

        if (!backupFile.exists()) {
            // Log-Ausgabe für berechtigte Spieler/OP
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean isOp = player.hasPermissions(2);
                boolean isPerm = BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
                if ((isOp || isPerm) && BackupConfig.permlogEnabled) {
                    String msg = LangManager.tr("backup.restore.noexist", backupFileName);
                    player.sendSystemMessage(Component.literal(msg));
                }
            }
            return false;
        }

        // Rest wie gehabt
        logToPlayers(server, LangManager.tr("backup.restore_requested", backupFileName), false, 0);

        String timestamp = backupFileName.replace(".zip", "");
        String logPath = BackupMod.BACKUP_DIR + File.separator + "logs" + File.separator + timestamp + ".log";
        BufferedWriter logWriter;
        try {
            logWriter = new BufferedWriter(new FileWriter(logPath, true));
        } catch (IOException e) {
            logWriter = null;
        }
        final BufferedWriter finalLogWriter = logWriter;

        Consumer<String> logRestore = (msg) -> {
            String out = "[RestoreManager] " + msg;
            if (finalLogWriter != null) {
                try {
                    finalLogWriter.write(out + "\n");
                    finalLogWriter.flush();
                } catch (IOException ignore) {}
            }
            logFile(out);
        };

        Set<String> dirsToDelete = new HashSet<>();
        Set<String> filesToDelete = new HashSet<>();

        boolean wasAutoSaveEnabled = true; // Dummy, you may implement server status query if needed

        try {
            logRestore.accept("Turning autosave OFF for restore.");
            setAutosave(server, false);

            // 1. Gather all files/folders from ZIP to delete
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName().replace("\\", "/");
                    if (entryName.equals(CONFIG_FILE_PATH)) {
                        logRestore.accept("Skipped config file from deletion/extraction: " + entryName);
                        zis.closeEntry();
                        continue;
                    }
                    if (entry.isDirectory()) {
                        dirsToDelete.add(entryName.split("/")[0]);
                    } else {
                        filesToDelete.add(entryName);
                        String topFolder = entryName.contains("/") ? entryName.split("/")[0] : null;
                        if (topFolder != null) dirsToDelete.add(topFolder);
                    }
                    zis.closeEntry();
                }
            }

            // 2. Lösche nur, was im ZIP ist!
            for (String dirName : dirsToDelete) {
                File dir = new File(dirName);
                if (dir.exists()) {
                    logRestore.accept("Deleting folder before restore: " + dir.getAbsolutePath());
                    deleteDirectoryAndLog(dir, logRestore);
                }
            }
            for (String fileName : filesToDelete) {
                if (fileName.equals(CONFIG_FILE_PATH)) continue;
                File file = new File(fileName);
                if (file.exists()) {
                    logRestore.accept("Deleting file before restore: " + file.getAbsolutePath());
                    file.delete();
                }
            }

            // 3. Extract ZIP
            int extracted = 0;
            int total = filesToDelete.size();
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName().replace("\\", "/");
                    if (entryName.equals(CONFIG_FILE_PATH)) {
                        zis.closeEntry();
                        continue;
                    }
                    if (entry.isDirectory()) {
                        File outFile = new File(entryName);
                        outFile.mkdirs();
                        logRestore.accept("Extracting folder: " + entryName);
                    } else {
                        File outFile = new File(entryName);
                        File parent = outFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        extracted++;
                        int percent = total > 0 ? (int)((extracted * 100.0f) / total) : 100;
                        percent = Math.min(percent, 100);
                        logToPlayers(server, null, true, percent);
                        logRestore.accept("Extracted file: " + entryName);
                        try { Thread.sleep(10); } catch (Exception ignored) {}
                    }
                    zis.closeEntry();
                }
            }

            logToPlayers(server, LangManager.tr("backup.restored", backupFileName), false, 100);
            logRestore.accept(LangManager.tr("backup.restored", backupFileName));

            File startSh = new File("start.sh");
            File startBat = new File("start.bat");
            setAutosave(server, wasAutoSaveEnabled);
            logRestore.accept("Autosave restored to previous value.");

            // Server-Neustart NUR wenn Restore erfolgreich!
            if (startSh.exists() && startSh.isFile()) {
                logRestore.accept("Restore finished. Server will now shut down and start.sh will be executed!");
                if (finalLogWriter != null) finalLogWriter.close();
                try {
                    Runtime.getRuntime().exec("sh start.sh");
                } catch (Exception e) {
                    logRestore.accept("Could not execute start.sh: " + e.getMessage());
                }
                // KEIN System.exit bei Fehler!
            } else if (startBat.exists() && startBat.isFile()) {
                logRestore.accept("Restore finished. Server will now shut down and start.bat will be executed!");
                if (finalLogWriter != null) finalLogWriter.close();
                try {
                    Runtime.getRuntime().exec("cmd /c start.bat");
                } catch (Exception e) {
                    logRestore.accept("Could not execute start.bat: " + e.getMessage());
                }
                // KEIN System.exit bei Fehler!
            } else {
                logRestore.accept("Start Script Linux start.sh or Windows start.bat were not found. Server is shutting down without restart. Please restart the server manually.");
                if (finalLogWriter != null) finalLogWriter.close();
                // KEIN System.exit bei Fehler!
            }

            return true;
        } catch (Exception e) {
            logToPlayers(server, LangManager.tr("backup.restore_failed", e.getMessage()), true, 0);
            logRestore.accept("Restore failed: " + e.getMessage());
            if (finalLogWriter != null) {
                try { finalLogWriter.close(); } catch (IOException ignore) {}
            }
            // KEIN System.exit bei Fehler!
            return false;
        }
    }

    private static void deleteDirectoryAndLog(File dir, Consumer<String> logRestore) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryAndLog(file, logRestore);
                } else {
                    logRestore.accept("Deleting file: " + file.getAbsolutePath());
                    file.delete();
                }
            }
        }
        logRestore.accept("Deleting folder: " + dir.getAbsolutePath());
        dir.delete();
    }

    private static void logFile(String msg) {
        System.out.println(msg);
        BackupMod.LOGGER.info(msg);
    }

    private static void setAutosave(MinecraftServer server, boolean enable) {
        if (enable) {
            runServerCommand(server, "save-on");
        } else {
            runServerCommand(server, "save-off");
        }
    }

    private static void runServerCommand(MinecraftServer server, String cmd) {
        try {
            server.getCommands().getDispatcher().execute(cmd, server.createCommandSourceStack());
        } catch (Exception e) {}
    }

    public static void logToPlayers(MinecraftServer server, String msg, boolean isDetail, int percent) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean isOp = player.hasPermissions(2);
            boolean isPerm = BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
            if ((isOp || isPerm) && BackupConfig.permlogEnabled) {
                if (msg != null) {
                    player.sendSystemMessage(Component.literal(LangManager.tr("backup.prefix") + msg));
                }
                if (percent > 0) {
                    player.sendSystemMessage(Component.literal(LangManager.tr("backup.prefix") + LangManager.tr("backup.progress", percent)));
                }
            } else if (BackupConfig.normallogEnabled) {
                if (percent > 0) {
                    player.sendSystemMessage(Component.literal(LangManager.tr("backup.prefix") + LangManager.tr("backup.progress", percent)));
                } else if (msg != null && !isDetail) {
                    player.sendSystemMessage(Component.literal(LangManager.tr("backup.prefix") + msg));
                }
            }
        }
    }
}