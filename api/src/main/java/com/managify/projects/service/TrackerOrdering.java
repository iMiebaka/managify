package com.managify.projects.service;

import com.managify.projects.model.TrackerItem;

import java.util.Collections;
import java.util.List;

/**
 * Ordering of sibling items (a project's tasks, or one task's tasklets). Pure list operations:
 * the caller passes the siblings in their current order and JPA dirty checking saves the new positions.
 * Positions are always rewritten as 0..n-1, which also closes any gaps.
 */
public final class TrackerOrdering {

    private TrackerOrdering() {}

    /** Swaps the item with the one above it. Returns false when it is already first. */
    public static boolean moveUp(List<TrackerItem> siblings, TrackerItem item) {
        int index = indexOf(siblings, item);
        if (index <= 0) {
            renumber(siblings);
            return false;
        }
        Collections.swap(siblings, index, index - 1);
        renumber(siblings);
        return true;
    }

    /** Swaps the item with the one below it. Returns false when it is already last. */
    public static boolean moveDown(List<TrackerItem> siblings, TrackerItem item) {
        int index = indexOf(siblings, item);
        if (index < 0 || index >= siblings.size() - 1) {
            renumber(siblings);
            return false;
        }
        Collections.swap(siblings, index, index + 1);
        renumber(siblings);
        return true;
    }

    /** Takes the item out of its siblings and closes the gap it leaves. */
    public static void remove(List<TrackerItem> siblings, TrackerItem item) {
        siblings.removeIf(s -> same(s, item));
        renumber(siblings);
    }

    /** Positions 0..n-1 in list order. Only items whose position changes are touched. */
    public static void renumber(List<TrackerItem> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getPosition() != i) {
                siblings.get(i).setPosition(i);
            }
        }
    }

    private static int indexOf(List<TrackerItem> siblings, TrackerItem item) {
        for (int i = 0; i < siblings.size(); i++) {
            if (same(siblings.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean same(TrackerItem a, TrackerItem b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }
}
