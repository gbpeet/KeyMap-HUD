package com.itinerant.keymaphud.client;

import com.itinerant.keymaphud.client.KeyboardLayout.KeyVisual;
import com.itinerant.keymaphud.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class OverlayRenderer {
    private static final int UNUSED_COLOR = 0xAA2E8B57;
    private static final int USED_COLOR = 0xAACC8800;
    private static final int CONFLICT_COLOR = 0xAACC3333;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private OverlayRenderer() {
    }

    public static void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        Map<Integer, Integer> bindCounts = countKeybinds(client.options.allKeys);

        int layoutWidth = 772;
        int layoutHeight = 210;
        int startX = (screenWidth - layoutWidth) / 2;
        int startY = (screenHeight - layoutHeight) / 2;

        context.fill(startX - 16, startY - 36, startX + layoutWidth + 16, startY + layoutHeight, 0xAA101010);
        context.drawBorder(startX - 16, startY - 36, layoutWidth + 32, layoutHeight + 36, BORDER_COLOR);

        Text title = Text.literal("KeyMap HUD");
        context.drawTextWithShadow(
                textRenderer,
                title,
                startX + (layoutWidth - textRenderer.getWidth(title)) / 2,
                startY - 24,
                TEXT_COLOR
        );

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            drawKey(context, textRenderer, bindCounts, key, startX, startY);
        }

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Green = unused   Amber = bound   Red = conflict"),
                startX + 12,
                startY + 128,
                0xFFCCCCCC
        );
    }

    private static Map<Integer, Integer> countKeybinds(KeyBinding[] allKeys) {
        Map<Integer, Integer> counts = new HashMap<>();

        for (KeyBinding binding : allKeys) {
            InputUtil.Key key =
                    ((KeyBindingAccessor) binding).getBoundKey();

            int keyCode = key.getCode();

            if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                counts.put(keyCode, counts.getOrDefault(keyCode, 0) + 1);
            }
        }

        return counts;
    }

    private static void drawKey(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, Integer> bindCounts,
            KeyVisual key,
            int startX,
            int startY
    ) {
        int count = bindCounts.getOrDefault(key.keyCode(), 0);
        int color = count == 0 ? UNUSED_COLOR : count == 1 ? USED_COLOR : CONFLICT_COLOR;

        int x = startX + key.x();
        int y = startY + key.y();

        context.fill(x, y, x + key.width(), y + key.height(), color);
        context.drawBorder(x, y, key.width(), key.height(), BORDER_COLOR);

        int textX = x + (key.width() - textRenderer.getWidth(key.label())) / 2;
        int textY = y + 7;

        context.drawTextWithShadow(textRenderer, Text.literal(key.label()), textX, textY, TEXT_COLOR);
    }
}