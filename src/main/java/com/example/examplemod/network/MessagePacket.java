/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.network.PacketBuffer;

public class MessagePacket {
    private final String message;
    private final boolean error;

    MessagePacket(String message, boolean error) {
        this.message = message;
        this.error = error;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isError() {
        return this.error;
    }

    static void encode(MessagePacket message, PacketBuffer buffer) {
        buffer.writeString(message.message);
        buffer.writeBoolean(message.error);
    }

    static MessagePacket decode(PacketBuffer buffer) {
        return new MessagePacket(buffer.readString(), buffer.readBoolean());
    }
}
