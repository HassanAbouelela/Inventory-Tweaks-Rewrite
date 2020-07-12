/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

/**
 * Class to spawn items around the player.
 */
public class OverFlowPacket {
    /**
     * The items to spawn.
     */
    private final ArrayList<ItemStack> items;

    /**
     * Class to spawn items around the player.
     *
     * @param items The items to spawn.
     */
    public OverFlowPacket(ArrayList<ItemStack> items) {
        this.items = items;
    }

    /**
     * Returns the items to spawn.
     *
     * @return The items to spawn.
     */
    public ArrayList<ItemStack> getItems( ) {
        return this.items;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(OverFlowPacket message, PacketBuffer buffer) {
        // Writing number of items to be sent, and items to buffer
        buffer.writeInt(message.getItems().size());
        for (ItemStack item: message.getItems()) buffer.writeItemStack(item);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static OverFlowPacket decode(PacketBuffer buffer) {
        // Getting number of ItemStacks
        int items = buffer.readInt();
        ArrayList<ItemStack> itemStacks = new ArrayList<>();

        // Adding items from buffer
        for (int item = 0; item < items; item++) {
            itemStacks.add(buffer.readItemStack());
        }

        return new OverFlowPacket(itemStacks);

    }
}

