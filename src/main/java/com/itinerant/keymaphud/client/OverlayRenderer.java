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

    private static final int DRAWER_X = 650;
    private static final int DRAWER_WIDTH = 240;
    private static final int DRAWER_PADDING = 14;
    private static final int DRAWER_LINE_HEIGHT = 12;
    private static final int DRAWER_HEADER_Y_OFFSET = 12;
    private static final int DRAWER_CONTENT_Y_OFFSET = 44;

    private static final int DRAWER_HOVER_COLOR = 0x553A3A3A;
    private static final int DRAWER_SELECTED_COLOR = 0x885577AA;

    private static final int ACTION_BOUND_COLOR = 0xFFFFCC55;
    private static final int ACTION_UNBOUND_COLOR = 0xFF888888;
    private static final int ACTION_CONFLICT_COLOR = 0xFFFF6666;

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
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods,
            boolean bindingMode,
            KeyBinding bindingTarget,
            InputUtil.Key lastBoundKey,
            long lastBoundAtMillis,
            KeyMapConfig config,
            boolean labelEditMode,
            Integer labelEditKeyCode,
            String labelEditText
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
        boolean bindingLabelHovered = false;

        context.getMatrices().push();
        context.getMatrices().translate(layout.originX(), layout.originY(), 0);
        context.getMatrices().scale(layout.scale(), layout.scale(), 1.0f);

        context.fill(LAYOUT_LEFT - 16, LAYOUT_TOP, LAYOUT_RIGHT + 16, LAYOUT_BOTTOM, 0xAA101010);
        context.drawBorder(LAYOUT_LEFT - 16, LAYOUT_TOP, layout.width() + 32, layout.height(), BORDER_COLOR);

        drawMouseClusterLabel(context, textRenderer);
        drawTitle(context, textRenderer);
        drawSearchAndTopFilters(context, textRenderer, searchQuery, filterDrawerOpen, bindingMode, bindingTarget, labelEditMode, labelEditKeyCode, labelEditText);
        drawStatsBar(context, textRenderer, bindingsByKey);

        for (KeyVisual key : KeyboardLayout.current()) {
            boolean isHoveredForBinding = bindingMode && isMouseOverKey(key, localMouseX, localMouseY);

            drawKey(context, textRenderer, bindingsByKey, key, searchQuery, config.getLabel(key.keyCode()));

            long flashElapsed = System.currentTimeMillis() - lastBoundAtMillis;

            boolean flashVisible =
                    flashElapsed < 900
                            && (flashElapsed % 300) < 150;

            if (lastBoundKey != null
                    && flashVisible
                    && key.keyCode() == keyCodeFromInputKey(lastBoundKey)) {

                context.drawBorder(
                        key.x() - 3,
                        key.y() - 3,
                        key.width() + 6,
                        key.height() + 6,
                        0xFFFFCC55
                );

                context.drawBorder(
                        key.x() - 4,
                        key.y() - 4,
                        key.width() + 8,
                        key.height() + 8,
                        0xFFFFFFFF
                );
            }

            if (isHoveredForBinding) {
                context.drawBorder(
                        key.x() - 2,
                        key.y() - 2,
                        key.width() + 4,
                        key.height() + 4,
                        0xFFFFFFFF
                );

                context.drawBorder(
                        key.x() - 3,
                        key.y() - 3,
                        key.width() + 6,
                        key.height() + 6,
                        0xFFFFCC55
                );
            }

            if (isMouseOverKey(key, localMouseX, localMouseY)
                    && !(filterDrawerOpen && isLocalMouseInsideDrawer(localMouseX, localMouseY))) {
                hoveredKey = key;
            }
        }

        if (filterDrawerOpen && !bindingMode && !labelEditMode) {
            KeyBinding hoveredCategoryBinding = getDrawerCategoryActionBindingClickAt(
                    mouseX,
                    mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    expandedCategories
            );

            KeyBinding hoveredModBinding = getDrawerActionBindingClickAt(
                    mouseX,
                    mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );

            bindingLabelHovered =
                    hoveredCategoryBinding != null
                            || hoveredModBinding != null;

            drawFilterDrawer(
                    context,
                    textRenderer,
                    bindingsByKey,
                    layout,
                    mouseX,
                    mouseY,
                    searchQuery,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );
        }

        context.getMatrices().pop();

        if (hoveredKey != null) {
            drawTooltip(context, textRenderer, bindingsByKey, hoveredKey, mouseX, mouseY, config.getLabel(hoveredKey.keyCode()));
        }

        if (bindingLabelHovered) {
            drawSimpleTooltip(
                    context,
                    textRenderer,
                    "Click to assign a key",
                    mouseX,
                    mouseY
            );
        }
    }

    private static void drawMouseClusterLabel(DrawContext context, TextRenderer textRenderer) {
        boolean left = "LEFT".equals(KeyMapConfigManager.get().mousePosition);
        int clusterX = left ? 0 : 790;
        int labelX = left ? 37 : 827;

        context.drawBorder(clusterX, 56, 108, 126, BORDER_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal("Mouse"), labelX, 44, 0xFFCCCCCC);
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
            boolean filterDrawerOpen,
            boolean bindingMode,
            KeyBinding bindingTarget,
            boolean labelEditMode,
            Integer labelEditKeyCode,
            String labelEditText
    ) {
        int searchX = LAYOUT_LEFT + 12;
        int searchY = -24;
        int searchWidth = 260;
        int searchHeight = 16;

        context.fill(searchX, searchY, searchX + searchWidth, searchY + searchHeight, 0xFF202020);
        context.drawBorder(searchX, searchY, searchWidth, searchHeight, BORDER_COLOR);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(displaySearchQuery(searchQuery, bindingMode)),
                searchX + 6,
                searchY + 4,
                0xFFAAAAAA
        );

        if (labelEditMode && labelEditKeyCode != null) {
            String keyName = getVisualKeyName(labelEditKeyCode);
            String cursor = (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "";

            int promptX = searchX + searchWidth + 12;
            int promptY = searchY + 4;
            int x = promptX;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("Label " + keyName + ": "),
                    x,
                    promptY,
                    0xFFFFFFFF
            );

            x += textRenderer.getWidth("Label " + keyName + ": ");

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(labelEditText + cursor),
                    x,
                    promptY,
                    0xFFFFCC55
            );

            x += textRenderer.getWidth(labelEditText + "_");

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("   ENTER = Save   ESC = Cancel   Middle-click = Clear   Max 6 characters"),
                    x,
                    promptY,
                    0xFFFF6666
            );

            return;
        }

        if (bindingMode && bindingTarget != null) {
            String actionName = Text.translatable(bindingTarget.getTranslationKey()).getString();

            int promptX = searchX + searchWidth + 12;
            int promptY = searchY + 4;

            int x = promptX;

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("Select a key for: "),
                    x,
                    promptY,
                    0xFFFFFFFF
            );

            x += textRenderer.getWidth("Select a key for: ");

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(actionName),
                    x,
                    promptY,
                    0xFFFFCC55
            );

            x += textRenderer.getWidth(actionName);

            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal("   ESC = Unbind   Right-click = Cancel"),
                    x,
                    promptY,
                    0xFFFF6666
            );

            return;
        }

        int buttonX = searchX + searchWidth + 12;
        int buttonY = -24;

        for (String filter : QUICK_FILTERS) {
            int width = textRenderer.getWidth(filter) + 12;
            drawButton(context, textRenderer, filter, buttonX, buttonY, width);
            buttonX += width + 6;
        }

        int drawerButtonX = buttonX;
        String drawerLabel = filterDrawerOpen ? "Filters ▲" : "Filters ▼";
        int drawerWidth = textRenderer.getWidth(drawerLabel) + 16;
        drawButton(context, textRenderer, drawerLabel, drawerButtonX, buttonY, drawerWidth);
    }

    private static void drawButton(DrawContext context, TextRenderer textRenderer, String label, int x, int y, int width) {
        context.fill(x, y, x + width, y + 14, 0xFF303030);
        context.drawBorder(x, y, width, 14, BORDER_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 6, y + 3, TEXT_COLOR);
    }

    private static void drawDrawerRowBackground(
            DrawContext context,
            int x,
            int y,
            int width,
            boolean hovered,
            boolean selected
    ) {
        if (selected) {
            context.fill(
                    x - 4,
                    y - 2,
                    x + width + 4,
                    y + DRAWER_LINE_HEIGHT - 1,
                    DRAWER_SELECTED_COLOR
            );
        } else if (hovered) {
            context.fill(
                    x - 4,
                    y - 2,
                    x + width + 4,
                    y + DRAWER_LINE_HEIGHT - 1,
                    DRAWER_HOVER_COLOR
            );
        }
    }

    private static void drawFilterDrawer(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey,
            LayoutInfo layout,
            int mouseX,
            int mouseY,
            String searchQuery,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;

        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);
        int localMouseX = local.x();
        int localMouseY = local.y();

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

        context.drawTextWithShadow(
                textRenderer,
                Text.literal("Click key labels to rebind"),
                DRAWER_X + DRAWER_PADDING,
                drawerY + DRAWER_HEADER_Y_OFFSET + 11,
                0xFF888888
        );

        int scissorX1 = (int) (layout.originX() + DRAWER_X * layout.scale());
        int scissorY1 = (int) (layout.originY() + (drawerY + 40) * layout.scale());
        int scissorX2 = (int) (layout.originX() + (DRAWER_X + DRAWER_WIDTH) * layout.scale());
        int scissorY2 = (int) (layout.originY() + (drawerY + drawerHeight) * layout.scale());

        context.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        context.drawTextWithShadow(textRenderer, Text.literal((quickExpanded ? "▼ " : "▶ ") + "Quick"), itemX, itemY, 0xFFAAAAAA);
        itemY += 14;

        if (quickExpanded) {
            for (String filter : QUICK_FILTERS) {
                String filterQuery = queryForFilter(filter);
                boolean selected = searchQuery.equals(filterQuery);
                boolean hovered = localMouseX >= itemX
                        && localMouseX <= DRAWER_X + DRAWER_WIDTH - 10
                        && localMouseY >= itemY - 2
                        && localMouseY <= itemY + 10;

                drawDrawerRowBackground(context, itemX, itemY, 160, hovered, selected);
                context.drawTextWithShadow(textRenderer, Text.literal("• " + filter), itemX, itemY, TEXT_COLOR);
                itemY += DRAWER_LINE_HEIGHT;
            }
            itemY += 10;
        }

        context.drawTextWithShadow(textRenderer, Text.literal((categoriesExpanded ? "▼ " : "▶ ") + "Categories"), itemX, itemY, 0xFFAAAAAA);
        itemY += 14;

        if (categoriesExpanded) {
            for (String category : getCategories(bindingsByKey)) {
                boolean categoryExpanded = expandedCategories.contains(category);
                boolean selected = searchQuery.equals("category:" + category);
                boolean hovered = localMouseX >= itemX
                        && localMouseX <= DRAWER_X + DRAWER_WIDTH - 10
                        && localMouseY >= itemY - 2
                        && localMouseY <= itemY + 10;

                drawDrawerRowBackground(context, itemX, itemY, 160, hovered, selected);
                context.drawTextWithShadow(
                        textRenderer,
                        Text.literal((categoryExpanded ? "▼ " : "▶ ") + category),
                        itemX,
                        itemY,
                        TEXT_COLOR
                );
                itemY += DRAWER_LINE_HEIGHT;

                if (categoryExpanded) {
                    for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), category)) {
                        String actionName = Text.translatable(binding.getTranslationKey()).getString();
                        String actionQuery = "action:" + category + "|" + actionName;
                        String bindingLabel = getBindingDisplayName(binding);
                        int actionColor = getActionStatusColor(binding);
                        boolean actionSelected = searchQuery.equals(actionQuery);
                        boolean actionHovered = localMouseX >= itemX + 12
                                && localMouseX <= DRAWER_X + DRAWER_WIDTH - 10
                                && localMouseY >= itemY - 2
                                && localMouseY <= itemY + 10;

                        drawDrawerRowBackground(
                                context, itemX + 12, itemY,
                                DRAWER_WIDTH - DRAWER_PADDING - 24,
                                actionHovered, actionSelected
                        );

                        int bindingLabelX = DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING - textRenderer.getWidth(bindingLabel);
                        int actionMaxWidth = Math.max(30, bindingLabelX - itemX - 18);

                        context.drawTextWithShadow(
                                textRenderer,
                                Text.literal("  • " + truncateToWidth(textRenderer, actionName, actionMaxWidth)),
                                itemX, itemY, actionColor
                        );
                        context.drawTextWithShadow(textRenderer, Text.literal(bindingLabel), bindingLabelX, itemY, actionColor);
                        itemY += DRAWER_LINE_HEIGHT;
                    }
                    itemY += 4;
                }
            }
            itemY += 10;
        }

        context.drawTextWithShadow(textRenderer, Text.literal((modsExpanded ? "▼ " : "▶ ") + "Mods"), itemX, itemY, 0xFFAAAAAA);
        itemY += 14;

        if (modsExpanded) {
            for (String modName : getMods(bindingsByKey)) {
                boolean modExpanded = expandedMods.contains(modName);
                boolean selected = searchQuery.equals("mod:" + modName);
                boolean hovered = localMouseX >= itemX
                        && localMouseX <= DRAWER_X + DRAWER_WIDTH - 10
                        && localMouseY >= itemY - 2
                        && localMouseY <= itemY + 10;

                drawDrawerRowBackground(context, itemX, itemY, 160, hovered, selected);
                context.drawTextWithShadow(textRenderer, Text.literal((modExpanded ? "▼ " : "▶ ") + modName), itemX, itemY, TEXT_COLOR);
                itemY += DRAWER_LINE_HEIGHT;

                if (modExpanded) {
                    for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), modName)) {
                        String actionName = Text.translatable(binding.getTranslationKey()).getString();
                        String actionQuery = "action:" + modName + "|" + actionName;
                        String bindingLabel = getBindingDisplayName(binding);
                        int actionColor = getActionStatusColor(binding);
                        boolean actionSelected = searchQuery.equals(actionQuery);
                        boolean actionHovered = localMouseX >= itemX + 12
                                && localMouseX <= DRAWER_X + DRAWER_WIDTH - 10
                                && localMouseY >= itemY - 2
                                && localMouseY <= itemY + 10;

                        drawDrawerRowBackground(
                                context, itemX + 12, itemY,
                                DRAWER_WIDTH - DRAWER_PADDING - 24,
                                actionHovered, actionSelected
                        );

                        int bindingLabelX = DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING - textRenderer.getWidth(bindingLabel);
                        int actionMaxWidth = Math.max(30, bindingLabelX - itemX - 18);

                        context.drawTextWithShadow(
                                textRenderer,
                                Text.literal("  • " + truncateToWidth(textRenderer, actionName, actionMaxWidth)),
                                itemX, itemY, actionColor
                        );
                        context.drawTextWithShadow(textRenderer, Text.literal(bindingLabel), bindingLabelX, itemY, actionColor);
                        itemY += DRAWER_LINE_HEIGHT;
                    }
                    itemY += 4;
                }
            }
        }

        drawDrawerScrollbar(
                context, drawerY, drawerHeight, drawerScroll,
                quickExpanded, categoriesExpanded, modsExpanded,
                expandedCategories, expandedMods
        );

        context.disableScissor();
        context.getMatrices().pop();
    }


    private static List<String> getCategories(Map<Integer, List<KeyBinding>> bindingsByKey) {
        return java.util.Arrays.stream(getAllKeyBindings())
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
        return java.util.Arrays.stream(getAllKeyBindings())
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

    private static List<KeyBinding> getBindingsForCategory(
            KeyBinding[] allKeys,
            String categoryName
    ) {
        return java.util.Arrays.stream(allKeys)
                .filter(binding -> Text.translatable(binding.getCategory()).getString().equals(categoryName))
                .distinct()
                .sorted(java.util.Comparator.comparing(binding ->
                        Text.translatable(binding.getTranslationKey()).getString()
                ))
                .toList();
    }

    private static KeyBinding[] getAllKeyBindings() {
        return MinecraftClient.getInstance().options.allKeys;
    }

    private static String getBindingDisplayName(KeyBinding binding) {
        String vanillaName = binding.getBoundKeyLocalizedText().getString();

        if (vanillaName.equals("Not Bound")) {
            return "Unbound";
        }

        return vanillaName;
    }

    private static int getActionStatusColor(KeyBinding binding) {
        int keyCode = getKeyCodeForBinding(binding);

        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return ACTION_UNBOUND_COLOR;
        }

        int conflictCount = 0;

        for (KeyBinding otherBinding : getAllKeyBindings()) {
            if (getKeyCodeForBinding(otherBinding) == keyCode) {
                conflictCount++;
            }
        }

        if (conflictCount > 1) {
            return ACTION_CONFLICT_COLOR;
        }

        return ACTION_BOUND_COLOR;
    }

    private static int getKeyCodeForBinding(KeyBinding binding) {
        InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();

        if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN
                && !binding.getBoundKeyLocalizedText().getString().equals("Not Bound")) {
            key = binding.getDefaultKey();
        }

        if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }

        return switch (key.getCategory()) {
            case MOUSE -> -100 + key.getCode();
            default -> key.getCode();
        };
    }

    private static int keyCodeFromInputKey(InputUtil.Key key) {
        if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }

        return switch (key.getCategory()) {
            case MOUSE -> -100 + key.getCode();
            default -> key.getCode();
        };
    }

    private static Map<Integer, List<KeyBinding>> groupKeybinds(KeyBinding[] allKeys) {
        Map<Integer, List<KeyBinding>> bindingsByKey = new HashMap<>();

        for (KeyBinding binding : allKeys) {
            InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();

            if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN
                    && !binding.getBoundKeyLocalizedText().getString().equals("Not Bound")) {
                key = binding.getDefaultKey();
            }

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
            String searchQuery,
            String customLabel
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

        boolean hasCustomLabel = customLabel != null && !customLabel.isBlank();

        int labelY = hasCustomLabel ? y + 2 : y + 7;
        int labelX = x + (key.width() - textRenderer.getWidth(key.label())) / 2;

        context.drawTextWithShadow(textRenderer, Text.literal(key.label()), labelX, labelY, textColor);

        if (hasCustomLabel) {
            drawScaledCenteredText(
                    context,
                    textRenderer,
                    customLabel,
                    x + key.width() / 2,
                    y + 13,
                    0.65f,
                    0xFFCCCCCC
            );
        }
    }

    private static boolean matchesSearch(KeyVisual key, List<KeyBinding> bindings, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return true;
        }

        String query = searchQuery.toLowerCase();

        if (query.startsWith("category:")) {
            String targetCategory = searchQuery.substring("category:".length());

            for (KeyBinding binding : bindings) {
                String category = Text.translatable(binding.getCategory()).getString();

                if (category.equals(targetCategory)) {
                    return true;
                }
            }

            return false;
        }

        if (query.startsWith("mod:")) {
            String targetMod = searchQuery.substring("mod:".length());

            for (KeyBinding binding : bindings) {
                String category = Text.translatable(binding.getCategory()).getString();

                if (category.equals(targetMod)) {
                    return true;
                }
            }

            return false;
        }

        if (query.startsWith("action:")) {
            String exact = searchQuery.substring("action:".length());

            int separator = exact.indexOf('|');

            if (separator > 0) {
                String targetCategory = exact.substring(0, separator);
                String targetAction = exact.substring(separator + 1);

                for (KeyBinding binding : bindings) {
                    String category = Text.translatable(binding.getCategory()).getString();
                    String action = Text.translatable(binding.getTranslationKey()).getString();

                    if (category.equals(targetCategory) && action.equals(targetAction)) {
                        return true;
                    }
                }
            }

            return false;
        }

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

    private static String truncateToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);

        String result = text;

        while (!result.isEmpty() && textRenderer.getWidth(result) + ellipsisWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result + ellipsis;
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
            int mouseY,
            String customLabel
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

        lines.add(Text.literal(""));

        if (customLabel != null && !customLabel.isBlank()) {
            lines.add(Text.literal("Label: " + customLabel));
            lines.add(Text.literal("Right-click to edit label"));
            lines.add(Text.literal("Middle-click to clear label"));
        } else {
            lines.add(Text.literal("Right-click to add label"));
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

    private static void drawSimpleTooltip(
            DrawContext context,
            TextRenderer textRenderer,
            String message,
            int mouseX,
            int mouseY
    ) {
        int padding = 6;
        int width = textRenderer.getWidth(message);
        int tooltipWidth = width + padding * 2;
        int tooltipHeight = 10 + padding * 2;

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

        context.fill(
                x - 1,
                y - 1,
                x + tooltipWidth + 1,
                y + tooltipHeight + 1,
                0xFF000000
        );

        context.fill(
                x,
                y,
                x + tooltipWidth,
                y + tooltipHeight,
                0xFF202020
        );

        context.drawBorder(
                x,
                y,
                tooltipWidth,
                tooltipHeight,
                0xFFFFFFFF
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(message),
                x + padding,
                y + padding,
                0xFFDDDDDD
        );

        context.getMatrices().pop();
    }

    private static void drawStatsBar(
            DrawContext context,
            TextRenderer textRenderer,
            Map<Integer, List<KeyBinding>> bindingsByKey
    ) {
        int totalKeys = KeyboardLayout.current().size();
        int boundKeys = 0;
        int conflictKeys = 0;

        for (KeyVisual key : KeyboardLayout.current()) {
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

        int drawerButtonX = buttonX;
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
            boolean categoriesExpanded,
            java.util.Set<String> expandedCategories
    ) {
        if (!categoriesExpanded) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll + 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }
        itemY += 14;

        for (String category : getCategories(bindingsByKey)) {
            boolean visible = local.y() >= drawerY + 40 && local.y() <= drawerY + drawerHeight;
            boolean onRow = local.y() >= itemY - 2 && local.y() <= itemY + 10;
            boolean onName = local.x() >= itemX + 12 && local.x() <= DRAWER_X + DRAWER_WIDTH - 10;

            if (visible && onRow && onName) return "category:" + category;

            itemY += DRAWER_LINE_HEIGHT;
            if (expandedCategories.contains(category)) {
                itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT + 4;
            }
        }
        return null;
    }


    public static String getDrawerCategoryArrowAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            java.util.Set<String> expandedCategories
    ) {
        if (!categoriesExpanded) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll + 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }
        itemY += 14;

        for (String category : getCategories(bindingsByKey)) {
            boolean visible = local.y() >= drawerY + 40 && local.y() <= drawerY + drawerHeight;
            boolean onRow = local.y() >= itemY - 2 && local.y() <= itemY + 10;
            boolean onArrow = local.x() >= itemX && local.x() <= itemX + 10;

            if (visible && onRow && onArrow) return category;

            itemY += DRAWER_LINE_HEIGHT;
            if (expandedCategories.contains(category)) {
                itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT + 4;
            }
        }
        return null;
    }

    public static String getDrawerCategoryActionQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            java.util.Set<String> expandedCategories
    ) {
        if (!categoriesExpanded) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll + 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }
        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String category : getCategories(bindingsByKey)) {
            itemY += DRAWER_LINE_HEIGHT;

            if (expandedCategories.contains(category)) {
                for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), category)) {
                    String actionName = Text.translatable(binding.getTranslationKey()).getString();
                    String bindingLabel = getBindingDisplayName(binding);
                    int bindingLabelX = DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING - textRenderer.getWidth(bindingLabel);

                    boolean visible = local.y() >= drawerY + 40 && local.y() <= drawerY + drawerHeight;
                    boolean onRow = local.y() >= itemY - 2 && local.y() <= itemY + 10;
                    boolean onAction = local.x() >= itemX + 12 && local.x() < bindingLabelX - 6;

                    if (visible && onRow && onAction) {
                        return "action:" + category + "|" + actionName;
                    }
                    itemY += DRAWER_LINE_HEIGHT;
                }
                itemY += 4;
            }
        }
        return null;
    }

    public static KeyBinding getDrawerCategoryActionBindingClickAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            java.util.Set<String> expandedCategories
    ) {
        if (!categoriesExpanded) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerY = LAYOUT_TOP + 8;
        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll + 14;

        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }
        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String category : getCategories(bindingsByKey)) {
            itemY += DRAWER_LINE_HEIGHT;

            if (expandedCategories.contains(category)) {
                for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), category)) {
                    String bindingLabel = getBindingDisplayName(binding);
                    int bindingLabelX = DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING - textRenderer.getWidth(bindingLabel);

                    boolean visible = local.y() >= drawerY + 40 && local.y() <= drawerY + drawerHeight;
                    boolean onRow = local.y() >= itemY - 2 && local.y() <= itemY + 10;
                    boolean onBinding = local.x() >= bindingLabelX - 6
                            && local.x() <= DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING;

                    if (visible && onRow && onBinding) return binding;
                    itemY += DRAWER_LINE_HEIGHT;
                }
                itemY += 4;
            }
        }
        return null;
    }

    public static String getDrawerModQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
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
            for (String category : getCategories(bindingsByKey)) {
                itemY += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT;
                    itemY += 4;
                }
            }
            itemY += 10;
        }

        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String modName : mods) {
            int width = Math.max(120, textRenderer.getWidth(modName) + 18);

            boolean insideVisibleDrawer =
                    local.y() >= drawerY + 28
                            && local.y() <= drawerY + drawerHeight;

            boolean onThisRow =
                    local.y() >= itemY - 2
                            && local.y() <= itemY + 10;

            boolean onModName =
                    local.x() >= itemX + 12
                            && local.x() <= itemX + width;

            if (insideVisibleDrawer && onThisRow && onModName) {
                return "mod:" + modName;
            }

            itemY += DRAWER_LINE_HEIGHT;

            if (expandedMods.contains(modName)) {
                itemY += getBindingsForCategory(getAllKeyBindings(), modName).size() * DRAWER_LINE_HEIGHT;
                itemY += 4;
            }
        }

        return null;
    }

    public static String getDrawerActionQueryAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        if (!modsExpanded) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

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
            for (String category : getCategories(bindingsByKey)) {
                itemY += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT;
                    itemY += 4;
                }
            }
            itemY += 10;
        }

        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String modName : getMods(bindingsByKey)) {
            itemY += DRAWER_LINE_HEIGHT;

            if (expandedMods.contains(modName)) {
                for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), modName)) {
                    String actionName = Text.translatable(binding.getTranslationKey()).getString();

                    int actionX = itemX + 12;
                    int width = Math.max(120, textRenderer.getWidth(actionName) + 24);

                    boolean insideVisibleDrawer =
                            local.y() >= drawerY + 28
                                    && local.y() <= drawerY + drawerHeight;

                    boolean onThisRow =
                            local.y() >= itemY - 2
                                    && local.y() <= itemY + 10;

                    boolean onAction =
                            local.x() >= actionX
                                    && local.x() <= actionX + width;

                    if (insideVisibleDrawer && onThisRow && onAction) {
                        return "action:" + modName + "|" + actionName;
                    }

                    itemY += DRAWER_LINE_HEIGHT;
                }

                itemY += 4;
            }
        }

        return null;
    }

    public static KeyBinding getDrawerActionBindingClickAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        if (!modsExpanded) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

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
            for (String category : getCategories(bindingsByKey)) {
                itemY += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT;
                    itemY += 4;
                }
            }
            itemY += 10;
        }

        itemY += 14;

        TextRenderer textRenderer = client.textRenderer;

        for (String modName : getMods(bindingsByKey)) {
            itemY += DRAWER_LINE_HEIGHT;

            if (expandedMods.contains(modName)) {
                for (KeyBinding binding : getBindingsForCategory(getAllKeyBindings(), modName)) {
                    String bindingLabel = getBindingDisplayName(binding);
                    int bindingLabelX = DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING - textRenderer.getWidth(bindingLabel);

                    boolean insideVisibleDrawer =
                            local.y() >= drawerY + 28
                                    && local.y() <= drawerY + drawerHeight;

                    boolean onThisRow =
                            local.y() >= itemY - 2
                                    && local.y() <= itemY + 10;

                    boolean onBindingLabel =
                            local.x() >= bindingLabelX - 6
                                    && local.x() <= DRAWER_X + DRAWER_WIDTH - DRAWER_PADDING;

                    if (insideVisibleDrawer && onThisRow && onBindingLabel) {
                        return binding;
                    }

                    itemY += DRAWER_LINE_HEIGHT;
                }

                itemY += 4;
            }
        }

        return null;
    }

    public static Integer getVisualKeyCodeAt(int mouseX, int mouseY) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        for (KeyVisual key : KeyboardLayout.current()) {
            if (isMouseOverKey(key, local.x(), local.y())) {
                return key.keyCode();
            }
        }

        return null;
    }

    public static InputUtil.Key getVisualKeyAt(int mouseX, int mouseY) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        for (KeyVisual key : KeyboardLayout.current()) {
            if (isMouseOverKey(key, local.x(), local.y())) {
                int keyCode = key.keyCode();

                if (keyCode < 0) {
                    int mouseButton = keyCode + 100;
                    return InputUtil.Type.MOUSE.createFromCode(mouseButton);
                }

                return InputUtil.fromKeyCode(keyCode, 0);
            }
        }

        return null;
    }

    public static String getDrawerModArrowAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        if (!modsExpanded) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

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
            for (String category : getCategories(bindingsByKey)) {
                itemY += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT;
                    itemY += 4;
                }
            }
            itemY += 10;
        }

        itemY += 14;

        for (String modName : getMods(bindingsByKey)) {
            boolean insideVisibleDrawer =
                    local.y() >= drawerY + 28
                            && local.y() <= drawerY + drawerHeight;

            boolean onThisRow =
                    local.y() >= itemY - 2
                            && local.y() <= itemY + 10;

            boolean onArrow =
                    local.x() >= itemX
                            && local.x() <= itemX + 10;

            if (insideVisibleDrawer && onThisRow && onArrow) {
                return modName;
            }

            itemY += DRAWER_LINE_HEIGHT;

            if (expandedMods.contains(modName)) {
                itemY += getBindingsForCategory(getAllKeyBindings(), modName).size() * DRAWER_LINE_HEIGHT;
                itemY += 4;
            }
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
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

        int drawerHeight = LAYOUT_BOTTOM - LAYOUT_TOP - 16;
        int visibleContentHeight = drawerHeight - 44;
        int contentHeight = 14;

        if (quickExpanded) {
            contentHeight += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }

        contentHeight += 14;

        if (categoriesExpanded) {
            for (String category : getCategories(bindingsByKey)) {
                contentHeight += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    contentHeight += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT + 4;
                }
            }
            contentHeight += 10;
        }

        contentHeight += 14;

        if (modsExpanded) {
            for (String modName : getMods(bindingsByKey)) {
                contentHeight += DRAWER_LINE_HEIGHT;
                if (expandedMods.contains(modName)) {
                    contentHeight += getBindingsForCategory(getAllKeyBindings(), modName).size() * DRAWER_LINE_HEIGHT + 4;
                }
            }
        }

        return Math.max(0, contentHeight - visibleContentHeight);
    }


    public static String getDrawerSectionHeaderAt(
            int mouseX,
            int mouseY,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            java.util.Set<String> expandedCategories
    ) {
        LayoutInfo layout = getLayoutInfo();
        LocalMouse local = toLocalMouse(mouseX, mouseY, layout);

        int drawerY = LAYOUT_TOP + 8;
        int itemX = DRAWER_X + DRAWER_PADDING;
        int itemY = drawerY + DRAWER_CONTENT_Y_OFFSET - drawerScroll;

        if (isInsideDrawerHeader(local, itemX, itemY)) return "quick";

        itemY += 14;
        if (quickExpanded) {
            itemY += QUICK_FILTERS.length * DRAWER_LINE_HEIGHT + 10;
        }

        if (isInsideDrawerHeader(local, itemX, itemY)) return "categories";

        itemY += 14;
        if (categoriesExpanded) {
            MinecraftClient client = MinecraftClient.getInstance();
            Map<Integer, List<KeyBinding>> bindingsByKey = groupKeybinds(client.options.allKeys);

            for (String category : getCategories(bindingsByKey)) {
                itemY += DRAWER_LINE_HEIGHT;
                if (expandedCategories.contains(category)) {
                    itemY += getBindingsForCategory(getAllKeyBindings(), category).size() * DRAWER_LINE_HEIGHT + 4;
                }
            }
            itemY += 10;
        }

        if (isInsideDrawerHeader(local, itemX, itemY)) return "mods";
        return null;
    }


    private static void drawDrawerScrollbar(
            DrawContext context,
            int drawerY,
            int drawerHeight,
            int drawerScroll,
            boolean quickExpanded,
            boolean categoriesExpanded,
            boolean modsExpanded,
            java.util.Set<String> expandedCategories,
            java.util.Set<String> expandedMods
    ) {
        int maxScroll = getMaxDrawerScroll(
                quickExpanded, categoriesExpanded, modsExpanded,
                expandedCategories, expandedMods
        );

        if (maxScroll <= 0) return;

        int trackX = DRAWER_X + DRAWER_WIDTH - 6;
        int trackY = drawerY + 40;
        int trackHeight = drawerHeight - 46;

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

    private static String displaySearchQuery(String searchQuery, boolean bindingMode) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return bindingMode ? "Search..." : "Search...";
        }

        if (searchQuery.startsWith("category:")) {
            return searchQuery.substring("category:".length());
        }

        if (searchQuery.startsWith("mod:")) {
            return searchQuery.substring("mod:".length());
        }

        if (searchQuery.startsWith("action:")) {
            String exact = searchQuery.substring("action:".length());
            int separator = exact.indexOf('|');

            if (separator > 0) {
                return exact.substring(0, separator) + " → " + exact.substring(separator + 1);
            }

            return exact;
        }

        boolean showCursor = !bindingMode && (System.currentTimeMillis() / 500) % 2 == 0;
        return searchQuery + (showCursor ? "_" : "");
    }

    private static String getVisualKeyName(int keyCode) {
        for (KeyVisual key : KeyboardLayout.current()) {
            if (key.keyCode() == keyCode) {
                return key.label();
            }
        }

        return Integer.toString(keyCode);
    }

    private static LayoutInfo getLayoutInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyMapConfig config = KeyMapConfigManager.get();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int layoutWidth = LAYOUT_RIGHT - LAYOUT_LEFT;
        int layoutHeight = LAYOUT_BOTTOM - LAYOUT_TOP;

        float fitScale = Math.min(
                (screenWidth - 24) / (float) layoutWidth,
                (screenHeight - 24) / (float) layoutHeight
        );

        float currentDefaultScale = Math.min(fitScale, 1.0f);
        float configuredScale = currentDefaultScale * config.hudScale;
        float scale = Math.min(configuredScale, fitScale);

        float scaledWidth = layoutWidth * scale;
        float scaledHeight = layoutHeight * scale;
        int margin = 12;

        String position = config.hudPosition == null ? "CENTER" : config.hudPosition;

        float contentLeft;
        float contentTop;

        if (position.endsWith("LEFT")) {
            contentLeft = margin;
        } else if (position.endsWith("RIGHT")) {
            contentLeft = screenWidth - scaledWidth - margin;
        } else {
            contentLeft = (screenWidth - scaledWidth) / 2.0f;
        }

        if (position.startsWith("TOP")) {
            contentTop = margin;
        } else if (position.startsWith("BOTTOM")) {
            contentTop = screenHeight - scaledHeight - margin;
        } else {
            contentTop = (screenHeight - scaledHeight) / 2.0f;
        }

        int originX = Math.round(contentLeft - LAYOUT_LEFT * scale);
        int originY = Math.round(contentTop - LAYOUT_TOP * scale);

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