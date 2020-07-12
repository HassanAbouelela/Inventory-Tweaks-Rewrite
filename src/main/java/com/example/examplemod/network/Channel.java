/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.events.ServerEventHandlers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;


public class Channel {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.ID, "main"),
            () -> PROTOCOL_VERSION,
            (server_protocol) -> {
                if (server_protocol.equals("ABSENT") || server_protocol.equals("ACCEPTVANILLA")) return false;

                int version;

                try {
                    version = Integer.parseInt(server_protocol);
                } catch (Exception ignored) {
                    return false;
                }

                return 0 < version && version <= Integer.parseInt(PROTOCOL_VERSION);
            },
            (client_protocol) -> {
                if (client_protocol.equals("ABSENT") || client_protocol.equals("ACCEPTVANILLA")) return false;

                int version;

                try {
                    version = Integer.parseInt(client_protocol);
                } catch (Exception ignored) {
                    return false;
                }

                return 0 < version && version <= Integer.parseInt(PROTOCOL_VERSION);
            }
    );

    private static int id = 0;
    public void register() {
        // Player -> Server
        INSTANCE.registerMessage(id++, OverFlowPacket.class,
                OverFlowPacket::encode, OverFlowPacket::decode, ServerEventHandlers::handleOverflow);
        INSTANCE.registerMessage(id++, SortPacket.class,
                SortPacket::encode, SortPacket::decode, ServerEventHandlers::handleSort);
        INSTANCE.registerMessage(id++, RefillPacket.class,
                RefillPacket::encode, RefillPacket::decode, ServerEventHandlers::handleRefill);
        INSTANCE.registerMessage(id++, ItemReplacePacket.class,
                ItemReplacePacket::encode, ItemReplacePacket::decode, ServerEventHandlers::handleReplace);
        INSTANCE.registerMessage(id++, OptimizationPacket.class,
                OptimizationPacket::encode, OptimizationPacket::decode, ServerEventHandlers::handleOptimization);
        INSTANCE.registerMessage(id++, DropPacket.class,
                DropPacket::encode, DropPacket::decode, ServerEventHandlers::handleDrop);
        INSTANCE.registerMessage(id++, MessagePacket.class,
                MessagePacket::encode, MessagePacket::decode, ServerEventHandlers::handleMessage);
        INSTANCE.registerMessage(id++, EquipArmorPacket.class,
                EquipArmorPacket::encode, EquipArmorPacket::decode, ServerEventHandlers::handleEquip);

        // Server -> Player
        INSTANCE.registerMessage(id++, EmptyPackets.class,
                EmptyPackets::encode, EmptyPackets::decode, EmptyPackets::handle);
    }
}

