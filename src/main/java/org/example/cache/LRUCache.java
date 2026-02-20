package org.example.cache;

import org.example.eviction.DequeEvictionPolicy;
import org.example.eviction.DoublyLinkedEvictionPolicy;
import org.example.eviction.EvictionPolicy;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU cache backed by ConcurrentHashMap with a pluggable eviction policy
 * (e.g. doubly linked list or deque). All operations are synchronized for consistency.
 */
public final class LRUCache<K, V> {

    /** Default maximum number of entries per cache. */
    public static final int DEFAULT_MAX_SIZE = 100;

    private final int capacity;
    private final ConcurrentHashMap<K, V> map;
    private final EvictionPolicy<K> evictionPolicy;
    private final ReentrantLock evictionLock = new ReentrantLock();

    public LRUCache(int capacity, EvictionPolicy<K> evictionPolicy) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.evictionPolicy = Objects.requireNonNull(evictionPolicy, "evictionPolicy");
        this.map = new ConcurrentHashMap<>(capacity);
    }

    /** Creates an LRU cache with doubly linked list eviction and default max size (100). */
    public static <K, V> LRUCache<K, V> withDoublyLinkedEviction() {
        return new LRUCache<>(DEFAULT_MAX_SIZE, new DoublyLinkedEvictionPolicy<>());
    }

    /** Creates an LRU cache with doubly linked list eviction (O(1) get/put/touch). */
    public static <K, V> LRUCache<K, V> withDoublyLinkedEviction(int capacity) {
        return new LRUCache<>(capacity, new DoublyLinkedEvictionPolicy<>());
    }

    /** Creates an LRU cache with deque eviction and default max size (100). */
    public static <K, V> LRUCache<K, V> withDequeEviction() {
        return new LRUCache<>(DEFAULT_MAX_SIZE, new DequeEvictionPolicy<>());
    }

    /** Creates an LRU cache with deque eviction (O(1) put/evict, O(n) touch). */
    public static <K, V> LRUCache<K, V> withDequeEviction(int capacity) {
        return new LRUCache<>(capacity, new DequeEvictionPolicy<>());
    }

    /**
     * Returns the value for the key if present and moves the key to MRU.
     * Thread-safe: entire read and order update under lock.
     */
    public Optional<V> get(K key) {
        evictionLock.lock();
        try {
            if (!map.containsKey(key)) return Optional.empty();
            V value = map.get(key);
            evictionPolicy.addOrTouch(key);
            return Optional.of(value);
        } finally {
            evictionLock.unlock();
        }
    }

    /**
     * Puts the key-value pair. Evicts LRU entries until size <= capacity.
     * Thread-safe: eviction and map update under lock.
     */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        evictionLock.lock();
        try {
            evictionPolicy.addOrTouch(key);
            while (evictionPolicy.size() > capacity) {
                K evictKey = evictionPolicy.pollLru();
                if (evictKey != null) map.remove(evictKey);
            }
            map.put(key, value);
        } finally {
            evictionLock.unlock();
        }
    }

    /**
     * Removes the key from cache and from the eviction order.
     * Thread-safe: both updates under lock.
     */
    public void remove(K key) {
        evictionLock.lock();
        try {
            evictionPolicy.remove(key);
            map.remove(key);
        } finally {
            evictionLock.unlock();
        }
    }

    /** Thread-safe: returns current number of entries. */
    public int size() {
        evictionLock.lock();
        try {
            return map.size();
        } finally {
            evictionLock.unlock();
        }
    }

    public int capacity() {
        return capacity;
    }

    /** Thread-safe: returns true if the key is present. */
    public boolean containsKey(K key) {
        evictionLock.lock();
        try {
            return map.containsKey(key);
        } finally {
            evictionLock.unlock();
        }
    }
}
