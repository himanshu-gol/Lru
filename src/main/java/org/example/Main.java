package org.example;

import org.example.cache.LRUCache;

import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        // Default max size for all caches is 100 (see LRUCache.DEFAULT_MAX_SIZE)
        System.out.println("Default cache max size: " + LRUCache.DEFAULT_MAX_SIZE);

        // LRU with doubly linked list eviction (thread-safe). Use (3) here for demo.
        LRUCache<String, Integer> cache = LRUCache.withDoublyLinkedEviction(3);

        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        System.out.println("After put a,b,c: size=" + cache.size()); // 3

        Optional<Integer> v = cache.get("a");
        System.out.println("get(a) = " + v); // Optional[1], and "a" becomes MRU

        cache.put("d", 4); // evicts "b" (LRU)
        System.out.println("After put d: size=" + cache.size());
        System.out.println("get(b) = " + cache.get("b")); // empty
        System.out.println("get(a) = " + cache.get("a")); // Optional[1]
        System.out.println("get(c) = " + cache.get("c")); // Optional[3]
        System.out.println("get(d) = " + cache.get("d")); // Optional[4]

        // Same API with deque-based eviction (thread-safe). Default max size 100; use 2 here for demo.
        LRUCache<Integer, String> cache2 = LRUCache.withDequeEviction(2);
        cache2.put(1, "one");
        cache2.put(2, "two");
        cache2.get(1); // touch
        cache2.put(3, "three"); // evicts 2
        System.out.println("Deque cache get(2) = " + cache2.get(2)); // empty
        System.out.println("Deque cache get(1) = " + cache2.get(1)); // Optional[one]
    }
}
