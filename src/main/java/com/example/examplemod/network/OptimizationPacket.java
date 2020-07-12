/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to reorganize an open crafting menu.
 */
public class OptimizationPacket {
    /**
     * The new crafting inventory.
     */
    private final List<ItemStack> optimizedList;

    /**
     * Class to reorganize an open crafting menu.
     *
     * @param optimizedList The new inventory.
     */
    public OptimizationPacket(List<ItemStack> optimizedList) {
        this.optimizedList = optimizedList;
    }

    /**
     * Returns the new inventory.
     *
     * @return The new inventory.
     */
    List<ItemStack> getOptimizedList() {
        return optimizedList;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(OptimizationPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.optimizedList.size());

        for (ItemStack itemStack: message.optimizedList) {
            if (itemStack == null) {
                buffer.writeItemStack(ExampleMod.AIR);
            } else {
                buffer.writeItemStack(itemStack);
            }
        }
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static OptimizationPacket decode(PacketBuffer buffer) {
        List<ItemStack> optimizedList = new ArrayList<>();

        int itemCount = buffer.readInt();

        for (int i = 0; i < itemCount; i++) {
            optimizedList.add(buffer.readItemStack());
        }

        return new OptimizationPacket(optimizedList);
    }
}
