/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
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
