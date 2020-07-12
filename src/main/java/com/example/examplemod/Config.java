/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.inventory.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Config {
    /**
     * Event logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * JSON object mapper.
     */
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * The content of the options.
     */
    private Map<String, Map> content = new HashMap<>();
    /**
     * Game constants.
     */
    private Setting constants;
    /**
     * Whether the settings have been read successfully.
     */
    private boolean readError = false;

    /**
     * The file of the config.
     */
    public final File configFile = new File(FMLPaths.CONFIGDIR.get() +
            String.format("/%s.json", ExampleMod.NAME));
    /**
     * Blacklisted screens.
     */
    public static final ArrayList<String> blacklist = new ArrayList<>(Arrays.asList(
            AbstractCommandBlockScreen.class.getName(), AddServerScreen.class.getName(), AlertScreen.class.getName(),
            ChatOptionsScreen.class.getName(), ChatScreen.class.getName(), CommandBlockScreen.class.getName(),
            ConfirmBackupScreen.class.getName(), ConfirmOpenLinkScreen.class.getName(), ConfirmScreen.class.getName(),
            ConnectingScreen.class.getName(), ControlsScreen.class.getName(), CreateBuffetWorldScreen.class.getName(),
            CreateFlatWorldScreen.class.getName(), CreateWorldScreen.class.getName(),
            CustomizeSkinScreen.class.getName(), DeathScreen.class.getName(), DemoScreen.class.getName(),
            DirtMessageScreen.class.getName(), DisconnectedScreen.class.getName(),
            DownloadTerrainScreen.class.getName(), EditBookScreen.class.getName(),
            EditMinecartCommandBlockScreen.class.getName(), EditSignScreen.class.getName(),
            EditStructureScreen.class.getName(), EditWorldScreen.class.getName(), ErrorScreen.class.getName(),
            FlatPresetsScreen.class.getName(), IngameMenuScreen.class.getName(), JigsawScreen.class.getName(),
            LanguageScreen.class.getName(), MainMenuScreen.class.getName(), MemoryErrorScreen.class.getName(),
            MouseSettingsScreen.class.getName(), MultiplayerScreen.class.getName(),
            MultiplayerWarningScreen.class.getName(), OptimizeWorldScreen.class.getName(),
            OptionsScreen.class.getName(), OptionsSoundsScreen.class.getName(), ReadBookScreen.class.getName(),
            ResourcePacksScreen.class.getName(), ServerListScreen.class.getName(), ServerSelectionList.class.getName(),
            SettingsScreen.class.getName(), ShareToLanScreen.class.getName(), SleepInMultiplayerScreen.class.getName(),
            StatsScreen.class.getName(), VideoSettingsScreen.class.getName(), WinGameScreen.class.getName(),
            WorkingScreen.class.getName(), WorldLoadProgressScreen.class.getName(), WorldSelectionList.class.getName(),
            WorldSelectionScreen.class.getName(), ChatLine.class.getName(), OptionsScreen.class.getName(),
            OptionsSoundsScreen.class.getName(), ChatOptionsScreen.class.getName(), ChatScreen.class.getName()
    ));
    // TODO: Add all screens above

    /**
     * Loading or creating mod settings.
     */
    Config() {
        LOGGER.debug(String.format("[%s] Instantiated Config", ExampleMod.NAME));
        if (checkFile()){
            LOGGER.debug(String.format("[%s] Loaded Config", ExampleMod.NAME));
            load();
        } else {
            LOGGER.debug(String.format("[%s] Created Config", ExampleMod.NAME));
            create();
        }
    }

    /**
     * Checks if the config file exists and is editable.
     *
     * @return True if the configFile is accessible and writable.
     */
    private boolean checkFile() {
        if (Files.exists(configFile.toPath())){
            return Files.isReadable(configFile.toPath()) && Files.isWritable(configFile.toPath());
        }
        return false;
    }

    /**
     * Loading option file.
     *
     * @return The success of the operation.
     * 0: File loaded.
     * 1: File created.
     * 2: Error.
     */
    public int load() {
        try {
            if (checkFile()) {
                this.content = new HashMap<>();
                Map raw = mapper.readValue(configFile, Map.class);

                for (Object key: raw.keySet()) {
                    try {
                        this.content.put(String.valueOf(key), (Map) raw.get(key));

                    } catch (Exception error) {
                        LOGGER.warn(String.format("[%s] Could not load %s property while loading CONFIG. " +
                                        "Reason: %s - %s",
                                ExampleMod.NAME, key,
                                error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
                    }
                }

                this.constants = getConstants();

                return 0;
            } else {
                create();
                return 1;
            }

        } catch (IOException error) {
            LOGGER.error(String.format("[%s] Could not load settings file, assuming defaults. %s - %s",
                    ExampleMod.NAME, error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            this.readError = true;
            this.content = null;
            return 2;
        }
    }


    /**
     * Class for managing single options, and option groups.
     */
    private static class Setting {
        private final Map<String, Map> top = new HashMap<>();
        private final Map<String, Object> content = new HashMap<>();
        private final String name;

        /**
         * Creates a setting object.
         *
         * @param settingName The category/name of the setting to be added.
         * @param description The description to be placed into the setting's sub category.
         */
        Setting(String settingName, String description) {
            this.name = settingName;
            this.content.put("Description", description);
            update();
        }

        /**
         * Create a setting and add it to a setting group.
         *
         * @param settingName The category/name of the setting to be added.
         * @param description The description to be placed into the setting's sub category.
         * @param group The list of setting to add this to.
         */
        Setting(String settingName, String description, ArrayList<Setting> group) {
            this.name = settingName;
            this.content.put("Description", description);
            update();
            group.add(this);
        }

        private void update() {
            if (this.name != null && this.top.containsKey(name)) {
                this.top.replace(name, this.content);
            } else {
                this.top.put(name, this.content);
            }
        }

        void addSetting(String name, Object value) {
            if (!this.content.isEmpty() && this.content.containsKey(name)) {
                this.content.replace(name, value);
            } else {
                this.content.put(name, value);
            }
            this.update();
        }

        Map<String, Map> get() {
            return this.top;
        }

        Map<String, Object> getSettings() {
            return this.content;
        }

    }

    /**
     * Returns game constants.
     *
     * @return Game constants.
     */
    private Setting getConstants() {
        try {
            if (this.content.containsKey("Constants")) {
                ArrayList value = (ArrayList) this.content.get("Constants");

                ArrayList<Map> savedConstants = new ArrayList<>();
                String description = null;

                for (Object setting: value) {
                    if (String.valueOf(((Map) setting).keySet().toArray()[0]).equals("Description")) {
                        description = String.valueOf(((Map) setting).values().toArray()[0]);

                    } else {
                        savedConstants.add((Map) setting);

                    }
                }

                Setting constants = new Setting("Constants", description);

                for (Map setting: savedConstants) {
                    constants.addSetting(String.valueOf(setting.keySet().toArray()[0]), setting.values());
                }

                return constants;
            }

        } catch (Exception ignored) {}

        Setting constants = new Setting("Constants","Builtin constants that the mod uses to handle logic. If you need mod support, ask the mod author to add to these.");

        constants.addSetting("Default Chest Width", 9);
        constants.addSetting("Player Inventory Width", 9);
        constants.addSetting("Player Inventory Height", 3);
        constants.addSetting("Player Hotbar Size", 9);

        return constants;
    }

    /**
     * Create options folder.
     */
    private void create() {
        try {
            Files.createFile(configFile.toPath());
        } catch (IOException error) {
            LOGGER.error(String.format("[%s] Could not create settings file, assuming defaults. %s - %s",
                    ExampleMod.NAME, error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            this.content = null;
            this.readError = true;
        }

        ArrayList<Class> supportedClassesPlayer = new ArrayList<>(Arrays.asList(
                AnvilScreen.class, BeaconScreen.class, BlastFurnaceScreen.class,
                BrewingStandScreen.class, CartographyTableScreen.class,
                CraftingScreen.class, CreativeScreen.class, FurnaceScreen.class,
                InventoryScreen.class, MerchantScreen.class, SmokerScreen.class,
                StonecutterScreen.class, GrindstoneScreen.class, HopperScreen.class,
                LecternScreen.class, LoomScreen.class
        ));
        ArrayList<Class> supportedClassesInventory = new ArrayList<>(Arrays.asList(
                ChestScreen.class, ShulkerBoxScreen.class
        ));

        ArrayList<Setting> settings = new ArrayList<>();

        Setting PFI = new Setting("Player-First Inventories", "These are the inventories where using a keybind would sort your player inventory.", settings);
        Setting IFI = new Setting("Inventory-First Inventories", "These are the inventories where using a keybind would sort the interacted screen.", settings);
        Setting blacklist = new Setting("Blacklisted Inventories", "These screens will always result in sorting your inventory. Some, such as OptionsScreen, are unchangeable (even if you delete them here).", settings);
        Setting backups = new Setting("Backups", "If items should be backed up externally on sort. Backups could be removed in future versions.", settings);
        Setting sortSettings = new Setting("Sort Options", "The options to consider when performing sorts.", settings);
        Setting refill = new Setting("Refill", "If items held in your hand should be replenished when they run out, if possible.", settings);
        Setting replace = new Setting("Replace", "Whether items should be removed from your hand (and replaced) before breaking.", settings);
        Setting MMM = new Setting("Middle Mouse Move", "Whether scrolling should move items from one inventory to another.", settings);
        Setting shortcuts = new Setting("Shortcuts", "Unchangeable shortcuts. Setting 'shortcuts' to off turns all of them off.", settings);
//        Setting unsupported = new Setting("Unsupported Servers", "Whether the mod should be activated on unsupported servers.", settings);

        Setting keybind = new Setting("Keybind", "The default keybind as an integer (read more at the reference). If this is unreadable, will default to \'R\' (82).");
        Setting changeOrder = new Setting("Change Order", "Weather performing multiple sorts in a row will change the order of the sort.");
        Setting sortMethod = new Setting("Sort Method", "This describes how sorting will be performed. If set to true, then items will be compacted and sorted alphabetically. If compact is true, the items will only be compacted, and the original order preserved. When neither is true, the items will be compacted and grouped based on type.");

        PFI.addSetting("Inventories", supportedClassesPlayer);
        IFI.addSetting("Inventories", supportedClassesInventory);
        blacklist.addSetting("Inventories", Config.blacklist);

        keybind.addSetting("Keybind", 82);
        keybind.addSetting("Reference", "https://www.glfw.org/docs/latest/group__keys.html");

        sortMethod.addSetting("Alphabetical", true);
        sortMethod.addSetting("Compact", false);
        sortMethod.addSetting("Additional Note", "If both are set to true, alphabetical takes priority.");

        changeOrder.addSetting("Change Order", false);
        changeOrder.addSetting("Timeout (seconds)", 2);

        backups.addSetting("Backups", true);
        backups.addSetting("Number of backups", 10);
        backups.addSetting("Location (Path)", "Default");

        MMM.addSetting("Middle Mouse Move", true);
        MMM.addSetting("Invert Scroll", false);

        refill.addSetting("Refill", true);

        replace.addSetting("Replace", true);

//        unsupported.addSetting("Allow", false);

        shortcuts.addSetting("Shortcuts", true);
        shortcuts.addSetting("Move one item - Control + Click", true);
        shortcuts.addSetting("Move one stack - Shift/W/Up/S/Down + Click", true);
        shortcuts.addSetting("Move all items of the same type - Control + Shift + Click", true);
        shortcuts.addSetting("Move everything - Space + Click", true);
        shortcuts.addSetting("Move to empty slot - Ctrl/Shift/ALT + Right Click", true);
        shortcuts.addSetting("Drop - Alt + Click", false);
        shortcuts.addSetting("Craft one item - Ctrl + Click", true);
        shortcuts.addSetting("Craft one stack - Shift + Click", true);
        shortcuts.addSetting("Craft all from inventory - Ctrl + Shift + Click", true);

        sortSettings.addSetting(keybind.name, keybind.getSettings());
        sortSettings.addSetting("Middle Mouse Sort", true);
        sortSettings.addSetting(changeOrder.name, changeOrder.getSettings());
        sortSettings.addSetting(sortMethod.name, sortMethod.getSettings());
        sortSettings.addSetting("Sort On Pickup", false);
        sortSettings.addSetting("Sort Outside Inventory", false);
        sortSettings.addSetting("Equip Best Armor on Sort", false);

        Map<String, Map> config = new HashMap<>(getConstants().get());

        for (Setting setting: settings) {
            config.putAll(setting.get());
        }

        this.constants = getConstants();
        this.content = config;

        try {
            this.update();
        } catch (FileNotFoundException | JsonProcessingException error) {
            LOGGER.error(String.format("[%s] Could not write to config file. Falling back to defaults. %s - %s",
                    ExampleMod.NAME, error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
        }
    }

    /**
     * Update the options file with the current options.
     *
     * @throws FileNotFoundException Couldn't find options file.
     * @throws JsonProcessingException Couldn't convert the options to JSON.
     */
    private void update() throws FileNotFoundException, JsonProcessingException {
        PrintWriter printer = new PrintWriter(this.configFile);
        printer.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(content));
        printer.flush(); printer.close();
    }

    /**
     * Get the sort options from all the options.
     *
     * @return The sort options.
     */
    @Nullable
    public Map getSortSettings() {
        if (this.content != null && this.content.containsKey("Sort Options")) {
            return this.content.get("Sort Options");
        }
        return null;
    }

    /**
     * Get a map from options.
     *
     * @param key The map's key.
     * @return The map.
     */
    @Nullable
    public Map getMap(String key) {
        if (key == null) return null;

        if (this.content != null && this.content.containsKey(key)) {
            return this.content.get(key);
        }

        return null;
    }

    /**
     * Gets a value from a map, and casts it to a map.
     *
     * @param key The key to get the map.
     * @param map The map to search.
     *
     * @return The map.
     */
    @Nullable
    public static Map getMap(String key, Map map) {
        if (key == null || map == null) return null;

        if (map.containsKey(key)) {
            return (Map) map.get(key);
        }

        return null;
    }

    /**
     * Get a game constant's value.
     *
     * @param constant The game constant.
     * @return The constant's value.
     */
    @Nullable
    public Object getConstant(String constant) {
        if (constant == null) return null;

        if (this.constants.getSettings().containsKey(constant)) {
            return this.constants.getSettings().get(constant);
        }

        return null;
    }

    /**
     * Updates the game's constants.
     *
     * @param key The constant to update.
     * @param value The new value.
     *
     * @throws FileNotFoundException Could not find the config file.
     * @throws JsonProcessingException Could not convert to JSON.
     */
    void updateConstants(String key, Object value) throws FileNotFoundException, JsonProcessingException {
        this.constants.addSetting(key, value);
        this.content.putAll(this.constants.get());

        this.update();
    }
}