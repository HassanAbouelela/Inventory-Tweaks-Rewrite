/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;

/**
 * Class to place item into the player's hand.
 */
public class RefillPacket {
    /**
     * The hand to fill.
     */
    private final Hand hand;
    /**
     * The index of the item.
     */
    private final int index;

    /**
     * Class to place item into the player's hand.
     *
     * @param hand The hand to place the item into.
     * @param index The index of the item.
     */
    public RefillPacket(Hand hand, int index) {
        this.hand = hand;
        this.index = index;
    }

    /**
     * Returns the hand to be filled.
     *
     * @return Hand to be filled.
     */
    Hand getHand() {
        return this.hand;
    }

    /**
     * Returns the index of the item.
     *
     * @return Index of the item.
     */
    int getIndex() {
        return this.index;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(RefillPacket message, PacketBuffer buffer) {
        if (message.hand == Hand.MAIN_HAND) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(1);
        }

        buffer.writeInt(message.index);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
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
