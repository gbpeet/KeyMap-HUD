package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KeyMapScreen extends Screen {
    private String searchQuery = "";

    private int drawerScroll = 0;

    private boolean filterDrawerOpen = false;
    private boolean quickExpanded = false;
    private boolean categoriesExpanded = false;
    private boolean modsExpanded = false;
    private final java.util.Set<String> expandedCategories = new java.util.HashSet<>();
    private final java.util.Set<String> expandedMods = new java.util.HashSet<>();

    private boolean bindingMode = false;
    private net.minecraft.client.option.KeyBinding bindingTarget = null;
    public KeyMapScreen() {
        super(Text.literal("KeyMap HUD"));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!Character.isISOControl(chr)) {
            searchQuery += chr;
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.matchesOverlayKey(keyCode, scanCode)) {
            KeyMapHUDClient.waitingForRelease = true;
            close();
            return true;
        }

        if (bindingMode && bindingTarget != null) {
            assignBinding(net.minecraft.client.util.InputUtil.fromKeyCode(keyCode, scanCode));
            return true;
        }

        if (filterDrawerOpen && keyCode == 264) { // Down arrow
            drawerScroll = Math.min(
                    drawerScroll + 12,
                    OverlayRenderer.getMaxDrawerScroll(
                            quickExpanded,
                            categoriesExpanded,
                            modsExpanded,
                            expandedCategories,
                            expandedMods
                    )
            );
            return true;
        }

        if (filterDrawerOpen && keyCode == 265) { // Up arrow
            drawerScroll = Math.max(0, drawerScroll - 12);
            return true;
        }

        if (keyCode == 259 && !searchQuery.isEmpty()) { // Backspace
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        OverlayRenderer.renderScreen(
                context,
                mouseX,
                mouseY,
                delta,
                searchQuery,
                filterDrawerOpen,
                drawerScroll,
                quickExpanded,
                categoriesExpanded,
                modsExpanded,
                expandedCategories,
                expandedMods,
                bindingMode,
                bindingTarget
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (filterDrawerOpen && OverlayRenderer.isMouseInsideFilterDrawer((int) mouseX, (int) mouseY)) {
            drawerScroll -= (int) Math.signum(verticalAmount) * 12;
            drawerScroll = Math.max(
                    0,
                    Math.min(
                            drawerScroll,
                            OverlayRenderer.getMaxDrawerScroll(
                            quickExpanded,
                            categoriesExpanded,
                            modsExpanded,
                            expandedCategories,
                            expandedMods
                    )
                    )
            );
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingMode && bindingTarget != null) {
            if (button == 1) {   // Right mouse button
                bindingMode = false;
                bindingTarget = null;
                return true;
            }

            net.minecraft.client.util.InputUtil.Key clickedKey =
                    OverlayRenderer.getVisualKeyAt((int) mouseX, (int) mouseY);

            if (clickedKey != null) {
                assignBinding(clickedKey);
                return true;
            }

            return true;
        }

        if (OverlayRenderer.isFilterDrawerButtonAt((int) mouseX, (int) mouseY, filterDrawerOpen)) {
            filterDrawerOpen = !filterDrawerOpen;
            return true;
        }

        if (filterDrawerOpen) {
            String section = OverlayRenderer.getDrawerSectionHeaderAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    expandedCategories
            );

            if (section != null) {
                switch (section) {
                    case "quick" -> quickExpanded = !quickExpanded;
                    case "categories" -> categoriesExpanded = !categoriesExpanded;
                    case "mods" -> modsExpanded = !modsExpanded;
                }

                drawerScroll = Math.min(
                        drawerScroll,
                        OverlayRenderer.getMaxDrawerScroll(
                            quickExpanded,
                            categoriesExpanded,
                            modsExpanded,
                            expandedCategories,
                            expandedMods
                    )
                );
                return true;
            }

            String drawerQuickFilter = OverlayRenderer.getDrawerQuickFilterQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded
            );

            if (drawerQuickFilter != null) {
                searchQuery = drawerQuickFilter;
                return true;
            }

            String categoryArrow = OverlayRenderer.getDrawerCategoryArrowAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    expandedCategories
            );

            if (categoryArrow != null) {
                if (expandedCategories.contains(categoryArrow)) {
                    expandedCategories.remove(categoryArrow);
                } else {
                    expandedCategories.add(categoryArrow);
                }

                drawerScroll = Math.min(
                        drawerScroll,
                        OverlayRenderer.getMaxDrawerScroll(
                                quickExpanded,
                                categoriesExpanded,
                                modsExpanded,
                                expandedCategories,
                                expandedMods
                        )
                );
                return true;
            }

            String categoryFilter = OverlayRenderer.getDrawerCategoryQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    expandedCategories
            );

            if (categoryFilter != null) {
                searchQuery = categoryFilter;
                return true;
            }

            net.minecraft.client.option.KeyBinding categoryRebindTarget =
                    OverlayRenderer.getDrawerCategoryActionBindingClickAt(
                            (int) mouseX,
                            (int) mouseY,
                            drawerScroll,
                            quickExpanded,
                            categoriesExpanded,
                            expandedCategories
                    );

            if (categoryRebindTarget != null) {
                bindingMode = true;
                bindingTarget = categoryRebindTarget;

                String category = Text.translatable(categoryRebindTarget.getCategory()).getString();
                String action = Text.translatable(categoryRebindTarget.getTranslationKey()).getString();
                searchQuery = "action:" + category + "|" + action;
                return true;
            }

            String categoryActionFilter = OverlayRenderer.getDrawerCategoryActionQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    expandedCategories
            );

            if (categoryActionFilter != null) {
                searchQuery = categoryActionFilter;
                return true;
            }

            String modArrow = OverlayRenderer.getDrawerModArrowAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );

            if (modArrow != null) {
                if (expandedMods.contains(modArrow)) {
                    expandedMods.remove(modArrow);
                } else {
                    expandedMods.add(modArrow);
                }

                drawerScroll = Math.min(
                        drawerScroll,
                        OverlayRenderer.getMaxDrawerScroll(
                            quickExpanded,
                            categoriesExpanded,
                            modsExpanded,
                            expandedCategories,
                            expandedMods
                    )
                );

                return true;
            }

            String modFilter = OverlayRenderer.getDrawerModQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );

            if (modFilter != null) {
                searchQuery = modFilter;
                return true;
            }

            net.minecraft.client.option.KeyBinding rebindTarget = OverlayRenderer.getDrawerActionBindingClickAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );

            if (rebindTarget != null) {
                bindingMode = true;
                bindingTarget = rebindTarget;

                String category = Text.translatable(rebindTarget.getCategory()).getString();
                String action = Text.translatable(rebindTarget.getTranslationKey()).getString();
                searchQuery = "action:" + category + "|" + action;

                return true;
            }

            String actionFilter = OverlayRenderer.getDrawerActionQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded,
                    expandedCategories,
                    expandedMods
            );

            if (actionFilter != null) {
                searchQuery = actionFilter;
                return true;
            }
        }

        String quickFilter = OverlayRenderer.getQuickFilterQueryAt((int) mouseX, (int) mouseY);

        if (quickFilter != null) {
            searchQuery = quickFilter;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void assignBinding(net.minecraft.client.util.InputUtil.Key key) {
        if (bindingTarget == null) {
            return;
        }

        // ESC unbinds, matching vanilla Controls behavior
        if (key.getCode() == 256) {
            bindingTarget.setBoundKey(net.minecraft.client.util.InputUtil.UNKNOWN_KEY);
        } else {
            bindingTarget.setBoundKey(key);
        }

        net.minecraft.client.option.KeyBinding.updateKeysByCode();

        if (this.client != null) {
            this.client.options.write();
        }

        bindingMode = false;
        bindingTarget = null;
    }
}