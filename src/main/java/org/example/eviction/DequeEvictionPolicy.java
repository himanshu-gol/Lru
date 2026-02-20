package org.example.eviction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * LRU eviction policy implemented with a deque.
 * - LRU at front (evict from front), MRU at back.
 * - addOrTouch: remove then add last (O(n) for touch due to linear remove in ArrayDeque).
 * - pollLru: O(1). Good when reads are rare relative to capacity.
 */
public final class DequeEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Deque<K> order = new ArrayDeque<>();
    private final Set<K> present = new HashSet<>();

    @Override
    public void addOrTouch(K key) {
        Objects.requireNonNull(key, "key");
        if (present.contains(key)) {
            order.remove(key); // O(n) for ArrayDeque
        } else {
            present.add(key);
        }
        order.addLast(key);
    }

    @Override
    public K pollLru() {
        K lru = order.pollFirst();
        if (lru != null) present.remove(lru);
        return lru;
    }

    @Override
    public void remove(K key) {
        if (present.remove(key)) {
            order.remove(key);
        }
    }

    @Override
    public int size() {
        return order.size();
    }

    @Override
    public boolean contains(K key) {
        return present.contains(key);
    }
}
