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

public class DropPacket {
    private final int index;
    private final boolean inPlayer;

    public DropPacket(int index, boolean inPlayer) {
        this.index = index;
        this.inPlayer = inPlayer;
    }

    public int getIndex() {
        return index;
    }

    public boolean isInPlayer() {
        return inPlayer;
    }

    static void encode(DropPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.index);
        buffer.writeBoolean(message.inPlayer);
    }

    static DropPacket decode(PacketBuffer buffer) {
        return new DropPacket(buffer.readInt(), buffer.readBoolean());
    }

}
