/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Class to rewrite the content of an inventory.
 */
public class SortPacket {
    /**
     * The player to perform the sort on.
     */
    private final boolean player;
    /**
     * The new inventory.
     */
    private final ArrayList<ItemStack> newInventory;
    /**
     * The index of the first item in the inventory.
     */
    private final int index;
    /**
     * The size of the inventory.
     */
    private final int size;

    /**
     * Player constructor for a sort packet. This is used to ask a server to rewrite what is in a given inventory.
     *
     * @param newInventory The inventory that will replace the old one.
     * @param startingIndex The number of protected slots, such as hotbar slots.
     */
    public SortPacket(ArrayList<ItemStack> newInventory, int startingIndex) {
        this.player = true;
        this.newInventory = newInventory;
        this.index = startingIndex;
        this.size = 0;
    }

    /**
     * Non-player constructor for a sort packet. This is used to ask a server to rewrite what is in a given inventory.
     *
     * @param newInventory The inventory that will replace the old one.
     * @param startingIndex The number of protected slots, such as hotbar slots.
     * @param size The size of the new inventory.
     */
    public SortPacket(ArrayList<ItemStack> newInventory, int startingIndex, int size) {
        this.player = false;
        this.newInventory = newInventory;
        this.index = startingIndex;
        this.size = size;
    }

    /**
     * Private full constructor for a sort packet. This is used to ask a server to rewrite what is in a given inventory.
     *
     * @param newInventory The inventory that will replace the old one.
     * @param startingIndex The number of protected slots, such as hotbar slots.
     * @param size The size of the new inventory.
     * @param player If the inventory belongs to a player.
     */
    private SortPacket(ArrayList<ItemStack> newInventory, int startingIndex, int size, boolean player) {
        this.player = player;
        this.newInventory = newInventory;
        this.index = startingIndex;
        this.size = size;
    }

    /**
     * Returns whether the inventory is the player's inventory, or the open container.
     *
     * @return Whether the inventory is the player's inventory, or the open container.
     */
    public boolean isPlayer() {
        return player;
    }

    /**
     * Returns the new inventory.
     *
     * @return The new inventory.
     */
    public ArrayList<ItemStack> getItems( ) {
        return this.newInventory;
    }

    /**
     * Returns the beginning index.
     *
     * @return The beginning index.
     */
    int getIndex() {
        return this.index;
    }

    /**
     * Returns the size of the inventory.
     *
     * @return The size of the inventory.
     */
    public int getSize() {
        return this.size;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(SortPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.index);
        buffer.writeInt(message.size);

        // Writing number of items to be sent, and items to buffer
        buffer.writeInt(message.newInventory.size());
        for (ItemStack item: message.newInventory) {
            buffer.writeItemStack(item);
        }

        buffer.writeBoolean(message.player);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static SortPacket decode(PacketBuffer buffer) {
        int index = buffer.readInt();
        int size = buffer.readInt();

        // Getting number of ItemStacks
        int itemNumber = buffer.readInt();
        ArrayList<ItemStack> inventory = new ArrayList<>();

        // Adding items from buffer
        for (int item = 0; item < itemNumber; item++) {
            inventory.add(buffer.readItemStack());
        }

        return new SortPacket(inventory, index, size, buffer.readBoolean());
    }
}