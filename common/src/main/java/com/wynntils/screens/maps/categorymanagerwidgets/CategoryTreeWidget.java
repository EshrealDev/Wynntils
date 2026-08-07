package com.wynntils.screens.maps.categorymanagerwidgets;

import com.wynntils.core.WynntilsMod;
import com.wynntils.screens.maps.CategoryManagementScreen;
import com.wynntils.screens.maps.type.CategoryTree;
import com.wynntils.screens.maps.type.CategoryTreeNode;
import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryTreeWidget extends DoubleScrollBarWidget {
    private final int x;
    private final int y;
    private final CategoryManagementScreen parent;

    // Tree data
    private CategoryTree fullTree;
    private CategoryTreeNode filteredRoot;
    private String currentSearch;

    // Row widgets and state
    private final List<CategoryTreeEntryWidget> rowWidgets = new ArrayList<>();
    private final Set<String> expandedFullIds = new HashSet<>();
    private String selectedFullId;

    // Color used for the tree connector lines drawn between rows
    private static final CustomColor LINE_COLOR = CustomColor.fromInt(0x654f3c);

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
        expandedFullIds.clear();
        selectedFullId = null;
        rebuildVisibleRows();
    }

    public void filter(String searchText) {
        if (fullTree == null) return;

        currentSearch = searchText;
        filteredRoot = fullTree.getFilteredTree(searchText);
        scrollOffsetX = 0;
        scrollOffsetY = 0;
        rebuildVisibleRows();
    }

    // ---- Row flattening and widget creation ----

    private void rebuildVisibleRows() {
        rowWidgets.clear();

        if (filteredRoot != null) {
            List<CategoryTreeNode> children = filteredRoot.getChildren();
            boolean reserveArrowSpace = groupHasExpandableMember(children);
            for (int i = 0; i < children.size(); i++) {
                addVisibleRows(children.get(i), 0, new boolean[0],
                        i < children.size() - 1, reserveArrowSpace);
            }
        }

        updateRowPositions();
        recalculateCanvasSize();
    }

    private void addVisibleRows(
            CategoryTreeNode node, int column, boolean[] parentTrail,
            boolean hasMoreSiblings, boolean reserveArrowSpace) {

        boolean[] trail = Arrays.copyOf(parentTrail, parentTrail.length + 1);
        trail[column] = hasMoreSiblings;

        boolean expanded = isExpanded(node);
        boolean selected = node.getFullId() != null && node.getFullId().equals(selectedFullId);

        CategoryTreeEntryWidget widget = new CategoryTreeEntryWidget(
                0, 0, 0,               // positions & width set later
                node, column, trail,
                reserveArrowSpace,
                expanded, selected,
                () -> toggleExpanded(node),
                () -> selectNode(node)
        );
        rowWidgets.add(widget);

        if (!expanded) return;

        List<CategoryTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            addVisibleRows(children.get(i), column + 1, trail,
                    i < children.size() - 1, true);
        }
    }

    private static boolean groupHasExpandableMember(List<CategoryTreeNode> siblings) {
        for (CategoryTreeNode sibling : siblings) {
            if (!sibling.isLeaf()) return true;
        }
        return false;
    }

    private boolean isExpanded(CategoryTreeNode node) {
        boolean searching = currentSearch != null && !currentSearch.isBlank();
        return searching || expandedFullIds.contains(node.getFullId());
    }

    private void toggleExpanded(CategoryTreeNode node) {
        if (node.isLeaf() || node.getFullId() == null) return;

        if (!expandedFullIds.add(node.getFullId())) {
            expandedFullIds.remove(node.getFullId());
        }
        rebuildVisibleRows();
    }

    private void selectNode(CategoryTreeNode node) {
        selectedFullId = node.getFullId();
        node.getCategory().ifPresent(parent::setSelectedCategory);
        rebuildVisibleRows(); // update selection highlight
    }

    private void recalculateCanvasSize() {
        int contentHeight = rowWidgets.size() * CategoryTreeEntryWidget.ROW_HEIGHT
                + SCROLL_BAR_HEIGHT_PADDING;

        int maxContentWidth = 0;
        for (CategoryTreeEntryWidget widget : rowWidgets) {
            // Content width from the column-0 anchor: indentation for this row's column, plus
            // its own arrow/icon/label extent.
            int rowContentWidth = widget.getColumn() * CategoryTreeEntryWidget.INDENT_WIDTH
                    + widget.computeContentWidth();
            maxContentWidth = Math.max(maxContentWidth, rowContentWidth);
        }

        setCanvasSize(maxContentWidth + 600, contentHeight + 600);
    }

    // ---- Layout ----

    /**
     * Positions every visible row widget according to the current scroll offsets, indenting
     * each row's x by its column so the entry widget itself doesn't need to know about depth.
     */
    private void updateRowPositions() {
        int baseX = this.x + SCROLL_BAR_WIDTH_PADDING - scrollOffsetX;
        int startY = this.y + SCROLL_BAR_HEIGHT_PADDING + 5;

        for (int i = 0; i < rowWidgets.size(); i++) {
            CategoryTreeEntryWidget widget = rowWidgets.get(i);
            int rowY = startY + i * CategoryTreeEntryWidget.ROW_HEIGHT - scrollOffsetY;
            int rowX = baseX + widget.getColumn() * CategoryTreeEntryWidget.INDENT_WIDTH;

            widget.setX(rowX);
            widget.setY(rowY);
            widget.setWidth(widget.computeContentWidth());
        }
    }

    // ---- Rendering ----

    @Override
    protected void renderCategoryTree(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (filteredRoot == null || rowWidgets.isEmpty()) return;

        updateRowPositions();

        for (CategoryTreeEntryWidget widget : rowWidgets) {
            // Cull rows outside the scissored area
            if (widget.getY() + CategoryTreeEntryWidget.ROW_HEIGHT < this.y || widget.getY() > this.y + this.height) {
                continue;
            }

            drawConnectorLines(guiGraphics, widget);
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawConnectorLines(GuiGraphics guiGraphics, CategoryTreeEntryWidget widget) {
        int column = widget.getColumn();
        if (column == 0) return;

        boolean[] continues = widget.getSiblingContinues();
        int baseX = widget.getX() - column * CategoryTreeEntryWidget.INDENT_WIDTH;
        int rowY = widget.getY();
        int rowHeight = CategoryTreeEntryWidget.ROW_HEIGHT;
        float midY = rowY + rowHeight / 2f;

        for (int c = 0; c < column; c++) {
            float lineX = baseX + c * CategoryTreeEntryWidget.INDENT_WIDTH
                    + CategoryTreeEntryWidget.ARROW_WIDTH / 2f;

            if (c == column - 1) {
                // Immediate parent: always draw the vertical stem for this row.
                // Extend to full row height only if the node has a next sibling.
                float endY = continues[column] ? rowY + rowHeight : midY;
                RenderUtils.drawLine(guiGraphics, LINE_COLOR, lineX, rowY, lineX, endY, 1f);
            } else {
                // Deeper ancestors: draw only if the ancestor or its child on the path
                // has a next sibling.
                if (!(continues[c] || continues[c + 1])) {
                    continue;
                }
                float endY = continues[c + 1] ? rowY + rowHeight : midY;
                RenderUtils.drawLine(guiGraphics, LINE_COLOR, lineX, rowY, lineX, endY, 1f);
            }
        }

        // Horizontal branch from immediate parent's column to the node's icon/arrow
        float parentX = baseX + (column - 1) * CategoryTreeEntryWidget.INDENT_WIDTH
                + CategoryTreeEntryWidget.ARROW_WIDTH / 2f;
        float endX = widget.getNode().isLeaf()
                ? widget.getIconX() - 2
                : widget.getArrowX() - 2;

        RenderUtils.drawLine(guiGraphics, LINE_COLOR, parentX, midY, endX, midY, 1f);
    }

    // ---- Interaction ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        updateRowPositions(); // ensure hit areas are correct
        for (CategoryTreeEntryWidget widget : rowWidgets) {
            if (widget.isMouseOver(event.x(), event.y())) {
                return widget.mouseClicked(event, isDoubleClick);
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        boolean result = super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        updateRowPositions();
        return result;
    }
}