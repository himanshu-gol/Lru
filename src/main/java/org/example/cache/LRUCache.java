package org.example.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple, thread-safe LRU cache with O(1) get/put/evict.
 *
 * - ConcurrentHashMap<K, V> stores the values.
 * - A doubly linked list stores keys from LRU (head) to MRU (tail).
 * - A second map K -> Node lets us jump to the list node for a key in O(1).
 * - All public methods are protected by a single lock.
 */
public final class LRUCache<K, V> {

    /** Default maximum number of entries per cache. */
    public static final int DEFAULT_MAX_SIZE = 100;

    private final int capacity;
    private final ConcurrentHashMap<K, V> map;
    private final Map<K, Node<K>> keyToNode;
    private Node<K> head; // least recently used
    private Node<K> tail; // most recently used
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Node for the doubly linked list of keys.
     */
    private static final class Node<K> {
        final K key;
        Node<K> prev;
        Node<K> next;

        Node(K key) {
            this.key = key;
        }
    }

    /**
     * Creates a cache with the given maximum size.
     */
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        this.keyToNode = new HashMap<>(capacity);
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
            moveToTail(key);
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
                moveToTail(key);
                return;
            }

            // New key: evict if cache is already full.
            if (map.size() >= capacity) {
                evict();
            }

            map.put(key, value);
            addToTail(key);
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
                removeNodeForKey(key);
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

    /**
     * Debug helper to print all entries (order is unspecified).
     */
    public void printAllEntries() {
        lock.lock();
        try {
            for (K key : map.keySet()) {
                System.out.println(key + ": " + map.get(key));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the least recently used entry from the cache.
     * Assumes the lock is already held by the caller.
     */
    private void evict() {
        if (head == null) {
            return;
        }
        K lruKey = head.key;
        // unlink head
        if (head.next != null) {
            head.next.prev = null;
        }
        head = head.next;
        if (head == null) {
            tail = null;
        }
        keyToNode.remove(lruKey);
        map.remove(lruKey);
    }

    /**
     * Moves the node for the given key to the tail (MRU position).
     */
    private void moveToTail(K key) {
        Node<K> node = keyToNode.get(key);
        if (node == null || node == tail) {
            return;
        }
        // unlink node
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            // node was head
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        // link at tail
        node.prev = tail;
        node.next = null;
        if (tail != null) {
            tail.next = node;
        }
        tail = node;
        if (head == null) {
            head = node;
        }
    }

    /**
     * Adds a new key at the tail (MRU position).
     */
    private void addToTail(K key) {
        Node<K> node = new Node<>(key);
        keyToNode.put(key, node);
        node.prev = tail;
        node.next = null;
        if (tail != null) {
            tail.next = node;
        }
        tail = node;
        if (head == null) {
            head = node;
        }
    }

    /**
     * Removes the node corresponding to the given key from the list and map.
     */
    private void removeNodeForKey(K key) {
        Node<K> node = keyToNode.remove(key);
        if (node == null) {
            return;
        }
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }
}
