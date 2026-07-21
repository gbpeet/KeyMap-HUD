package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.util.List;

public final class KeyMapProfilesScreen extends Screen {
    private static final int PROFILES_PER_PAGE = 5;

    private final Screen parent;

    private TextFieldWidget profileNameField;
    private List<Path> profiles = List.of();
    private int page = 0;
    private String status = "";

    public KeyMapProfilesScreen(Screen parent) {
        super(Text.literal("KeyMap HUD Profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        refreshProfiles();

        int centerX = width / 2;

        profileNameField = new TextFieldWidget(
                textRenderer,
                centerX - 155,
                54,
                210,
                20,
                Text.literal("Profile name")
        );
        profileNameField.setMaxLength(64);

        if (profileNameField.getText().isBlank()) {
            profileNameField.setText("My KeyMap Profile");
        }

        addDrawableChild(profileNameField);

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Export"),
                                button -> exportProfile()
                        )
                        .dimensions(centerX + 65, 54, 90, 20)
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Open Profiles Folder"),
                                button -> Util.getOperatingSystem().open(
                                        KeyMapProfileManager.getProfilesDirectory().toFile()
                                )
                        )
                        .dimensions(centerX - 155, 82, 150, 20)
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Reset to Default"),
                                button -> {
                                    if (client != null) {
                                        client.setScreen(new KeyMapDefaultResetConfirmScreen(this));
                                    }
                                }
                        )
                        .dimensions(centerX + 5, 82, 150, 20)
                        .build()
        );

        int startIndex = page * PROFILES_PER_PAGE;
        int endIndex = Math.min(startIndex + PROFILES_PER_PAGE, profiles.size());
        int y = 116;

        for (int i = startIndex; i < endIndex; i++) {
            Path profile = profiles.get(i);
            String filename = profile.getFileName().toString();
            String display = filename.endsWith(".json")
                    ? filename.substring(0, filename.length() - 5)
                    : filename;

            addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Import: " + display),
                                    button -> openPreview(profile)
                            )
                            .dimensions(centerX - 155, y, 240, 20)
                            .build()
            );

            addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Delete"),
                                    button -> {
                                        if (client != null) {
                                            client.setScreen(new KeyMapProfileDeleteConfirmScreen(this, profile));
                                        }
                                    }
                            )
                            .dimensions(centerX + 90, y, 65, 20)
                            .build()
            );

            y += 24;
        }

        int bottomY = height - 38;

        ButtonWidget previous = addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("◀ Previous"),
                                button -> {
                                    page = Math.max(0, page - 1);
                                    init();
                                }
                        )
                        .dimensions(centerX - 155, bottomY, 100, 20)
                        .build()
        );

        ButtonWidget next = addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Next ▶"),
                                button -> {
                                    page++;
                                    init();
                                }
                        )
                        .dimensions(centerX - 50, bottomY, 100, 20)
                        .build()
        );

        int maxPage = profiles.isEmpty()
                ? 0
                : (profiles.size() - 1) / PROFILES_PER_PAGE;

        previous.active = page > 0;
        next.active = page < maxPage;

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Done"),
                                button -> close()
                        )
                        .dimensions(centerX + 55, bottomY, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Export a shareable profile, or import one from the profiles folder."),
                centerX,
                34,
                0xAAAAAA
        );

        if (!status.isBlank()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(status),
                    centerX,
                    height - 56,
                    0xFFFFCC55
            );
        }

        if (profiles.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("No profile files found yet."),
                    centerX,
                    128,
                    0xAAAAAA
            );
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    public void setStatus(String status) {
        this.status = status == null ? "" : status;
    }

    private void exportProfile() {
        KeyMapProfileManager.ExportResult result =
                KeyMapProfileManager.exportProfile(profileNameField.getText());

        String resultMessage = result.message();
        refreshProfiles();
        init();
        status = resultMessage;
    }

    public void deleteProfile(Path profile) {
        KeyMapProfileManager.DeleteResult result =
                KeyMapProfileManager.deleteProfile(profile);

        String resultMessage = result.message();
        refreshProfiles();
        init();
        status = resultMessage;
    }

    private void openPreview(Path profile) {
        if (client == null) {
            return;
        }

        try {
            KeyMapProfileData data = KeyMapProfileManager.readProfile(profile);
            client.setScreen(new KeyMapProfilePreviewScreen(this, profile, data));
        } catch (Exception exception) {
            exception.printStackTrace();
            status = "Could not read " + profile.getFileName();
        }
    }

    private void refreshProfiles() {
        profiles = KeyMapProfileManager.listProfiles();

        int maxPage = profiles.isEmpty()
                ? 0
                : (profiles.size() - 1) / PROFILES_PER_PAGE;

        page = Math.max(0, Math.min(page, maxPage));
    }
}
