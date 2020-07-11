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
import com.example.examplemod.functions.Backup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import static com.example.examplemod.ExampleMod.CONFIG;

public class EmptyPackets {
    private String instruction;
    private ArrayList<String> args;
    private static final Logger LOGGER = LogManager.getLogger();

    public EmptyPackets(String instruction, ArrayList<String> args) {
        this.instruction = instruction;
        this.args = args;
    }

    static void encode(EmptyPackets message, PacketBuffer buffer) {
        buffer.writeInt(message.args.size());
        for (String arg: message.args) {
            buffer.writeString(arg);
        }

        buffer.writeString(message.instruction);
    }

    static EmptyPackets decode(PacketBuffer buffer) {
        ArrayList<String> args = new ArrayList<>();
        int argLength = buffer.readInt();

        for (int i = 0; i < argLength; i++) {
            args.add(buffer.readString());
        }

        return new EmptyPackets(buffer.readString(), args);
    }

    static void handle(EmptyPackets message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            switch (message.instruction.toLowerCase()) {
                case "reload":
                    reload();
                    break;
                case "restore":
                    restore(message.args);
                    break;

                default:
                    LOGGER.warn(String.format("[%s] Received unknown instruction from the server [%s]",
                            ExampleMod.NAME, message.instruction));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static String sendMessage(boolean error, String message) {
        Channel.INSTANCE.sendToServer(new MessagePacket(message, error));

        return message;
    }

    private static void reload() {
        int result = ExampleMod.CONFIG.load();

        switch (result) {
            case 0:
                // Everything went fine, files reloaded
                LOGGER.debug(sendMessage(false, String.format(
                        "[%s] Settings file located, reloaded successfully", ExampleMod.NAME
                )));
                break;

            case 1:
                // Everything went fine, had to create settings folder
                LOGGER.debug(sendMessage(false, String.format(
                        "[%s] Settings file not found, created successfully as: %s",
                        ExampleMod.NAME, ExampleMod.CONFIG.configFile.toString()
                )));
                break;

            case 2:
                // Could not load settings file, IOException
                LOGGER.warn(sendMessage(true, String.format(
                        "[%s] Could not load/read settings file, assuming defaults. Check log for more information",
                        ExampleMod.NAME
                )));
                break;

            default:
                // Should not get here
                LOGGER.warn(sendMessage(true, String.format(
                        "[%s] Unknown error occurred, check the logs for more information, or ask the developer for help",
                        ExampleMod.NAME
                )));
        }
    }

    private static void restore(ArrayList<String> args) {
        Map backups = CONFIG.getMap("Backups");
        if (backups == null) {
            LOGGER.warn(sendMessage(true, String.format(
                    "[%s] Restore Failed: Could not read config file.", ExampleMod.NAME
            )));
            return;
        }


        ArrayList<ItemStack> restored = new ArrayList<>();
        int result = Backup.restore(backups, args.get(0), restored);

        switch (result) {
            case 0:
                Channel.INSTANCE.sendToServer(new SortPacket(restored, 0));
                LOGGER.debug(sendMessage(false, String.format("[%s] Restore Completed", ExampleMod.NAME)));
                break;

            case 1:
                sendMessage(true, "Could not find file: " + args.get(0));
                break;

            case 2:
                sendMessage(true, "Couldn't open restore file. More information available in logs.");
                break;

            case 3:
                sendMessage(true, "Couldn't restore from given folder. Does it contain a JSON index?");
                break;

            case 5:
                sendMessage(true, "Unknown error. More info in logs. Please report this as a bug");
                break;

            case 10:
                sendMessage(true, "Could not open game folder. Please report this as a bug");
        }
    }
}
