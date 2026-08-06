package com.wynntils.screens.maps.type;

import com.wynntils.services.mapdata.type.MapCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CategoryTreeNode {
    private final String fullId;
    private final String name;
    private MapCategory category;
    private final List<CategoryTreeNode> children = new ArrayList<>();

    public CategoryTreeNode(String fullId, String name, MapCategory category, List<CategoryTreeNode> children) {
        this.fullId = fullId;
        this.name = name;
        this.category = category;
        this.children.addAll(children);
    }

    public CategoryTreeNode(String fullId, String name, MapCategory category) {
        this(fullId, name, category, new ArrayList<>());
    }

    public String getFullId() {
        return fullId;
    }

    public String getName() {
        return name;
    }

    public Optional<MapCategory> getCategory() {
        return Optional.ofNullable(category);
    }

    public List<CategoryTreeNode> getChildren() {
        return java.util.Collections.unmodifiableList(children);
    }

    public boolean isCategory() {
        return category != null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
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

    public void setCategory(MapCategory category) {
        this.category = category;
    }

    // ---- New method for sorting ----
    public void sortChildren(Comparator<CategoryTreeNode> comparator) {
        children.sort(comparator);
    }
}