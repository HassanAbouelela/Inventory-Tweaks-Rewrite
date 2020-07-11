/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
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
