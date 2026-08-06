package com.wynntils.screens.maps.type;  // adjust to your package

import com.wynntils.services.mapdata.type.MapCategory;
import com.wynntils.utils.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryTree {
    private final CategoryTreeNode root;

    public CategoryTree(List<MapCategory> categories) {
        root = new CategoryTreeNode(null, "root", null);
        for (MapCategory category : categories) {
            String[] parts = category.getCategoryId().split(":");
            CategoryTreeNode currentNode = root;
            StringBuilder fullIdBuilder = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (i > 0) fullIdBuilder.append(':');
                fullIdBuilder.append(part);
                String fullId = fullIdBuilder.toString();

                CategoryTreeNode child = currentNode.getChildByName(part);
                if (child == null) {
                    MapCategory catForNode = (i == parts.length - 1) ? category : null;
                    child = new CategoryTreeNode(fullId, part, catForNode);
                    currentNode.addChild(child);
                } else {
                    if (i == parts.length - 1 && child.getCategory().isEmpty()) {
                        child.setCategory(category);
                    }
                }
                currentNode = child;
            }
        }
        sortTree(root);
    }

    private void sortTree(CategoryTreeNode node) {
        node.sortChildren(Comparator.comparing(CategoryTreeNode::getName));
        for (CategoryTreeNode child : node.getChildren()) {
            sortTree(child);
        }
    }

    public CategoryTreeNode getRoot() {
        return root;
    }

    public CategoryTreeNode getFilteredTree(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return root;
        }
        String normalized = searchText.trim();
        List<CategoryTreeNode> filteredChildren = filterChildren(root, normalized);
        return new CategoryTreeNode(null, "root", null, filteredChildren);
    }

    private List<CategoryTreeNode> filterChildren(CategoryTreeNode node, String searchText) {
        List<CategoryTreeNode> result = new ArrayList<>();
        for (CategoryTreeNode child : node.getChildren()) {
            List<CategoryTreeNode> filteredGrandChildren = filterChildren(child, searchText);
            boolean childMatches = StringUtils.partialMatch(child.getName(), searchText);
            boolean hasMatchingDescendant = !filteredGrandChildren.isEmpty();

            if (childMatches || hasMatchingDescendant) {
                CategoryTreeNode newNode = new CategoryTreeNode(
                        child.getFullId(),
                        child.getName(),
                        child.getCategory().orElse(null),
                        filteredGrandChildren
                );
                result.add(newNode);
            }
        }
        return result;
    }
}