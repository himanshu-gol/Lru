package org.example.eviction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LRU eviction policy implemented with a doubly linked list.
 * - LRU at head (next to evict), MRU at tail.
 * - O(1) add, touch, remove, and pollLru via map from key to node.
 */
public final class DoublyLinkedEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, DoublyLinkedListNode<K>> keyToNode = new HashMap<>();
    private DoublyLinkedListNode<K> head; // LRU
    private DoublyLinkedListNode<K> tail; // MRU

    @Override
    public void addOrTouch(K key) {
        Objects.requireNonNull(key, "key");
        DoublyLinkedListNode<K> node = keyToNode.get(key);
        if (node != null) {
            moveToTail(node);
            return;
        }
        node = new DoublyLinkedListNode<>(key);
        keyToNode.put(key, node);
        linkAtTail(node);
    }

    @Override
    public K pollLru() {
        if (head == null) return null;
        DoublyLinkedListNode<K> lru = head;
        unlink(lru);
        keyToNode.remove(lru.getKey());
        return lru.getKey();
    }

    @Override
    public void remove(K key) {
        DoublyLinkedListNode<K> node = keyToNode.remove(key);
        if (node != null) unlink(node);
    }

    @Override
    public int size() {
        return keyToNode.size();
    }

    @Override
    public boolean contains(K key) {
        return keyToNode.containsKey(key);
    }

    private void moveToTail(DoublyLinkedListNode<K> node) {
        if (node == tail) return;
        unlink(node);
        linkAtTail(node);
    }

    private void unlink(DoublyLinkedListNode<K> node) {
        if (node == head) head = node.getNext();
        if (node == tail) tail = node.getPrev();
        node.unlink();
    }

    private void linkAtTail(DoublyLinkedListNode<K> node) {
        if (tail == null) {
            head = tail = node;
            return;
        }
        tail.setNext(node);
        node.setPrev(tail);
        tail = node;
    }
}
