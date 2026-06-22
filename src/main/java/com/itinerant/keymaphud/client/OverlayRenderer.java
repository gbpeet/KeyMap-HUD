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

    private static final int LAYOUT_LEFT = 0;
    private static final int LAYOUT_RIGHT = 900;
    private static final int LAYOUT_TOP = -62;
    private static final int LAYOUT_BOTTOM = 210;

    private static final int DRAWER_X = 700;
    private static final int DRAWER_WIDTH = 190;
    private static final int DRAWER_PADDING = 14;
    private static final int DRAWER_LINE_HEIGHT = 12;
    private static final int DRAWER_HEADER_Y_OFFSET = 12;
    private static final int DRAWER_CONTENT_Y_OFFSET = 32;

    private static final String[] QUICK_FILTERS = {
            "All",
            "Bound",
            "Unused",
            "Conflict",
            "Mouse",
            "Keyboard"
    };

    private OverlayRenderer() {
    }

    public static void renderScreen(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            String searchQuery,
            boolean filterDrawerOpen,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        LayoutInfo layout = getLayoutInfo();

        int localMouseX = (int) ((mouseX - layout.originX()) / layout.scale());
        int localMouseY = (int) ((mouseY - layout.originY()) / layout.scale());

        KeyVisual hoveredKey = null;

        context.getMatrices().push();
        context.getMatrices().translate(layout.originX(), layout.originY(), 0);
        context.getMatrices().scale(layout.scale(), layout.scale(), 1.0f);

        context.fill(LAYOUT_LEFT - 16, LAYOUT_TOP, LAYOUT_RIGHT + 16, LAYOUT_BOTTOM, 0xAA101010);
        context.drawBorder(LAYOUT_LEFT - 16, LAYOUT_TOP, layout.width() + 32, layout.height(), BORDER_COLOR);

        drawMouseClusterLabel(context, textRenderer);
        drawTitle(context, textRenderer);
        drawSearchAndTopFilters(context, textRenderer, searchQuery, filterDrawerOpen);
        drawStatsBar(context, textRenderer, bindingsByKey);

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            drawKey(context, textRenderer, bindingsByKey, key, searchQuery);

            if (isMouseOverKey(key, localMouseX, localMouseY)
                    && !(filterDrawerOpen && isLocalMouseInsideDrawer(localMouseX, localMouseY))) {
                hoveredKey = key;
            }
        }

        if (filterDrawerOpen) {
            drawFilterDrawer(
                    context,
                    textRenderer,
                    bindingsByKey,
                    layout,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded
            );
        }

        context.getMatrices().pop();

        if (hoveredKey != null) {
            drawTooltip(context, textRenderer, bindingsByKey, hoveredKey, mouseX, mouseY);
        }
    }

    private static void drawMouseClusterLabel(DrawContext context, TextRenderer textRenderer) {
        context.drawBorder(790, 56, 108, 126, BORDER_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Mouse"), 827, 44, 0xFFCCCCCC);
    }

    private static void drawTitle(DrawContext context, TextRenderer textRenderer) {
        Text title = Text.literal("KeyMap HUD");

        context.getMatrices().push();
        context.getMatrices().translate(LAYOUT_LEFT + 12, -56, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);
        context.drawTextWithShadow(textRenderer, title, 0, 0, TEXT_COLOR);
        context.getMatrices().pop();
    }

    private static void drawSearchAndTopFilters(
            DrawContext context,
            TextRenderer textRenderer,
            String searchQuery,
            boolean filterDrawerOpen
    ) {
        int searchX = LAYOUT_LEFT + 12;
        int searchY = -24;
        int searchWidth = 260;
        int searchHeight = 16;

        context.fill(searchX, searchY, searchX + searchWidth, searchY + searchHeight, 0xFF202020);
        context.drawBorder(searchX, searchY, searchWidth, searchHeight, BORDER_COLOR);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(searchQuery == null || searchQuery.isBlank() ? "Search..." : searchQuery + "_"),
                searchX + 6,
                searchY + 4,
                0xFFAAAAAA
        );

        int buttonX = searchX + searchWidth + 12;
        int buttonY = -24;

        for (String filter : QUICK_FILTERS) {
            int width = textRenderer.getWidth(filter) + 12;
            drawButton(context, textRenderer, filter, buttonX, buttonY, width);
            buttonX += width + 6;
        }

        int drawerButtonX = buttonX + 12;
        String drawerLabel = filterDrawerOpen ? "Filters ▲" : "Filters ▼";
        int drawerWidth = textRenderer.getWidth(drawerLabel) + 16;
        drawButton(context, textRenderer, drawerLabel, drawerButtonX, buttonY, drawerWidth);
    }

    private static void drawButton(DrawContext context, TextRenderer textRenderer, String label, int x, int y, int width) {
        context.fill(x, y, x + width, y + 14, 0xFF303030);
        context.drawBorder(x, y, width, 14, BORDER_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 6, y + 3, TEXT_COLOR);
    }

    private static void drawFilterDrawer(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            LayoutInfo layout,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded
    ) {
        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);

        context.fill(DRAWER_X, drawerY, DRAWER_X + DRAWER_WIDTH, drawerY + drawerHeight, 0xFF181818);
        context.drawBorder(DRAWER_X, drawerY, DRAWER_WIDTH, drawerHeight, BORDER_COLOR);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Filters"),
                DRAWER_X + DRAWER_PADDING,
                drawerY + DRAWER_HEADER_Y_OFFSET,
                TEXT_COLOR
        );

        int scissorX1 = (int) (layout.originX() + DRAWER_X * layout.scale());
        int scissorY1 = (int) (layout.originY() + (drawerY + 28) * layout.scale());
        int scissorX2 = (int) (layout.originX() + (DRAWER_X + DRAWER_WIDTH) * layout.scale());
        int scissorY2 = (int) (layout.originY() + (drawerY + drawerHeight) * layout.scale());

        context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        context.drawTextWithShadow(
                textRenderer,
                Text.literal((quickExpanded ? "▼ " : "▶ ") + "Quick"),
                itemX,
                itemY,
                0xFFAAAAAA
        );
        itemY += 14;

        if (quickExpanded) {
            for (String filter : QUICK_FILTERS) {
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal("• " + filter),
                        itemX,
                        itemY,
                        TEXT_COLOR
                );

                itemY += DRAWER_LINE_HEIGHT;
            }

            itemY += 10;
        }

        context.drawTextWithShadow(
                textRenderer,
                Text.literal((categoriesExpanded ? "▼ " : "▶ ") + "Categories"),
                itemX,
                itemY,
                0xFFAAAAAA
        );
        itemY += 14;

        if (categoriesExpanded) {
            for (String category : getCategories(bindingsByKey)) {
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal("• " + category),
                        itemX,
                        itemY,
                        TEXT_COLOR
                );

                itemY += DRAWER_LINE_HEIGHT;
            }

            itemY += 10;
        }

        context.drawTextWithShadow(
                textRenderer,
                Text.literal((modsExpanded ? "▼ " : "▶ ") + "Mods"),
                itemX,
                itemY,
                0xFFAAAAAA
        );
        itemY += 14;

        if (modsExpanded) {
            for (String modName : getMods(bindingsByKey)) {
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal("▶ " + modName),
                        itemX,
                        itemY,
                        TEXT_COLOR
                );

                itemY += DRAWER_LINE_HEIGHT;
            }
        }

        drawDrawerScrollbar(
                context,
                drawerY,
                drawerHeight,
                drawerScroll,
                quickExpanded,
                categoriesExpanded,
                modsExpanded
        );

        context.disableScissor();
        context.getMatrices().pop();
    }

    private static List<String> getCategories(Map<Integer, List<KeyBinding>> bindingsByKey) {
        return bindingsByKey.values().stream()
                .flatMap(List::stream)
                .map(binding -> Text.translatable(binding.getCategory()).getString())
                .filter(category ->
                        category.equals("Movement")
                                || category.equals("Gameplay")
                                || category.equals("Inventory")
                                || category.equals("Multiplayer")
                                || category.equals("Creative Mode")
                                || category.equals("Miscellaneous")
                                || category.equals("UI")
                )
                .distinct()
                .sorted()
                .toList();
    }

    private static List<String> getMods(Map<Integer, List<KeyBinding>> bindingsByKey) {
        return bindingsByKey.values().stream()
                .flatMap(List::stream)
                .map(binding -> Text.translatable(binding.getCategory()).getString())
                .filter(category ->
                        !category.equals("Movement")
                                && !category.equals("Gameplay")
                                && !category.equals("Inventory")
                                && !category.equals("Multiplayer")
                                && !category.equals("Creative Mode")
                                && !category.equals("Miscellaneous")
                                && !category.equals("UI")
                )
                .distinct()
                .sorted()
                .toList();
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
                bindingsByKey.computeIfAbsent(keyCode, ignored -> new ArrayList<>()).add(binding);
            }
        }

        return bindingsByKey;
    }

    private static void drawKey(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            KeyVisual key,
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

        int x = key.x();
        int y = key.y();

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

        if (query.equals("mouse")) return key.label().contains("MB");
        if (query.equals("left mouse")) return key.label().equals("LMB");
        if (query.equals("right mouse")) return key.label().equals("RMB");
        if (query.equals("middle mouse")) return key.label().equals("MMB");
        if (query.equals("unused")) return bindings.isEmpty();
        if (query.equals("bound")) return !bindings.isEmpty();
        if (query.equals("conflict") || query.equals("conflicts")) return bindings.size() > 1;
        if (query.equals("keyboard")) return key.keyCode() >= 0;

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

    private static int drawStatPart(
            DrawContext context,
            TextRenderer textRenderer,
            String text,
            int x,
            int y,
            int color
    ) {
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
        return x + textRenderer.getWidth(text);
    }

    private static boolean isMouseOverKey(KeyVisual key, int mouseX, int mouseY) {
        return mouseX >= key.x()
                && mouseX <= key.x() + key.width()
                && mouseY >= key.y()
                && mouseY <= key.y() + key.height();
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
            context.drawTextWithShadow(textRenderer, lines.get(i), x + padding, y + padding + i * lineHeight, color);
        }

        context.getMatrices().pop();
    }

    private static void drawStatsBar(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey
    ) {
        int totalKeys = KeyboardLayout.ansiFull().size();
        int boundKeys = 0;
        int conflictKeys = 0;

        for (KeyVisual key : KeyboardLayout.ansiFull()) {
            List<KeyBinding> bindings = bindingsByKey.getOrDefault(key.keyCode(), List.of());

            if (!bindings.isEmpty()) {
                boundKeys++;
            }

            if (bindings.size() > 1) {
                conflictKeys++;
            }
        }

        int freeKeys = totalKeys - boundKeys;

        int y = -40;
        int x = LAYOUT_LEFT + 290;

        x = drawStatPart(context, textRenderer, "Keys: " + totalKeys, x, y, 0xFFCCCCCC);
        x = drawStatPart(context, textRenderer, "Free: " + freeKeys, x + 14, y, 0xFF55FF99);
        x = drawStatPart(context, textRenderer, "Bound: " + boundKeys, x + 14, y, 0xFFFFCC55);
        drawStatPart(context, textRenderer, "Conflicts: " + conflictKeys, x + 14, y, 0xFFFF6666);
    }

    public static String getQuickFilterQueryAt(int mouseX, int mouseY) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int searchX = LAYOUT_LEFT + 12;
        int searchWidth = 260;

        int buttonX = searchX + searchWidth + 12;
        int buttonY = -24;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        for (String filter : QUICK_FILTERS) {
            int width = textRenderer.getWidth(filter) + 12;

            if (local.x() >= buttonX
                    && local.x() <= buttonX + width
                    && local.y() >= buttonY
                    && local.y() <= buttonY + 14) {
                return queryForFilter(filter);
            }

            buttonX += width + 6;
        }

        return null;
    }

    public static boolean isFilterDrawerButtonAt(int mouseX, int mouseY, boolean filterDrawerOpen) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int searchX = LAYOUT_LEFT + 12;
        int searchWidth = 260;

        int buttonX = searchX + searchWidth + 12;
        int buttonY = -24;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        for (String filter : QUICK_FILTERS) {
            int width = textRenderer.getWidth(filter) + 12;
            buttonX += width + 6;
        }

        int drawerButtonX = buttonX + 12;
        String drawerLabel = filterDrawerOpen ? "Filters ▲" : "Filters ▼";
        int drawerWidth = textRenderer.getWidth(drawerLabel) + 16;

        return local.x() >= drawerButtonX
                && local.x() <= drawerButtonX + drawerWidth
                && local.y() >= buttonY
                && local.y() <= buttonY + 14;
    }

    public static String getDrawerQuickFilterQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded
    ) {
        if (!quickExpanded) {
            return null;
        }

        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int drawerY = LAYOUT_TOP + 8;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        itemY += 14;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        for (String filter : QUICK_FILTERS) {
            int width = Math.max(90, textRenderer.getWidth(filter) + 18);

            if (local.x() >= itemX
                    && local.x() <= itemX + width
                    && local.y() >= itemY - 2
                    && local.y() <= itemY + 10) {
                return queryForFilter(filter);
            }

            itemY += DRAWER_LINE_HEIGHT;
        }

        return null;
    }

    public static String getDrawerCategoryQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded
    ) {
        if (!categoriesExpanded) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);
        List<String> categories = getCategories(bindingsByKey);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        itemY += 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT;
            itemY += 10;
        }

        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String category : categories) {
            int width = Math.max(120, textRenderer.getWidth(category) + 18);

            if (local.x() >= itemX
                    && local.x() <= itemX + width
                    && local.y() >= itemY - 2
                    && local.y() <= itemY + 10
                    && local.y() >= drawerY + 28
                    && local.y() <= drawerY + drawerHeight) {
                return category;
            }

            itemY += DRAWER_LINE_HEIGHT;
        }

        return null;
    }

    public static String getDrawerModQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded
    ) {
        if (!modsExpanded) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);
        List<String> mods = getMods(bindingsByKey);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        itemY += 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT;
            itemY += 10;
        }

        itemY += 14;

        if (categoriesExpanded) {
            itemY += getCategories(bindingsByKey).size() * DRAWER_LINE_HEIGHT;
            itemY += 10;
        }

        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String modName : mods) {
            int width = Math.max(120, textRenderer.getWidth(modName) + 18);

            if (local.x() >= itemX
                    && local.x() <= itemX + width
                    && local.y() >= itemY - 2
                    && local.y() <= itemY + 10
                    && local.y() >= drawerY + 28
                    && local.y() <= drawerY + drawerHeight) {
                return modName;
            }

            itemY += DRAWER_LINE_HEIGHT;
        }

        return null;
    }

    public static boolean isMouseInsideFilterDrawer(int mouseX, int mouseY) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        return local.x() >= DRAWER_X
                && local.x() <= DRAWER_X + DRAWER_WIDTH
                && local.y() >= drawerY
                && local.y() <= drawerY + drawerHeight;
    }

    public static int getMaxDrawerScroll(
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int visibleContentHeight = drawerHeight - 32;

        int contentHeight = 0;

        contentHeight += 14;

        if (quickExpanded) {
            contentHeight += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT;
            contentHeight += 10;
        }

        contentHeight += 14;

        if (categoriesExpanded) {
            contentHeight += getCategories(bindingsByKey).size() * DRAWER_LINE_HEIGHT;
            contentHeight += 10;
        }

        contentHeight += 14;

        if (modsExpanded) {
            contentHeight += getMods(bindingsByKey).size() * DRAWER_LINE_HEIGHT;
        }

        return Math.max(0, contentHeight - visibleContentHeight);
    }

    public static String getDrawerSectionHeaderAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded
    ) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int drawerY = LAYOUT_TOP + 8;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        if (isInsideDrawerHeader(local, itemX, itemY)) {
            return "quick";
        }

        itemY += 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT;
            itemY += 10;
        }

        if (isInsideDrawerHeader(local, itemX, itemY)) {
            return "categories";
        }

        itemY += 14;

        if (categoriesExpanded) {
            MinecraftClient client = MinecraftClient.getInstance();
            Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

            itemY += getCategories(bindingsByKey).size() * DRAWER_LINE_HEIGHT;
            itemY += 10;
        }

        if (isInsideDrawerHeader(local, itemX, itemY)) {
            return "mods";
        }

        return null;
    }

    private static void drawDrawerScrollbar(
            DrawContext context,
            int drawerY,
            int drawerHeight,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded
    ) {
        int maxScroll = getMaxDrawerScroll(quickExpanded, categoriesExpanded, modsExpanded);

        if (maxScroll <= 0) {
            return;
        }

        int trackX = DRAWER_X + DRAWER_WIDTH - 6;
        int trackY = drawerY + 28;
        int trackHeight = drawerHeight - 34;

        context.fill(trackX, trackY, trackX + 3, trackY + trackHeight, 0xFF303030);

        int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * (drawerScroll / (float) maxScroll));

        context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private static boolean isLocalMouseInsideDrawer(int localMouseX, int localMouseY) {
        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        return localMouseX >= DRAWER_X
                && localMouseX <= DRAWER_X + DRAWER_WIDTH
                && localMouseY >= drawerY
                && localMouseY <= drawerY + drawerHeight;
    }

    private static boolean isInsideDrawerHeader(LocalMouse local, int itemX, int itemY) {
        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        return local.x() >= itemX
                && local.x() <= DRAWER_X + DRAWER_WIDTH - 10
                && local.y() >= itemY - 2
                && local.y() <= itemY + 10
                && local.y() >= drawerY + 28
                && local.y() <= drawerY + drawerHeight;
    }

    private static String queryForFilter(String filter) {
        return switch (filter) {
            case "All" -> "";
            case "Bound" -> "bound";
            case "Unused" -> "unused";
            case "Conflict" -> "conflict";
            case "Mouse" -> "mouse";
            case "Keyboard" -> "keyboard";
            default -> "";
        };
    }

    private static LayoutInfo getLayoutInfo() {
        MinecraftClient client = MinecraftClient.getInstance();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int layoutWidth = LAYOUT_RIGHT - LAYOUT_LEFT;
        int layoutHeight = LAYOUT_BOTTOM - LAYOUT_TOP;

        float scale = Math.min(
                (screenWidth - 24) / (float) layoutWidth,
                (screenHeight - 24) / (float) layoutHeight
        );
        scale = Math.min(scale, 1.0f);

        int originX = (int) ((screenWidth - layoutWidth * scale) / 2.0f - LAYOUT_LEFT * scale);
        int originY = (int) ((screenHeight - layoutHeight * scale) / 2.0f - LAYOUT_TOP * scale);

        return new LayoutInfo(originX, originY, scale, layoutWidth, layoutHeight);
    }

    private static LocalMouse toLocalMouse(int mouseX, int mouseY, LayoutInfo layout) {
        return new LocalMouse(
                (int) ((mouseX - layout.originX()) / layout.scale()),
                (int) ((mouseY - layout.originY()) / layout.scale())
        );
    }

    private record LayoutInfo(int originX, int originY, float scale, int width, int height) {
    }

    private record LocalMouse(int x, int y) {
    }
}