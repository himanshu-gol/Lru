package org.example.eviction;

import java.util.Objects;

/**
 * Node for a doubly linked list used in LRU ordering.
 */
public final class DoublyLinkedListNode<K> {

    private K key;
    private DoublyLinkedListNode<K> prev;
    private DoublyLinkedListNode<K> next;

    public DoublyLinkedListNode(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public DoublyLinkedListNode<K> getPrev() {
        return prev;
    }

    public void setPrev(DoublyLinkedListNode<K> prev) {
        this.prev = prev;
    }

    public DoublyLinkedListNode<K> getNext() {
        return next;
    }

    public void setNext(DoublyLinkedListNode<K> next) {
        this.next = next;
    }

    /** Unlinks this node from its neighbours. */
    public void unlink() {
        if (prev != null) prev.next = next;
        if (next != null) next.prev = prev;
        prev = null;
        next = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoublyLinkedListNode<?> that = (DoublyLinkedListNode<?>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
