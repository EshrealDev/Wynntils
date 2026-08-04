package com.wynntils.screens.maps;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.screens.WynntilsScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CategoryManagementScreen extends WynntilsScreen {
    private final MainMapScreen previousScreen;
    private int offsetX;
    private int offsetY;

    private CategoryManagementScreen(MainMapScreen previousScreen) {
        super(Component.literal("Category Management Screen"));
        this.previousScreen = previousScreen;
    }

    public static Screen create(MainMapScreen previousScreen) {
        return new CategoryManagementScreen(previousScreen);
    }

    @Override
    protected void doInit() {
        super.doInit();

        offsetX = (int) ((this.width - Texture. MANAGER_BACKGROUND.width()) / 2f);
        offsetY = (int) ((this.height - Texture. MANAGER_BACKGROUND.height()) / 2f);
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(guiGraphics);
    }

    private void renderBackgroundTexture(GuiGraphics guiGraphics) {
        RenderUtils.drawTexturedRect(guiGraphics, Texture. MANAGER_BACKGROUND, offsetX, offsetY);
    }

    @Override
    public void onClose() {
        McUtils.mc().setScreen(previousScreen);
    }


}
