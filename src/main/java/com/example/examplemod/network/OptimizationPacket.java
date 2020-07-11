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

public class OptimizationPacket {
    private final List<ItemStack> optimizedList;

    public OptimizationPacket(List<ItemStack> optimizedList) {
        this.optimizedList = optimizedList;
    }

    public List<ItemStack> getOptimizedList() {
        return optimizedList;
    }

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

    static OptimizationPacket decode(PacketBuffer buffer) {
        List<ItemStack> optimizedList = new ArrayList<>();

        int itemCount = buffer.readInt();

        for (int i = 0; i < itemCount; i++) {
            optimizedList.add(buffer.readItemStack());
        }

        return new OptimizationPacket(optimizedList);
    }
}
