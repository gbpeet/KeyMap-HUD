package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class KeyMapDefaultResetConfirmScreen extends Screen {
    private final KeyMapProfilesScreen parent;

    public KeyMapDefaultResetConfirmScreen(KeyMapProfilesScreen parent) {
        super(Text.literal("Reset to Default Profile"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = height / 2 + 34;

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Cancel"),
                                button -> close()
                        )
                        .dimensions(centerX - 155, buttonY, 150, 20)
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Reset to Defaults"),
                                button -> resetToDefaults()
                        )
                        .dimensions(centerX + 5, buttonY, 150, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int y = height / 2 - 52;

        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                centerX,
                y,
                0xFFFFFF
        );

        y += 26;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Reset all current Minecraft keybindings to their defaults?"),
                centerX,
                y,
                0xFFFFCC55
        );

        y += 16;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("This will also clear all custom key labels and reset KeyMap HUD settings."),
                centerX,
                y,
                0xFFDDDDDD
        );

        y += 16;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Your exported profile files will not be changed or deleted."),
                centerX,
                y,
                0xFFAAAAAA
        );
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void resetToDefaults() {
        KeyMapProfileManager.ResetResult result =
                KeyMapProfileManager.resetToDefaults();

        parent.setStatus(result.message());

        if (client != null) {
            client.setScreen(parent);
        }
    }
}
