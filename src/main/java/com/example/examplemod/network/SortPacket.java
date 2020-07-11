/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;


public class SortPacket {
    private final boolean player;
    private final ArrayList<ItemStack> newInventory;
    private final int index;
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

    public boolean isPlayer() {
        return player;
    }

    public ArrayList<ItemStack> getItems( ) {
        return this.newInventory;
    }

    public int getIndex() {
        return this.index;
    }

    public int getSize() {
        return this.size;
    }

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