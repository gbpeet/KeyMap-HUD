package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KeyMapScreen extends Screen {
    public KeyMapScreen() {
        super(Text.literal("KeyMap HUD"));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty so the world remains visible behind the HUD.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        OverlayRenderer.renderScreen(context, mouseX, mouseY, delta);
    }
}