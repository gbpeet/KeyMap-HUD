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

    public static void renderScreen(DrawContext context, int mouseX, int mouseY, float delta, String searchQuery) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int layoutLeft = 0;
        int layoutRight = 900;
        int layoutTop = -62;
        int layoutBottom = 210;

        int layoutWidth = layoutRight - layoutLeft;
        int layoutHeight = layoutBottom - layoutTop;

        float scale = Math.min(
                (screenWidth - 24) / (float) layoutWidth,
                (screenHeight - 24) / (float) layoutHeight
        );
        scale = Math.min(scale, 1.0f);

        int originX = (int) ((screenWidth - layoutWidth * scale) / 2.0f - layoutLeft * scale);
        int originY = (int) ((screenHeight - layoutHeight * scale) / 2.0f - layoutTop * scale);

        int localMouseX = (int) ((mouseX - originX) / scale);
        int localMouseY = (int) ((mouseY - originY) / scale);

        KeyVisual hoveredKey = null;

        context.getMatrices().push();
        context.getMatrices().translate(originX, originY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        context.fill(layoutLeft - 16, layoutTop, layoutRight + 16, layoutBottom, 0xAA101010);
        context.drawBorder(layoutLeft - 16, layoutTop, layoutWidth + 32, layoutHeight, BORDER_COLOR);

        // Mouse cluster outline
        context.drawBorder(790, 56, 108, 126, BORDER_COLOR);
        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Mouse"),
                827,
                44,
                0xFFCCCCCC
        );

        Text title = Text.literal("KeyMap HUD");
        context.drawTextWithShadow(
                textRenderer,
                title,
                layoutLeft + (layoutWidth - textRenderer.getWidth(title)) / 2,
                -52,
                TEXT_COLOR
        );

        // Search box (placeholder)
        int searchX = layoutLeft + 12;
        int searchY = -24;
        int searchWidth = 260;
        int searchHeight = 16;

        context.fill(
                searchX,
                searchY,
                searchX + searchWidth,
                searchY + searchHeight,
                0xFF202020
        );

        context.drawBorder(
                searchX,
                searchY,
                searchWidth,
                searchHeight,
                BORDER_COLOR
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(searchQuery == null || searchQuery.isBlank() ? "Search..." : searchQuery + "_"),
                searchX + 6,
                searchY + 4,
                0xFFAAAAAA
        );

        // Statistics bar
        drawStatsBar(
                context,
                textRenderer,
                bindingsByKey,
                layoutLeft,
                layoutWidth
        );

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            drawKey(context, textRenderer, bindingsByKey, key, 0, 0, searchQuery);

            if (isMouseOverKey(key, 0, 0, localMouseX, localMouseY)) {
                hoveredKey = key;
            }
        }

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Green = unused   Amber = bound   Red = conflict"),
                layoutLeft + 12,
                188,
                0xFFCCCCCC
        );

        context.getMatrices().pop();

        if (hoveredKey != null) {
            drawTooltip(context, textRenderer, bindingsByKey, hoveredKey, mouseX, mouseY);
        }
    }

    private static Map<Integer, List<KeyBinding>> groupKeybinds(KeyBinding[] allKeys) {
        Map<Integer, List<KeyBinding>> bindingsByKey = new HashMap<>();

        for (KeyBinding binding : allKeys) {
            InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();

            int keyCode;
            switch (key.getCategory()) {
                case MOUSE -> keyCode = -100 + key.getCode();
                default -> keyCode = key.getCode();
            }

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
            int startY,
            String searchQuery
    ) {
        List<KeyBinding> bindings = bindingsByKey.getOrDefault(key.keyCode(), List.of());
        int count = bindings.size();

        boolean matchesSearch = matchesSearch(key, bindings, searchQuery);

        int color = count == 0 ? UNUSED_COLOR : count == 1 ? USED_COLOR : CONFLICT_COLOR;
        int textColor = TEXT_COLOR;

        if (!matchesSearch) {
            color = (color & 0x00FFFFFF) | 0x33000000;
            textColor = 0x66FFFFFF;
        }

        int x = startX + key.x();
        int y = startY + key.y();

        context.fill(x, y, x + key.width(), y + key.height(), color);
        context.drawBorder(x, y, key.width(), key.height(), matchesSearch ? BORDER_COLOR : 0x66FFFFFF);

        int labelY = count == 0 ? y + 7 : y + 2;
        int labelX = x + (key.width() - textRenderer.getWidth(key.label())) / 2;

        context.drawTextWithShadow(textRenderer, Text.literal(key.label()), labelX, labelY, textColor);

        if (count > 0) {
            String miniLabel = count > 1 ? count + "x" : makeMiniLabel(bindings.get(0));

            drawScaledCenteredText(
                    context,
                    textRenderer,
                    miniLabel,
                    x + key.width() / 2,
                    y + 13,
                    0.65f,
                    textColor
            );
        }
    }

    private static boolean matchesSearch(KeyVisual key, List<KeyBinding> bindings, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return true;
        }

        String query = searchQuery.toLowerCase();

        if (query.equals("mouse")) {
            return key.label().contains("MB");
        }

        if (query.equals("left mouse")) {
            return key.label().equals("LMB");
        }

        if (query.equals("right mouse")) {
            return key.label().equals("RMB");
        }

        if (query.equals("middle mouse")) {
            return key.label().equals("MMB");
        }

        if (key.label().toLowerCase().contains(query)) {
            return true;
        }

        for (KeyBinding binding : bindings) {
            String category = Text.translatable(binding.getCategory()).getString().toLowerCase();
            String action = Text.translatable(binding.getTranslationKey()).getString().toLowerCase();
            String mini = makeMiniLabel(binding).toLowerCase();

            if (category.contains(query) || action.contains(query) || mini.contains(query)) {
                return true;
            }
        }

        return false;
    }

    private static String makeMiniLabel(KeyBinding binding) {
        String name = Text.translatable(binding.getTranslationKey()).getString();

        name = name
                .replace("Open ", "")
                .replace("Toggle ", "")
                .replace("Show ", "")
                .replace("Use ", "")
                .replace("KeyMap HUD", "HUD")
                .trim();

        String[] words = name.split("[\\s_\\-/]+");

        if (words.length == 0 || name.isBlank()) {
            return "?";
        }

        if (words.length == 1) {
            String word = words[0].replaceAll("[^A-Za-z0-9]", "");
            return word.length() <= 3 ? word.toUpperCase() : word.substring(0, 3).toUpperCase();
        }

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isBlank() && Character.isLetterOrDigit(word.charAt(0))) {
                result.append(Character.toUpperCase(word.charAt(0)));
            }

            if (result.length() >= 3) {
                break;
            }
        }

        return result.isEmpty() ? "?" : result.toString();
    }

    private static void drawScaledCenteredText(
            DrawContext context,
            TextRenderer textRenderer,
            String text,
            int centerX,
            int y,
            float scale,
            int color
    ) {
        int textWidth = textRenderer.getWidth(text);
        float x = centerX - (textWidth * scale) / 2.0f;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawTextWithShadow(textRenderer, Text.literal(text), 0, 0, color);
        context.getMatrices().pop();
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

            lines.remove(lines.size() - 1);
        }

        int padding = 6;
        int lineHeight = 10;
        int width = 0;

        for (Text line : lines) {
            width = Math.max(width, textRenderer.getWidth(line));
        }

        int tooltipWidth = width + padding * 2;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int x = mouseX + 12;
        int y = mouseY + 12;

        if (x + tooltipWidth > screenWidth - 4) {
            x = mouseX - tooltipWidth - 12;
        }

        if (y + tooltipHeight > screenHeight - 4) {
            y = mouseY - tooltipHeight - 12;
        }

        x = Math.max(4, x);
        y = Math.max(4, y);

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500);

        context.fill(x - 1, y - 1, x + tooltipWidth + 1, y + tooltipHeight + 1, 0xFF000000);
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xFF202020);
        context.drawBorder(x, y, tooltipWidth, tooltipHeight, 0xFFFFFFFF);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFFFFF : 0xDDDDDD;
            context.drawTextWithShadow(
                    textRenderer,
                    lines.get(i),
                    x + padding,
                    y + padding + i * lineHeight,
                    color
            );
        }

        context.getMatrices().pop();
    }

    private static void drawStatsBar(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            int layoutLeft,
            int layoutWidth
    ) {
        int totalKeys = KeyboardLayout.ansiFull().size();
        int boundKeys = 0;
        int conflictKeys = 0;
        int totalBindings = 0;

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            List<KeyBinding> bindings = bindingsByKey.getOrDefault(key.keyCode(), List.of());

            if (!bindings.isEmpty()) {
                boundKeys++;
            }

            if (bindings.size() > 1) {
                conflictKeys++;
            }

            totalBindings += bindings.size();
        }

        int freeKeys = totalKeys - boundKeys;

        String stats = "Keys: " + totalKeys
                + "   Free: " + freeKeys
                + "   Bound: " + boundKeys
                + "   Conflicts: " + conflictKeys
                + "   Actions: " + totalBindings;

        int x = layoutLeft + (layoutWidth - textRenderer.getWidth(stats)) / 2;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(stats),
                x,
                -40,
                0xFFCCCCCC
        );
    }
}