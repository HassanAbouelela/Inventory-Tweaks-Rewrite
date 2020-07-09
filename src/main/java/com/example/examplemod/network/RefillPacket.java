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


public class RefillPacket {
    private final Hand hand;
    private final int index;

    public RefillPacket(Hand hand, int index) {
        this.hand = hand;
        this.index = index;
    }

    public Hand getHand() {
        return this.hand;
    }

    public int getIndex() {
        return this.index;
    }

    static void encode(RefillPacket message, PacketBuffer buffer) {
        if (message.hand == Hand.MAIN_HAND) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(1);
        }

        buffer.writeInt(message.index);
    }

    static RefillPacket decode(PacketBuffer buffer) {
        Hand hand;
        int read = buffer.readInt();

        if (read == 0) {
            hand = Hand.MAIN_HAND;
        } else {
            hand = Hand.OFF_HAND;
        }

        return new RefillPacket(hand, buffer.readInt());
    }
}

