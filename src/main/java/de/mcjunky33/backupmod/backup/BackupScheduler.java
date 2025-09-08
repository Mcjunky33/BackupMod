package de.mcjunky33.backupmod.backup;

import de.mcjunky33.backupmod.config.BackupConfig;
import net.minecraft.server.MinecraftServer;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;

public class BackupScheduler {
    private static final String HISTORY_FILE = "backups/.temp/schedule-history.json";
    private static final String SCHEDULE_FILE = "backups/.temp/schedulesystem.yml";
    private static List<LocalDateTime> todayBackups = new ArrayList<>();
    private static LocalDate lastCheckedDay = null;

    private static boolean isDebugTimerActive = false;
    private static int debugSeconds = 0;
    private static LocalDateTime lastBackupTime = null;
    private static double lastIntervalSeconds = 36.0 * 60;

    // Erweiterung: speichere verbleibende Zeit bis zum nächsten Backup beim Shutdown
    private static long secondsRemaining = -1; // -1 = nicht gesetzt

    private static void saveLastBackupTime() {
        File file = new File(SCHEDULE_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("lastBackupTime: " + (lastBackupTime == null ? "" : lastBackupTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) + "\n");
            writer.write("isDebugTimerActive: " + isDebugTimerActive + "\n");
            writer.write("debugSeconds: " + debugSeconds + "\n");
            writer.write("lastIntervalSeconds: " + lastIntervalSeconds + "\n");
            writer.write("secondsRemaining: " + secondsRemaining + "\n");
        } catch (Exception e) {
            // Keine weitere Notification
        }
    }

    private static void loadLastBackupTime() {
        File file = new File(SCHEDULE_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            LocalDateTime lbTime = null;
            boolean debugActive = false;
            int dbgSeconds = 0;
            double liSeconds = 36.0 * 60;
            long secRem = -1;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("lastBackupTime: ")) {
                    String tLine = line.substring("lastBackupTime: ".length()).trim();
                    lbTime = (tLine.isEmpty()) ? null : LocalDateTime.parse(tLine, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                if (line.startsWith("isDebugTimerActive: ")) {
                    debugActive = "true".equals(line.substring("isDebugTimerActive: ".length()).trim());
                }
                if (line.startsWith("debugSeconds: ")) {
                    try { dbgSeconds = Integer.parseInt(line.substring("debugSeconds: ".length()).trim()); } catch (Exception ignored) {}
                }
                if (line.startsWith("lastIntervalSeconds: ")) {
                    try { liSeconds = Double.parseDouble(line.substring("lastIntervalSeconds: ".length()).trim()); } catch (Exception ignored) {}
                }
                if (line.startsWith("secondsRemaining: ")) {
                    try { secRem = Long.parseLong(line.substring("secondsRemaining: ".length()).trim()); } catch (Exception ignored) {}
                }
            }
            lastBackupTime = lbTime;
            isDebugTimerActive = debugActive;
            debugSeconds = dbgSeconds;
            lastIntervalSeconds = liSeconds;
            secondsRemaining = secRem;
        } catch (Exception e) {
            // Keine weitere Notification
        }
    }

    public static void setAutoBackupTimes(int backupsPerDay) {
        if (backupsPerDay < 1) {
            BackupConfig.autoBackupTimes = 0;
            BackupConfig.autoBackupEnabled = 0;
            BackupConfig.saveConfig();
            resetSchedule();
            return;
        }
        BackupConfig.autoBackupTimes = backupsPerDay;
        BackupConfig.autoBackupEnabled = 1;
        BackupConfig.saveConfig();
        todayBackups.clear();
        lastIntervalSeconds = 1440.0 * 60 / backupsPerDay;
        lastBackupTime = LocalDateTime.now();
        isDebugTimerActive = false;
        debugSeconds = 0;
        secondsRemaining = -1;
        saveLastBackupTime();
    }

    public static void setDebugTimer(int hours, int minutes, int seconds, MinecraftServer server) {
        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        isDebugTimerActive = true;
        debugSeconds = totalSeconds;
        lastBackupTime = LocalDateTime.now();
        secondsRemaining = -1;
        saveLastBackupTime();
        todayBackups.clear();
        // Timer-Meldung handled von Command!
    }

