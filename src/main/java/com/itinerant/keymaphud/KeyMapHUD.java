package com.itinerant.keymaphud;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyMapHUD implements ModInitializer {
    public static final String MOD_ID = "keymaphud";
    public static final Logger LOGGER = LoggerFactory.getLogger("KeyMap HUD");

    @Override
    public void onInitialize() {
        LOGGER.info("KeyMap HUD initialized.");
    }
}
