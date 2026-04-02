package org.example;

import org.example.cache.LRUCache;

import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        // Test 1: default capacity constant
        // What we test: DEFAULT_MAX_SIZE is 100 for all caches.
        System.out.println("Default cache max size: " + LRUCache.DEFAULT_MAX_SIZE); // expect 100

        // Test 2: basic put and get without eviction
        // What we test: values are stored and returned correctly, size grows as expected.
        LRUCache<String, Integer> cache = new LRUCache<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        System.out.println("[T2] size after put a,b = " + cache.size()); // expect 2
        System.out.println("[T2] get(a) = " + cache.get("a"));           // expect Optional[1]
        System.out.println("[T2] get(b) = " + cache.get("b"));           // expect Optional[2]
        System.out.println("[T2] get(x) = " + cache.get("x"));           // expect Optional.empty
        System.out.println("[T2] get(x) = " + cache.get("x")); 

        // Test 3: LRU eviction when capacity is exceeded
        // What we test: when we insert 4th element into capacity-3 cache, the least recently used is evicted.
        // Current order of use (LRU -> MRU) before adding c,d:
        //   - "b" was used last, then "a", so order should be: a (LRU), b (MRU)
        cache.put("c", 3); // now keys: a,b,c  (a is LRU, c is MRU)
        cache.put("d", 4); // should evict "a"
        System.out.println("[T3] size after put c,d = " + cache.size()); // expect 3
        System.out.println("[T3] get(a) = " + cache.get("a"));           // expect Optional.empty
        System.out.println("[T3] get(b) = " + cache.get("b"));           // expect Optional[2]
        System.out.println("[T3] get(c) = " + cache.get("c"));           // expect Optional[3]
        System.out.println("[T3] get(d) = " + cache.get("d"));           // expect Optional[4]

        // Test 4: LRU update on access
        // What we test: accessing a key makes it most recently used, so a different key gets evicted.
        LRUCache<String, Integer> cache2 = new LRUCache<>(2);
        cache2.put("x", 10);
        cache2.put("y", 20);                 // keys: x (LRU), y (MRU)
        cache2.get("x");                     // access x -> keys: y (LRU), x (MRU)
        cache2.put("z", 30);                 // should evict "y"
        System.out.println("[T4] get(x) = " + cache2.get("x"));          // expect Optional[10]
        System.out.println("[T4] get(y) = " + cache2.get("y"));          // expect Optional.empty
        System.out.println("[T4] get(z) = " + cache2.get("z"));          // expect Optional[30]

        // Test 5: remove
        // What we test: remove deletes the key and does not affect other keys.
        LRUCache<String, Integer> cache3 = new LRUCache<>(2);
        cache3.put("p", 100);
        cache3.put("q", 200);
        cache3.remove("p");
        System.out.println("[T5] size after removing p = " + cache3.size()); // expect 1
        System.out.println("[T5] get(p) = " + cache3.get("p"));              // expect Optional.empty
        System.out.println("[T5] get(q) = " + cache3.get("q"));              // expect Optional[200]

        // Test 6: containsKey
        // What we test: containsKey reflects presence of keys.
        LRUCache<String, Integer> cache4 = new LRUCache<>(1);
        cache4.put("k", 5);
        System.out.println("[T6] contains k = " + cache4.containsKey("k")); // expect true
        cache4.remove("k");
        System.out.println("[T6] contains k after remove = " + cache4.containsKey("k")); // expect false

        System.out.println("Starting custom test-----");
        LRUCache<String, Integer> cache5 = new LRUCache<>(2);
        cache5.put("k", 5);
        cache5.put("p", 100);
        cache5.printAllEntries();
        cache5.get("k");
        cache5.get("k");
        cache5.get("p");
        cache5.put("q", 200);
        cache5.printAllEntries();
    }
}
