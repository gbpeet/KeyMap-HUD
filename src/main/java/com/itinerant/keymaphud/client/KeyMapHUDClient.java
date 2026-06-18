package com.itinerant.keymaphud.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class KeyMapHUDClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindings.register();
        HudRenderCallback.EVENT.register(OverlayRenderer::render);
    }
}
