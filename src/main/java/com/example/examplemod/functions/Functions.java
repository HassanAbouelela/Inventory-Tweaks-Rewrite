/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.functions;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.events.ClientEventHandlers;
import com.example.examplemod.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.examplemod.ExampleMod.CONFIG;

/**
 * Class for general helper methods.
 */
public class Functions {
    /**
     * Event Logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The permission level of a given class for sorting purposes.
     */
    public enum Permission {
        /** The player inventory takes priority while sorting. */ PLAYER_FIRST,
        /** The open inventory takes priority while sorting. */ INVENTORY_FIRST,
        /** Inventories should not be sorted. */ BLACKLIST
    }

    /**
     * The amount of items to move between inventories.
     */
    public enum MoveType {
        /** Move full inventory. */ MOVE_ALL,
        /** Move all items of the same type. */ MOVE_ALL_TYPE,
        /** Move the all items at the slot. */ MOVE_STACK,
        /** Move the given amount. */ MOVE_INT,
        /** Move slot to an empty slot. */ MOVE_EMPTY
    }

    /**
     * The sorting logic to use.
     */
    private enum Mode {
        /*** Alphabetical sorting. */ DEFAULT,
        /** Combine ItemStacks without rearranging. */ COMPACT,
        /** Doesn't perform any logic. Returns unchanged. */ NONE
    }

    /**
     * Calculate the order items should be sorted on, based on the last time sorting happened, and if
     * sorting changes with clicks.
     *
     * @param lastSortTime The last time sorting happened.
     * @return The order to be used while sorting.
     */
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

    /**
     * Get the permission level of an inventory's class for sorting purposes.
     *
     * @param className The name of the class of the inventory.
     * @return The permission level.
     */
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

    /**
     * Replace the item held by a player.
     *
     * @param playerEntity The player to perform the action on.
     * @param hand Main hand, or secondary hand.
     * @param itemStack The ItemStack to place.
     *
     * @return The success of the operation.
     */
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

