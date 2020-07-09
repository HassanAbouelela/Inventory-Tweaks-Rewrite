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

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;

public class ItemReplacePacket {
    private final Hand hand;
    private final int replacement;
    private final int oldIndex;
    private final boolean useHand;
    private final boolean inPlayerInventory;

    public ItemReplacePacket(Hand hand, int replacement) {
        this.hand = hand;
        this.replacement = replacement;
        this.oldIndex = 0;
        this.useHand = true;
        this.inPlayerInventory = true;
    }

    public ItemReplacePacket(int oldIndex, int replacement, boolean inPlayerInventory) {
        this.hand = Hand.MAIN_HAND;
        this.replacement = replacement;
        this.oldIndex = oldIndex;
        this.useHand = false;
        this.inPlayerInventory = inPlayerInventory;
    }

    private ItemReplacePacket(Hand hand, int replacement, int oldIndex, boolean useHand, boolean inPlayerInventory) {
        this.hand = hand;
        this.replacement = replacement;
        this.oldIndex = oldIndex;
        this.useHand = useHand;
        this.inPlayerInventory = inPlayerInventory;
    }

    public Hand getHand() {
        return this.hand;
    }

    public int getReplacementIndex() {
        return this.replacement;
    }

    public int getOldIndex() {
        return this.oldIndex;
    }

    public boolean useHand() {
        return this.useHand;
    }

    public boolean isInPlayerInventory() {
        return this.inPlayerInventory;
    }

    static void encode(ItemReplacePacket message, PacketBuffer buffer) {
        if (message.hand == Hand.MAIN_HAND) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(1);
        }

        buffer.writeInt(message.replacement);
        buffer.writeInt(message.oldIndex);
        buffer.writeBoolean(message.useHand);
        buffer.writeBoolean(message.inPlayerInventory);
    }

    static ItemReplacePacket decode(PacketBuffer buffer) {
        Hand hand;
        int read = buffer.readInt();

        if (read == 0) {
            hand = Hand.MAIN_HAND;
        } else {
            hand = Hand.OFF_HAND;
        }

        return new ItemReplacePacket(
                hand, buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean()
        );
    }
}
