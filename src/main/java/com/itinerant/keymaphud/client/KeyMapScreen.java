package com.itinerant.keymaphud.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KeyMapScreen extends Screen {
    private String searchQuery = "";

    private int drawerScroll = 0;

    private boolean filterDrawerOpen = false;
    private boolean quickExpanded = true;
    private boolean categoriesExpanded = true;
    private boolean modsExpanded = false;
    private final java.util.Set<String> expandedMods = new java.util.HashSet<>();

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

        if (filterDrawerOpen && keyCode == 264) { // Down arrow
            drawerScroll = Math.min(
                    drawerScroll + 12,
                    OverlayRenderer.getMaxDrawerScroll(quickExpanded, categoriesExpanded, modsExpanded)
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
                expandedMods
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
                            OverlayRenderer.getMaxDrawerScroll(quickExpanded, categoriesExpanded, modsExpanded)
                    )
            );
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                    categoriesExpanded
            );

            if (section != null) {
                switch (section) {
                    case "quick" -> quickExpanded = !quickExpanded;
                    case "categories" -> categoriesExpanded = !categoriesExpanded;
                    case "mods" -> modsExpanded = !modsExpanded;
                }

                drawerScroll = Math.min(
                        drawerScroll,
                        OverlayRenderer.getMaxDrawerScroll(quickExpanded, categoriesExpanded, modsExpanded)
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

            String categoryFilter = OverlayRenderer.getDrawerCategoryQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded
            );

            if (categoryFilter != null) {
                searchQuery = categoryFilter;
                return true;
            }

            String modFilter = OverlayRenderer.getDrawerModQueryAt(
                    (int) mouseX,
                    (int) mouseY,
                    drawerScroll,
                    quickExpanded,
                    categoriesExpanded,
                    modsExpanded
            );

            if (modFilter != null) {
                searchQuery = modFilter;
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
}