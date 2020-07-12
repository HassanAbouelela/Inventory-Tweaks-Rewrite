/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.events;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.functions.Functions;
import com.example.examplemod.network.Channel;
import com.example.examplemod.network.DropPacket;
import com.example.examplemod.network.OptimizationPacket;
import com.example.examplemod.network.SortPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;

import static com.example.examplemod.ExampleMod.CONFIG;

public class ClientEventHandlers {
    // Getting Event Logger
    private static final Logger LOGGER = LogManager.getLogger();
    // TODO: Clean up logging

    public enum Order {
        DEFAULT, COLUMNS, ROWS
    }

    private enum Source {
        KEYBIND, MIDDLE_MOUSE, DEFAULT_BUTTON, COL_BUTTON, ROW_BUTTON, PLAYER_INVENTORY
    }

    private Order currentOrder = Order.DEFAULT;
    private long lastSort = System.currentTimeMillis();
    /**
     * Main sorting handler.
     *
     * @param event The event window.
     * @param source The method that initiated the sort.
     */
    private void runSort(GuiScreenEvent event, Source source) {
        try {
            if (Functions.getChangeOrder(lastSort)) {
                switch (currentOrder) {
                    case DEFAULT:
                        currentOrder = Order.COLUMNS;
                        break;

                    case COLUMNS:
                        currentOrder = Order.ROWS;
                        break;

                    default:
                        currentOrder = Order.DEFAULT;
                }

            } else {
                currentOrder = Order.DEFAULT;
            }

            switch (source) {
                case KEYBIND:
                    if (keySort(event)) {
                        break;
                    }

                    LOGGER.error(String.format("[%s] Screen Sort Error Occurred, See Above.", ExampleMod.NAME));
                    break;

                //TODO: The buttons below need to check which inventory they are sorting

                case DEFAULT_BUTTON:
                    currentOrder = Order.DEFAULT;

                case COL_BUTTON:
                    currentOrder = Order.COLUMNS;

                case ROW_BUTTON:
                    currentOrder = Order.ROWS;

                case MIDDLE_MOUSE:
                    if (Functions.sort(false, currentOrder, (ContainerScreen) event.getGui())) {
                        break;
                    }

                    LOGGER.error(String.format("[%s] Container Sort Error Occurred, See Above.", ExampleMod.NAME));
                    break;

                case PLAYER_INVENTORY:
                    if (Functions.sort(true, currentOrder, null)) {
                        break;
                    }

                    LOGGER.error(String.format("[%s] Player Sort Error Occurred, See Above.", ExampleMod.NAME));
                    break;

                default:
            }

            LOGGER.debug(String.format("[%s] Successful sort Call - Bind: %b", ExampleMod.NAME, source));
            lastSort = System.currentTimeMillis();
        } catch (Error error) {
            LOGGER.error(String.format("[%s] Error Occurred: (%s) - %s", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
        }
    }

    /**
     * Sort an inventory, when a keybind initiates sorting.
     *
     * @param event The current client screen.
     * @return The success of the sort.
     */
    private boolean keySort(GuiScreenEvent event) {
        // Supported GUIs
        Functions.Permission permission = Functions.getPermission(event.getGui().getClass().getName());

        if (permission == Functions.Permission.BLACKLIST) return true;

        if (permission == Functions.Permission.INVENTORY_FIRST) {
            return Functions.sort(false, currentOrder, (ContainerScreen) event.getGui());
        } else {
            // If the current screen inventory defaults to player-sort
            return Functions.sort(true, currentOrder, null);
        }
    }

    private boolean guiCalled = false;
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        // Running on key bind
        try {
            guiCalled = true;

            int bind = Functions.getKeyBind();
            if (bind == 0) return;

            // Executing
            if (event.getKeyCode() == bind) {
                LOGGER.debug(String.format("[%s] Key bind detected, attempting sort on: %s",
                        ExampleMod.NAME, event.getGui().getClass().getName()));
                runSort(event, Source.KEYBIND);
            }
        } catch (Exception error) {
            LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
        }
    }

    private int lastKey = -1;
    private int lastMod = 0;
    private long lastKeyPress = System.currentTimeMillis();
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onKey(InputEvent.KeyInputEvent event) {
        try {
            lastKey = event.getKey();
            lastMod = event.getModifiers();
            lastKeyPress = System.currentTimeMillis();

            try {
                Map settings = CONFIG.getSortSettings();
                if (settings != null && settings.containsKey("Sort Outside Inventory")
                        && !(boolean) settings.get("Sort Outside Inventory")) return;

            } catch (Exception error) {
                LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                        error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
                return;
            }

            if (event.getAction() != 0) return;

            if (guiCalled) {
                guiCalled = false;
                return;
            }

            int bind = Functions.getKeyBind();
            if (bind == 0) return;

            // Executing
            if (event.getKey() == bind) {
                LOGGER.debug(String.format("[%s] Key bind detected, attempting sort on the player.", ExampleMod.NAME));
                runSort(null, Source.PLAYER_INVENTORY);
            }
        } catch (Exception error) {
            LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseClickedEvent event) {
        if (event.getGui().isPauseScreen() ||
                Config.blackListScreens.contains(event.getGui().getClass().getName())) return;

        Slot slot = null;
        try {
            slot = ((ContainerScreen) event.getGui()).getSlotUnderMouse();
        } catch (Exception ignored) {}

        if (slot != null && event.getButton() != 2) {
            if ((lastMod > 0 || lastKey != -1) && System.currentTimeMillis() < lastKeyPress + 500) {
                Map config = CONFIG.getMap("Shortcuts");
                if (config == null) return;
                if (config.containsKey("Shortcuts") && !(boolean) config.get("Shortcuts")) {
                    return;
                }

                if (event.getButton() == 0 && lastMod != 1 && slot.inventory instanceof CraftResultInventory) {
                    // Left click in a crafting inventory
                    ClientPlayerEntity player = Minecraft.getInstance().player;
                    try {
                        if (player != null) {
                            List<Slot> slots;
                            ItemStack craftStack;

                            if (player.openContainer instanceof PlayerContainer) {
                                // Player crafting inventory
                                craftStack = player.openContainer.inventorySlots.get(0).getStack();
                                slots = player.openContainer.inventorySlots.subList(1, 5);
                            } else {
                                // External crafting inventory
                                WorkbenchContainer workbench = (WorkbenchContainer) player.openContainer;
                                craftStack = workbench.inventorySlots.get(0).getStack();
                                slots = workbench.inventorySlots.subList(1, workbench.getSize());
                            }

                            boolean canCraft = !Functions.isAir(craftStack);
                            ItemStack craftResult = craftStack.copy();

                            if (canCraft && !slots.isEmpty()) {
                                ArrayList<ItemStack> updatedCraftingInv = new ArrayList<>();
                                int crafted = 0;

                                switch (lastMod) {
                                    case 2:
                                        // Control. Craft one item.
                                        if (config.containsKey("Craft one item - Ctrl + Click") &&
                                                !(boolean) config.get("Craft one item - Ctrl + Click")) return;

                                        for (Slot craftingSlot : slots) {
                                            ItemStack newItem = craftingSlot.getStack().copy();
                                            newItem.setCount(craftingSlot.getStack().getCount() - 1);

                                            updatedCraftingInv.add(newItem);
                                        }
                                        crafted++;

                                        craftResult.setCount(craftResult.getCount() * crafted);

                                        if (crafted > 0 && Functions.craftItem(craftResult, player)) {
                                            Channel.INSTANCE.sendToServer(new OptimizationPacket(updatedCraftingInv));
                                        }

                                        break;
                                    case 3:
                                        // Control + Shift. Craft all items from inventory.
                                        if (config.containsKey("Craft all from inventory - Ctrl + Shift + Click") &&
                                                !(boolean) config.get("Craft all from inventory - Ctrl + Shift + Click")) {
                                            return;
                                        }

                                        ArrayList<ItemStack> neededItems = new ArrayList<>();
                                        for (Slot craftingSlot : slots) {
                                            neededItems.add(craftingSlot.getStack());
                                        }

                                        Functions.craftAllInventory(neededItems, craftResult, player);
                                }

                                event.setCanceled(true);
                            }
                        }
                    } catch (ClassCastException ignored) {
                        LOGGER.warn(String.format("[%s] Couldn't convert class: %s",
                                ExampleMod.NAME, player.openContainer.getClass().getCanonicalName()));
                    }
                }

                if (event.getButton() == 0) {
                    // Left Click
                    switch (lastMod) {
                        case 2:
                            // Control. Move one item.
                            if (config.containsKey("Move one item - Control + Click") &&
                                    !(boolean) config.get("Move one item - Control + Click")) return;

                            event.setCanceled(true);
                            Functions.moveUnknownInv(slot, 1, Functions.MoveType.MOVE_INT);
                            lastKey = -1;
                            break;

                        case 3:
                            // Control + Shift. Move all items of the same type.
                            if (config.containsKey("Move all items of the same type - Control + Shift + Click") &&
                                    !(boolean) config.get("Move all items of the same type - Control + Shift + Click"))
                                return;

                            event.setCanceled(true);
                            Functions.moveUnknownInv(slot, 0, Functions.MoveType.MOVE_ALL_TYPE);
                            lastKey = -1;
                            break;

                        case 4:
                            // Alt. Drop item.
                            if (config.containsKey("Drop - Alt + Click") &&
                                    (boolean) config.get("Drop - Alt + Click")) {
                                event.setCanceled(true);
                                Channel.INSTANCE.sendToServer(
                                        new DropPacket(slot.getSlotIndex(), slot.inventory instanceof PlayerInventory)
                                );
                            }

                            lastKey = -1;
                            break;
                    }

                    if (lastKey != -1) {
                        switch (lastKey) {
                            case 87:
                            case 265:
                            case 83:
                            case 264:
                                // W or Up, or S or Down. Move full stack.
                                if (config.containsKey("Move one stack - Shift/W/Up/S/Down + Click") &&
                                        !(boolean) config.get("Move one stack - Shift/W/Up/S/Down + Click")) return;

                                event.setCanceled(true);
                                Functions.moveUnknownInv(slot, 0, Functions.MoveType.MOVE_STACK);
                                break;

                            case 32:
                                // Space. Move full inventory.
                                if (config.containsKey("Move everything - Space + Click") &&
                                        !(boolean) config.get("Move everything - Space + Click")) return;

                                event.setCanceled(true);
                                Functions.moveUnknownInv(slot, 0, Functions.MoveType.MOVE_ALL);
                                break;
                        }
                    }

                } else if (event.getButton() == 1 && lastMod > 0
                        && !Functions.isAir(slot.getStack())) {
                    // Right Click. Move to empty slot.
                    Functions.moveUnknownInv(slot, slot.getStack().getCount(), Functions.MoveType.MOVE_EMPTY);
                    event.setCanceled(true);
                }

                lastKey = -1;
                lastMod = 0;
            }

            return;
        }

        if (event.getButton() != 2) return;

        try {
            Map settings = CONFIG.getSortSettings();
            if (settings != null
                    && settings.containsKey("Sort On Pickup") && !(boolean) settings.get("Middle Mouse Sort")) return;

        } catch (Exception error) {
            LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            return;
        }

        if (slot != null && slot.inventory instanceof CraftingInventory) {
            // Optimizing Crafting Screen
            int invSize = slot.inventory.getSizeInventory();

            // Collecting every unique item, and their places
            Map<CompoundNBT, Map.Entry<Integer, ArrayList<Integer>>> uniqueItems = new HashMap<>();
            for (int i = 0; i < invSize; i++) {
                ItemStack itemStack = slot.inventory.getStackInSlot(i);

                if (Functions.isAir(itemStack)) {
                    continue;
                }

                CompoundNBT tags = itemStack.getTag();
                if (tags == null) {
                    tags = new CompoundNBT();
                }

                if (tags.contains("Count")) tags.remove("Count");
                tags.putInt("Custom-ID-Tag", Item.getIdFromItem(itemStack.getItem()));

                if (uniqueItems.containsKey(tags)) {
                    ArrayList<Integer> indexes = uniqueItems.get(tags).getValue();
                    indexes.add(i);
                    uniqueItems.replace(tags,
                            new SimpleEntry<>(uniqueItems.get(tags).getKey() + itemStack.getCount(), indexes));
                } else {
                    ArrayList<Integer> indexes = new ArrayList<>();
                    indexes.add(i);

                    uniqueItems.put(tags, new SimpleEntry<>(itemStack.getCount(), indexes));
                }
            }

            if (uniqueItems.size() == 0) return;

            // Optimizing
            List<ItemStack> optimizedItems = Arrays.asList(new ItemStack[invSize]);

            for (CompoundNBT tag: uniqueItems.keySet()) {
                int itemsLeft = uniqueItems.get(tag).getKey();
                List<Integer> indexes = uniqueItems.get(tag).getValue();

                ItemStack itemStack = new ItemStack(Item.getItemById(tag.getInt("Custom-ID-Tag")));
                itemStack.setTag(tag);

                tag.remove("Custom-ID-Tag");

                // Splitting into equal stacks
                int base = Math.floorDiv(itemsLeft, indexes.size());
                itemStack.setCount(base);

                for (int index: indexes) {
                    optimizedItems.set(index, itemStack.copy());
                }

                // Adding remaining
                itemsLeft -= base * indexes.size();

                int itemsPerSlot = itemStack.getMaxStackSize() - base;

                int currIndex = 0;
                while (itemsLeft != 0) {
                    if (itemsPerSlot < itemsLeft) {
                        itemStack.setCount(itemStack.getMaxStackSize());
                        itemsLeft -= itemsPerSlot;
                    } else {
                        itemStack.setCount(itemStack.getCount() + itemsLeft);
                        itemsLeft = 0;
                    }

                    optimizedItems.set(indexes.get(currIndex++), itemStack.copy());
                }
            }

            LOGGER.debug(String.format("[%s] Sending network optimization packet.", ExampleMod.NAME));
            Channel.INSTANCE.sendToServer(new OptimizationPacket(optimizedItems));

        } else if (slot != null && !(slot.inventory instanceof PlayerInventory)) {
            // Sorting for an inventory
            if (Functions.getPermission(event.getGui().getClass().getName()) == Functions.Permission.BLACKLIST) {
                return;
            }

            LOGGER.debug(String.format("[%s] Middle mouse click detected, attempting sort on: %s",
                    ExampleMod.NAME, event.getGui().getClass().getName()));
            runSort(event, Source.MIDDLE_MOUSE);

        } else {
            // Sorting for a player
            if (Functions.getPermission(event.getGui().getClass().getName()) == Functions.Permission.BLACKLIST) {
                return;
            }

            LOGGER.debug(String.format("[%s] Middle mouse click detected, attempting player sort on: %s",
                    ExampleMod.NAME, event.getGui().getClass().getName()));
            runSort(event, Source.PLAYER_INVENTORY);
        }
    }

    private long lastScroll = System.currentTimeMillis();
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onMouseScroll(GuiScreenEvent.MouseScrollEvent event) {
        if (System.currentTimeMillis() < lastScroll + 50 || event.getGui().isPauseScreen()) return;

        int delta = (int) Math.floor(event.getScrollDelta());

        try {
            Map settings = CONFIG.getMap("Middle Mouse Move");
            if (settings != null) {
                if (!(boolean) settings.get("Middle Mouse Move")) return;
                if (settings.containsKey("Invert Scroll") && (boolean) settings.get("Invert Scroll")) delta *= -1;
            }

        } catch (Exception error) {
            LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            return;
        }

        LOGGER.debug(String.format("[%s] Detected mouse scroll.", ExampleMod.NAME));

        ClientPlayerEntity playerEntity = Minecraft.getInstance().player;
        Slot slot = ((ContainerScreen) event.getGui()).getSlotUnderMouse();
        if (playerEntity == null || slot == null || playerEntity.openContainer == playerEntity.container
                || Functions.isAir(slot.getStack())
                || playerEntity.openContainer instanceof CreativeScreen.CreativeContainer) return;

        int size = 0;
        for (Slot containerSlot: playerEntity.openContainer.inventorySlots) {
            if (containerSlot.inventory instanceof PlayerInventory) {
                continue;
            }

            size++;
        }

        ArrayList<ItemStack> extInventory = new ArrayList<>(playerEntity.openContainer.getInventory().subList(0, size));
        ArrayList<ItemStack> playerInventory = new ArrayList<>(playerEntity.inventory.mainInventory);

        boolean send;

        if (slot.inventory instanceof PlayerInventory) {
            // Move in player
            send = Functions.moveItem(playerInventory, extInventory, slot.getSlotIndex(), delta, delta > 0);

        } else {
            // Move from external inventory to player inventory
            send = Functions.moveItem(extInventory, playerInventory, slot.getSlotIndex(), delta, delta < 0);
        }

        if (send) {
            LOGGER.debug(String.format("[%s] Sending network sort packets for scroll-shift.", ExampleMod.NAME));
            Channel.INSTANCE.sendToServer(new SortPacket(playerInventory, 0));
            Channel.INSTANCE.sendToServer(new SortPacket(extInventory, 0, extInventory.size()));
        }

        lastScroll = System.currentTimeMillis();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        try {
            Map settings = CONFIG.getSortSettings();
            if (settings != null
                    && settings.containsKey("Sort On Pickup") && !(boolean) settings.get("Sort On Pickup")) return;

        } catch (Exception error) {
            LOGGER.error(String.format("[%s] An error has occurred: (%s) - [%s]", ExampleMod.NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
            return;
        }

        LOGGER.debug(String.format("[%s] Item picked up, attempting sort on: %s",
                ExampleMod.NAME, event.getEntity().getDisplayName().getFormattedText()));
        if (Functions.playerSort(event.getPlayer().inventory.mainInventory)) {
            return;
        }
        LOGGER.error(String.format("[%s] Inventory Sort Error Occurred, See Above.", ExampleMod.NAME));
    }

    private long lastReplace = System.currentTimeMillis();

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getItemStack().isDamageable() && System.currentTimeMillis() >= lastReplace + 100) {
            if (Functions.replaceItem(event.getPlayer(), event.getHand(), null)) {
                event.setCanceled(true);
            }

            lastReplace = System.currentTimeMillis();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttack(AttackEntityEvent event) {
        if (System.currentTimeMillis() < lastReplace + 100 || !event.getPlayer().getHeldItemMainhand().isDamageable()) {
            return;
        }

        lastReplace = System.currentTimeMillis();

        if (Functions.replaceItem(event.getPlayer(), Hand.MAIN_HAND, null)) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (System.currentTimeMillis() < lastReplace + 100) return;

        Functions.replaceItem(event.getPlayer(), event.getHand(), null);
        lastReplace = System.currentTimeMillis();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onHoeUse(UseHoeEvent event) {
        if (System.currentTimeMillis() < lastReplace + 100) return;

        Functions.replaceItem(event.getPlayer(), event.getContext().getHand(), null);
        lastReplace = System.currentTimeMillis();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBonemeal(BonemealEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player || event.getPlayer().isCreative()
                || System.currentTimeMillis() < lastReplace + 100) return;

        if (event.getPlayer().getHeldItemMainhand() == event.getStack()) {
            Functions.replaceItem(event.getPlayer(), Hand.MAIN_HAND, null);
        } else if (event.getPlayer().getHeldItemOffhand() == event.getStack()) {
            Functions.replaceItem(event.getPlayer(), Hand.OFF_HAND, null);
        }

        lastReplace = System.currentTimeMillis();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemToss(ItemTossEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || event.getPlayer().getUniqueID() != player.getUniqueID() || event.getPlayer().isCreative()
                || !Functions.isAir(event.getPlayer().getHeldItemMainhand())
                || System.currentTimeMillis() < lastReplace + 100) return;

        Functions.replaceItem(event.getPlayer(), Hand.MAIN_HAND, event.getEntityItem().getItem());
        lastReplace = System.currentTimeMillis();
    }

    private Hand lastUsedHand = null;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Saving the last used hand in order to determine which hand caused the action
        // This fires before the EntityPlaceEvent and RightClickItem events
        lastUsedHand = event.getHand();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Ensuring a player placed the block
        if (event.getEntity() == null || lastUsedHand == null) return;
        else if (Minecraft.getInstance().player == null ||
                event.getEntity().getUniqueID() != Minecraft.getInstance().player.getEntity().getUniqueID()) {
            // Block not placed by the player, skipping
            return;
        }

        Map config = CONFIG.getMap("Refill");

        if (config != null && config.containsKey("Refill") && (boolean) config.get("Refill")) {
            int status = Functions.refill(
                    Minecraft.getInstance().player, event.getPlacedBlock().getBlock().asItem(), lastUsedHand);

            if (status == 2) {
                LOGGER.warn(String.format("[%s] Refill failed", ExampleMod.NAME));
            }
        }
    }
}