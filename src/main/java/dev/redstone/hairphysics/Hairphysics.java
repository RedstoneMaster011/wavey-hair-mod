package dev.redstone.hairphysics;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hairphysics implements ModInitializer {
    public static String MOD_ID = "hair-physics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("bros hair is starting up");
    }
}
