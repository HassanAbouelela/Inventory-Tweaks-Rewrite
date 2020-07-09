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
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;

public class Reload implements Command<CommandSource> {
    @Override
    public int run(CommandContext<CommandSource> ctx) {
        int result = ExampleMod.CONFIG.load();

        switch (result) {
            case 0:
                // Everything went fine, files reloaded
                ctx.getSource().sendFeedback(
                        new StringTextComponent("Settings file located, reloaded successfully"), true
                );
                break;

            case 1:
                // Everything went fine, had to create settings folder
                ctx.getSource().sendFeedback(
                        new StringTextComponent(String.format(
                                "Settings file not found, created successfully as: %s",
                                ExampleMod.CONFIG.configFile.toString())), true
                );
                break;

            case 2:
                // Could not load settings file, IOException
                ctx.getSource().sendErrorMessage(
                        new StringTextComponent(
                                "Could not load/read settings file, assuming defaults." +
                                        " Check log for more information"
                        ));
                break;

            default:
                // Should not get here
                ctx.getSource().sendErrorMessage(
                        new StringTextComponent(
                                "Unknown error occurred, check the logs for more information," +
                                        " or ask the developer for help"
                        ));
        }

        return 0;
    }
}