                if (isAir(currItemStack)) {
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

    /**
     * Perform one craft. Used for "craft one item" shortcut.
     *
     * @param craftResult The item produced by the craft.
     * @param player The player to craft items to.
     *
     * @return The success of the operation.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean craftItem(ItemStack craftResult, PlayerEntity player) {
        ArrayList<ItemStack> playerInventory = new ArrayList<>(player.inventory.mainInventory);
        ArrayList<Integer> airIndexes = new ArrayList<>();

        int remaining = craftResult.getCount();

        for (int index = 0; index < playerInventory.size(); index++) {
            ItemStack itemStack = playerInventory.get(index);
            if (itemStack.getCount() == itemStack.getMaxStackSize() || itemStack.getItem() != craftResult.getItem()
                    || itemStack.getTag() != craftResult.getTag()) {
                if (isAir(itemStack)) airIndexes.add(index);
                continue;
            }

            int delta = itemStack.getMaxStackSize() - itemStack.getCount();

            if (delta >= remaining) {
                // Move All
                itemStack.setCount(itemStack.getCount() + remaining);
                playerInventory.set(index, itemStack);

                remaining = 0;
                break;

            } else if (delta > 0) {
                itemStack.setCount(itemStack.getMaxStackSize());
                playerInventory.set(index, itemStack);

                remaining -= delta;
            }
        }

        if (remaining != 0 && !airIndexes.isEmpty() && airIndexes.size() * craftResult.getMaxStackSize() >= remaining) {
            // Can fit remaining items into empty slots
            int index = 0;
            craftResult.setCount(craftResult.getMaxStackSize());

            while (remaining >= craftResult.getMaxStackSize()) {
                playerInventory.set(airIndexes.get(index++), craftResult.copy());
                remaining -=  craftResult.getMaxStackSize();
            }

            if (remaining != 0) {
                craftResult.setCount(remaining);
                playerInventory.set(airIndexes.get(index), craftResult);

                remaining = 0;
            }
        }

        if (remaining == 0) {
            Channel.INSTANCE.sendToServer(new SortPacket(playerInventory, 0));
            return true;
        }

        return false;
    }

    /**
     * Craft as many items as possible.
     *
     * @param craftingItemStacks The materials required for the craft.
     * @param craftResult The item produced by the craft.
     * @param player The player to craft items to.
     */
    @OnlyIn(Dist.CLIENT)
    public static void craftAllInventory(List<ItemStack> craftingItemStacks, ItemStack craftResult,
                                         PlayerEntity player) {
        ArrayList<ItemStack> playerInventory = new ArrayList<>(player.inventory.mainInventory);

        ArrayList<Integer> airIndexes = new ArrayList<>();
        ArrayList<Integer> resultIndexes = new ArrayList<>();

        HashMap<Item, Integer> itemCount = new HashMap<>();
        HashMap<Item, Integer> uniqueCraftingItems = new HashMap<>();

        for (ItemStack craftingItem: craftingItemStacks) {
            // Get all unique items, and initialize counting map.
            if (isAir(craftingItem)) continue;
            if (itemCount.containsKey(craftingItem.getItem())) {
                itemCount.replace(craftingItem.getItem(),
                        itemCount.get(craftingItem.getItem()) + craftingItem.getCount());
            } else {
                itemCount.put(craftingItem.getItem(), craftingItem.getCount());
            }

            if (uniqueCraftingItems.containsKey(craftingItem.getItem())) {
                uniqueCraftingItems.replace(craftingItem.getItem(), uniqueCraftingItems.get(craftingItem.getItem()) + 1);
            } else {
                uniqueCraftingItems.put(craftingItem.getItem(), 1);
            }
        }

        for (int index = 0; index < playerInventory.size(); index++) {
            // Get index of all empty slots and slots that can take the result.
            // Get total of items for each item in craftingItems.
            ItemStack itemStack = playerInventory.get(index);

            if (isAir(itemStack)) {
                airIndexes.add(index);
            } else if (uniqueCraftingItems.containsKey(itemStack.getItem())) {
                Item item = itemStack.getItem();
                itemCount.replace(item, itemCount.get(item) + itemStack.getCount());

                playerInventory.set(index, ExampleMod.AIR.copy());
                airIndexes.add(index);
            } else if (itemStack.getItem() == craftResult.getItem()) {
                resultIndexes.add(index);
            }
        }

        // Calculate how many crafts can be done.
        int maxCraft = 0;
        for (Item item: itemCount.keySet()) {
            // Maximum items in relation to item required per craft
            int max = Math.floorDiv(itemCount.get(item), uniqueCraftingItems.get(item));

            if (maxCraft == 0 && max != 0) {
                maxCraft =  max;
            } else if (max < maxCraft) {
                maxCraft = max;
            }
        }
        int craftedItems = maxCraft * craftResult.getCount();

        // Record how many items left
        for (Item item: itemCount.keySet()) {
            int itemsLeft = itemCount.get(item) - (maxCraft * uniqueCraftingItems.get(item));
            itemCount.replace(item, itemsLeft);
        }

        // Fit as much input items as possible into the inventory and crafting grid
        ArrayList<ItemStack> updatedCraftingGrid = new ArrayList<>();
        for (Item item: itemCount.keySet()) {
            int count = itemCount.get(item);
            ItemStack itemStack = new ItemStack(item);

            if (count == 0) continue;

            // Fit into crafting grid.
            for (int i = 0; i < uniqueCraftingItems.get(item); i++) {
                if (count >= itemStack.getMaxStackSize()) {
                    itemStack.setCount(itemStack.getMaxStackSize());
                    updatedCraftingGrid.add(itemStack.copy());

                    count -= itemStack.getMaxStackSize();
                } else {
                    itemStack.setCount(count);
                    updatedCraftingGrid.add(itemStack.copy());

                    count = 0;
                }

                if (count == 0) break;
            }

            // Fit into inventory
            while (count > 0) {
                if (count >= itemStack.getMaxStackSize()) {
                    itemStack.setCount(itemStack.getMaxStackSize());
                    count -= itemStack.getMaxStackSize();
                } else {
                    itemStack.setCount(count);
                    count = 0;
                }
                if (airIndexes.size() != 0) {
                    playerInventory.set(airIndexes.remove(0), itemStack.copy());
                }
            }

            itemCount.replace(item, count);
        }

        // Fit as many craft results as possible into the inventory
        for (int index: resultIndexes) {
            ItemStack invItem = playerInventory.get(index);
            int delta = invItem.getMaxStackSize() - invItem.getCount();
            if (craftedItems >= delta) {
                invItem.setCount(invItem.getMaxStackSize());
                craftedItems -= delta;
            } else {
                invItem.setCount(invItem.getCount() + craftedItems);
                craftedItems = 0;
            }

            playerInventory.set(index, invItem);

            if (craftedItems == 0) {
                break;
            }
        }
        ArrayList<ItemStack> overFlow = new ArrayList<>();

        while (craftedItems > 0) {
            if (craftedItems >= craftResult.getMaxStackSize()) {
                craftResult.setCount(craftResult.getMaxStackSize());
                craftedItems -= craftResult.getMaxStackSize();
            } else {
                craftResult.setCount(craftedItems);
                craftedItems = 0;
            }
            if (airIndexes.size() > 0) {
                playerInventory.set(airIndexes.remove(0), craftResult.copy());
            } else {
                overFlow.add(craftResult.copy());
            }
        }

        // Update Empty Spots
        while (updatedCraftingGrid.size() < craftingItemStacks.size()) {
            updatedCraftingGrid.add(ExampleMod.AIR.copy());
        }

        // Send inventory and crafting grid to server
        Channel.INSTANCE.sendToServer(new SortPacket(playerInventory, 0));
        Channel.INSTANCE.sendToServer(new OptimizationPacket(updatedCraftingGrid));

        // Dump any items that didn't fit
        for (Item item: itemCount.keySet()) {
            int count = itemCount.get(item);
            if (count > 0) {
                ItemStack result = new ItemStack(item);
                while (count > 0) {
                    if (count >= result.getMaxStackSize()) {
                        result.setCount(result.getMaxStackSize());
                        count -= result.getMaxStackSize();
                    } else {
                        result.setCount(count);
                        count = 0;
                    }

                    overFlow.add(result.copy());
                }

                itemCount.replace(item, 0);
            }
        }

        Channel.INSTANCE.sendToServer(new OverFlowPacket(overFlow));
    }

    /**
     * Helper method to move an ItemStack between two inventories.
     *
     * @param oldInventory The initial inventory.
     * @param newInventory The target inventories.
     * @param oldIndex The index of the item.
     * @param number The number of items to move.
     * @param oldToNew The direction to move items in.
     *
     * @return The success of the operation.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean moveItem(ArrayList<ItemStack> oldInventory, ArrayList<ItemStack> newInventory,
                                   int oldIndex, int number, boolean oldToNew) {
        number = Math.abs(number);
        int notMoved = number;

        if (oldIndex >= oldInventory.size()) {
            LOGGER.warn(String.format("[%s] Move item called outside of inventory.", ExampleMod.NAME));
            return true;
        }

        CompoundNBT tag = oldInventory.get(oldIndex).getTag();
        if (tag == null) tag = new CompoundNBT();
        tag.putInt("Custom-ID-Tag", Item.getIdFromItem(oldInventory.get(oldIndex).getItem()));

        if (oldToNew) {
            int emptyIndex = - 1;

            for (int index = 0; index < newInventory.size(); index++) {
                ItemStack newItem = newInventory.get(index);
                if (newItem.getCount() == newItem.getMaxStackSize()) continue;
                if (isAir(newItem)) {
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
                if (isAir(newItem)) continue;

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

    /**
     * Helper method to get the sort key bind from the config.
     *
     * @return The current key bind or 82 (R).
     */
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

    /**
     * Move items between unknown inventories.
     * Try to move an item from a player inventory to an open-container, or the other way around.
     * If the open container is the player inventory, moves items withing the inventory.
     *
     * @param slot The slot to move items from.
     * @param count The number of items to move.
     * @param moveType The logic to use while performing the operation.
     */
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
            count = slot.getStack().getCount();
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
                Map.Entry<ArrayList<ItemStack>, ArrayList<ItemStack>> temp = splitInventory(
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
            Map.Entry<ArrayList<ItemStack>, ArrayList<ItemStack>> temp = splitInventory(
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

    /**
     * Checks if an ItemStack is air.
     *
     * @param itemStack The ItemStack to check.
     * @return If the ItemStack is air.
     */
    public static boolean isAir(ItemStack itemStack) {
        return itemStack.getItem().toString().equals("air");
    }

    /**
     * Get the level of a particular tag if possible, else get 0.
     *
     * @param tagName The name of the tag as it appears in an NBT tag.
     * @param itemStack The ItemStack to look in for the tag.
     *
     * @return The level of the tag or 0.
     */
    static int getTagLevel(String tagName, ItemStack itemStack) {
        if (itemStack.isEnchanted()) {
            Pattern pattern = Pattern.compile("(?<=lvl:)([0-9]+)");
            for (INBT nbt : itemStack.getEnchantmentTagList()) {
                if (nbt.getString().contains(tagName)) {
                    Matcher matcher = pattern.matcher(nbt.getString());

                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group());
                    }
                }
            }
        }

        return 0;
    }

    /**
     * Helper method to move an ItemStack from one inventory, to an empty slot in another.
     *
     * @param slotIndex The index of the item.
     * @param origin The origin inventories.
     * @param destination The destination inventories.
     *
     * @return Whether there was a change in the inventories.
     */
    private static boolean findEmptySlot(int slotIndex, ArrayList<ItemStack> origin, ArrayList<ItemStack> destination) {
        boolean updated = false;
        for (int i = 0; i < destination.size(); i++) {
            if (isAir(destination.get(i))) {
                updated = true;
                destination.set(i, origin.set(slotIndex, ExampleMod.AIR));

                break;
            }
        }
        return updated;
    }

    /**
     * Organizing inventory by given order.
     *
     * @param inventory The inventory to organize.
     * @param order The order to organize the items into.
     * @param width The width of the inventory.
     * @param height The height of the inventory.
     *
     * @return The sorted inventory.
     */
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

    /**
     * Helper method to replace null items.
     *
     * @param array The array to replace items within.
     * @param itemStack The ItemStack to place.
     */
    private static void fillEmpty(ItemStack[][] array, ItemStack itemStack) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j <array[i].length; j++) {
                if (array[i][j] == null) {
                    array[i][j] = itemStack.copy();
                    return;
                }
            }
        }
    }

    /**
     * Helper method to organize inventories into rows and columns.
     *
     * @param inventory The inventory to organize.
     * @param width The width of the inventory.
     * @param height The height of the inventory.
     * @param vertical Whether to group into rows or columns.
     *
     * @return The sorted inventory.
     */
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

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (vertical) {
                    if (array[i][j] == null) {
                        sorted.add(ExampleMod.AIR);
                    } else {
                        sorted.add(array[i][j]);
                    }
                } else {
                    if (array[j][i] == null) {
                        sorted.add(ExampleMod.AIR);
                    } else {
                        sorted.add(array[j][i]);
                    }
                }
            }
        }

        return sorted;
    }

    /**
     * Switch two items within an inventory.
     *
     * @param oldIndex The first item.
     * @param newIndex The second item.
     * @param list The inventory to organize.
     */
    private static void switchItems(int oldIndex, int newIndex, ArrayList<ItemStack> list) {
        ItemStack old = list.get(oldIndex);
        list.set(oldIndex, list.get(newIndex));
        list.set(newIndex, old);
    }

    /**
     * Helper method to sort an inventory.
     *
     * @param inventory The inventory to sort.
     * @param mode The mode to use while sorting.
     *
     * @return The sorted inventory.
     */
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

    /***
     * Helper method to get the sort order from the config.
     *
     * @return The sort order.
     */
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

    /**
     * Helper method to clean up after performing a sort.
     * Backs up the inventory, spawns any items that could not be fit into the world,
     * logs the event, and cleans up the backup folder.
     *
     * @param inventory The inventory to back up.
     * @param maxSize The maximum items that can fit within an inventory.
     *
     * @return The success of the operation.
     */
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

    /**
     * Exclude the first x items from a list. Used to remove hotbar items from an inventory.
     *
     * @param inventory The inventory to exclude the hotbar from.
     * @param hotbarSize The size of the hotbar.
     *
     * @return The new inventory without the hotbar.
     */
    private static ArrayList<ItemStack> excludeHotbar(List<ItemStack> inventory, int hotbarSize) {
        ArrayList<ItemStack> excluded = new ArrayList<>();

        for (int i = hotbarSize; i < inventory.size(); i++) {
            ItemStack item = inventory.get(i);

            if (!isAir(item)) {
                excluded.add(item);
            }
        }

        return excluded;
    }

    /**
     * Splits an inventory into hotbar and main inventory items.
     *
     * @param inventory The inventory to split.
     * @param hotbarSize The size of the hotbar.
     *
     * @return The split inventory.
     */
    private static Map.Entry<ArrayList<ItemStack>, ArrayList<ItemStack>> splitInventory(List<ItemStack> inventory,
                                                                                        int hotbarSize) {
        return new SimpleEntry<>(new ArrayList<>(inventory.subList(0, hotbarSize)),
                new ArrayList<>(inventory.subList(hotbarSize, inventory.size())));
    }

    /**
     * Move as many items as possible from one inventory into another.
     *
     * @param origin The origin to empty.
     * @param newInv The inventory to fill.
     * @param item The item to move. Pass in null to move all items of all types.
     *
     * @return The success of the operation.
     */
    @OnlyIn(Dist.CLIENT)
    private static boolean moveAllItems(ArrayList<ItemStack> origin, ArrayList<ItemStack> newInv, Item item) {
        boolean toSend = false;

        if (item == null) {
            for (int index = 0; index < origin.size(); index++) {
                if (isAir(origin.get(index))) continue;

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

            NonNullList<ItemStack> armorInv = player.inventory.armorInventory;
            NonNullList<ItemStack> mainInv = player.inventory.mainInventory;

            ArrayList<Map.Entry<ItemStack, Integer>> fullInv = new ArrayList<>();

            for (ItemStack itemStack: armorInv) {
                fullInv.add(new SimpleEntry<>(itemStack, -1));
            }

            NonNullList<ItemStack> invArray = NonNullList.create();
            int id = 0;
            for (ItemStack itemStack: mainInv) {
                ItemStack newItem = itemStack.copy();
                fullInv.add(new SimpleEntry<>(newItem, id++));
                invArray.add(newItem);
            }

            Map config = CONFIG.getMap("Sort Options");
            if (config != null && config.containsKey("Equip Best Armor on Sort") &&
                    (boolean) config.get("Equip Best Armor on Sort")) {
                int[] armorSlots = ArmorEquip.getBestArmor(fullInv);
                for (int i = 0; i < armorSlots.length; i++) {
                    if (armorSlots[i] >= 0) {
                        EquipmentSlotType equipSlot;
                        switch (i) {
                            case 0:
                                // Helmet
                                equipSlot = EquipmentSlotType.HEAD;
                                break;

                            case 1:
                                // Chest Plate
                                equipSlot = EquipmentSlotType.CHEST;
                                break;

                            case 2:
                                // Leggings
                                equipSlot = EquipmentSlotType.LEGS;
                                break;

                            case 3:
                                // Boots
                                equipSlot = EquipmentSlotType.FEET;
                                break;

                            default:
                                continue;
                        }

                        ItemStack newItem = invArray.set(armorSlots[i], armorInv.get(3 - i));

                        LOGGER.debug(String.format("[%s] Sending network equip packet.", ExampleMod.NAME));
                        Channel.INSTANCE.sendToServer(new EquipArmorPacket(newItem, equipSlot));
                    }
                }
            }

            // Stop if inventory is empty
            if (invArray.isEmpty()) {
                return true;
            }

            if (player.world.isRemote) {
                int hotbarSize = 9;
                if (CONFIG.getConstant("Player Hotbar Size") != null) {
                    hotbarSize = (int) CONFIG.getConstant("Player Hotbar Size");
                }

                // TODO: add skip hotbar option
                boolean skipHotbar = true;
                if (CONFIG.getConstant("a") != null) {
                    skipHotbar = false;
                }

                toSend = excludeHotbar(invArray, skipHotbar ? hotbarSize : 0);

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

                if (skipHotbar) {
                    for (int i = 0; i < hotbarSize; i++) {
                        toSend.add(i, invArray.get(i));
                    }
                }

                LOGGER.debug(String.format("[%s] Sending network sort packet", ExampleMod.NAME));
                Channel.INSTANCE.sendToServer(new SortPacket(toSend, 0));

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

                if (!isAir(slot.getStack())) {
                    toSend.add(slot.getStack());
                }

                size++;

            }

            maxSize = size;

            // Stop if inventory is empty
            if (toSend.isEmpty()) {
                return true;
            }

            if (container.getMinecraft().world != null && container.getMinecraft().world.isRemote) {
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

    /**
     * Helper method to sort the inventory of a player.
     *
     * @param itemList The player's inventory.
     * @return The success of the operation.
     */
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

            for (int i = 0; i < hotbarSize; i++) {
                toSend.add(i, itemList.get(i));
            }

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
     *
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

            if (isAir(heldItem)) {
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