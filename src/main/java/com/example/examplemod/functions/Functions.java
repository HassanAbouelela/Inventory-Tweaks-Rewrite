/*
 * MIT License
 *
 * Copyright (c) 2020 Hassan Abouelela
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.example.examplemod.functions;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.events.ClientEventHandlers;
import com.example.examplemod.network.*;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static com.example.examplemod.ExampleMod.CONFIG;


public class Functions {
    // Getting Event Logger
    private static final Logger LOGGER = LogManager.getLogger();

    /** The permission level of a given class for sorting purposes. */
    public enum Permission {
        /** The player inventory takes priority while sorting. */ PLAYER_FIRST,
        /** The open inventory takes priority while sorting. */ INVENTORY_FIRST,
        /** Inventories should not be sorted. */ BLACKLIST
    }

    public enum MoveType {
        MOVE_ALL,
        MOVE_ALL_TYPE,
        MOVE_STACK,
        MOVE_INT,
        MOVE_EMPTY
    }

    private enum Mode {
        DEFAULT, COMPACT, NONE
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean getChangeOrder(long lastSortTime) {
        Map config = null;
        if (CONFIG != null) {
            Map outer = CONFIG.getSortSettings();
            if (outer != null) {
                if (outer.containsKey("Change Order")) {
                    config = Config.getMap("Change Order", outer);
                }
            }
        }

        boolean changeOrder = false;

        if (config != null && config.containsKey("Change Order") && config.containsKey("Timeout (seconds)")) {
            try {
                if ((boolean) config.get("Change Order")) {
                    if (System.currentTimeMillis() < lastSortTime + (int) config.get("Timeout (seconds)") * 1000) {
                        changeOrder = true;
                    }
                }
            } catch (ClassCastException error) {
                LOGGER.warn(String.format("[%s] Could not read change order options, skipping.", ExampleMod.NAME));
            }
        }

        return changeOrder;
    }

    @OnlyIn(Dist.CLIENT)
    public static Permission getPermission(String className) {
        if (Config.blacklist.contains(className)) {
            return Permission.BLACKLIST;
        }

        if (CONFIG != null && CONFIG.getConstant(className) != null) {
            try {
                Map settings = (Map) CONFIG.getConstant(className);
                if (settings != null && settings.containsKey("type")) {
                    switch (String.valueOf(settings.get("type")).toLowerCase()) {
                        case "blacklist":
                            return Permission.BLACKLIST;
                        case "player_first":
                            return Permission.PLAYER_FIRST;
                        case "inventory_first":
                            return Permission.INVENTORY_FIRST;
                    }
                }

                return Permission.PLAYER_FIRST;
            } catch (Exception ignored) {
                return Permission.PLAYER_FIRST;
            }
        }

        if (CONFIG == null) return Permission.PLAYER_FIRST;
        try {
            Map playerFirst = CONFIG.getMap("Player-First Inventories");
            Map inventoryFirst = CONFIG.getMap("Inventory-First Inventories");
            Map blackList = CONFIG.getMap("Blacklisted Inventories");

            if (playerFirst != null && playerFirst.containsKey("Inventories")) {
                if (((List) playerFirst.get("Inventories")).contains(className)) {
                    return Permission.PLAYER_FIRST;
                }

            } else if (inventoryFirst != null && inventoryFirst.containsKey("Inventories")) {
                if (((List) inventoryFirst.get("Inventories")).contains(className)) {
                    return Permission.INVENTORY_FIRST;
                }

            } else if (blackList != null && blackList.containsKey("Inventories")) {
                if (((List) blackList.get("Inventories")).contains(className)) {
                    return Permission.BLACKLIST;
                }

            }

        } catch (Exception ignored) {}

        return Permission.PLAYER_FIRST;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean replaceItem(PlayerEntity playerEntity, Hand hand, ItemStack itemStack) {
        if (playerEntity.isCreative() || Minecraft.getInstance().player == null
                || !(Minecraft.getInstance().player.getUniqueID() == playerEntity.getUniqueID())) return false;

        Map config = CONFIG.getMap("Replace");
        if (config != null && config.containsKey("Replace") && !(boolean) config.get("Replace")) {
            return false;
        }

        try {
            Thread.sleep(50);
        } catch (Exception ignored) { }

        PlayerInventory inventory = playerEntity.inventory;
        int hotbarSize = 9;
        if (CONFIG.getConstant("Player Hotbar Size") != null) {
            hotbarSize = (int) CONFIG.getConstant("Player Hotbar Size");
        }

        int index = inventory.mainInventory.size() - 1;
        ItemStack item;
        if (itemStack != null) {
            item = itemStack;
        } else {
            item = playerEntity.getHeldItem(hand);
        }

        if (item.isDamageable() && item.getMaxDamage() - item.getDamage() <= 4) {
            // Getting all possible indexes for switching
            int bestSameItem = -1;
            int bestSameItemHealth = 0;
            int firstEmptyIndex = -1;
            int lastEmptyHotbar = -1;
            int lastNonItem = -1;
            int lastSafeItem = -1;

            for (int i = 0; i <= index; i++) {
                ItemStack currItemStack = inventory.mainInventory.get(i);

                if (currItemStack.getItem().toString().equals("air")) {
                    if (i < hotbarSize) {
                        lastEmptyHotbar = i;
                    } else {
                        if (firstEmptyIndex == -1) {
                            firstEmptyIndex = i;
                        }
                    }

                    continue;
                }

                if (currItemStack.isDamageable()) {
                    if (currItemStack.getMaxDamage() - currItemStack.getDamage() > 4) {
                        if (currItemStack.getItem() == item.getItem() &&
                                currItemStack.getMaxDamage() - currItemStack.getDamage() > bestSameItemHealth) {
                            bestSameItem = i;
                            bestSameItemHealth = currItemStack.getMaxDamage() - currItemStack.getDamage();
                        } else {
                            lastSafeItem = i;
                        }
                    }
                } else {
                    lastNonItem = i;
                }
            }

            if (bestSameItem != -1) {
                index = bestSameItem;
            } else if (firstEmptyIndex != -1) {
                index = firstEmptyIndex;
            } else if (lastEmptyHotbar != -1) {
                index = lastEmptyHotbar;
            } else if (lastNonItem != -1) {
                index = lastNonItem;
            } else if (lastSafeItem != -1) {
                index = lastSafeItem;
            }

        } else if (item.isDamageable()) {
            return false;

        } else {
            if (item.getCount() > 1) return false;

            int finalIndex = -1;
            int count = 0;

            for (int i = 0; i <= index; i++) {
                ItemStack currItemStack = inventory.mainInventory.get(i);

                if (currItemStack.getItem() == item.getItem() && currItemStack.getCount() >= count) {
                    finalIndex = i;
                    count = currItemStack.getCount();
                }
            }

            if (finalIndex == -1) return false;
            index = finalIndex;
        }

        LOGGER.debug(String.format("[%s] Sending network item replace packet.", ExampleMod.NAME));
        Channel.INSTANCE.sendToServer(new ItemReplacePacket(hand, index));

        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean moveItem(ArrayList<ItemStack> oldInventory, ArrayList<ItemStack> newInventory,
                                int oldIndex, int number, boolean oldToNew) {
        number = Math.abs(number);
        int notMoved = number;

        CompoundNBT tag = oldInventory.get(oldIndex).getTag();
        if (tag == null) tag = new CompoundNBT();
        tag.putInt("Custom-ID-Tag", Item.getIdFromItem(oldInventory.get(oldIndex).getItem()));

        if (oldToNew) {
            int emptyIndex = - 1;

            for (int index = 0; index < newInventory.size(); index++) {
                ItemStack newItem = newInventory.get(index);
                if (newItem.getCount() == newItem.getMaxStackSize()) continue;
                if (newItem.getItem().toString().equals("air")) {
                    if (emptyIndex == -1) {
                        emptyIndex = index;
                    }

                    continue;
                }

                CompoundNBT newTag = newItem.getTag();
                if (newTag == null) newTag = new CompoundNBT();
                newTag.putInt("Custom-ID-Tag", Item.getIdFromItem(newItem.getItem()));

                if (newTag.equals(tag)) {
                    int itemDif = newItem.getMaxStackSize() - newItem.getCount();

                    if (notMoved > itemDif) {
                        newItem.setCount(newItem.getMaxStackSize());
                        newInventory.set(index, newItem.copy());

                        notMoved -= itemDif;

                    } else {
                        newItem.setCount(newItem.getCount() + notMoved);
                        newInventory.set(index, newItem.copy());

                        notMoved = 0;
                        break;
                    }
                }
            }

            if (notMoved > 0 && emptyIndex != -1) {
                Item item = Item.getItemById(tag.getInt("Custom-ID-Tag"));
                tag.remove("Custom-ID-Tag");
                if (tag.isEmpty()) {
                    tag = null;
                }

                ItemStack itemStack = new ItemStack(item, notMoved);
                itemStack.setTag(tag);

                newInventory.set(emptyIndex, itemStack);

                if (tag == null) tag = new CompoundNBT();
                tag.putInt("Custom-ID-Tag", Item.getIdFromItem(item));

                notMoved = 0;
            }

        } else {
            if (oldInventory.get(oldIndex).getCount() == oldInventory.get(oldIndex).getMaxStackSize()) return false;

            for (int index = newInventory.size() - 1; index >= 0; index--) {
                ItemStack newItem = newInventory.get(index);
                if (newItem.getItem().toString().equals("air")) continue;

                CompoundNBT newTag = newItem.getTag();
                if (newTag == null) newTag = new CompoundNBT();
                newTag.putInt("Custom-ID-Tag", Item.getIdFromItem(newItem.getItem()));

                if (newTag.equals(tag)) {
                    if (newItem.getCount() < notMoved) {
                        notMoved -= newItem.getCount();
                        newInventory.remove(index);
                    } else {
                        newItem.setCount(newItem.getCount() - notMoved);
                        newInventory.set(index, newItem.copy());

                        notMoved = 0;
                        break;
                    }
                }
            }
        }

        if (number != notMoved) {
            Item item = Item.getItemById(tag.getInt("Custom-ID-Tag"));
            tag.remove("Custom-ID-Tag");
            if (tag.isEmpty()) {
                tag = null;
            }

            ItemStack itemStack = new ItemStack(item);
            itemStack.setTag(tag);

            if (oldToNew) {
                itemStack.setCount(oldInventory.get(oldIndex).getCount() - (number - notMoved));
            } else {
                itemStack.setCount(oldInventory.get(oldIndex).getCount() - (notMoved - number));
            }

            oldInventory.set(oldIndex, itemStack);

            return true;

        } else {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static int getKeyBind() {
        int bind = 82;
        try {
            Map sortSettings = CONFIG.getSortSettings();
            if (sortSettings != null) {
                Map keySettings = Config.getMap("Keybind", sortSettings);
                if (keySettings != null && keySettings.containsKey("Keybind")) {
                    bind = (int) keySettings.get("Keybind");
                }
            }

        } catch (Exception ignored) {}

        return bind;
    }

    @OnlyIn(Dist.CLIENT)
    public static void moveUnknownInv(Slot slot, int count, MoveType moveType) {
        ArrayList<ItemStack> ext;
        ArrayList<ItemStack> inv;
        ClientPlayerEntity player = Minecraft.getInstance().player;

        if (player == null) return;

        int hotbarSize = 9;
        if (CONFIG.getConstant("Player Hotbar Size") != null) {
            hotbarSize = (int) CONFIG.getConstant("Player Hotbar Size");
        }

        int size = 0;
        for (Slot containerSlot: player.openContainer.inventorySlots) {
            if (containerSlot.inventory instanceof PlayerInventory) {
                continue;
            }

            size++;
        }

        if (moveType == MoveType.MOVE_STACK) {
            count = slot.getSlotStackLimit();
        }

        if (moveType == MoveType.MOVE_ALL_TYPE) {
            // Move all items of the same type between two different inventories
            if (player.openContainer instanceof CreativeScreen.CreativeContainer ||
                    player.openContainer instanceof PlayerContainer) return;

            ext = new ArrayList<>(
                    player.openContainer.getInventory().subList(0, size)
            );
            inv = new ArrayList<>(
                    player.inventory.mainInventory
            );

            boolean toSend;

            if (slot.inventory instanceof PlayerInventory) {
                // Move from player to external
                toSend = Functions.moveAllItems(inv, ext, slot.getStack().getItem());
            } else {
                // Move from external to player
                toSend = Functions.moveAllItems(ext, inv, slot.getStack().getItem());
            }

            if (toSend) {
                LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                Channel.INSTANCE.sendToServer(new SortPacket(inv, 0));
                Channel.INSTANCE.sendToServer(new SortPacket(ext, 0, ext.size()));
            }

        } else if (moveType == MoveType.MOVE_ALL) {
            // Move all items between two different inventories
            if (player.openContainer == player.container
                    || player.openContainer instanceof CreativeScreen.CreativeContainer) return;

            ext = new ArrayList<>(
                    player.openContainer.getInventory().subList(0, size)
            );
            inv = new ArrayList<>(
                    player.inventory.mainInventory
            );

            boolean toSend;

            if (slot.inventory instanceof PlayerInventory) {
                // Move from player to external
                toSend = Functions.moveAllItems(inv, ext, null);
            } else {
                // Move from external to player
                toSend = Functions.moveAllItems(ext, inv, null);
            }

            if (toSend) {
                LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                Channel.INSTANCE.sendToServer(new SortPacket(inv, 0));
                Channel.INSTANCE.sendToServer(new SortPacket(ext, 0, ext.size()));
            }

        } else if (moveType == MoveType.MOVE_EMPTY) {
            // Move to an empty slot
            if (player.openContainer == player.container
                    || player.openContainer instanceof CreativeScreen.CreativeContainer) {
                Pair<ArrayList<ItemStack>, ArrayList<ItemStack>> temp = splitInventory(
                        player.inventory.mainInventory, hotbarSize
                );

                ext = temp.getKey(); // Hotbar
                inv = temp.getValue(); // Inventory

                boolean toSend;

                if (slot.getSlotIndex() < hotbarSize) {
                    toSend = findEmptySlot(slot.getSlotIndex(), ext, inv);

                } else {
                    toSend = findEmptySlot(slot.getSlotIndex() - hotbarSize, inv, ext);
                }

                if (toSend) {
                    LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                    ext.addAll(inv);
                    Channel.INSTANCE.sendToServer(new SortPacket(ext, 0));
                }

            } else {
                ext = new ArrayList<>(
                        player.openContainer.getInventory().subList(0, size)
                );
                inv = new ArrayList<>(
                        player.inventory.mainInventory
                );

                boolean toSend;

                if (slot.inventory instanceof PlayerInventory) {
                    toSend = findEmptySlot(slot.getSlotIndex(), inv, ext);
                } else {
                    toSend = findEmptySlot(slot.getSlotIndex(), ext, inv);
                }

                if (toSend) {
                    LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                    Channel.INSTANCE.sendToServer(new SortPacket(inv, 0));
                    Channel.INSTANCE.sendToServer(new SortPacket(ext, 0, ext.size()));
                }
            }

        } else if (player.openContainer == player.container
                || player.openContainer instanceof CreativeScreen.CreativeContainer) {
            // Move within inventory
            Pair<ArrayList<ItemStack>, ArrayList<ItemStack>> temp = splitInventory(
                    player.inventory.mainInventory, hotbarSize
            );

            ext = temp.getKey(); // Hotbar
            inv = temp.getValue(); // Inventory

            boolean toSend;

            if (slot.getSlotIndex() < hotbarSize) {
                toSend = Functions.moveItem(ext, inv, slot.getSlotIndex(), count, true);
            } else {
                toSend = Functions.moveItem(inv, ext, slot.getSlotIndex() - hotbarSize, count, true);
            }

            if (toSend) {
                LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                ext.addAll(inv);
                Channel.INSTANCE.sendToServer(new SortPacket(ext, 0));
            }

        } else {
            // Move between inventories
            ext = new ArrayList<>(
                    player.openContainer.getInventory().subList(0, size)
            );
            inv = new ArrayList<>(
                    player.inventory.mainInventory
            );

            boolean toSend;

            if (slot.inventory instanceof PlayerInventory) {
                toSend = Functions.moveItem(inv, ext, slot.getSlotIndex(), count, true);
            } else {
                toSend = Functions.moveItem(ext, inv, slot.getSlotIndex(), count, true);
            }

            if (toSend) {
                LOGGER.debug(String.format("[%s] Sending network sort packet for a shortcut.", ExampleMod.NAME));
                Channel.INSTANCE.sendToServer(new SortPacket(inv, 0));
                Channel.INSTANCE.sendToServer(new SortPacket(ext, 0, ext.size()));
            }

        }
    }

    private static boolean findEmptySlot(int slotIndex, ArrayList<ItemStack> origin, ArrayList<ItemStack> destination) {
        boolean updated = false;
        for (int i = 0; i < destination.size(); i++) {
            if (destination.get(i).getItem().toString().equals("air")) {
                updated = true;
                destination.set(i, origin.set(slotIndex, new ItemStack(Item.getItemById(0))));

                break;
            }
        }
        return updated;
    }

    private static ArrayList<ItemStack> orderInventory(ArrayList<ItemStack> inventory, ClientEventHandlers.Order order,
                                                       int width, int height) {
        if (inventory.isEmpty()) return inventory;

        if (order == ClientEventHandlers.Order.COLUMNS) {
            return splitRows(inventory, width, height, true);

        } else if (order == ClientEventHandlers.Order.ROWS) {
            return splitRows(inventory, width, height, false);

        } else {
            return inventory;
        }
    }

    private static void fillEmpty(ItemStack[][] array, ItemStack itemStack) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j <array[i].length; j++) {
                if (array[i][j] == null) {
                    array[i][j] = itemStack;
                    return;
                }
            }
        }
    }

    private static ArrayList<ItemStack> splitRows(ArrayList<ItemStack> inventory, int width, int height,
                                                  boolean vertical) {
        int longer = width;
        int shorter = height;

        if (vertical) {
            longer = height;
            shorter = width;
        }

        ItemStack[][] array = new ItemStack[longer][shorter];

        Item lastItem = inventory.get(0).getItem();

        boolean overflow = false;
        int row = 0;
        int col = 0;

        for (ItemStack itemStack: inventory) {
            if (!overflow && itemStack.getItem() == lastItem) {
                array[col][row] = itemStack;

                if (row + 1 < shorter) {
                    row++;

                } else if (col + 1 < longer) {
                    row = 0;
                    col++;

                } else {
                    overflow = true;
                }

            } else if (!overflow) {
                if (col + 1 < longer && row != 0) {
                    col++;
                    row = 0;

                    array[col][row] = itemStack;

                } else if (row == 0) {
                    array[col][row] = itemStack;

                } else {
                    overflow = true;
                    fillEmpty(array, itemStack);
                }

                row++;
                lastItem = itemStack.getItem();

            } else {
                // Overflow protection code
                fillEmpty(array, itemStack);
            }
        }

        ArrayList<ItemStack> sorted = new ArrayList<>();
        ItemStack filler = new ItemStack(Item.getItemById(0));

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (vertical) {
                    if (array[i][j] == null) {
                        sorted.add(filler);
                    } else {
                        sorted.add(array[i][j]);
                    }
                } else {
                    if (array[j][i] == null) {
                        sorted.add(filler);
                    } else {
                        sorted.add(array[j][i]);
                    }
                }
            }
        }

        return sorted;
    }

    private static void switchItems(int oldIndex, int newIndex, ArrayList<ItemStack> list) {
        ItemStack old = list.get(oldIndex);
        list.set(oldIndex, list.get(newIndex));
        list.set(newIndex, old);
    }

    private static ArrayList<ItemStack> sortLogic(ArrayList<ItemStack> inventory, Mode mode) {
        // TODO: Allow implementation of custom logic
        Map<CompoundNBT, Integer> items = new LinkedHashMap<>();

        for (ItemStack itemStack: inventory) {
            CompoundNBT plain = itemStack.getTag();
            if (plain == null) {
                plain = new CompoundNBT();
            }

            if (plain.contains("Count")) {
                plain.remove("Count");
            }

            plain.putInt("Custom-ID-Tag", Item.getIdFromItem(itemStack.getItem()));

            if (items.containsKey(plain)) {
                items.replace(plain, items.get(plain) + itemStack.getCount());
            } else {
                items.put(plain, itemStack.getCount());
            }
        }

        ArrayList<ItemStack> sorted = new ArrayList<>();

        for (CompoundNBT nbt : items.keySet()) {
            int count = items.get(nbt);
            Item item = Item.getItemById(nbt.getInt("Custom-ID-Tag"));

            nbt.remove("Custom-ID-Tag");
            if (nbt.isEmpty()) {
                nbt = null;
            }

            ItemStack itemStack = new ItemStack(item);
            itemStack.setTag(nbt);
            itemStack.setCount(itemStack.getMaxStackSize());

            if (count == 1 || count < itemStack.getMaxStackSize()) {
                ItemStack toAdd = new ItemStack(item, count, nbt);
                toAdd.setTag(nbt);

                sorted.add(toAdd);
                continue;
            }

            int maxStacks = Math.floorDiv(count, itemStack.getMaxStackSize());
            for (int i = 0; i < maxStacks; i++) {
                sorted.add(itemStack.copy());
                count -= itemStack.getMaxStackSize();
            }

            if (count > 0) {
                ItemStack toAdd = new ItemStack(item, count);
                toAdd.setTag(nbt);
                sorted.add(toAdd);
            }
        }

        if (mode == Mode.DEFAULT) {
            // Alphabet mode sorts everything alphabetically
            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (sorted.get(i).getItem() == sorted.get(j).getItem()) {
                        if (sorted.get(i).getCount() < sorted.get(j).getCount()) {
                            switchItems(i, j, sorted);

                        } else if (sorted.get(i).isDamageable() && sorted.get(j).isDamageable()
                                && sorted.get(i).getDamage() > sorted.get(j).getDamage()) {
                            switchItems(i, j, sorted);
                        }

                    } else {
                        if (sorted.get(i).getDisplayName().getUnformattedComponentText()
                                .compareTo(sorted.get(j).getDisplayName().getUnformattedComponentText()) > 0) {
                            switchItems(i, j, sorted);
                        }
                    }
                }
            }

        } else if (mode == Mode.COMPACT) {
            ArrayList<ItemStack> tempSorted = new ArrayList<>();

            for (ItemStack originalItem: inventory) {
                for (ItemStack sortedItem: sorted) {
                    if (originalItem.getItem() == sortedItem.getItem()) {
                        tempSorted.add(sortedItem);
                        sorted.remove(sortedItem);
                        break;
                    }
                }
            }

            tempSorted.addAll(sorted);
            sorted = tempSorted;
        }

        return sorted;
    }

    @OnlyIn(Dist.CLIENT)
    private static Mode getSortOrder() {
        Mode sortOrder;
        try {
            Map sortMethod = Config.getMap("Sort Method", CONFIG.getSortSettings());
            if (sortMethod != null && sortMethod.containsKey("Alphabetical") &&
                    (boolean) sortMethod.get("Alphabetical")) {
                sortOrder = Mode.DEFAULT;
            } else if (sortMethod != null && sortMethod.containsKey("Compact") &&
                    (boolean) sortMethod.get("Compact")) {
                sortOrder = Mode.COMPACT;
            } else sortOrder = Mode.NONE;

        } catch (Exception error) {
            LOGGER.warn(String.format("[%s] Config error, could not find sort method, assuming default.",
                    ExampleMod.NAME));
            sortOrder = Mode.DEFAULT;
        }

        return sortOrder;
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean sortCleanup(ArrayList<ItemStack> inventory, int maxSize) {
        // Dumping inventory items (in case of error)
        Map backups = CONFIG.getMap("Backups");
        if (backups != null) {
            Object backupToggle = backups.get("Backups");
            if (backupToggle != null && (boolean) backupToggle ) {
                try {
                    Backup.backup(backups, inventory);
                } catch (IOException error) {
                    LOGGER.error(String.format("[%s] Sorting backup error: (%s) - %s", ExampleMod.NAME,
                            error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
                    return false;
                }
            }
        }

        // In case all items can't be stored within inventory
        if (inventory.size() > maxSize) {
            ArrayList<ItemStack> overflow = new ArrayList<>(
                    inventory.subList(maxSize, inventory.size())
            );

            LOGGER.warn(String.format("[%s] Sending overflow packet.", ExampleMod.NAME));
            Channel.INSTANCE.sendToServer(new OverFlowPacket(overflow));
        }

        return true;
    }

    private static ArrayList<ItemStack> excludeHotbar(List<ItemStack> inventory, int hotbarSize) {
        ArrayList<ItemStack> excluded = new ArrayList<>();

        int index = 0;
        for (ItemStack item: inventory) {
            if (index < hotbarSize) {
                // Excluding hotbar items
                index++;
                continue;
            }

            if (!item.getItem().toString().equals("air")) {
                excluded.add(item);
            }
        }

        return excluded;
    }

    private static Pair<ArrayList<ItemStack>, ArrayList<ItemStack>> splitInventory(List<ItemStack> inventory,
                                                                                   int hotbarSize) {
        return new Pair<>(new ArrayList<>(inventory.subList(0, hotbarSize)),
                new ArrayList<>(inventory.subList(hotbarSize, inventory.size())));
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean moveAllItems(ArrayList<ItemStack> origin, ArrayList<ItemStack> newInv, Item item) {
        boolean toSend = false;

        if (item == null) {
            for (int index = 0; index < origin.size(); index++) {
                if (origin.get(index).getItem().toString().equals("air")) continue;

                if (Functions.moveItem(origin, newInv, index, origin.get(index).getCount(), true)) {
                    toSend = true;
                } else {
                    break;
                }
            }

        } else {
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < origin.size(); i++) {
                if (origin.get(i).getItem() == item) {
                    indexes.add(i);
                }
            }


            for (int index: indexes) {
                if (Functions.moveItem(origin, newInv, index, origin.get(index).getCount(), true)) {
                    toSend = true;
                } else {
                    break;
                }
            }
        }

        return toSend;
    }

    /**
     * The general sorting function.
     *
     * @param playerInventory Whether to sort player inventory or other inventory.
     * @param order The structure the items should be set in.
     * @param container The container to perform the sort on. Leave as null for player sorting.
     * @return Success of the operation.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean sort(boolean playerInventory, ClientEventHandlers.Order order, ContainerScreen container) {
        // Items to be sent to sort
        ArrayList<ItemStack> toSend = new ArrayList<>();
        int maxSize;

        // Determining Inventory
        if (playerInventory) {
            // Getting inventory to sort
            ClientPlayerEntity player = Minecraft.getInstance().player;

            if (player == null) {
                LOGGER.info(String.format("[%s] Canceled sort due to invalid player.", ExampleMod.NAME));
                return false;
            }

            NonNullList<ItemStack> invArray = player.inventory.mainInventory;

            // Stop if inventory is empty
            if (invArray.isEmpty()) {
                return true;
            }

            if (player.world.isRemote) {
                int hotbarSize = 9;
                if (CONFIG.getConstant("Player Hotbar Size") != null) {
                    hotbarSize = (int) CONFIG.getConstant("Player Hotbar Size");
                }

                // TODO: add skiphotbar option
                boolean skipHotbar = true;
                if (CONFIG.getConstant("a") != null) {
                    skipHotbar = false;
                }

                if (skipHotbar) {
                    toSend = excludeHotbar(invArray, hotbarSize);

                } else {
                    for (ItemStack item: invArray) {
                        if (!item.getItem().toString().equals("air")) {
                            toSend.add(item);
                        }
                    }
                }

                // Sorting the array
                Mode sortOrder = getSortOrder();

                if (CONFIG.getConstant("Player Inventory Width") != null &&
                        CONFIG.getConstant("Player Inventory Height") != null) {
                    toSend = orderInventory(sortLogic(toSend, sortOrder), order,
                            (int) CONFIG.getConstant("Player Inventory Width"),
                            (int) CONFIG.getConstant("Player Inventory Height"));
                } else {
                    toSend = sortLogic(toSend, sortOrder);
                }

                maxSize = player.inventory.mainInventory.size();

                LOGGER.debug(String.format("[%s] Sending network sort packet", ExampleMod.NAME));
                Channel.INSTANCE.sendToServer(new SortPacket(toSend, hotbarSize));

            } else {
                LOGGER.warn(String.format("[%s] Client side sort code detected on the logical server. Aborting.",
                        ExampleMod.NAME));
                return false;

            }

        } else {
            if (container.isPauseScreen()) return true;

            int size = 0;
            for (Slot slot: container.getContainer().inventorySlots) {
                if (slot.inventory instanceof PlayerInventory) {
                    continue;
                }

                if (!slot.getStack().getItem().toString().equals("air")) {
                    toSend.add(slot.getStack());
                }

                size++;

            }

            maxSize = size;

            // Stop if inventory is empty
            if (toSend.isEmpty()) {
                return true;
            }

            if (Objects.requireNonNull(container.getMinecraft().world).isRemote) {
                // Sorting the array
                Mode sortOrder = getSortOrder();

                boolean defaultSort = true;

                if (CONFIG.getConstant(container.getClass().getName()) != null) {
                    // Custom Registered Inventories
                    defaultSort = false;

                    Map options = null;
                    try {
                        options = (Map) CONFIG.getConstant(container.getClass().getName());
                    } catch (Exception ignored) {
                        LOGGER.debug(String.format("[%s] Could not read options for: %s",
                                ExampleMod.NAME, container.getClass().getName()));
                        defaultSort = true;
                    }

                    if (options != null && options.containsKey("width") && options.containsKey("height")) {
                        toSend = orderInventory(sortLogic(toSend, sortOrder), order,
                                (int) options.get("width"), (int) options.get("height"));

                        LOGGER.debug(String.format("[%s] Sending network sort packet", ExampleMod.NAME));
                        if (options.containsKey("protectedIndex")) {
                            maxSize -= (int) options.get("protectedIndex");
                            Channel.INSTANCE.sendToServer(new SortPacket(toSend,
                                    (int) options.get("protectedIndex"), maxSize));

                        } else {
                            Channel.INSTANCE.sendToServer(new SortPacket(toSend, 0, maxSize));
                        }

                    } else {
                        // No custom width or height, letting default handler take over
                        defaultSort = true;
                    }
                }

                if (defaultSort) {
                    if (CONFIG.getConstant("Default Chest Width") != null) {
                        toSend = orderInventory(sortLogic(toSend, sortOrder), order,
                                (int) CONFIG.getConstant("Default Chest Width"),
                                maxSize / (int) CONFIG.getConstant("Default Chest Width"));
                    } else {
                        toSend = sortLogic(toSend, sortOrder);
                    }

                    LOGGER.debug(String.format("[%s] Sending network sort packet", ExampleMod.NAME));
                    Channel.INSTANCE.sendToServer(new SortPacket(toSend, 0, maxSize));
                }

            } else {
                LOGGER.warn(String.format("[%s] Client side sort code detected on the logical server. Aborting.",
                        ExampleMod.NAME));
                return false;
            }
        }

        return sortCleanup(toSend, maxSize);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean playerSort(NonNullList<ItemStack> itemList) {
        // Items to be sent to sort
        ArrayList<ItemStack> toSend;
        int maxSize = itemList.size();

        // Getting inventory to sort
        ClientPlayerEntity player = Minecraft.getInstance().player;

        if (player == null) {
            LOGGER.info(String.format("[%s] Canceled sort due to invalid player.", ExampleMod.NAME));
            return false;
        }

        // Stop if inventory is empty
        if (itemList.isEmpty()) {
            return true;
        }

        if (player.world.isRemote) {
            int hotbarSize = 9;
            if (CONFIG.getConstant("Player Hotbar Size") != null) {
                hotbarSize = (int) CONFIG.getConstant("Player Hotbar Size");
            }

            toSend = excludeHotbar(itemList, hotbarSize);

            // Sorting the array
            Mode sortOrder = getSortOrder();
            toSend = sortLogic(toSend, sortOrder);

            LOGGER.debug(String.format("[%s] Sending network sort packet", ExampleMod.NAME));
            Channel.INSTANCE.sendToServer(new SortPacket(toSend, hotbarSize));

        } else {
            LOGGER.warn(String.format("[%s] Client side sort code detected on the logical server. Aborting.",
                    ExampleMod.NAME));
            return false;

        }

        return sortCleanup(toSend, maxSize);
    }

    /**
     * Refills a player's held item if possible.
     *
     * @param playerEntity The player to refill.
     * @param item The item which should be refilled.
     * @param hand Hand that will be refilled.
     * @return The result of the operations. 0 Represents a successful refill, 1 represents no refill without error,
     * 2 represents an error.
     */
    @OnlyIn(Dist.CLIENT)
    public static int refill(ClientPlayerEntity playerEntity, Item item, Hand hand) {
        try {
            Thread.sleep(50);
            /* I am ashamed of this
            Explanation: For some reason, attempting to access the current held item can sometimes give you
            the slot contents right before the block gets placed, and sometimes after. This means if you are on
            your last item, the function can return the slot as containing air, or 1 of the item.

            There does not appear to be any inherent order to it, so the next best thing is to wait a bit.
            This seems to be a bit more reliable. 50ms is used because its slow enough to not have unexpected
            results, but also quick enough it's barely noticeable for the client, and doesn't have any negative effects
            on the game.

            One possible workaround is asking the server for the information, as it appears to be more reliable.
            (It is possible the apparent reliability is simply due to delay).
            Just waiting seems more efficient, so that's what I'll settle on for now.
            */

            ItemStack heldItem = playerEntity.getHeldItem(hand);

            if (heldItem.getItem().toString().equals("air")) {
                // Finding all instances of the item
                ArrayList<Integer> slots = new ArrayList<>();

                for (int i = playerEntity.inventory.mainInventory.size() - 1; i >= 0; i--) {
                    if (playerEntity.inventory.getStackInSlot(i).getItem() == item &&
                            playerEntity.inventory.getStackInSlot(i) != playerEntity.getHeldItem(hand)) {
                        slots.add(i);
                    }
                }

                if (slots.size() == 0) {
                    // No items found to refill
                    return 1;
                }

                if (playerEntity.world.isRemote) {
                    LOGGER.debug(String.format("[%s] Sending network refill packet", ExampleMod.NAME));
                    Channel.INSTANCE.sendToServer(new RefillPacket(hand, slots.get(0)));

                } else {
                    LOGGER.warn(String.format("[%s] Client side refill code detected on the logical server. Aborting.",
                            ExampleMod.NAME));
                    return 2;
                }

                // Refill Sent
                return 0;

            }

            return 1; // Hand not empty, don't need to refill

        } catch (Exception error) {
            LOGGER.warn(String.format("[%s] Block refill Error: %s - %s", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            return 2;
        }
    }
}