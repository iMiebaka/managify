package com.managify.projects.service;

import com.managify.projects.model.TrackerItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerOrderingTest {

    @Test
    void moveUpSwapsWithTheItemAbove() {
        List<TrackerItem> siblings = items(1, 2, 3);

        boolean moved = TrackerOrdering.moveUp(siblings, siblings.get(2));

        assertThat(moved).isTrue();
        assertThat(idsByPosition(siblings)).containsExactly(1L, 3L, 2L);
    }

    @Test
    void moveUpOnTheFirstItemChangesNothing() {
        List<TrackerItem> siblings = items(1, 2, 3);

        assertThat(TrackerOrdering.moveUp(siblings, siblings.get(0))).isFalse();
        assertThat(idsByPosition(siblings)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void moveDownSwapsWithTheItemBelow() {
        List<TrackerItem> siblings = items(1, 2, 3);

        assertThat(TrackerOrdering.moveDown(siblings, siblings.get(0))).isTrue();
        assertThat(idsByPosition(siblings)).containsExactly(2L, 1L, 3L);
    }

    @Test
    void moveDownOnTheLastItemChangesNothing() {
        List<TrackerItem> siblings = items(1, 2, 3);

        assertThat(TrackerOrdering.moveDown(siblings, siblings.get(2))).isFalse();
        assertThat(idsByPosition(siblings)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void removeClosesTheGap() {
        List<TrackerItem> siblings = items(1, 2, 3);
        TrackerItem middle = siblings.get(1);

        TrackerOrdering.remove(siblings, middle);

        assertThat(siblings).extracting(TrackerItem::getId).containsExactly(1L, 3L);
        assertThat(siblings).extracting(TrackerItem::getPosition).containsExactly(0, 1);
    }

    @Test
    void positionsWithGapsAreRenumberedOnMove() {
        List<TrackerItem> siblings = items(1, 2, 3);
        siblings.get(0).setPosition(4);
        siblings.get(1).setPosition(9);
        siblings.get(2).setPosition(20);

        TrackerOrdering.moveDown(siblings, siblings.get(1));

        assertThat(siblings).extracting(TrackerItem::getPosition).containsExactly(0, 1, 2);
        assertThat(idsByPosition(siblings)).containsExactly(1L, 3L, 2L);
    }

    private static List<TrackerItem> items(long... ids) {
        List<TrackerItem> list = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            TrackerItem item = new TrackerItem();
            item.setId(ids[i]);
            item.setPosition(i);
            list.add(item);
        }
        return list;
    }

    private static List<Long> idsByPosition(List<TrackerItem> items) {
        return items.stream()
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .map(TrackerItem::getId)
                .toList();
    }
}
