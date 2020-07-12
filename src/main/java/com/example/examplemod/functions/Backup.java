/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.functions;

import com.example.examplemod.ExampleMod;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Class for Backing Up Items.
 */
public class Backup {
    /**
     * Event Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * JSON Object Mapper.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * A method that deletes folder, files, and files within folders.
     *
     * @param file The file/folder to clear.
     * @return A boolean representing the success of the operation; A false value represents success.
     */
    private static boolean recursiveDelete (File file ) {
        if (file.isDirectory()) {
            File[] inner = file.listFiles();
            if (inner == null) {
                return !file.delete();
            } else {
                for (File innerFile: inner) {
                    if (recursiveDelete(innerFile)) {
                        return true;
                    }
                }
                return !file.delete();
            }
        } else {
            return !file.delete();
        }
    }

    /**
     * Reduces number of backup files in a folder to a specified number.
     *
     * @param backupFolder The folder to clean up.
     * @param config The settings to follow while performing the cleanup.
     */
    private static void cleanup(Path backupFolder, Map config) {
        File[] files = backupFolder.toFile().listFiles();
        int max = (int) config.get("Number of backups");

        if (files == null) { return; }
        if (files.length > max) {
            int index = max - files.length;

            for (File file: files) {
                if (index < 0) {
                    if (recursiveDelete(file)){
                        LOGGER.warn(String.format("[%s] Could not delete the backup folder: %s",
                                ExampleMod.NAME, file.getName()));
                    }
                }
                index++;
            }

            LOGGER.debug(String.format("[%s] Cleaned Up Files", ExampleMod.NAME));
        }
    }

    /**
     * Gets the path of the last json file in a folder.
     *
     * @param folderPath The folder to search in.
     * @return The path of the latest json file in a folder.
     */
    private static Path getLatest(Path folderPath) {
        File[] files = folderPath.toFile().listFiles();

        if (files == null) {
            LOGGER.warn(String.format("[%s] Tried to restore, but the default backup folder is empty.",
                    ExampleMod.NAME));
            return null;
        }

        File[] backupFolder = null;
        int index = 0;
        for (File file: files ) {
            if (index == files.length - 1 ) {
                backupFolder = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            }
            index++;
        }

        if (backupFolder == null) { return null; }
        index = 0;
        for (File file: backupFolder ) {
            if (index == backupFolder.length - 1 ) {
                return file.toPath();
            }
            index++;
        }

        return null;
    }