    public static String getNextScheduledBackupTimerString() {
        loadLastBackupTime();
        LocalDateTime now = LocalDateTime.now();
        if (lastBackupTime == null) {
            lastBackupTime = now;
            saveLastBackupTime();
        }
        Duration sinceLast = Duration.between(lastBackupTime, now);
        long secondsPassed = sinceLast.getSeconds();

        if (isDebugTimerActive) {
            long secondsRem = debugSeconds - secondsPassed;
            if (secondsRem <= 0) {
                return "Debug timer finished! Backup will be executed automatically.";
            } else {
                long hours = secondsRem / 3600;
                long minutes = (secondsRem % 3600) / 60;
                long seconds = secondsRem % 60;
                return String.format("%02dh %02dmin %02dsec", hours, minutes, seconds);
            }
        } else if (secondsRemaining > 0) {
            // Fortsetzung nach Server-Start mit Restzeit
            long hours = secondsRemaining / 3600;
            long minutes = (secondsRemaining % 3600) / 60;
            long seconds = secondsRemaining % 60;
            return String.format("%02dh %02dmin %02dsec", hours, minutes, seconds);
        } else {
            int timesPerDay = Math.max(BackupConfig.autoBackupTimes, 1);
            double intervalSeconds = 1440.0 * 60 / timesPerDay;
            long secondsRem = (long)intervalSeconds - secondsPassed;
            if (secondsRem < 0) secondsRem = 0;
            long hours = secondsRem / 3600;
            long minutes = (secondsRem % 3600) / 60;
            long seconds = secondsRem % 60;
            return String.format("%02dh %02dmin %02dsec", hours, minutes, seconds);
        }
    }

    // Erweiterung: beim Shutdown aufrufen!
    public static void saveRemainingTimeOnShutdown() {
        loadLastBackupTime();
        LocalDateTime now = LocalDateTime.now();
        double intervalSeconds = lastIntervalSeconds;
        long secondsPassed = 0;
        if (lastBackupTime != null) {
            secondsPassed = Duration.between(lastBackupTime, now).getSeconds();
        }
        long remaining = (long)intervalSeconds - secondsPassed;
        if (remaining < 0) remaining = 0;
        secondsRemaining = remaining;
        saveLastBackupTime();
    }

    public static void checkAndRunBackup(MinecraftServer server) {
        loadLastBackupTime();
        LocalDateTime now = LocalDateTime.now();
        int timesPerDay = Math.max(BackupConfig.autoBackupTimes, 1);
        double intervalSeconds = 1440.0 * 60 / timesPerDay;

        if (BackupConfig.autoBackupEnabled != 1 || timesPerDay < 1) {
            return;
        }

        if (lastBackupTime == null) {
            lastIntervalSeconds = intervalSeconds;
            lastBackupTime = now;
            saveLastBackupTime();
        }

        Duration sinceLast = Duration.between(lastBackupTime, now);
        long secondsPassed = sinceLast.getSeconds();

        boolean doBackup = false;

        if (isDebugTimerActive) {
            if (secondsPassed >= debugSeconds) {
                doBackup = true;
            }
        } else if (secondsRemaining > 0) {
            // Server wurde neu gestartet und es gab Restzeit
            secondsRemaining -= 1; // Tick = 1 Sekunde (wenn Tick)
            if (secondsRemaining <= 0) {
                doBackup = true;
                secondsRemaining = -1; // zurücksetzen
            } else {
                saveLastBackupTime();
            }
        } else {
            if (secondsPassed >= (long)intervalSeconds) {
                doBackup = true;
            }
        }

        if (doBackup) {
            boolean success = runScheduledBackup(server);
            if (success) {
                lastBackupTime = now;
                todayBackups.add(now);

                if (isDebugTimerActive) {
                    isDebugTimerActive = false;
                    debugSeconds = 0;
                    lastIntervalSeconds = intervalSeconds;
                }
                secondsRemaining = -1;
                saveLastBackupTime();
            }
        }
    }

    public static boolean runScheduledBackup(MinecraftServer server) {
        String cmd = "backup create autobackup";
        try {
            server.getCommands().getDispatcher().execute(
                    cmd,
                    server.createCommandSourceStack().withPermission(4)
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void resetSchedule() {
        todayBackups.clear();
        lastCheckedDay = LocalDate.now();
        File file = new File(HISTORY_FILE);
        File file2 = new File(SCHEDULE_FILE);
        if (file.exists()) file.delete();
        if (file2.exists()) file2.delete();
        lastBackupTime = null;
        isDebugTimerActive = false;
        debugSeconds = 0;
        lastIntervalSeconds = 36.0 * 60;
        secondsRemaining = -1;
    }

    public static boolean isDebugTimerActive() {
        return isDebugTimerActive;
    }
}