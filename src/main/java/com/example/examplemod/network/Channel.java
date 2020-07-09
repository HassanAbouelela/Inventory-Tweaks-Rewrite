/*
 * MIT License
 *
 * Copyright (c) 2020 Hassan Abouelela
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    private int id = 0;
    public void register() {
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
    }
}

