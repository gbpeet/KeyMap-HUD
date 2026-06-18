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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

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

        KeyVisual hoveredKey = null;

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            drawKey(context, textRenderer, bindingsByKey, key, startX, startY);

            if (isMouseOverKey(key, startX, startY, mouseX, mouseY)) {
                hoveredKey = key;
            }
        }

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Green = unused   Amber = bound   Red = conflict"),
                startX + 12,
                startY + 188,
                0xFFCCCCCC
        );

        if (hoveredKey != null) {
            drawTooltip(context, textRenderer, bindingsByKey, hoveredKey, mouseX, mouseY);
        }
    }

    private static Map<Integer, List<KeyBinding>> groupKeybinds(KeyBinding[] allKeys) {
        Map<Integer, List<KeyBinding>> bindingsByKey = new HashMap<>();

        for (KeyBinding binding : allKeys) {
            InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();
            int keyCode = key.getCode();

            if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                bindingsByKey
                        .computeIfAbsent(keyCode, ignored -> new ArrayList<>())
                        .add(binding);
            }
        }

        return bindingsByKey;
    }

    private static void drawKey(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            KeyVisual key,
            int startX,
            int startY
    ) {
        List<KeyBinding> bindings = bindingsByKey.getOrDefault(key.keyCode(), List.of());
        int count = bindings.size();

        int color = count == 0 ? UNUSED_COLOR : count == 1 ? USED_COLOR : CONFLICT_COLOR;

        int x = startX + key.x();
        int y = startY + key.y();

        context.fill(x, y, x + key.width(), y + key.height(), color);
        context.drawBorder(x, y, key.width(), key.height(), BORDER_COLOR);

        int textX = x + (key.width() - textRenderer.getWidth(key.label())) / 2;
        int textY = y + 7;

        context.drawTextWithShadow(textRenderer, Text.literal(key.label()), textX, textY, TEXT_COLOR);
    }

    private static boolean isMouseOverKey(KeyVisual key, int startX, int startY, int mouseX, int mouseY) {
        int x = startX + key.x();
        int y = startY + key.y();

        return mouseX >= x
                && mouseX <= x + key.width()
                && mouseY >= y
                && mouseY <= y + key.height();
    }

    private static void drawTooltip(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            KeyVisual key,
            int mouseX,
            int mouseY
    ) {
        List<KeyBinding> bindings = bindingsByKey.getOrDefault(key.keyCode(), List.of());

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(key.label()));

        if (bindings.isEmpty()) {
            lines.add(Text.literal("Unused"));
        } else {
            lines.add(Text.literal(""));

            for (KeyBinding binding : bindings) {
                lines.add(Text.translatable(binding.getCategory()));
                lines.add(Text.translatable(binding.getTranslationKey()));
                lines.add(Text.literal(""));
            }

            if (!lines.isEmpty()) {
                lines.remove(lines.size() - 1);
            }
        }

        int padding = 6;
        int lineHeight = 10;
        int width = 0;

        for (Text line : lines) {
            width = Math.max(width, textRenderer.getWidth(line));
        }

        int tooltipWidth = width + padding * 2;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        int x = mouseX + 12;
        int y = mouseY + 12;

        context.fill(x - 1, y - 1, x + tooltipWidth + 1, y + tooltipHeight + 1, 0xFF000000);
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xFF202020);
        context.drawBorder(x, y, tooltipWidth, tooltipHeight, 0xFFFFFFFF);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFFFFF : 0xDDDDDD;
            context.drawTextWithShadow(textRenderer, lines.get(i), x + padding, y + padding + i * lineHeight, color);
        }
    }
}