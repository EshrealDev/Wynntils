package com.wynntils.screens.maps.categorymanagerwidgets;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.maps.CategoryManagementScreen;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CategoryTreeWidget extends AbstractWidget {
    private static final float SCROLL_FACTOR = 10f;
    private static final int SCROLL_BAR_HEIGHT_PADDING = 4;
    private static final int SCROLL_BAR_WIDTH_PADDING = 4;
    // Thumb never shrinks below this, otherwise it'd become an unclickable sliver on huge canvases.
    private static final int MIN_SCROLL_BUTTON_LENGTH = 12;
    // Gap left between the thumb and the track's ends when scrolled all the way to either extreme.
    private static final int SCROLL_BUTTON_EDGE_GAP = 1;
    // How far the vertical bar sits from the widget's right edge.
    private static final int VERTICAL_SCROLL_BAR_EDGE_PADDING = 3;
    // How far the horizontal bar sits from the widget's bottom edge.
    private static final int HORIZONTAL_SCROLL_BAR_EDGE_PADDING = 3;
    // The up/down/left/right arrows are baked into the top/bottom (or left/right) caps of the
// private static final int SCROLL_ARROW_BUTTON_SIZE = 9;
    private static final int SCROLL_ARROW_BUTTON_WIDTH_VERTICAL = 9;
    private static final int SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL = 10;
    private static final int SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL = 10;
    private static final int SCROLL_ARROW_BUTTON_HEIGHT_HORIZONTAL = 9;
    // Gap between an arrow cap and the start/end of the thumb's travel area.
    private static final int SCROLL_ARROW_BUTTON_GAP = 0;
    // How far scrollOffset moves per arrow button click.
    private static final int SCROLL_ARROW_STEP = 10;

    private final int x;
    private final int y;
    private final CategoryManagementScreen parent;

    // Full size of the tree content. The viewport (this.width/this.height) stays fixed;
    // the canvas grows as nodes are expanded. Call setCanvasSize() whenever the tree changes.
    private int canvasWidth;
    private int canvasHeight;

    public int scrollOffsetX = 0;
    public int scrollOffsetY = 0;

    private double dragOffsetX;
    private double dragOffsetY;
    private boolean draggingScrollX = false;
    private boolean draggingScrollY = false;

    private float scrollBarX;
    private float scrollBarY;
    private float verticalButtonLength;
    private float horizontalButtonLength;

    public CategoryTreeWidget(int x, int y, int width, int height, CategoryManagementScreen parent) {
        super(x, y, width, height, Component.literal("Category Tree Widget"));
        this.x = x;
        this.y = y;
        this.parent = parent;

        this.canvasWidth = width + 600;
        this.canvasHeight = height + 50;
    }

    public void setCanvasSize(int canvasWidth, int canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        scrollOffsetX = Math.max(0, Math.min(scrollOffsetX, getMaxScrollOffsetX()));
        scrollOffsetY = Math.max(0, Math.min(scrollOffsetY, getMaxScrollOffsetY()));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawNineSliceScalingTexturedRect(
                guiGraphics, Texture.MANAGER_WIDGET_BORDER, x, y, this.width, this.height);

        RenderUtils.enableScissor(
                guiGraphics,
                this.x + SCROLL_BAR_WIDTH_PADDING,
                this.y + SCROLL_BAR_HEIGHT_PADDING,
                this.width - SCROLL_BAR_HEIGHT_PADDING * 2 - Texture.MANAGER_SCROLL_BAR_VERTICAL.width() - 4,
                this.height - SCROLL_BAR_WIDTH_PADDING * 2 - Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height() - 4);

        // TODO: draw tree nodes + connecting lines here, translated by -scrollOffsetX/-scrollOffsetY,
        // e.g. guiGraphics.pose().pushMatrix();
        //      guiGraphics.pose().translate(-scrollOffsetX, -scrollOffsetY);
        //      ...
        //      guiGraphics.pose().popMatrix();
        FontRenderer.getInstance()
                .renderText(
                        guiGraphics,
                        StyledText.fromString("test test test test test test test test test test test test test test test test test test test test test test test test test test test test"),
                        this.x + SCROLL_BAR_WIDTH_PADDING - scrollOffsetX,
                        this.y + SCROLL_BAR_HEIGHT_PADDING + 5 - scrollOffsetY,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        RenderUtils.disableScissor(guiGraphics);

        renderVerticalScroll(guiGraphics);
        renderHorizontalScroll(guiGraphics);

        if (draggingScrollY) {
            guiGraphics.requestCursor(CursorTypes.RESIZE_NS);
        } else if (draggingScrollX) {
            guiGraphics.requestCursor(CursorTypes.RESIZE_EW);
        } else if (isOntopOfVerticalScrollDragButton(mouseX, mouseY)
                || isOntopOfHorizontalScrollDragButton(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        } else if (isOntopOfVerticalUpButton(mouseX, mouseY)
                || isOntopOfVerticalDownButton(mouseX, mouseY)
                || isOntopOfHorizontalLeftButton(mouseX, mouseY)
                || isOntopOfHorizontalRightButton(mouseX, mouseY)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    private void renderVerticalScroll(GuiGraphics guiGraphics) {
        if (!isVerticalScrollNeeded()) {
            verticalButtonLength = 0;
            scrollBarY = this.y;
            return;
        }

        float barX = getVerticalScrollBarX();
        float barAreaTop = getVerticalBarAreaTop();
        float barAreaHeight = getVerticalBarAreaBottom() - barAreaTop;

        // The up/down arrows are baked into this texture's top/bottom caps now, so it's drawn
        // across the entire bar area instead of just the middle strip between two separate
        // arrow images.
        RenderUtils.drawNineSliceScalingTexturedRect(
                guiGraphics, Texture.MANAGER_SCROLL_BAR_VERTICAL, barX, barAreaTop, Texture.MANAGER_SCROLL_BAR_VERTICAL.width(), barAreaHeight);

        // Thumb sizing/travel still needs to stay clear of the (now baked-in) arrow caps.
        verticalButtonLength = getScaledButtonLength(getVerticalTrackHeight(), this.height, canvasHeight);

        int maxScrollOffset = getMaxScrollOffsetY();
        float thumbTop = getVerticalScrollTrackTop();
        float thumbBottom = getVerticalScrollTrackBottom();

        scrollBarY = maxScrollOffset <= 0 ? thumbTop : MathUtils.map(scrollOffsetY, 0, maxScrollOffset, thumbTop, thumbBottom);

        RenderUtils.drawNineSliceScalingTexturedRect(
                guiGraphics, Texture.MANAGER_SCROLL_BAR_BUTTON, barX, scrollBarY, Texture.MANAGER_SCROLL_BAR_VERTICAL.width(), verticalButtonLength);
    }

    private void renderHorizontalScroll(GuiGraphics guiGraphics) {
        if (!isHorizontalScrollNeeded()) {
            horizontalButtonLength = 0;
            scrollBarX = this.x;
            return;
        }

        float barY = getHorizontalScrollBarY();
        float barAreaLeft = getHorizontalBarAreaLeft();
        float barAreaWidth = getHorizontalBarAreaRight() - barAreaLeft;

        // The left/right arrows are baked into this texture's left/right caps now, so it's drawn
        // across the entire bar area instead of just the middle strip between two separate
        // arrow images.
        RenderUtils.drawNineSliceScalingTexturedRect(
                guiGraphics, Texture.MANAGER_SCROLL_BAR_HORIZONTAL, barAreaLeft, barY, barAreaWidth, Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height());

        // Thumb sizing/travel still needs to stay clear of the (now baked-in) arrow caps.
        horizontalButtonLength = getScaledButtonLength(getHorizontalTrackWidth(), this.width, canvasWidth);

        int maxScrollOffset = getMaxScrollOffsetX();
        float thumbLeft = getHorizontalScrollTrackLeft();
        float thumbRight = getHorizontalScrollTrackRight();

        scrollBarX = maxScrollOffset <= 0 ? thumbLeft : MathUtils.map(scrollOffsetX, 0, maxScrollOffset, thumbLeft, thumbRight);

        RenderUtils.drawNineSliceScalingTexturedRect(
                guiGraphics, Texture.MANAGER_SCROLL_BAR_BUTTON, scrollBarX, barY, horizontalButtonLength, Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height());
    }


    /** Thumb length scales with how much of the canvas is currently visible, like a normal OS scrollbar. */
    private float getScaledButtonLength(float trackLength, int viewportSize, int canvasSize) {
        if (canvasSize <= 0) return trackLength;

        float visibleRatio = Math.min(1f, viewportSize / (float) canvasSize);
        return Math.max(MIN_SCROLL_BUTTON_LENGTH, trackLength * visibleRatio);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollY) {
            double newScrollY = event.y() - dragOffsetY;

            int newOffset = Math.round(MathUtils.map(
                    (float) newScrollY, getVerticalScrollTrackTop(), getVerticalScrollTrackBottom(), 0, getMaxScrollOffsetY()));

            scrollOffsetY = Math.max(0, Math.min(newOffset, getMaxScrollOffsetY()));

            return super.mouseDragged(event, dragX, dragY);
        }

        if (draggingScrollX) {
            double newScrollX = event.x() - dragOffsetX;

            int newOffset = Math.round(MathUtils.map(
                    (float) newScrollX, getHorizontalScrollTrackLeft(), getHorizontalScrollTrackRight(), 0, getMaxScrollOffsetX()));

            scrollOffsetX = Math.max(0, Math.min(newOffset, getMaxScrollOffsetX()));

            return super.mouseDragged(event, dragX, dragY);
        }

        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (isOntopOfVerticalUpButton(event.x(), event.y())) {
            scrollOffsetY = Math.max(0, scrollOffsetY - SCROLL_ARROW_STEP);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (isOntopOfVerticalDownButton(event.x(), event.y())) {
            scrollOffsetY = Math.min(getMaxScrollOffsetY(), scrollOffsetY + SCROLL_ARROW_STEP);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (isOntopOfHorizontalLeftButton(event.x(), event.y())) {
            scrollOffsetX = Math.max(0, scrollOffsetX - SCROLL_ARROW_STEP);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (isOntopOfHorizontalRightButton(event.x(), event.y())) {
            scrollOffsetX = Math.min(getMaxScrollOffsetX(), scrollOffsetX + SCROLL_ARROW_STEP);
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        if (!draggingScrollY && getMaxScrollOffsetY() > 0 && isOntopOfVerticalScrollDragButton(event.x(), event.y())) {
            draggingScrollY = true;
            dragOffsetY = event.y() - scrollBarY;
            return true;
        }

        if (!draggingScrollX && getMaxScrollOffsetX() > 0 && isOntopOfHorizontalScrollDragButton(event.x(), event.y())) {
            draggingScrollX = true;
            dragOffsetX = event.x() - scrollBarX;
            return true;
        }

        // TODO: hit-test tree nodes here once they exist.

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollX = false;
        draggingScrollY = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int scrollAmount = (int) (-deltaY * SCROLL_FACTOR);

        if (isOverHorizontalScrollBar(mouseX, mouseY)) {
            scrollOffsetX = Math.max(0, Math.min(scrollOffsetX + scrollAmount, getMaxScrollOffsetX()));
        } else {
            scrollOffsetY = Math.max(0, Math.min(scrollOffsetY + scrollAmount, getMaxScrollOffsetY()));
        }

        return true;
    }

    private boolean isOverHorizontalScrollBar(double mouseX, double mouseY) {
        if (!isHorizontalScrollNeeded()) return false;

        float top = Math.min(getHorizontalScrollBarY(), getHorizontalArrowButtonY());

        return MathUtils.isInside((int) mouseX, (int) mouseY, this.x, this.x + this.width, (int) top, this.y + this.height);
    }

    private int getMaxScrollOffsetX() {
        return Math.max(0, canvasWidth - (this.width - SCROLL_BAR_WIDTH_PADDING * 2));
    }

    private int getMaxScrollOffsetY() {
        return Math.max(0, canvasHeight - (this.height - SCROLL_BAR_HEIGHT_PADDING * 2));
    }

    private boolean isVerticalScrollNeeded() {
        return getMaxScrollOffsetY() > 0;
    }

    private boolean isHorizontalScrollNeeded() {
        return getMaxScrollOffsetX() > 0;
    }

    private float getVerticalScrollBarX() {
        return this.x + this.width - Texture.MANAGER_SCROLL_BAR_VERTICAL.width() - VERTICAL_SCROLL_BAR_EDGE_PADDING;
    }

    private float getHorizontalScrollBarY() {
        return this.y + this.height - Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height() - HORIZONTAL_SCROLL_BAR_EDGE_PADDING;
    }

    // --- Vertical bar geometry ---
    // The bar texture (arrows baked into its top/bottom caps) is drawn across the full area
    // between getVerticalBarAreaTop() and getVerticalBarAreaBottom() - see renderVerticalScroll().
    // The methods below instead carve out the sub-region reserved for the thumb, which must stay
    // clear of those baked-in arrow caps.

    private float getVerticalBarAreaTop() {
        return this.y + SCROLL_BAR_HEIGHT_PADDING;
    }

    private float getVerticalBarAreaBottom() {
        float bottom = this.y + this.height - SCROLL_BAR_HEIGHT_PADDING;
        if (isHorizontalScrollNeeded()) {
            bottom -= Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height() + HORIZONTAL_SCROLL_BAR_EDGE_PADDING;
        }
        return bottom;
    }

    private float getVerticalTrackTop() {
        return getVerticalBarAreaTop() + SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL + SCROLL_ARROW_BUTTON_GAP;
    }

    private float getVerticalTrackBottom() {
        return getVerticalBarAreaBottom() - SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL - SCROLL_ARROW_BUTTON_GAP;
    }

    private float getVerticalTrackHeight() {
        return Math.max(0, getVerticalTrackBottom() - getVerticalTrackTop());
    }

    private float getVerticalArrowButtonX() {
        return getVerticalScrollBarX() + (Texture.MANAGER_SCROLL_BAR_VERTICAL.width() - SCROLL_ARROW_BUTTON_WIDTH_VERTICAL) / 2f;
    }

    private float getVerticalUpButtonY() {
        return getVerticalBarAreaTop();
    }

    private float getVerticalDownButtonY() {
        return getVerticalBarAreaBottom() - SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL;
    }

    // --- Horizontal bar geometry ---
    // The bar texture (arrows baked into its left/right caps) is drawn across the full area
    // between getHorizontalBarAreaLeft() and getHorizontalBarAreaRight() - see
    // renderHorizontalScroll(). The methods below instead carve out the sub-region reserved for
    // the thumb, which must stay clear of those baked-in arrow caps.

    private float getHorizontalBarAreaLeft() {
        return this.x + SCROLL_BAR_WIDTH_PADDING;
    }

    private float getHorizontalBarAreaRight() {
        float right = this.x + this.width - SCROLL_BAR_WIDTH_PADDING;
        if (isVerticalScrollNeeded()) {
            right -= Texture.MANAGER_SCROLL_BAR_VERTICAL.width() + VERTICAL_SCROLL_BAR_EDGE_PADDING;
        }
        return right;
    }

    private float getHorizontalTrackLeft() {
        return getHorizontalBarAreaLeft() + SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL + SCROLL_ARROW_BUTTON_GAP;
    }

    private float getHorizontalTrackRight() {
        return getHorizontalBarAreaRight() - SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL - SCROLL_ARROW_BUTTON_GAP;
    }

    private float getHorizontalTrackWidth() {
        return Math.max(0, getHorizontalTrackRight() - getHorizontalTrackLeft());
    }

    private float getHorizontalArrowButtonY() {
        return getHorizontalScrollBarY() + (Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height() - SCROLL_ARROW_BUTTON_HEIGHT_HORIZONTAL) / 2f;
    }

    private float getHorizontalLeftButtonX() {
        return getHorizontalBarAreaLeft();
    }

    private float getHorizontalRightButtonX() {
        return getHorizontalBarAreaRight() - SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL;
    }

    // --- Thumb travel bounds (inset by SCROLL_BUTTON_EDGE_GAP from the track ends). ---

    private float getVerticalScrollTrackTop() {
        return getVerticalTrackTop() + SCROLL_BUTTON_EDGE_GAP;
    }

    private float getVerticalScrollTrackBottom() {
        return getVerticalTrackBottom() - verticalButtonLength - SCROLL_BUTTON_EDGE_GAP;
    }

    private float getHorizontalScrollTrackLeft() {
        return getHorizontalTrackLeft() + SCROLL_BUTTON_EDGE_GAP;
    }

    private float getHorizontalScrollTrackRight() {
        return getHorizontalTrackRight() - horizontalButtonLength - SCROLL_BUTTON_EDGE_GAP;
    }

    // --- Hit testing ---

    private boolean isOntopOfVerticalScrollDragButton(double mouseX, double mouseY) {
        float trackX = getVerticalScrollBarX();

        return MathUtils.isInside(
                (int) mouseX,
                (int) mouseY,
                (int) trackX,
                (int) (trackX + Texture.MANAGER_SCROLL_BAR_VERTICAL.width()),
                (int) scrollBarY,
                (int) (scrollBarY + verticalButtonLength));
    }

    private boolean isOntopOfHorizontalScrollDragButton(double mouseX, double mouseY) {
        float trackY = getHorizontalScrollBarY();

        return MathUtils.isInside(
                (int) mouseX,
                (int) mouseY,
                (int) scrollBarX,
                (int) (scrollBarX + horizontalButtonLength),
                (int) trackY,
                (int) (trackY + Texture.MANAGER_SCROLL_BAR_HORIZONTAL.height()));
    }

    private boolean isOntopOfVerticalUpButton(double mouseX, double mouseY) {
        if (!isVerticalScrollNeeded()) return false;
        return isOntopOfArrowButton(mouseX, mouseY, getVerticalArrowButtonX(), getVerticalUpButtonY(),
                SCROLL_ARROW_BUTTON_WIDTH_VERTICAL, SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL);
    }

    private boolean isOntopOfVerticalDownButton(double mouseX, double mouseY) {
        if (!isVerticalScrollNeeded()) return false;
        return isOntopOfArrowButton(mouseX, mouseY, getVerticalArrowButtonX(), getVerticalDownButtonY(),
                SCROLL_ARROW_BUTTON_WIDTH_VERTICAL, SCROLL_ARROW_BUTTON_HEIGHT_VERTICAL);
    }

    private boolean isOntopOfHorizontalLeftButton(double mouseX, double mouseY) {
        if (!isHorizontalScrollNeeded()) return false;
        return isOntopOfArrowButton(mouseX, mouseY, getHorizontalLeftButtonX(), getHorizontalArrowButtonY(),
                SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL, SCROLL_ARROW_BUTTON_HEIGHT_HORIZONTAL);
    }

    private boolean isOntopOfHorizontalRightButton(double mouseX, double mouseY) {
        if (!isHorizontalScrollNeeded()) return false;
        return isOntopOfArrowButton(mouseX, mouseY, getHorizontalRightButtonX(), getHorizontalArrowButtonY(),
                SCROLL_ARROW_BUTTON_WIDTH_HORIZONTAL, SCROLL_ARROW_BUTTON_HEIGHT_HORIZONTAL);
    }

    private boolean isOntopOfArrowButton(double mouseX, double mouseY, float buttonX, float buttonY,
                                         int buttonWidth, int buttonHeight) {
        return MathUtils.isInside(
                (int) mouseX,
                (int) mouseY,
                (int) buttonX,
                (int) (buttonX + buttonWidth),
                (int) buttonY,
                (int) (buttonY + buttonHeight));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}