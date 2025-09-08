package de.mcjunky33.backupmod.backup;

import de.mcjunky33.backupmod.config.BackupConfig;
import de.mcjunky33.backupmod.BackupMod;
import de.mcjunky33.backupmod.lang.LangManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final String BACKUP_DIR = BackupMod.BACKUP_DIR;
    private static final String LOG_DIR = BACKUP_DIR + File.separator + "logs";
    private static final String CONFIG_FILE_PATH = BackupConfig.CONFIG_FILE.replace("\\", "/");
    private static final List<String> DEFAULT_EXCLUDE = Arrays.asList("session.lock", CONFIG_FILE_PATH);

    private static BufferedWriter logWriter = null;

    private static void openLogFile(String backupFileName) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) logDir.mkdirs();
            String logFileName = backupFileName.replace(".zip", "") + ".log";
            File logFile = new File(LOG_DIR, logFileName);
            logWriter = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            System.out.println("[BackupMod] Could not open log file for backup: " + backupFileName);
            logWriter = null;
        }
    }

    private static void writeLog(String msg) {
        try {
            if (logWriter != null) {
                logWriter.write(msg);
                logWriter.newLine();
                logWriter.flush();
            }
        } catch (IOException ignored) {}
    }

    private static void closeLogFile() {
        if (logWriter != null) {
            try {
                logWriter.flush();
                logWriter.close();
            } catch (IOException ignored) {}
            logWriter = null;
        }
    }

    public static void createBackupAsync(MinecraftServer server, String customName) {
        new Thread(() -> createBackup(server, customName), "BackupMod-BackupThread").start();
    }

    public static String createAutoBackupWithTimestamp(MinecraftServer server, String plannedTimestamp) {
        String backupFileName = "autobackup-" + plannedTimestamp + ".zip";
        createBackupAsync(server, backupFileName);
        return backupFileName;
    }

    public static String createBackup(MinecraftServer server) {
        return createBackup(server, null);
    }

    public static String createBackup(MinecraftServer server, String customName) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String backupFileName;
        if (customName != null && !customName.isEmpty()) {
            backupFileName = customName + "-" + timestamp + ".zip";
        } else {
            backupFileName = "backup-" + timestamp + ".zip";
        }
        return createBackupInternal(server, backupFileName);
    }

    private static String createBackupInternal(MinecraftServer server, String backupFileName) {
        String zipFilePath = BACKUP_DIR + File.separator + backupFileName;

        openLogFile(backupFileName);

        broadcastAllPlayers(server, LangManager.tr("backup.started"), ChatFormatting.GREEN);
        writeLog("[BackupMod] Backup started.");

        Set<String> excludeList = new HashSet<>(DEFAULT_EXCLUDE);
        if (BackupConfig.excludePaths != null) {
            for (String path : BackupConfig.excludePaths) {
                String cleanPath = path.replace("\\", "/");
                excludeList.add(cleanPath);
            }
        }
        excludeList.add(CONFIG_FILE_PATH);

        Thread backupThread = new Thread(() -> {
            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                List<Path> filesToBackup = new ArrayList<>();
                Files.walkFileTree(Paths.get("."), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName().toString();
                        String dirPath = dir.toString().replace("\\", "/");
                        if (name.equals("backups") || name.equals(".temp")) return FileVisitResult.SKIP_SUBTREE;
                        if (excludeList.contains(name) || excludeList.contains(dirPath)) return FileVisitResult.SKIP_SUBTREE;
                        if (!dir.equals(Paths.get("."))) {
                            String zipDirName = dir.toString().replace("\\", "/");
                            if (zipDirName.startsWith("./")) zipDirName = zipDirName.substring(2);
                            ZipEntry zipEntry = new ZipEntry(zipDirName + "/");
                            try {
                                zos.putNextEntry(zipEntry);
                                zos.closeEntry();
                            } catch (IOException ignored) {}
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String name = file.getFileName().toString();
                        String filePath = file.toString().replace("\\", "/");
                        if (excludeList.contains(name) || excludeList.contains(filePath)) return FileVisitResult.CONTINUE;
                        if (!file.toFile().isFile()) return FileVisitResult.CONTINUE;
                        filesToBackup.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                });

                int totalFiles = filesToBackup.size();
                if (totalFiles == 0) {
                    broadcastAllPlayers(server, LangManager.tr("backup.failed", "No files to backup"), ChatFormatting.RED);
                    writeLog("[BackupMod] Backup failed: No files to backup.");
                    closeLogFile();
                    return;
                }

                File tempDir = new File(".temp");
                if (!tempDir.exists()) tempDir.mkdirs();

                int copied = 0;
                for (Path file : filesToBackup) {
                    copied++;
                    File f = file.toFile();
                    boolean copiedToZip = false;

                    String zipEntryName = file.toString().replace("\\", "/");
                    if (zipEntryName.startsWith("./")) zipEntryName = zipEntryName.substring(2);

                    try (FileInputStream fis = new FileInputStream(f)) {
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zos.putNextEntry(zipEntry);
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                        copiedToZip = true;
                    } catch (Exception e) {
                        writeLog("Direct copy failed for " + file.toAbsolutePath() + ": " + e.getMessage());
                    }

                    if (!copiedToZip) {
                        try {
                            Path rel = Paths.get(".").toAbsolutePath().relativize(file.toAbsolutePath());
                            File tempFile = new File(tempDir, rel.toString());
                            File parent = tempFile.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();
                            Files.copy(f.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            try (FileInputStream fis = new FileInputStream(tempFile)) {
                                ZipEntry zipEntry = new ZipEntry(zipEntryName);
                                zos.putNextEntry(zipEntry);
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                                zos.closeEntry();
                            }
                            deleteFileAndEmptyParents(tempFile, tempDir);
                            copiedToZip = true;
                        } catch (Exception e2) {
                            writeLog("Temp copy failed for " + file.toAbsolutePath() + ": " + e2.getMessage());
                        }
                    }

                    int percent = (int) ((copied / (double) totalFiles) * 100);
                    String percentMsg = percent + "%";
                    broadcastAllPlayers(server, percentMsg, ChatFormatting.YELLOW);
                    writeLog(percentMsg);

                    String progressFileMsg = "[BackupMod] Progress: " + copied + "/" + totalFiles + " - " + file.toString();
                    writeLog(progressFileMsg);
                    System.out.println(progressFileMsg);

                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        boolean isPerm = BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
                        boolean isOp = player.hasPermissions(2);
                        if ((isOp || isPerm) && BackupConfig.permlogEnabled) {
                            player.sendSystemMessage(Component.literal(progressFileMsg).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }

                broadcastAllPlayers(server, LangManager.tr("backup.completed_no_file"), ChatFormatting.GREEN);
                writeLog("[BackupMod] Backup completed.");

                String savedMsg = "[BackupMod] saved file as " + backupFileName;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    boolean isPerm = BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
                    boolean isOp = player.hasPermissions(2);
                    if ((isOp || isPerm) && BackupConfig.permlogEnabled) {
                        player.sendSystemMessage(Component.literal(savedMsg).withStyle(ChatFormatting.GRAY));
                    }
                }
                writeLog(savedMsg);
                System.out.println(savedMsg);

            } catch (Exception e) {
                broadcastAllPlayers(server, LangManager.tr("backup.failed", e.getMessage()), ChatFormatting.RED);
                writeLog("[BackupMod] Backup failed: " + e.getMessage());
            }
            closeLogFile();

        }, "BackupMod-ZipThread");

        backupThread.setPriority(Thread.MIN_PRIORITY);
        backupThread.start();

        return backupFileName;
    }

    private static void deleteFileAndEmptyParents(File file, File rootDir) {
        if (file.exists()) file.delete();
        File parent = file.getParentFile();
        while (parent != null && !parent.equals(rootDir) && parent.list().length == 0) {
            parent.delete();
            parent = parent.getParentFile();
        }
    }

    // *** GEÄNDERT: normallogEnabled berücksichtigt! ***
    private static void broadcastAllPlayers(MinecraftServer server, String msg, ChatFormatting color) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean isOp = player.hasPermissions(2);
            boolean isPerm = BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
            if (BackupConfig.normallogEnabled || ((isOp || isPerm) && BackupConfig.permlogEnabled)) {
                player.sendSystemMessage(Component.literal(msg).withStyle(color));
            }
        }
    }

    public static List<String> listBackups() {
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".zip"));
        List<String> names = new ArrayList<>();
        if (backups != null) {
            for (File f : backups) {
                names.add(f.getName());
            }
        }
        return names;
    }

    public static String getLatestBackup() {
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (backups == null || backups.length == 0) return null;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        return backups[0].getName();
    }

    public static void deleteOldBackupsIfNeeded() {
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (backups == null) return;
        if (backups.length > BackupConfig.maxBackups) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            int toDelete = backups.length - BackupConfig.maxBackups;
            for (int i = 0; i < toDelete; i++) {
                backups[i].delete();
            }
        }
    }

    public static void runServerCommand(MinecraftServer server, String cmd) {
        try {
            server.getCommands().getDispatcher().execute(cmd, server.createCommandSourceStack());
        } catch (Exception e) {}
    }
}