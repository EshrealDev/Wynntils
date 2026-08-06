package com.wynntils.screens.maps.categorymanagerwidgets;

import com.wynntils.core.text.StyledText;
import com.wynntils.screens.maps.CategoryManagementScreen;
import com.wynntils.screens.maps.type.CategoryTree;
import com.wynntils.screens.maps.type.CategoryTreeNode;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

public class CategoryTreeWidget extends DoubleScrollBarWidget {
    private final int x;
    private final int y;
    private final CategoryManagementScreen parent;

    // Tree data
    private CategoryTree fullTree;
    private CategoryTreeNode filteredRoot;

    public CategoryTreeWidget(int x, int y, int width, int height, CategoryManagementScreen parent) {
        super(x, y, width, height, parent);
        this.x = x;
        this.y = y;
        this.parent = parent;
    }

    // ---- Public API ----

    public void setCategories(List<MapCategory> categories) {
        fullTree = new CategoryTree(categories);
        filteredRoot = fullTree.getRoot();
        // Recalculate canvas size based on filteredRoot (you'll implement later)
        // recalculateCanvasSize();
    }

    public void filter(String searchText) {
        if (fullTree == null) return;
        filteredRoot = fullTree.getFilteredTree(searchText);
        // Reset scroll and recalc canvas
        scrollOffsetX = 0;
        scrollOffsetY = 0;
        // recalculateCanvasSize();
    }

    // ---- Rendering ----

    @Override
    protected void renderCategoryTree(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (filteredRoot == null) {
            // No data yet
            return;
        }

        // Starting coordinates inside the scissored area
        int startX = this.x + SCROLL_BAR_WIDTH_PADDING;
        int startY = this.y + SCROLL_BAR_HEIGHT_PADDING + 5;

        // Draw the tree recursively
        drawNode(guiGraphics, filteredRoot, startX, startY, 0);
    }

    private int drawNode(GuiGraphics guiGraphics, CategoryTreeNode node, int xPos, int yPos, int depth) {
        // Indent per depth level (e.g., 10 pixels per level)
        int indent = depth * 10;

        // Draw the node's name
        FontRenderer.getInstance().renderText(
                guiGraphics,
                StyledText.fromString(node.getName()),
                xPos + indent - scrollOffsetX,
                yPos - scrollOffsetY,
                node.isCategory() ? CommonColors.GREEN : CommonColors.GRAY,
                HorizontalAlignment.LEFT,
                VerticalAlignment.MIDDLE,
                TextShadow.NORMAL
        );

        // Measure the height of this line (assume 10px per node)
        int lineHeight = 10;
        int currentY = yPos + lineHeight;

        // Recurse through children
        for (CategoryTreeNode child : node.getChildren()) {
            currentY = drawNode(guiGraphics, child, xPos, currentY, depth + 1);
        }

        return currentY;
    }
}