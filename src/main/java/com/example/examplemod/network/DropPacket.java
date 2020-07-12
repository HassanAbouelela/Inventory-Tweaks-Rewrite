/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.network.PacketBuffer;

/**
 * Packet to drop items.
 */
public class DropPacket {
    /**
     * The index of the item to drop.
     */
    private final int index;
    /**
     * If the item is in the player's inventory or open container.
     */
    private final boolean inPlayer;

    /**
     * Class to remove an item from an inventory and spawn it in the world.
     *
     * @param index The index of the item.
     * @param inPlayer If the item is in the player's inventory or open inventory.
     */
    public DropPacket(int index, boolean inPlayer) {
        this.index = index;
        this.inPlayer = inPlayer;
    }

    /**
     * Get the index of the item.
     *
     * @return The index of the item.
     */
    int getIndex() {
        return index;
    }

    /**
     * Get the location of the item.
     *
     * @return The location of the item.
     */
    boolean isInPlayer() {
        return inPlayer;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(DropPacket message, PacketBuffer buffer) {
        buffer.writeInt(message.index);
        buffer.writeBoolean(message.inPlayer);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static DropPacket decode(PacketBuffer buffer) {
        return new DropPacket(buffer.readInt(), buffer.readBoolean());
    }

}
