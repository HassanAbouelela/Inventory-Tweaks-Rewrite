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

package com.example.examplemod.events;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.functions.commands.CommandControl;
import com.example.examplemod.network.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class ServerEventHandlers {
    // Getting Event Logger
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        LOGGER.info(String.format("Server Starting, %s Active", ExampleMod.NAME));
        CommandControl.register(event.getCommandDispatcher());
    }

    public static void handleOverflow(OverFlowPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received overflow packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Overflow sender is null, aborting.", ExampleMod.NAME));
                return;
            }

            for (ItemStack item: message.getItems()){
                // TODO: Implement check to prevent malicious item spawning
                // Note: Could not figure out how to cause malicious item spawning to begin with
                sender.dropItem(item, false, true);
            }

            LOGGER.debug(String.format("[%s] Processed overflow packet.", ExampleMod.NAME));
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleDrop(DropPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received network drop packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Attempted server sort on a null player, returning.", ExampleMod.NAME));
                return;
            }

            if (message.isInPlayer()) {
                ItemStack item = sender.inventory.removeStackFromSlot(message.getIndex());
                sender.dropItem(item, false, true);

                sender.sendContainerToPlayer(sender.container);
            } else {
                ItemStack item = sender.openContainer.getInventory().remove(message.getIndex());
                sender.dropItem(item, false, true);

                sender.sendContainerToPlayer(sender.container);
                sender.sendContainerToPlayer(sender.openContainer);
            }

            LOGGER.debug(String.format("[%s] Processed drop packet.", ExampleMod.NAME));
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleSort(SortPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received sort packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();
            int index = message.getIndex();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Attempted server sort on a null player, returning.", ExampleMod.NAME));
                return;
            }

            if (message.isPlayer()) {
                // Sort Player Inventory
                for (ItemStack item: message.getItems()) {
                    sender.inventory.setInventorySlotContents(index++, item);
                }

                while (index < sender.inventory.mainInventory.size()) {
                    sender.inventory.setInventorySlotContents(index++, ExampleMod.AIR);
                }

                sender.sendContainerToPlayer(sender.container);

            } else {
                // Sort Open Inventory
                for (ItemStack item: message.getItems()) {
                    sender.openContainer.putStackInSlot(index++, item);
                }

                while (index < message.getSize()) {
                    sender.openContainer.putStackInSlot(index++, ExampleMod.AIR);
                }

                sender.sendContainerToPlayer(sender.openContainer);
            }

            LOGGER.debug(String.format("[%s] Performed sort on: %s",
                    ExampleMod.NAME, sender.getDisplayName().getFormattedText()));
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleRefill(RefillPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received refill packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Attempted server refill on a null player, returning.",
                        ExampleMod.NAME));
                return;
            }

            ItemStack newItem = sender.inventory.removeStackFromSlot(message.getIndex());
            sender.setHeldItem(message.getHand(), newItem);

            sender.sendContainerToPlayer(sender.container);

            LOGGER.debug(String.format("[%s] Performed refill on: %s",
                    ExampleMod.NAME, sender.getDisplayName().getFormattedText()));
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleReplace(ItemReplacePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received item replace packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Attempted server replace on a null player, returning.",
                        ExampleMod.NAME));
                return;
            }

            if (message.useHand()) {
                ItemStack oldItem = sender.getHeldItem(message.getHand());
                ItemStack newItem = sender.inventory.getStackInSlot(message.getReplacementIndex());

                sender.setHeldItem(message.getHand(), newItem);
                sender.inventory.setInventorySlotContents(message.getReplacementIndex(), oldItem);

                sender.sendContainerToPlayer(sender.container);

            } else {
                if (message.isInPlayerInventory()) {
                    ItemStack oldItem = sender.inventory.getStackInSlot(message.getOldIndex());
                    ItemStack newItem = sender.inventory.getStackInSlot(message.getReplacementIndex());

                    sender.inventory.setInventorySlotContents(message.getOldIndex(), newItem);
                    sender.inventory.setInventorySlotContents(message.getReplacementIndex(), oldItem);

                    sender.sendContainerToPlayer(sender.container);

                } else {
                    ItemStack oldItem = sender.inventory.getStackInSlot(message.getOldIndex());
                    ItemStack newItem = sender.openContainer.getInventory().get(message.getReplacementIndex());

                    sender.inventory.setInventorySlotContents(message.getOldIndex(), newItem);
                    sender.openContainer.putStackInSlot(message.getReplacementIndex(), oldItem);

                    sender.sendContainerToPlayer(sender.container);
                    sender.sendContainerToPlayer(sender.openContainer);
                }
            }

            LOGGER.debug(String.format("[%s] Performed replace on: %s",
                    ExampleMod.NAME, sender.getDisplayName().getFormattedText()));
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleOptimization(OptimizationPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LOGGER.debug(String.format("[%s] Received crafting optimization packet.", ExampleMod.NAME));

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender == null) {
                LOGGER.warn(String.format("[%s] Attempted server optimization on a null player, returning.",
                        ExampleMod.NAME));
                return;
            }

            if (sender.openContainer instanceof WorkbenchContainer) {
                // External Crafting Grid
                for (int i = 0; i < message.getOptimizedList().size(); i++) {
                    sender.openContainer.putStackInSlot(i + 1, message.getOptimizedList().get(i));
                }

                sender.sendContainerToPlayer(sender.openContainer);
                LOGGER.debug(String.format("[%s] Optimized crafting grid.", ExampleMod.NAME));

            } else {
                // Player Crafting Grid
                int index = 1;

                /* Note:
                This works because, unlike the inventory, the container, does include the crafting grid (1-4), and the
                crafting result (0).
                 */

                for (int i = 0; i < message.getOptimizedList().size(); i++) {
                    sender.container.putStackInSlot(index++, message.getOptimizedList().get(i));
                }

                sender.sendContainerToPlayer(sender.container);
                LOGGER.debug(String.format("[%s] Optimized player crafting grid.", ExampleMod.NAME));
            }
        });

        ctx.get().setPacketHandled(true);
    }

    public static void handleMessage(MessagePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            StringTextComponent stringComponent = new StringTextComponent(message.getMessage());
            if (message.isError()) {
                stringComponent.applyTextStyle(TextFormatting.RED);
            }

            ServerPlayerEntity sender = ctx.get().getSender();

            if (sender != null) {
                sender.sendMessage(stringComponent);
            } else {
                LOGGER.warn(String.format("[%s] Failed to deliver server message to player.", ExampleMod.NAME));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}