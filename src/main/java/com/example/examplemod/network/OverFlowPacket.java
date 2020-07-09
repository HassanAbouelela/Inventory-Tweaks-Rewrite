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

