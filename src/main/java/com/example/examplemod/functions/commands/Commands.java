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
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;

import java.util.Collection;
import java.util.Collections;

public class Commands {
    private static final ArgumentType<String> stringArgument = new ArgumentType<String>() {
        @Override
        public String parse(StringReader reader) {
            String b = reader.getRemaining();

            reader.setCursor(reader.getTotalLength());

            return b;//.replaceAll("[\"']", "");
        }

        @Override
        public Collection<String> getExamples() {
            return Collections.singletonList("%appdata%\\.minecraft\\saves\\World\\ITBackups");
        }

//        @Override
//        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
//            return Suggestions.create(RestoreCommand., "");
//        }
    };

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // Top level restore command
        LiteralCommandNode<CommandSource> restore = dispatcher.register(net.minecraft.command.Commands.literal("restore")
                        .requires(source -> source.hasPermissionLevel(0))
                        .executes(new Restore())
                        .then(net.minecraft.command.Commands.argument("Source (Path)", stringArgument))
//                .executes(RestoreCommand -> {
//                    dispatcher.parse(StringArgumentType.getString(dispatcher.))
//                })
        );

        // Main Command register
        dispatcher.register(net.minecraft.command.Commands.literal(ExampleMod.NAME_SHORT)
                .requires(source -> source.hasPermissionLevel(0))
                .then(net.minecraft.command.Commands.literal("reload").executes(new Reload()))
                .then(restore)
        );
    }
}
