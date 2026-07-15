package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class KeyMapConfigScreen extends Screen {
    private static final List<Float> SCALES = List.of(0.50f, 0.75f, 1.00f, 1.25f, 1.50f);
    private static final List<String> POSITIONS = List.of(
            "TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
            "CENTER_LEFT", "CENTER", "CENTER_RIGHT",
            "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"
    );

    private final Screen parent;
    private final KeyMapConfig config;

    public KeyMapConfigScreen(Screen parent) {
        super(Text.literal("KeyMap HUD Settings"));
        this.parent = parent;
        this.config = KeyMapConfigManager.get();
    }

    @Override
    protected void init() {
        clearChildren();

        int centerX = width / 2;
        int left = centerX - 155;
        int buttonX = centerX + 5;
        int rowY = 58;
        int rowGap = 30;

        ButtonWidget layoutButton = addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("ANSI (US)"),
                                button -> {
                                    // ANSI is the only layout included in 1.0.0.
                                }
                        )
                        .dimensions(buttonX, rowY, 150, 20)
                        .build()
        );
        layoutButton.active = false;
        layoutButton.setTooltip(Tooltip.of(
                Text.literal("Selects the physical keyboard layout. Additional international layouts are planned.")
        ));

        rowY += rowGap;

        ButtonWidget scaleButton = addDrawableChild(
                ButtonWidget.builder(
                                scaleText(),
                                button -> {
                                    config.hudScale = nextScale(config.hudScale);
                                    button.setMessage(scaleText());
                                }
                        )
                        .dimensions(buttonX, rowY, 150, 20)
                        .build()
        );
        scaleButton.setTooltip(Tooltip.of(
                Text.literal("Scales KeyMap HUD relative to its current automatic size.")
        ));

        rowY += rowGap;

        ButtonWidget positionButton = addDrawableChild(
                ButtonWidget.builder(
                                positionText(),
                                button -> {
                                    config.hudPosition = nextValue(POSITIONS, config.hudPosition, "CENTER");
                                    button.setMessage(positionText());
                                }
                        )
                        .dimensions(buttonX, rowY, 150, 20)
                        .build()
        );
        positionButton.setTooltip(Tooltip.of(
                Text.literal("Positions the HUD when unused screen space is available.")
        ));

        rowY += rowGap;

        ButtonWidget mouseButton = addDrawableChild(
                ButtonWidget.builder(
                                mouseText(),
                                button -> {
                                    config.mousePosition = "LEFT".equals(config.mousePosition) ? "RIGHT" : "LEFT";
                                    button.setMessage(mouseText());
                                }
                        )
                        .dimensions(buttonX, rowY, 150, 20)
                        .build()
        );
        mouseButton.setTooltip(Tooltip.of(
                Text.literal("Displays the mouse cluster on the left or right side of the keyboard.")
        ));

        rowY += rowGap;

        ButtonWidget profilesButton = addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Manage Profiles..."),
                                button -> {
                                    KeyMapConfigManager.save();

                                    if (client != null) {
                                        client.setScreen(new KeyMapProfilesScreen(this));
                                    }
                                }
                        )
                        .dimensions(buttonX, rowY, 150, 20)
                        .build()
        );
        profilesButton.setTooltip(Tooltip.of(
                Text.literal("Export, import, and share complete keybinding profiles.")
        ));

        int bottomY = height - 38;

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Reset Defaults"),
                                button -> resetDefaults()
                        )
                        .dimensions(centerX - 155, bottomY, 150, 20)
                        .build()
        );

        addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Done"),
                                button -> close()
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
        int rowY = 64;
        int rowGap = 30;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Changes are saved when this screen is closed."),
                centerX,
                36,
                0xAAAAAA
        );

        context.drawTextWithShadow(textRenderer, Text.literal("Keyboard Layout"), left, rowY, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("HUD Scale"), left, rowY + rowGap, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("HUD Position"), left, rowY + rowGap * 2, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Mouse Position"), left, rowY + rowGap * 3, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Keybinding Profiles"), left, rowY + rowGap * 4, 0xFFFFFF);
    }

    @Override
    public void close() {
        KeyMapConfigManager.save();

        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        KeyMapConfigManager.save();
        super.removed();
    }

    private void resetDefaults() {
        config.keyboardLayout = "ANSI_US";
        config.hudScale = 1.0f;
        config.hudPosition = "CENTER";
        config.mousePosition = "RIGHT";
        init();
    }

    private Text scaleText() {
        return Text.literal(Math.round(config.hudScale * 100.0f) + "%");
    }

    private Text positionText() {
        return Text.literal(displayPosition(config.hudPosition));
    }

    private Text mouseText() {
        return Text.literal("LEFT".equals(config.mousePosition) ? "Left" : "Right");
    }

    private static float nextScale(float current) {
        int index = 0;

        for (int i = 0; i < SCALES.size(); i++) {
            if (Math.abs(SCALES.get(i) - current) < 0.001f) {
                index = i;
                break;
            }
        }

        return SCALES.get((index + 1) % SCALES.size());
    }

    private static String nextValue(List<String> values, String current, String fallback) {
        int index = values.indexOf(current);

        if (index < 0) {
            index = values.indexOf(fallback);
        }

        return values.get((index + 1) % values.size());
    }

    private static String displayPosition(String value) {
        if (value == null || value.isBlank()) {
            return "Center";
        }

        return switch (value) {
            case "TOP_LEFT" -> "Top Left";
            case "TOP_CENTER" -> "Top Center";
            case "TOP_RIGHT" -> "Top Right";
            case "CENTER_LEFT" -> "Center Left";
            case "CENTER_RIGHT" -> "Center Right";
            case "BOTTOM_LEFT" -> "Bottom Left";
            case "BOTTOM_CENTER" -> "Bottom Center";
            case "BOTTOM_RIGHT" -> "Bottom Right";
            default -> "Center";
        };
    }
}
