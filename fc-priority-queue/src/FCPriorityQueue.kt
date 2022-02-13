import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReferenceArray

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val array: AtomicReferenceArray<Any?> = AtomicReferenceArray(ARRAY_SIZE)
    private val lock = Lock()

    init {
        for (i in 0 until ARRAY_SIZE) array.set(i, FREE)
    }

    private fun randomIndex() = ThreadLocalRandom.current().nextInt(ARRAY_SIZE)

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? = commitOperation(PollOperation.INSTANCE)

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? = commitOperation(PeekOperation.INSTANCE)

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        commitOperation(AddOperation(element))
    }

    private fun commitOperation(operation: Operation): E? {
        val idx = placeOperation(operation)
        if (idx == -1) {
            asCombiner()
            val res = combinerOperation(operation)
            lock.unlock()
            return res
        }
        while (true) {
            val res = array.get(idx)
            if (res !is Operation) {
                array.compareAndSet(idx, res, FREE)
                return res as E?
            }
            if (lock.tryLock()) {
                asCombiner()
                lock.unlock()
            }
        }
    }

    private fun placeOperation(operation: Operation): Int {
        while (true) {
            if (lock.tryLock()) return -1
            val idx = randomIndex()
            if (array.compareAndSet(idx, FREE, operation)) return idx
        }
    }

    private fun asCombiner() {
        for (i in 0 until ARRAY_SIZE) {
            val operation = array.get(i)
            if (operation !is Operation) continue
            array.set(i, combinerOperation(operation))
        }
    }

    private fun combinerOperation(operation: Operation): E? =
        when (operation) {
            is PollOperation -> q.poll()
            is PeekOperation -> q.peek()
            is AddOperation -> {
                q.add(operation.x as E)
                null
            }
        }
}

private val FREE = Any()
private val ARRAY_SIZE = Runtime.getRuntime().availableProcessors() * 2


sealed interface Operation

class PollOperation : Operation {
    companion object {
        val INSTANCE = PollOperation()
    }
}

class PeekOperation : Operation {
    companion object {
        val INSTANCE = PeekOperation()
    }
}

class AddOperation(val x: Any) : Operation

class Lock {
    private val lock = AtomicBoolean(false)

    fun tryLock() = lock.compareAndSet(false, true)

    fun unlock() {
        lock.getAndSet(false)
    }
}
