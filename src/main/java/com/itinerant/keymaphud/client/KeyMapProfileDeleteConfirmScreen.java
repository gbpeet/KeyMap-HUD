package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class KeyMapProfileDeleteConfirmScreen extends Screen {
    private final KeyMapProfilesScreen parent;
    private final Path profile;

    public KeyMapProfileDeleteConfirmScreen(KeyMapProfilesScreen parent, Path profile) {
        super(Text.literal("Delete Profile"));
        this.parent = parent;
        this.profile = profile;
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
                                Text.literal("Delete Profile"),
                                button -> {
                                    parent.deleteProfile(profile);
                                    if (client != null) {
                                        client.setScreen(parent);
                                    }
                                }
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
        int y = height / 2 - 44;

        String filename = profile.getFileName().toString();
        String displayName = filename.endsWith(".json")
                ? filename.substring(0, filename.length() - 5)
                : filename;

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Delete profile \"" + displayName + "\"?"),
                centerX,
                y,
                0xFFFFCC55
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("This permanently deletes the profile JSON file."),
                centerX,
                y + 20,
                0xFFDDDDDD
        );
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
