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

class Reload implements Command<CommandSource> {
    @Override
    public int run(CommandContext<CommandSource> ctx) {
        try {
            CommandControl.sendToClient(ctx.getSource().asPlayer(), "reload", new ArrayList<>());
            return 0;

        } catch (CommandSyntaxException e) {
            ctx.getSource().sendErrorMessage(new StringTextComponent("Reload Failed: " + e));
            return 1;

        }
    }
}