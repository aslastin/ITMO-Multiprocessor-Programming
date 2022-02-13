import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicReferenceArray

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while(true) {
            val tail = this.tail.value
            val enqIdx = tail.enqIdx.getAndIncrement()
            if (enqIdx >= SEGMENT_SIZE) {
                val newTail = Segment(x)
                if (tail.next.compareAndSet(null, newTail)) {
                    this.tail.compareAndSet(tail, newTail)
                    return
                } else {
                    this.tail.compareAndSet(tail, tail.next.value!!) // helps to move tail
                }
            } else {
                if (tail.elements.compareAndSet(enqIdx, null, x)) {
                    return
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val head = this.head.value
            val deqIdx = head.deqIdx.getAndIncrement()
            if (deqIdx >= SEGMENT_SIZE) {
                val headNext = head.next.value ?: return null
                this.head.compareAndSet(head, headNext)
                continue
            }
            val res = head.elements.getAndSet(deqIdx, DONE) ?: continue
            return res as T?
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val head = head.value
            val deqIdx = head.deqIdx.value
            val enqIdx = head.enqIdx.value
            if (deqIdx >= enqIdx || deqIdx >= SEGMENT_SIZE) {
                val headNext = head.next.value ?: return true
                this.head.compareAndSet(head, headNext)
                continue
            }
            return false
        }
    }
}

private class Segment(
    val next: AtomicRef<Segment?> = atomic(null),
    val enqIdx: AtomicInt = atomic(0),
    val deqIdx: AtomicInt = atomic(0),
    val elements: AtomicReferenceArray<Any?> = AtomicReferenceArray(SEGMENT_SIZE)
) {
    constructor(x: Any?) : this() { // each next new segment should be constructed with an element
        enqIdx.incrementAndGet()
        elements.getAndSet(0, x)
    }
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS
