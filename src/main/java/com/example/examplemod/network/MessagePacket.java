/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.network.PacketBuffer;

/**
 * Class to display chat messages from the client.
 */
class MessagePacket {
    /**
     * The message to display.
     */
    private final String message;
    /**
     * Whether the message is an error. (Displays errors using red text)
     */
    private final boolean error;

    /**
     * Class to display chat messages from the client.
     *
     * @param message The message to display.
     * @param error Whether the message is an error. (Errors displayed using red text)
     */
    MessagePacket(String message, boolean error) {
        this.message = message;
        this.error = error;
    }

    /**
     * Returns the message.
     *
     * @return The message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Returns whether the message is an error.
     * @return Whether the message is an error.
     */
    boolean isError() {
        return this.error;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(MessagePacket message, PacketBuffer buffer) {
        buffer.writeString(message.message);
        buffer.writeBoolean(message.error);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static MessagePacket decode(PacketBuffer buffer) {
        return new MessagePacket(buffer.readString(), buffer.readBoolean());
    }
}
