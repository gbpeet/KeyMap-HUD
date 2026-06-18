package com.itinerant.keymaphud.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class KeyMapHUDClient implements ClientModInitializer {

    private boolean wasPressed = false;

    @Override
    public void onInitializeClient() {
        KeyBindings.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            boolean pressed = KeyBindings.isOverlayHeld();

            if (pressed && !wasPressed) {

                MinecraftClient mc = MinecraftClient.getInstance();
            }

            wasPressed = pressed;
        });
    }
}