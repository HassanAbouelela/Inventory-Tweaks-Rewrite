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

package com.example.examplemod.functions.commands;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.Channel;
import com.example.examplemod.network.EmptyPackets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;

public class CommandControl {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // Main Command registration
        dispatcher.register(
                Commands.literal(ExampleMod.NAME_SHORT)
                        .requires(source -> source.hasPermissionLevel(0))
                        .then(Commands.literal("reload").executes(new Reload()))
                        .then(Commands.literal("restore")
                                .then(Commands.argument(
                                        "Source (Backup Name)", StringArgumentType.greedyString()
                                )
                                        .executes(new Restore())
                                )
                                .executes(new Restore()) // No input executes on the latest backup
                        )
        );
    }

    static void sendToClient(ServerPlayerEntity player, String instruction, ArrayList<String> args) {
        Channel.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new EmptyPackets(instruction, args));
    }
}
