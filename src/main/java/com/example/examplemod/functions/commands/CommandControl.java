/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
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

/**
 * The main class for registering commands.
 */
public class CommandControl {
    /**
     * Command registration method.
     *
     * @param dispatcher The CommandDispatcher to register commands with.
     */
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

    /**
     * Helper method to send instructions to a client.
     *
     * @param player The client to send a message to.
     * @param instruction The instruction to perform. Options are "reload" to reload settings, and "restore" for backups.
     * @param args Arguments for the instruction, if any.
     */
    static void sendToClient(ServerPlayerEntity player, String instruction, ArrayList<String> args) {
        Channel.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new EmptyPackets(instruction, args));
    }
}
