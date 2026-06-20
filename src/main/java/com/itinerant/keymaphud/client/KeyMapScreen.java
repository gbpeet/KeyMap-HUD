package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KeyMapScreen extends Screen {
    private String searchQuery = "";

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
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!Character.isISOControl(chr)) {
            searchQuery += chr;
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.matchesOverlayKey(keyCode, scanCode)) {
            KeyMapHUDClient.waitingForRelease = true;
            close();
            return true;
        }

        if (keyCode == 259 && !searchQuery.isEmpty()) { // Backspace
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        OverlayRenderer.renderScreen(context, mouseX, mouseY, delta, searchQuery);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String quickFilter = OverlayRenderer.getQuickFilterQueryAt((int) mouseX, (int) mouseY);

        if (quickFilter != null) {
            searchQuery = quickFilter;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}