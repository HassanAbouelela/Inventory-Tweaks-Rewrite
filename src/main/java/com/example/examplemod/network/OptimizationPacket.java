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

package com.example.examplemod.network;

import net.minecraft.item.Item;
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

        ItemStack filler = new ItemStack(Item.getItemById(0));
        for (ItemStack itemStack: message.optimizedList) {
            if (itemStack == null) {
                buffer.writeItemStack(filler);
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
