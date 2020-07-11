/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;


public class OverFlowPacket {
    // Items that have overflown
    private final ArrayList<ItemStack> items;

    public OverFlowPacket(ArrayList<ItemStack> items) {
        this.items = items;
    }
    public ArrayList<ItemStack> getItems( ) {
        return this.items;
    }


    static void encode(OverFlowPacket message, PacketBuffer buffer) {
        // Writing number of items to be sent, and items to buffer
        buffer.writeInt(message.getItems().size());
        for (ItemStack item: message.getItems()) buffer.writeItemStack(item);
    }

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

