package de.mcjunky33.backupmod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import de.mcjunky33.backupmod.lang.LangManager;
import de.mcjunky33.backupmod.backup.BackupManager;
import de.mcjunky33.backupmod.backup.BackupScheduler;
import de.mcjunky33.backupmod.backup.RestoreManager;
import de.mcjunky33.backupmod.config.BackupConfig;
import de.mcjunky33.backupmod.backup.BackupUploadManager;
import de.mcjunky33.backupmod.backup.BackupRenameManager;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class BackupCommand {
    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_AUTO = (ctx, builder) -> {
        builder.suggest("Enter a number between 1 and 40");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_MAX = (ctx, builder) -> {
        builder.suggest("Enter a number between 1 and 20");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_ROOT_FILES = (ctx, builder) -> {
        for (String entry : getRootEntries(".")) {
            builder.suggest(entry);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_EXCLUDES = (ctx, builder) -> {
        for (String entry : BackupConfig.excludePaths) {
            if (entry.startsWith("/")) {
                builder.suggest(entry.substring(1));
            } else {
                builder.suggest(entry);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_ZIPS = (ctx, builder) -> {
        for (String zip : getBackupZipFiles()) {
            builder.suggest(zip);
        }
        return builder.buildFuture();
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("backup")
                            .requires(source -> hasPerm(source))
                            .then(literal("help")
                                    .executes(ctx -> {
                                        ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.help")), false);
                                        return 1;
                                    })
                            )
                            // Permission-Check für create
                            .then(literal("create")
                                    .executes(ctx -> {
                                        if (!hasPerm(ctx.getSource())) {
                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                            return 0;
                                        }
                                        ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.started")), false);
                                        String backupFile = BackupManager.createBackup(ctx.getSource().getServer());
                                        if (backupFile != null) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.completed", backupFile)), false);
                                            return 1;
                                        } else {
                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.failed")), false);
                                            return 0;
                                        }
                                    })
                                    .then(argument("name", StringArgumentType.string())
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String customName = StringArgumentType.getString(ctx, "name");
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.started")), false);
                                                String backupFile = BackupManager.createBackup(ctx.getSource().getServer(), customName);
                                                if (backupFile != null) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.completed", backupFile)), false);
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.failed")), false);
                                                    return 0;
                                                }
                                            })
                                    )
                            )
                            .then(literal("upload")
                                    .then(argument("url", StringArgumentType.greedyString())
                                            .then(argument("name", StringArgumentType.string())
                                                    .executes(ctx -> {
                                                        if (!hasPerm(ctx.getSource())) {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                            return 0;
                                                        }
                                                        String url = StringArgumentType.getString(ctx, "url");
                                                        String name = StringArgumentType.getString(ctx, "name");
                                                        try {
                                                            String resultName = BackupUploadManager.uploadBackupFromUrl(url, name);
                                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                                    LangManager.tr("backup.upload_success", resultName)
                                                            ), false);
                                                            return 1;
                                                        } catch(Exception e) {
                                                            ctx.getSource().sendFailure(Component.literal(
                                                                    LangManager.tr("backup.upload_failed", e.getMessage())
                                                            ));
                                                            return 0;
                                                        }
                                                    })
                                            )
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String url = StringArgumentType.getString(ctx, "url");
                                                try {
                                                    String resultName = BackupUploadManager.uploadBackupFromUrl(url, "");
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            LangManager.tr("backup.upload_success", resultName)
                                                    ), false);
                                                    return 1;
                                                } catch(Exception e) {
                                                    ctx.getSource().sendFailure(Component.literal(
                                                            LangManager.tr("backup.upload_failed", e.getMessage())
                                                    ));
                                                    return 0;
                                                }
                                            })
                                    )
                            )
                            .then(literal("rename")
                                    .then(argument("zipfile", StringArgumentType.string())
                                            .suggests(SUGGESTION_ZIPS)
                                            .then(argument("newname", StringArgumentType.string())
                                                    .executes(ctx -> {
                                                        if (!hasPerm(ctx.getSource())) {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                            return 0;
                                                        }
                                                        String zipName = StringArgumentType.getString(ctx, "zipfile");
                                                        String newName = StringArgumentType.getString(ctx, "newname");
                                                        File zipFile = new File(BackupConfig.BACKUP_DIR, zipName);
                                                        if (!zipFile.exists() || !zipFile.isFile()) {
                                                            ctx.getSource().sendFailure(Component.literal(
                                                                    LangManager.tr("backup.rename_not_found", zipName)
                                                            ));
                                                            return 0;
                                                        }
                                                        if (!BackupRenameManager.isRenameAllowed(zipFile)) {
                                                            ctx.getSource().sendFailure(Component.literal(
                                                                    LangManager.tr("backup.rename_not_allowed", zipName)
                                                            ));
                                                            return 0;
                                                        }
                                                        String renamed = BackupRenameManager.renameBackupZip(zipFile, newName);
                                                        if (!renamed.equals(zipName)) {
                                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                                    LangManager.tr("backup.rename_success", renamed)
                                                            ), false);
                                                            return 1;
                                                        } else {
                                                            ctx.getSource().sendFailure(Component.literal(
                                                                    LangManager.tr("backup.rename_failed", zipName)
                                                            ));
                                                            return 0;
                                                        }
                                                    })
                                            )
                                    )
                            )
                            .then(literal("list")
                                    .executes(ctx -> {
                                        if (!hasPerm(ctx.getSource())) {
                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                            return 0;
                                        }
                                        List<String> backups = BackupManager.listBackups();
                                        ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.backup_list")), false);
                                        backups.forEach(name ->
                                                ctx.getSource().sendSuccess(() -> Component.literal(name), false)
                                        );
                                        return 1;
                                    })
                            )
                            .then(literal("lang")
                                    .then(argument("lang_lang", StringArgumentType.word())
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String lang = StringArgumentType.getString(ctx, "lang_lang");
                                                BackupConfig.lang = lang;
                                                BackupConfig.saveConfig();
                                                LangManager.load();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.lang_changed", lang)), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(literal("restore")
                                    .then(argument("backupzip", StringArgumentType.string())
                                            .suggests(SUGGESTION_ZIPS)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String zip = StringArgumentType.getString(ctx, "backupzip");
                                                File file = new File(BackupConfig.BACKUP_DIR, zip);
                                                if (!file.exists() || !file.isFile()) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.restore.noexist", zip)));
                                                    return 0;
                                                }
                                                ctx.getSource().getServer().getPlayerList().getPlayers().forEach(player ->
                                                        player.connection.disconnect(Component.literal(LangManager.tr("backup.player_kicked")))
                                                );
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.restoring", zip)), false);
                                                boolean success = RestoreManager.restoreBackup(zip, ctx.getSource().getServer());
                                                if (success) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.restored", zip)), false);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.stopping")), false);
                                                    ctx.getSource().getServer().halt(false);
                                                }
                                                return success ? 1 : 0;
                                            })
                                    )
                            )
                            .then(literal("restorelatest")
                                    .executes(ctx -> {
                                        if (!hasPerm(ctx.getSource())) {
                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                            return 0;
                                        }
                                        String backupName = BackupManager.getLatestBackup();
                                        if (backupName == null) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.no_backup_found")), false);
                                            return 0;
                                        }
                                        ctx.getSource().getServer().getPlayerList().getPlayers().forEach(player ->
                                                player.connection.disconnect(Component.literal(LangManager.tr("backup.player_kicked")))
                                        );
                                        ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.restoring", backupName)), false);
                                        boolean success = RestoreManager.restoreBackup(backupName, ctx.getSource().getServer());
                                        if (success) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.restored", backupName)), false);
                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.stopping")), false);
                                            ctx.getSource().getServer().halt(false);
                                        }
                                        return success ? 1 : 0;
                                    })
                            )
                            .then(literal("delete")
                                    .then(argument("backupzip", StringArgumentType.string())
                                            .suggests(SUGGESTION_ZIPS)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String zipName = StringArgumentType.getString(ctx, "backupzip");
                                                File zipFile = new File(BackupConfig.BACKUP_DIR, zipName);
                                                if (zipFile.exists() && zipFile.isFile()) {
                                                    if (zipFile.delete()) {
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                LangManager.tr("backup.delete_success", zipName)
                                                        ), false);
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal(
                                                                LangManager.tr("backup.delete_failed", zipName)
                                                        ));
                                                        return 0;
                                                    }
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal(
                                                            LangManager.tr("backup.delete_not_found", zipName)
                                                    ));
                                                    return 0;
                                                }
                                            })
                                    )
                            )
                            .then(literal("autobackup")
                                    .then(literal("off")
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.autoBackupEnabled = 0;
                                                BackupConfig.autoBackupTimes = 0;
                                                BackupConfig.saveConfig();
                                                BackupScheduler.resetSchedule();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.autobackup_disabled")), false);
                                                return 1;
                                            })
                                    )
                                    .then(literal("request")
                                            .executes(ctx -> {
                                                String msg = getNextAutoBackupString();
                                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                return 1;
                                            })
                                    )
                                    .then(argument("times", IntegerArgumentType.integer(1, 40))
                                            .suggests(SUGGESTION_AUTO)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                int times = IntegerArgumentType.getInteger(ctx, "times");
                                                if (times < 1 || times > 40) {
                                                    ctx.getSource().sendFailure(Component.literal("Please enter a number between 1 and 40."));
                                                    return 0;
                                                }
                                                BackupScheduler.setAutoBackupTimes(times);
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        LangManager.tr("backup.autobackup_enabled", String.valueOf(times))
                                                ), false);
                                                String timerString = BackupScheduler.getNextScheduledBackupTimerString();
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        LangManager.tr("backup.autobackup_timer", timerString)
                                                ).withStyle(ChatFormatting.AQUA), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(literal("maxbackup")
                                    .then(literal("off")
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.maxBackups = Integer.MAX_VALUE;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.maxbackup_off")), false);
                                                return 1;
                                            })
                                    )
                                    .then(literal("request")
                                            .executes(ctx -> {
                                                String msg = getMaxBackupStatus();
                                                ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                                                return 1;
                                            })
                                    )
                                    .then(argument("max", IntegerArgumentType.integer(1, 100))
                                            .suggests(SUGGESTION_MAX)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                int max = IntegerArgumentType.getInteger(ctx, "max");
                                                if (max < 1 || max > 100) {
                                                    ctx.getSource().sendFailure(Component.literal("Please enter a number between 1 and 100."));
                                                    return 0;
                                                }
                                                BackupConfig.maxBackups = max;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.maxbackup_set", String.valueOf(max))), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(literal("exclude")
                                    .then(argument("path", StringArgumentType.string())
                                            .suggests(SUGGESTION_ROOT_FILES)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String path = StringArgumentType.getString(ctx, "path");
                                                if (path.contains("/")) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.exclude_notfolder")));
                                                    return 0;
                                                }
                                                String configPath = "/" + path;
                                                if (!BackupConfig.excludePaths.contains(configPath)) {
                                                    BackupConfig.excludePaths.add(configPath);
                                                    BackupConfig.saveConfig();
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.exclude_success", configPath)), false);
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.exclude_already")));
                                                    return 0;
                                                }
                                            })
                                    )
                            )
                            .then(literal("enclude")
                                    .then(argument("path", StringArgumentType.string())
                                            .suggests(SUGGESTION_EXCLUDES)
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                String path = StringArgumentType.getString(ctx, "path");
                                                String configPath = "/" + path.replaceFirst("^/", "");
                                                if (BackupConfig.excludePaths.remove(configPath)) {
                                                    BackupConfig.saveConfig();
                                                    ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.enclude_success", configPath)), false);
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.exclude_notfound")));
                                                    return 0;
                                                }
                                            })
                                    )
                                    .executes(ctx -> {
                                        ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.exclude_list")), false);
                                        for (String entry : BackupConfig.excludePaths) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(entry), false);
                                        }
                                        return 1;
                                    })
                            )
                            .then(literal("permissions")
                                    .then(literal("add")
                                            .then(argument("player", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        if (!hasPerm(ctx.getSource())) {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                            return 0;
                                                        }
                                                        String name = StringArgumentType.getString(ctx, "player");
                                                        ServerPlayer player = getPlayerByName(ctx.getSource(), name);
                                                        if (player != null) {
                                                            BackupConfig.addPermUser(player.getUUID().toString(), name);
                                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.permissions_added", name)), false);
                                                            return 1;
                                                        } else {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.permissions_not_found")));
                                                            return 0;
                                                        }
                                                    })
                                            )
                                    )
                                    .then(literal("remove")
                                            .then(argument("player", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        if (!hasPerm(ctx.getSource())) {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                            return 0;
                                                        }
                                                        String name = StringArgumentType.getString(ctx, "player");
                                                        ServerPlayer player = getPlayerByName(ctx.getSource(), name);
                                                        if (player != null) {
                                                            BackupConfig.removePermUser(player.getUUID().toString(), name);
                                                            ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.permissions_removed", name)), false);
                                                            return 1;
                                                        } else {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("backup.permissions_not_found")));
                                                            return 0;
                                                        }
                                                    })
                                            )
                                    )
                                    .then(literal("list")
                                            .executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                StringBuilder sb = new StringBuilder();
                                                for (BackupConfig.PermUser u : BackupConfig.permissionUsers) {
                                                    sb.append(u.name).append(" (").append(u.uuid).append("), ");
                                                }
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.permissions_list", sb.toString())), false);
                                                return 1;
                                            })
                                    )
                            )
                            .then(literal("log")
                                    .then(literal("normallog")
                                            .then(literal("on").executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.normallogEnabled = true;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.log_normallog_on")), false);
                                                return 1;
                                            }))
                                            .then(literal("off").executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.normallogEnabled = false;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.log_normallog_off")), false);
                                                return 1;
                                            }))
                                    )
                                    .then(literal("permlog")
                                            .then(literal("on").executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.permlogEnabled = true;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.log_permlog_on")), false);
                                                return 1;
                                            }))
                                            .then(literal("off").executes(ctx -> {
                                                if (!hasPerm(ctx.getSource())) {
                                                    ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                    return 0;
                                                }
                                                BackupConfig.permlogEnabled = false;
                                                BackupConfig.saveConfig();
                                                ctx.getSource().sendSuccess(() -> Component.literal(LangManager.tr("backup.log_permlog_off")), false);
                                                return 1;
                                            }))
                                    )
                            )
                            // DEBUG/TEST-TIMER - nur noch die Variante mit Stunden, Minuten, Sekunden!
                            .then(literal("debug")
                                    .requires(source -> source.hasPermission(4) || hasPerm(source))
                                    .then(literal("set.timer.value")
                                            .then(argument("hours", IntegerArgumentType.integer(0, 23))
                                                    .then(argument("minutes", IntegerArgumentType.integer(0, 59))
                                                            .then(argument("seconds", IntegerArgumentType.integer(0, 59))
                                                                    .executes(ctx -> {
                                                                        int h = IntegerArgumentType.getInteger(ctx, "hours");
                                                                        int m = IntegerArgumentType.getInteger(ctx, "minutes");
                                                                        int s = IntegerArgumentType.getInteger(ctx, "seconds");
                                                                        BackupScheduler.setDebugTimer(h, m, s, ctx.getSource().getServer());
                                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                                "[BackupMod] Debug-Timer set to: %02d:%02d:%02d".formatted(h, m, s)
                                                                        ), false);
                                                                        String timerString = BackupScheduler.getNextScheduledBackupTimerString();
                                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                                LangManager.tr("backup.autobackup_timer", timerString)
                                                                        ).withStyle(ChatFormatting.AQUA), false);
                                                                        return 1;
                                                                    })
                                                            )
                                                    )
                                            )
                                    )
                                    .then(literal("set.autobackup.value")
                                            .then(argument("times", IntegerArgumentType.integer(1, 1440)) // <- Debug: bis 80 erlaubt!
                                                    .executes(ctx -> {
                                                        if (!hasPerm(ctx.getSource())) {
                                                            ctx.getSource().sendFailure(Component.literal(LangManager.tr("error.permission")));
                                                            return 0;
                                                        }
                                                        int times = IntegerArgumentType.getInteger(ctx, "times");
                                                        if (times < 1 || times > 1440) {
                                                            ctx.getSource().sendFailure(Component.literal("Please enter a number between 1 and 80."));
                                                            return 0;
                                                        }
                                                        BackupScheduler.setAutoBackupTimes(times);
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "[BackupMod] Debug-AutoBackup value set to: " + times
                                                        ), false);
                                                        String timerString = BackupScheduler.getNextScheduledBackupTimerString();
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                LangManager.tr("backup.autobackup_timer", timerString)
                                                        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(literal("create.schedulebackup")
                                            .executes(ctx -> {
                                                boolean ok = BackupScheduler.runScheduledBackup(ctx.getSource().getServer());
                                                if (ok) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal("[BackupMod] Scheduled backup executed!"), false);
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("[BackupMod] Scheduled backup failed!"));
                                                }
                                                return ok ? 1 : 0;
                                            })
                                    )
                                    .then(literal("create.schedulebackup.name")
                                            .then(argument("name", StringArgumentType.string())
                                                    .executes(ctx -> {
                                                        boolean ok = BackupScheduler.runScheduledBackup(ctx.getSource().getServer());
                                                        String customName = StringArgumentType.getString(ctx, "name");
                                                        if (ok) {
                                                            ctx.getSource().sendSuccess(() -> Component.literal("[BackupMod] Scheduled backup executed as 'autobackup'! (Custom name ignored)"), false);
                                                        } else {
                                                            ctx.getSource().sendFailure(Component.literal("[BackupMod] Scheduled backup failed!"));
                                                        }
                                                        return ok ? 1 : 0;
                                                    })
                                            )
                                    )
                            )
            );
        });
    }

    private static List<String> getRootEntries(String rootPath) {
        File root = new File(rootPath);
        File[] files = root.listFiles();
        List<String> result = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.getParentFile().equals(root)) {
                    if (f.getName().equalsIgnoreCase("backups")) continue;
                    result.add(f.getName());
                }
            }
        }
        return result;
    }

    private static List<String> getBackupZipFiles() {
        File dir = new File(BackupConfig.BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".zip"));
        List<String> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                result.add(file.getName());
            }
        }
        return result;
    }

    public static String getNextAutoBackupString() {
        if (BackupConfig.autoBackupEnabled != 1 || BackupConfig.autoBackupTimes < 1) {
            return LangManager.tr("backup.autobackup_disabled");
        }
        int times = BackupConfig.autoBackupTimes;
        String timerString = BackupScheduler.getNextScheduledBackupTimerString();
        return LangManager.tr("backup.autobackup_status", String.valueOf(times), timerString);
    }

    public static String getMaxBackupStatus() {
        if (BackupConfig.maxBackups >= Integer.MAX_VALUE) {
            return LangManager.tr("backup.maxbackup_not_set");
        }
        return LangManager.tr("backup.maxbackup_current", String.valueOf(BackupConfig.maxBackups));
    }

    private static boolean hasPerm(CommandSourceStack source) {
        if (source.hasPermission(4)) return true;
        if (source.getEntity() instanceof ServerPlayer player) {
            return BackupConfig.hasBackupPerm(player.getUUID().toString(), player.getName().getString());
        }
        return false;
    }

    private static ServerPlayer getPlayerByName(CommandSourceStack source, String name) {
        for (ServerPlayer sp : source.getServer().getPlayerList().getPlayers()) {
            if (sp.getName().getString().equalsIgnoreCase(name)) return sp;
        }
        return null;
    }
}