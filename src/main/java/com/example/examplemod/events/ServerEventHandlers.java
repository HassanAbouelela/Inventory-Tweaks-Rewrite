/*
 * Copyright (c) 2020 Hassan Abouelela
 * Licensed under the MIT License
 */

package com.example.examplemod.events;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.functions.commands.CommandControl;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class for handling game events on the server.
 */
public class ServerEventHandlers {
    /**
     * Event logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Event handler for server starting.
     * Used to start command registration.
     *
     * @param event The server starting event.
     */
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        LOGGER.info(String.format("Server Starting, %s Active", ExampleMod.NAME));
        CommandControl.register(event.getCommandDispatcher());
    }
}