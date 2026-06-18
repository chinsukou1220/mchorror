package com.example.util;

import com.example.SsttaallkkeerrMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class WorldBackupManager {
    public static void checkAndBackup(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path backupDir = worldDir.resolve("ssttaallkkeerr_backup");

        if (Files.exists(backupDir)) {
            SsttaallkkeerrMod.LOGGER.info("[HorrorSteve] Backup already exists. Skipping.");
            return;
        }

        SsttaallkkeerrMod.LOGGER.info("[HorrorSteve] First time running on this world. Creating a backup...");
        
        try {
            Files.createDirectories(backupDir);
            copyDirectory(worldDir, backupDir);
            
            // Create a marker file
            Files.writeString(backupDir.resolve("backup_info.txt"), "This is a clean backup created by Ssttaallkkeerr Mod before any corruption.");
            SsttaallkkeerrMod.LOGGER.info("[HorrorSteve] Backup completed successfully.");
            
        } catch (Exception e) {
            SsttaallkkeerrMod.LOGGER.error("[HorrorSteve] Failed to create world backup!", e);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName() != null && dir.getFileName().toString().equals("ssttaallkkeerr_backup")) {
                    return FileVisitResult.SKIP_SUBTREE; // バックアップフォルダ自身はコピーしない
                }
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // ロックファイルなどはコピー中にエラーになる可能性があるため除外
                if (file.getFileName() != null && file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    SsttaallkkeerrMod.LOGGER.warn("[HorrorSteve] Could not copy file: " + file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void restoreBackup(MinecraftServer server) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path backupDir = worldDir.resolve("ssttaallkkeerr_backup");

        if (!Files.exists(backupDir)) {
            SsttaallkkeerrMod.LOGGER.error("[HorrorSteve] Cannot restore: backup does not exist.");
            return;
        }

        SsttaallkkeerrMod.LOGGER.info("[HorrorSteve] Restoring world from backup...");
        
        try {
            copyDirectoryForRestore(backupDir, worldDir);
            SsttaallkkeerrMod.LOGGER.info("[HorrorSteve] Restore completed.");
        } catch (Exception e) {
            SsttaallkkeerrMod.LOGGER.error("[HorrorSteve] Failed to restore world backup!", e);
        }
    }

    private static void copyDirectoryForRestore(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName() != null && file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                if (file.getFileName() != null && file.getFileName().toString().equals("backup_info.txt")) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    SsttaallkkeerrMod.LOGGER.warn("[HorrorSteve] Could not restore file: " + file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
