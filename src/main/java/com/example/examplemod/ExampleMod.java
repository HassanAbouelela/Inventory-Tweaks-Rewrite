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

package com.example.examplemod;

import com.example.examplemod.events.ClientEventHandlers;
import com.example.examplemod.events.ServerEventHandlers;
import com.example.examplemod.network.Channel;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.util.Pair;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>Inventory Tweaks Reborn (NAME TEMP)</h1>
 * Inventory Tweaks is a minecraft mod meant to improve QoL. IT allows players to interact with inventories quickly,
 * and efficiently.
 *
 * @author Hassan Abouelela
 * @version 1.0.0
 *
 */
// The value here should match an entry in the META-INF/mods.toml file
// TODO: Change mod name
@Mod("examplemod")
public class ExampleMod
{
    // Getting Event Logger
    private static final Logger LOGGER = LogManager.getLogger();
    // TODO: Automate this ->
    public static final String ID = "examplemod";
    public static final String NAME = "Inventory Tweaks Rewrite";
    public static final String NAME_SHORT = "ITR";
    public static Config CONFIG = null;
//    public static boolean serverAllowed = false; - Not Implemented

    static {
        // Loading Config
        try {
            CONFIG = new Config();
        } catch (Exception error) {
            LOGGER.error(String.format("[%s] Config setup error: (%s) - %s", NAME,
                    error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
        }
    }

    public ExampleMod( ) {
        // Register IMC processing
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register network communications
        new Channel().register();

        // Allow server if protocol matches, and check override. - Not Implemented
//        if (!serverAllowed) {
//            Map config = CONFIG.getMap("Unsupported Servers");
//            if (config != null && (config.containsKey("Allow") && (boolean) config.get("Allow"))) {
//                LOGGER.warn(String.format("[%s] Connecting to unsupported server because of setting override.",
//                        ExampleMod.NAME));
//
//            } else {
//                LOGGER.warn(String.format("[%s] Server incompatible, mod disabled.", ExampleMod.NAME));
//                return;
//            }
//        }

        // Register event handlers
        ClientEventHandlers clientEventHandlers = new ClientEventHandlers();
        MinecraftForge.EVENT_BUS.register(clientEventHandlers);

        ServerEventHandlers serverEventHandlers = new ServerEventHandlers();
        MinecraftForge.EVENT_BUS.register(serverEventHandlers);

//        InterModComms.sendTo(ID, "Add Constant", (Supplier<Pair<String, Integer>>) () -> new Pair<>("A", 3));
    }

    private void processIMC(final InterModProcessEvent event) {
        LOGGER.debug(String.format("[%s] Started IMC", NAME));

        event.getIMCStream().forEach((imcMessage -> {
            if (imcMessage.getMethod().equals("Add Constant")) {
                LOGGER.debug(String.format("[%s] Received Constant Update request from '%s'. Content: %s",
                        NAME, imcMessage.getSenderModId(), imcMessage.getMessageSupplier().get()));

                Pair message = (Pair) imcMessage.getMessageSupplier().get();

                try {
                    CONFIG.updateConstants(String.valueOf(message.getKey()), message.getValue());
                } catch (FileNotFoundException | JsonProcessingException error) {
                    LOGGER.error(String.format("[%s] Failed to update constants as per request of %s, aborting. " +
                            "Message: %s. Reason: %s - %s", NAME, imcMessage.getSenderModId(),
                            imcMessage.getMessageSupplier().get(), error.getClass().getCanonicalName(),
                            Arrays.toString(error.getStackTrace())));
                }

            } else if (imcMessage.getMethod().equals("Add Inventory")) {
                LOGGER.debug(String.format("[%s] Received Inventory Registration request from '%s'. Content: %s",
                        NAME, imcMessage.getSenderModId(), imcMessage.getMessageSupplier().get()));

                /* Example Structure:
                name: modName.items.myInventory (Whatever is returned by `.getClass().getName()`)
                (Optional) width: 9
                (Optional) height: 3
                (Optional) type: blacklist/player_first/inventory_first
                (Optional) protectedIndex: 0 (If there are slots at the beginning needing to be left empty)
                 */

                //TODO: Implement logic for the type

                Map message = new HashMap();

                try {
                    message = (Map) imcMessage.getMessageSupplier().get();
                } catch (Exception error) {
                    LOGGER.debug(String.format("[%s] Could not read options for %s's requested registration. %s - %s",
                            NAME, imcMessage.getSenderModId(),
                            error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
                }

                try {
                    if (message.containsKey("name")) {
                        CONFIG.updateConstants(String.valueOf(message.remove("name")), message);

                    } else {
                        LOGGER.warn(String.format("[%s] Mod %s attempted to register an inventory, but " +
                                "one or more fields was missing.", NAME, imcMessage.getSenderModId()));
                    }

                } catch (Exception error) {
                    LOGGER.warn(String.format("[%s] Failed to register inventory from %s. Reason: %s - %s",
                            NAME, imcMessage.getSenderModId(),
                            error.getClass().getCanonicalName(), Arrays.toString(error.getStackTrace())));
                }

            } else {
                LOGGER.warn(String.format("[%s] Received an unknown message (%s) from %s",
                        NAME, imcMessage.getMethod(), imcMessage.getSenderModId()));
            }
        }));

    }
}
