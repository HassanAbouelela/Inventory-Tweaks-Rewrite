/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.functions.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.Collections;


class Restore implements Command<CommandSource> {
    @Override
    public int run(CommandContext<CommandSource> ctx) {
        String input = ctx.getInput().toLowerCase().replaceFirst("/itr restore", "").trim();
        String message;

        if (input.isEmpty()) {
            // Restore the latest file
            message = "Attempting restore on the latest backup.";
            input = "latest";
        } else {
            // Restore input
            input = input.replaceAll("[\"]", "'");
            input = input.replaceAll("[:]", "");
            message = "Attempting restore on: " + input;
        }

        ctx.getSource().sendFeedback(new StringTextComponent(message), true);

        try {
            CommandControl.sendToClient(
                    ctx.getSource().asPlayer(), "restore", new ArrayList<>(Collections.singletonList(input))
            );

            return 0;

        } catch (CommandSyntaxException e) {
            ctx.getSource().sendErrorMessage(new StringTextComponent("Restore Failed: " + e));
            return 1;
        }
    }
}
