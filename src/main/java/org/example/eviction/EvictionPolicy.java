package org.example.eviction;

/**
 * Contract for LRU eviction: maintains access order (least recently used at eviction end).
 */
public interface EvictionPolicy<K> {

    /**
     * Adds the key if not present, or moves it to most-recently-used end if already present.
     */
    void addOrTouch(K key);

    /**
     * Removes and returns the least recently used key, or null if empty.
     */
    K pollLru();

    /**
     * Removes the key from the eviction order (e.g. when entry is explicitly removed from cache).
     */
    void remove(K key);

    /**
     * Current number of keys in the eviction order.
     */
    int size();

    /**
     * Returns true if the key is present in the eviction order.
     */
    boolean contains(K key);
}
