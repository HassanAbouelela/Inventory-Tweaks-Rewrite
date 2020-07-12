/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

public class EquipArmorPacket {
    private ItemStack itemStack;
    private EquipmentSlotType slotType;

    public EquipArmorPacket(ItemStack itemStack, EquipmentSlotType slotType) {
        this.itemStack = itemStack;
        this.slotType = slotType;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public EquipmentSlotType getSlotType() {
        return this.slotType;
    }

    static void encode(EquipArmorPacket message, PacketBuffer buffer) {
        buffer.writeItemStack(message.itemStack);
        buffer.writeEnumValue(message.slotType);
    }

    static EquipArmorPacket decode(PacketBuffer buffer) {
        return new EquipArmorPacket(buffer.readItemStack(), buffer.readEnumValue(EquipmentSlotType.class));
    }
}
