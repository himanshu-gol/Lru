package org.example.cache;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple, thread-safe LRU cache.
 *
 * - Uses {@link ConcurrentHashMap} to store key/value pairs.
 * - Uses a {@link Deque} to keep keys in LRU order (front = oldest, back = newest).
 * - All public methods are protected by a single lock for clarity.
 */
public final class LRUCache<K, V> {

    /** Default maximum number of entries per cache. */
    public static final int DEFAULT_MAX_SIZE = 100;

    private final int capacity;
    private final ConcurrentHashMap<K, V> map;
    private final Deque<K> order; // front = LRU, back = MRU
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a cache with the given maximum size.
     */
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        this.order = new ArrayDeque<>(capacity);
    }

    /**
     * Creates a cache with the default maximum size (100).
     */
    public static <K, V> LRUCache<K, V> withDefaultCapacity() {
        return new LRUCache<>(DEFAULT_MAX_SIZE);
    }

    /**
     * Returns the value for the key if present and marks it as most recently used.
     */
    public Optional<V> get(K key) {
        lock.lock();
        try {
            V value = map.get(key);
            if (value == null) {
                return Optional.empty();
            }
            // Move key to MRU end.
            order.remove(key);
            order.addLast(key);
            return Optional.of(value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Puts the key/value pair in the cache.
     * If the key already exists, its value is replaced and it becomes MRU.
     * If adding a new key exceeds capacity, the LRU key is evicted.
     */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        lock.lock();
        try {
            if (map.containsKey(key)) {
                // Update existing value and move key to MRU.
                map.put(key, value);
                order.remove(key);
                order.addLast(key);
                return;
            }

            // New key: evict if cache is already full.
            if (map.size() >= capacity) {
                evict();
            }

            map.put(key, value);
            order.addLast(key); // new key is MRU
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the key (if present) from the cache.
     */
    public void remove(K key) {
        lock.lock();
        try {
            if (map.remove(key) != null) {
                order.remove(key);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Current number of entries in the cache.
     */
    public int size() {
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Maximum number of entries this cache can hold.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * True if the key is present in the cache.
     */
    public boolean containsKey(K key) {
        lock.lock();
        try {
            return map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public void printAllEntries() {
        for(K key : map.keySet()) {
            System.out.println(key + ": " + map.get(key));
        }
    }

    /**
     * Removes the least recently used entry from the cache.
     * Assumes the lock is already held by the caller.
     */
    private void evict() {
        K lruKey = order.pollFirst();
        if (lruKey != null) {
            System.out.println("removing the key: " + lruKey);
            map.remove(lruKey);
        }
    }
}