    /**
     * Writes backups to SER file.
     *
     * @param config The settings to follow when performing backup.
     * @param itemList The items to be written to file.
     *
     * @throws IOException The backup file could not be located, backup written to logs.
     */
    @OnlyIn(Dist.CLIENT)
    static void backup(Map config, ArrayList<ItemStack> itemList) throws IOException {
        try {
            if (itemList.size() == 0) { return; }
            String folderName;
            Path worldPath;
            if (Minecraft.getInstance().getIntegratedServer() != null) {
                folderName = Minecraft.getInstance().getIntegratedServer().getFolderName();
                worldPath = Paths.get((Minecraft.getInstance().gameDir + "/saves/" + folderName));

                if (config.containsKey("Location (Path)") && !config.get("Location (Path)").equals("Default")){
                    worldPath = Paths.get((String) config.get("Location (Path)"));
                }
            } else {
                worldPath = Minecraft.getInstance().gameDir.toPath();
            }

            if (Files.exists(worldPath)){
                Path backupFolder = Paths.get((worldPath + "/" + ExampleMod.NAME_SHORT + "Backups"));
                if (!Files.exists(backupFolder)) {
                    backupFolder = Files.createDirectories(backupFolder);
                }

                if (!Files.isWritable(backupFolder)) {
                    LOGGER.warn(String.format("[%s] The backup folder is not writable, initiating emergency backup:",
                            ExampleMod.NAME));
                    for (ItemStack item: itemList) {
                        LOGGER.warn(item);
                    }
                    return;
                }
                long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
                Path nbtFolder = Files.createDirectory(Paths.get((backupFolder + String.format("/%s",
                        timestamp))));
                File backupFile = Files.createFile(Paths.get((nbtFolder + String.format("/%s.json",
                        timestamp)))).toFile();

                ArrayList<String> serialized = new ArrayList<>();
                int itemNumber = 0;
                for (ItemStack item: itemList) {
                    serialized.add(new ItemStackSerialization(item, nbtFolder, itemNumber).serialize());
                    itemNumber++;
                }

                PrintWriter printer = new PrintWriter(backupFile);
                printer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serialized));
                printer.flush(); printer.close();
                
                cleanup(backupFolder, config);
            }
        } catch (Exception error) {
            LOGGER.error(String.format("[%s] Backup Error: (%s) - %s", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            LOGGER.warn(String.format("[%s] Backup:", ExampleMod.NAME));
            for (ItemStack item: itemList ) {
                LOGGER.warn(item);
            }
            throw error;
        }
    }

    /**
     * Read a backup file and get the items within it.
     *
     * @param config The settings to follow while performing restore.
     * @param file The name of the backup to restore from. Use "latest" to restore the latest backup.
     * @param result A list to write results to.
     *
     * @return The success of the operation.
     *   1: Can't find file `file`.
     *   2: Can't open file.
     *   3: Can't find index json file in folder.
     *   5: Other (logged).
     *  10: Can't locate game folder.
     */
    @OnlyIn(Dist.CLIENT)
    public static int restore(Map config, String file, ArrayList<ItemStack> result) {
        // Getting backup file
        File backupFile = null;

        if (config.get("Location (Path)").equals("Default")){
            if (Minecraft.getInstance().getIntegratedServer() == null) {
                LOGGER.warn(String.format("[%s] Could not get game folder.", ExampleMod.NAME));
                return 10;
            }

            String folderName = Minecraft.getInstance().getIntegratedServer().getFolderName();
            Path backupPath = Paths.get(
                    Minecraft.getInstance().gameDir.getAbsolutePath(), "saves", folderName, ExampleMod.NAME_SHORT + "Backups"
            );
            if (file.toLowerCase().equals("latest")) {
                Path latest = getLatest(backupPath);
                if (latest == null) {
                    LOGGER.warn(String.format("[%s] Restore Error: Could not find folder", ExampleMod.NAME));
                    return 1;
                }
                backupFile = latest.toFile();
            } else {
                backupFile = Paths.get(backupPath.toFile().getAbsolutePath(), file).toFile();
            }

        } else {
            Path backupPath = Paths.get((String) config.get("Location (Path)"));
            if (file.toLowerCase().equals("latest")) {
                Path latest = getLatest(backupPath);
                if (latest != null) {
                    backupFile = latest.toFile();
                }
            } else {
                backupFile = Paths.get(backupPath.toFile().getAbsolutePath(), file).toFile();
            }
        }

        if (backupFile == null || !Files.exists(backupFile.toPath())) {
            LOGGER.warn(String.format("[%s] Restore Error: Can't find file.", ExampleMod.NAME));
            return 1;
        }

        if (backupFile.isDirectory()) {
            if (backupFile.listFiles((ignored, name) -> name.equals(file + ".json")) != null) {
                backupFile = Paths.get(backupFile.getAbsoluteFile().getAbsolutePath(), file + ".json").toFile();
            } else {
                return 3;
            }
        }

        // Reading file
        try {
            if (Files.isReadable(backupFile.toPath())){
                Object[] backupItems = mapper.readValue(backupFile, Object[].class);

                for (Object item: backupItems) {
                    result.add(ItemStackSerialization.deserialize((String) item));
                }

                return 0;

            } else {
                LOGGER.warn(String.format("[%s] Restore Error: Can't read file.", ExampleMod.NAME));
                return 2;
            }

        } catch (Exception error) {
            LOGGER.error(String.format("[%s] Restore Error: (%s) - %s", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            return 5;
        }
    }
}
