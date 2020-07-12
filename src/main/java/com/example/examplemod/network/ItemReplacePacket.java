/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;

/**
 * Class to switch items within a player inventory.
 */
public class ItemReplacePacket {
    private final Hand hand;
    private final int replacement;
    private final int oldIndex;
    private final boolean useHand;
    private final boolean inPlayerInventory;

    /**
     * Class to switch items within a player inventory.
     *
     * @param hand Main hand or off hand.
     * @param replacement The item to place into the hand.
     */
    public ItemReplacePacket(Hand hand, int replacement) {
        this.hand = hand;
        this.replacement = replacement;
        this.oldIndex = 0;
        this.useHand = true;
        this.inPlayerInventory = true;
    }

    /**
     * Class to switch items within a player inventory.
     *
     * @param hand Main hand or off hand or null.
     * @param replacement The item to place.
     * @param oldIndex The index to place the old item into.
     * @param useHand Whether to use the held item.
     * @param inPlayerInventory Whether the item is in the player's inventory or open container.
     */
    private ItemReplacePacket(Hand hand, int replacement, int oldIndex, boolean useHand, boolean inPlayerInventory) {
        this.hand = hand;
        this.replacement = replacement;
        this.oldIndex = oldIndex;
        this.useHand = useHand;
        this.inPlayerInventory = inPlayerInventory;
    }

    /**
     * Gets the hand to use.
     *
     * @return Main hand or off hand.
     */
    Hand getHand() {
        return this.hand;
    }

    /**
     * Get the replacement index.
     *
     * @return Replacement index.
     */
    int getReplacementIndex() {
        return this.replacement;
    }

    /**
     * Gets the old index.
     *
     * @return Old index.
     */
    int getOldIndex() {
        return this.oldIndex;
    }

    /**
     * Gets whether to use the hand.
     *
     * @return Whether to use the hand.
     */
    boolean useHand() {
        return this.useHand;
    }

    /**
     * Gets whether the old item is in the player's inventory of open container.
     *
     * @return Whether the old item is in the player's inventory of open container.
     */
    boolean isInPlayerInventory() {
        return this.inPlayerInventory;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
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

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
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
