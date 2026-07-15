package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class KeyMapProfilePreviewScreen extends Screen {
    private final KeyMapProfilesScreen parent;
    private final Path profilePath;
    private final KeyMapProfileData profile;
    private final KeyMapProfileManager.PreviewResult preview;

    public KeyMapProfilePreviewScreen(
            KeyMapProfilesScreen parent,
            Path profilePath,
            KeyMapProfileData profile
    ) {
        super(Text.literal("Import KeyMap HUD Profile"));
        this.parent = parent;
        this.profilePath = profilePath;
        this.profile = profile;
        this.preview = KeyMapProfileManager.preview(profile);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int bottomY = height - 38;

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Cancel"),
                                button -> close()
                        )
                        .dimensions(centerX - 155, bottomY, 150, 20)
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Apply Profile"),
                                button -> applyProfile()
                        )
                        .dimensions(centerX + 5, bottomY, 150, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int left = centerX - 155;
        int y = 18;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, y, 0xFFFFFF);
        y += 22;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Profile: " + profile.profileName),
                left,
                y,
                0xFFFFCC55
        );
        y += 14;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("File: " + profilePath.getFileName()),
                left,
                y,
                0xAAAAAA
        );
        y += 14;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Minecraft: " + profile.minecraftVersion
                        + "   KeyMap HUD: " + profile.keyMapHudVersion),
                left,
                y,
                0xAAAAAA
        );
        y += 22;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Bindings that can be applied: " + preview.matchedBindings()),
                left,
                y,
                0xFF55FF99
        );
        y += 14;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Bindings unavailable in this installation: " + preview.unavailableBindings()),
                left,
                y,
                preview.unavailableBindings() > 0 ? 0xFFFFCC55 : 0xFFAAAAAA
        );
        y += 14;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Key labels included: " + preview.keyLabelCount()),
                left,
                y,
                0xFFAAAAAA
        );
        y += 14;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("HUD settings that will change: " + preview.settingChanges()),
                left,
                y,
                0xFFAAAAAA
        );
        y += 22;

        int missingCount = preview.missingMods().size();

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Mods from the exported setup not currently present: " + missingCount),
                left,
                y,
                missingCount > 0 ? 0xFFFFCC55 : 0xFFAAAAAA
        );
        y += 14;

        if (missingCount > 0) {
            int shown = Math.min(4, missingCount);

            for (int i = 0; i < shown; i++) {
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal("  • " + preview.missingMods().get(i)),
                        left,
                        y,
                        0xFFAAAAAA
                );
                y += 12;
            }

            if (missingCount > shown) {
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal("  ...and " + (missingCount - shown) + " more"),
                        left,
                        y,
                        0xFFAAAAAA
                );
                y += 12;
            }
        }

        y += 10;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Unavailable bindings are skipped, not deleted from the profile file."),
                left,
                y,
                0xFF888888
        );
        y += 12;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Re-import the same profile later after installing missing mods."),
                left,
                y,
                0xFF888888
        );
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void applyProfile() {
        KeyMapProfileManager.ApplyResult result =
                KeyMapProfileManager.applyProfile(profile);

        parent.setStatus(result.message());

        if (client != null) {
            client.setScreen(parent);
        }
    }
}
