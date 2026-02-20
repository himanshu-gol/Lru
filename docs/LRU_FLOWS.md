# LRU Cache — Flows & Architecture

Mermaid diagrams for the thread-safe LRU cache (ConcurrentHashMap + pluggable eviction).

---

## 1. Class diagram

```mermaid
classDiagram
    class LRUCache~K, V~ {
        -int capacity
        -ConcurrentHashMap~K,V~ map
        -EvictionPolicy~K~ evictionPolicy
        -ReentrantLock evictionLock
        +DEFAULT_MAX_SIZE : int
        +withDoublyLinkedEviction() LRUCache
        +withDoublyLinkedEviction(capacity) LRUCache
        +withDequeEviction() LRUCache
        +withDequeEviction(capacity) LRUCache
        +get(key) Optional~V~
        +put(key, value) void
        +remove(key) void
        +size() int
        +capacity() int
        +containsKey(key) boolean
    }

    class EvictionPolicy~K~ {
        <<interface>>
        +addOrTouch(key) void
        +pollLru() K
        +remove(key) void
        +size() int
        +contains(key) boolean
    }

    class DoublyLinkedEvictionPolicy~K~ {
        -Map~K, Node~ keyToNode
        -Node head
        -Node tail
        +addOrTouch(key) void
        +pollLru() K
        +remove(key) void
        +size() int
        +contains(key) boolean
    }

    class DequeEvictionPolicy~K~ {
        -Deque~K~ order
        -Set~K~ present
        +addOrTouch(key) void
        +pollLru() K
        +remove(key) void
        +size() int
        +contains(key) boolean
    }

    class DoublyLinkedListNode~K~ {
        -K key
        -Node prev
        -Node next
        +getKey() K
        +unlink() void
    }

    LRUCache --> EvictionPolicy : uses
    EvictionPolicy <|.. DoublyLinkedEvictionPolicy : implements
    EvictionPolicy <|.. DequeEvictionPolicy : implements
    DoublyLinkedEvictionPolicy --> DoublyLinkedListNode : head, tail, keyToNode
```

---

## 2. Get flow (sequence)

```mermaid
sequenceDiagram
    participant Client
    participant LRUCache
    participant evictionLock
    participant ConcurrentHashMap
    participant EvictionPolicy

    Client->>LRUCache: get(key)
    LRUCache->>evictionLock: lock()
    evictionLock-->>LRUCache: acquired

    LRUCache->>ConcurrentHashMap: containsKey(key)
    alt key absent
        ConcurrentHashMap-->>LRUCache: false
        LRUCache->>evictionLock: unlock()
        LRUCache-->>Client: Optional.empty()
    else key present
        ConcurrentHashMap-->>LRUCache: true
        LRUCache->>ConcurrentHashMap: get(key)
        ConcurrentHashMap-->>LRUCache: value
        LRUCache->>EvictionPolicy: addOrTouch(key)
        Note over EvictionPolicy: move key to MRU
        EvictionPolicy-->>LRUCache: ok
        LRUCache->>evictionLock: unlock()
        LRUCache-->>Client: Optional.of(value)
    end
```

---

## 3. Put flow (sequence)

```mermaid
sequenceDiagram
    participant Client
    participant LRUCache
    participant evictionLock
    participant EvictionPolicy
    participant ConcurrentHashMap

    Client->>LRUCache: put(key, value)
    LRUCache->>LRUCache: requireNonNull(key, value)
    LRUCache->>evictionLock: lock()
    evictionLock-->>LRUCache: acquired

    LRUCache->>EvictionPolicy: addOrTouch(key)
    Note over EvictionPolicy: add new or move to MRU
    EvictionPolicy-->>LRUCache: ok

    loop while evictionPolicy.size() > capacity
        LRUCache->>EvictionPolicy: pollLru()
        EvictionPolicy-->>LRUCache: evictKey
        LRUCache->>ConcurrentHashMap: remove(evictKey)
        ConcurrentHashMap-->>LRUCache: ok
    end

    LRUCache->>ConcurrentHashMap: put(key, value)
    ConcurrentHashMap-->>LRUCache: ok
    LRUCache->>evictionLock: unlock()
    LRUCache-->>Client: void
```

