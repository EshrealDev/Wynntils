/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps.categorymanagerwidgets;

import com.wynntils.core.text.StyledText;
import com.wynntils.screens.maps.type.CategoryTreeNode;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class CategoryTreeEntryWidget extends AbstractWidget {
    public static final int ROW_HEIGHT = 14;
    public static final int INDENT_WIDTH = 10;
    public static final int ARROW_WIDTH = 8;
    public static final int ARROW_ICON_GAP = 4;
    public static final int DEFAULT_ICON_SIZE = 12;
    public static final int ICON_TEXT_GAP = 3;

    private static final CustomColor HOVER_HIGHLIGHT = CommonColors.GRAY.withAlpha(0.35f);
    private static final CustomColor SELECTED_HIGHLIGHT = CommonColors.BLUE.withAlpha(0.35f);

    private final CategoryTreeNode node;
    private final int column;
    private final boolean[] siblingContinues;
    private final boolean reserveArrowSpace;
    private boolean expanded;
    private boolean selected;
    private int iconSize = DEFAULT_ICON_SIZE;

    private int x;
    private int y;

    private final Runnable onToggleExpand;
    private final Runnable onSelect;

    public CategoryTreeEntryWidget(
            int x,
            int y,
            int width,
            CategoryTreeNode node,
            int column,
            boolean[] siblingContinues,
            boolean reserveArrowSpace,
            boolean expanded,
            boolean selected,
            Runnable onToggleExpand,
            Runnable onSelect) {
        super(x, y, width, ROW_HEIGHT, Component.literal(""));
        this.x = x;
        this.y = y;
        this.node = node;
        this.column = column;
        this.siblingContinues = siblingContinues;
        this.reserveArrowSpace = reserveArrowSpace;
        this.expanded = expanded;
        this.selected = selected;
        this.onToggleExpand = onToggleExpand;
        this.onSelect = onSelect;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.x = x;
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.y = y;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getColumn() {
        return column;
    }

    public boolean[] getSiblingContinues() {
        return siblingContinues;
    }

    public CategoryTreeNode getNode() {
        return node;
    }

    public boolean isArrowSpaceReserved() {
        return reserveArrowSpace;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int getArrowX() {
        return x;
    }

    public int getArrowCenterX() {
        return x + ARROW_WIDTH / 2;
    }

    public int getIconX() {
        return reserveArrowSpace ? x + ARROW_WIDTH + ARROW_ICON_GAP : x;
    }

    public int getIconCenterX() {
        return getIconX() + iconSize / 2;
    }

    public int getRowCenterY() {
        return y + ROW_HEIGHT / 2;
    }

    public int computeContentWidth() {
        int iconOffset = reserveArrowSpace ? ARROW_WIDTH + ARROW_ICON_GAP : 0;
        int textWidth = FontRenderer.getInstance().getFont().width(node.getName());
        return iconOffset + iconSize + ICON_TEXT_GAP + textWidth;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (selected) {
            RenderUtils.drawRect(guiGraphics, SELECTED_HIGHLIGHT, x, y, width, ROW_HEIGHT);
        } else if (isHovered) {
            RenderUtils.drawRect(guiGraphics, HOVER_HIGHLIGHT, x, y, width, ROW_HEIGHT);
        }

        if (!node.isLeaf()) {
            renderArrow(guiGraphics, getArrowX());
        }

        renderIcon(guiGraphics, getIconX());

        FontRenderer.getInstance()
                .renderText(
                        guiGraphics,
                        StyledText.fromString(node.getName()),
                        getIconX() + iconSize + ICON_TEXT_GAP,
                        y + ROW_HEIGHT / 2f,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);
    }

    private void renderArrow(GuiGraphics guiGraphics, int arrowX) {
        float cx = arrowX + ARROW_WIDTH / 2f;
        float cy = y + ROW_HEIGHT / 2f;
        float r = ARROW_WIDTH / 2.5f;

        List<Vector2f> vertices = expanded
                ? List.of(
                        new Vector2f(cx - r, cy - r / 1.5f),
                        new Vector2f(cx + r, cy - r / 1.5f),
                        new Vector2f(cx, cy + r / 1.5f))
                : List.of(
                        new Vector2f(cx - r / 1.5f, cy - r),
                        new Vector2f(cx - r / 1.5f, cy + r),
                        new Vector2f(cx + r / 1.5f, cy));

        RenderUtils.drawPolygon(guiGraphics, CommonColors.WHITE, CustomColor.NONE, 0f, vertices);
    }

    private void renderIcon(GuiGraphics guiGraphics, int iconX) {
        // Placeholder – replace with actual icon loading later
        RenderUtils.drawRect(
                guiGraphics, CommonColors.GRAY, iconX, y + (ROW_HEIGHT - iconSize) / 2f, iconSize, iconSize);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (!isMouseOver(event.x(), event.y())) return false;

        // Arrow click (only for non-leaf nodes)
        if (!node.isLeaf() && isMouseOverArrow(event.x(), event.y())) {
            onToggleExpand.run();
            return true;
        }

        // Label click – only if this node represents an actual category
        if (node.isCategory()) {
            onSelect.run();
            return true;
        }

        return false;
    }

    private boolean isMouseOverArrow(double mouseX, double mouseY) {
        return MathUtils.isInside((int) mouseX, (int) mouseY, x, x + ARROW_WIDTH, y, y + ROW_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
