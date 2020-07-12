/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
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

/**
 * Class to send instructions to the client.
 */
public class EmptyPackets {
    /**
     * Event logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The instruction.
     */
    private final String instruction;
    /**
     * The arguments for the instruction.
     */
    private final ArrayList<String> args;

    /**
     * Helper class to send instructions to be performed on the client.
     *
     * @param instruction The instruction to perform. Options are "reload" to reload settings, and "restore" for backups.
     * @param args The arguments for the instruction.
     */
    public EmptyPackets(String instruction, ArrayList<String> args) {
        this.instruction = instruction;
        this.args = args;
    }

    /**
     * The encoder for this packet.
     *
     * @param message The packet.
     * @param buffer The buffer to write to.
     */
    static void encode(EmptyPackets message, PacketBuffer buffer) {
        buffer.writeInt(message.args.size());
        for (String arg: message.args) {
            buffer.writeString(arg);
        }

        buffer.writeString(message.instruction);
    }

    /**
     * The decoder for this packet.
     *
     * @param buffer The buffer to read from.
     * @return The packet.
     */
    static EmptyPackets decode(PacketBuffer buffer) {
        ArrayList<String> args = new ArrayList<>();
        int argLength = buffer.readInt();

        for (int i = 0; i < argLength; i++) {
            args.add(buffer.readString());
        }

        return new EmptyPackets(buffer.readString(), args);
    }

    /**
     * The handler for this packet. All logic performed on the client side*.
     *
     * *in theory.
     *
     * @param message An {@link EmptyPackets}
     * @param ctx Supplier of the network event context.
     */
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

    /**
     * Wrapper method to display text to player in game.
     *
     * @param error Whether the message is an error (displays in red).
     * @param message The message to send.
     *
     * @return The message.
     */
    private static String sendMessage(boolean error, String message) {
        Channel.INSTANCE.sendToServer(new MessagePacket(message, error));

        return message;
    }

    /**
     * Wrapper method to reload Config options.
     */
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

    /**
     * Wrapper method to restore a backup.
     *
     * @param args An ArrayList with the first argument being the source to restore from.
     */
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
