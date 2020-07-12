/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/**
 * Class to Equip Armor on a Player.
 */
public class EquipArmorPacket {
    /**
     * The armor ItemStack.
     */
    private final ItemStack itemStack;
    /**
     * The EquipmentSlot.
     */
    private final EquipmentSlotType slotType;

    /**
     * Class to equip armor on a player.
     *
     * @param itemStack The armor ItemStack to replace.
     * @param slotType The EquipmentSlot.
     */
    public EquipArmorPacket(ItemStack itemStack, EquipmentSlotType slotType) {
        this.itemStack = itemStack;
        this.slotType = slotType;
    }

    /**
     * Gets the armor ItemStack.
     *
     * @return The armor ItemStack.
     */
    ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * Gets the EquipmentSlot.
     *
     * @return The EquipmentSlot.
     */
    EquipmentSlotType getSlotType() {
        return this.slotType;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(EquipArmorPacket message, PacketBuffer buffer) {
        buffer.writeItemStack(message.itemStack);
        buffer.writeEnumValue(message.slotType);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static EquipArmorPacket decode(PacketBuffer buffer) {
        return new EquipArmorPacket(buffer.readItemStack(), buffer.readEnumValue(EquipmentSlotType.class));
    }
}
