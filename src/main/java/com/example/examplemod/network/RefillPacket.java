/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
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
