/*
 * Copyright © Wynntils 2026.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryTreeNode {
    private final String fullId;
    private final String name;
    private final List<CategoryTreeNode> children = new ArrayList<>();
    private boolean
            category; // true if this node corresponds to an actual category (leaf or branch that is also a category)

    // For internal/folder nodes (no category)
    public CategoryTreeNode(String fullId, String name, List<CategoryTreeNode> children) {
        this.fullId = fullId;
        this.name = name;
        this.children.addAll(children);
        this.category = false;
    }

    // For leaf/internal nodes that are categories
    public CategoryTreeNode(String fullId, String name, boolean category, List<CategoryTreeNode> children) {
        this.fullId = fullId;
        this.name = name;
        this.children.addAll(children);
        this.category = category;
    }

    // Two-arg convenience constructor (folder nodes by default)
    public CategoryTreeNode(String fullId, String name) {
        this(fullId, name, false, new ArrayList<>());
    }

    public String getFullId() {
        return fullId;
    }

    public String getName() {
        return name;
    }

    public List<CategoryTreeNode> getChildren() {
        return java.util.Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isCategory() {
        return category;
    }

    public void setCategory(boolean category) {
        this.category = category;
    }

    // ---- internal methods used by CategoryTree ----
    public void addChild(CategoryTreeNode child) {
        children.add(child);
    }

    public CategoryTreeNode getChildByName(String name) {
        for (CategoryTreeNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public void sortChildren(Comparator<CategoryTreeNode> comparator) {
        children.sort(comparator);
    }
}