---

## 4. Remove flow (sequence)

```mermaid
sequenceDiagram
    participant Client
    participant LRUCache
    participant evictionLock
    participant EvictionPolicy
    participant ConcurrentHashMap

    Client->>LRUCache: remove(key)
    LRUCache->>evictionLock: lock()
    evictionLock-->>LRUCache: acquired

    LRUCache->>EvictionPolicy: remove(key)
    EvictionPolicy-->>LRUCache: ok
    LRUCache->>ConcurrentHashMap: remove(key)
    ConcurrentHashMap-->>LRUCache: ok

    LRUCache->>evictionLock: unlock()
    LRUCache-->>Client: void
```

---

## 5. Get flow (flowchart)

```mermaid
flowchart TD
    A[get(key)] --> B[Lock]
    B --> C{map.containsKey(key)?}
    C -->|No| D[Unlock]
    D --> E[Return Optional.empty()]
    C -->|Yes| F[value = map.get(key)]
    F --> G[evictionPolicy.addOrTouch(key)]
    G --> H[Unlock]
    H --> I[Return Optional.of(value)]
```

---

## 6. Put flow (flowchart)

```mermaid
flowchart TD
    A[put(key, value)] --> B[Validate key, value non-null]
    B --> C[Lock]
    C --> D[evictionPolicy.addOrTouch(key)]
    D --> E{evictionPolicy.size() > capacity?}
    E -->|Yes| F[pollLru() -> evictKey]
    F --> G[map.remove(evictKey)]
    G --> E
    E -->|No| H[map.put(key, value)]
    H --> I[Unlock]
    I --> J[Return]
```

---

## 7. Eviction policy — Doubly linked list layout

```mermaid
flowchart LR
    subgraph DoublyLinkedEvictionPolicy
        direction LR
        H[head: LRU] --> N1[Node A]
        N1 --> N2[Node B]
        N2 --> N3[Node C]
        N3 --> T[tail: MRU]
    end
    style H fill:#f96
    style T fill:#9f6
```

- **Evict**: remove from `head`, advance head.
- **Touch / Add**: add or move node to `tail` (MRU).

---

## 8. Eviction policy — Deque layout

```mermaid
flowchart LR
    subgraph DequeEvictionPolicy
        direction LR
        F[front: LRU] --> Q["Deque [A, B, C]"]
        Q --> B[back: MRU]
    end
    style F fill:#f96
    style B fill:#9f6
```

- **Evict**: `pollFirst()` (front).
- **Touch / Add**: remove key if present, then `addLast(key)` (MRU).

---

## 9. End-to-end flow (cache + eviction)

```mermaid
flowchart TB
    subgraph Client
        C1[get/put/remove]
    end

    subgraph LRUCache
        L[ReentrantLock]
        M[ConcurrentHashMap]
        E[EvictionPolicy]
    end

    subgraph EvictionPolicy_impl["Eviction policy (DoublyLinked or Deque)"]
        direction TB
        EP[Order: LRU ... MRU]
    end

    C1 --> L
    L --> M
    L --> E
    E --> EP
    M --> EP
```

---

## 10. Thread safety (all operations under lock)

```mermaid
flowchart LR
    subgraph Operations
        O1[get]
        O2[put]
        O3[remove]
        O4[size]
        O5[containsKey]
    end

    subgraph Guard
        L[evictionLock]
    end

    O1 --> L
    O2 --> L
    O3 --> L
    O4 --> L
    O5 --> L
```

Every public operation that reads or updates the map or the eviction policy does so while holding `evictionLock`, so the cache is thread-safe and the maximum size (default 100) is enforced consistently.
