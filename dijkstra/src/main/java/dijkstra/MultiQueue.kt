package dijkstra

import kotlinx.atomicfu.locks.ReentrantLock
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MultiQueue<E>(private val workers: Int, private val cmp: Comparator<in E>) {
    private val qs: Array<Triple<ReentrantLock, PriorityQueue<E>, AtomicReference<E?>>> = Array(workers) {
        Triple(ReentrantLock(), PriorityQueue(workers, cmp), AtomicReference(null))
    }
    private val waitingThreads = AtomicInteger(workers)
    private val nonEmptyQueuesCnt = AtomicInteger(0)

    private fun getRandomQueue() = qs[ThreadLocalRandom.current().nextInt(workers)]

    fun add(el: E) {
        while (true) {
            val (lock, q, top) = getRandomQueue()
            if (lock.tryLock()) {
                if (q.isEmpty()) nonEmptyQueuesCnt.incrementAndGet()
                q.add(el)
                updateTop(top, q.peek())
                lock.unlock()
                return
            }
        }
    }

    fun poll(): E? {
        while (true) {
            val triple1 = getRandomQueue()
            val top1Val = triple1.third.get()

            val triple2 = getRandomQueue()
            val top2Val = triple2.third.get()

            if (top1Val == null && top2Val == null) return null

            val (lock, q, top) = if (cmpTops(top1Val, top2Val)) triple1 else triple2

            if (lock.tryLock()) {
                if (q.peek() == null) {
                    lock.unlock()
                    return null
                }
                waitingThreads.decrementAndGet()
                val res = q.poll()
                if (q.isEmpty()) nonEmptyQueuesCnt.decrementAndGet()
                updateTop(top, q.peek())
                lock.unlock()
                return res
            }
        }
    }

    private fun updateTop(top: AtomicReference<E?>, newTop: E?) {
        if (top.get() != newTop) top.compareAndSet(top.get(), newTop)
    }

    private fun cmpTops(top1: E?, top2: E?) = top2 == null || (top1 != null && cmp.compare(top1, top2) == -1)

    fun notifyDone() = waitingThreads.incrementAndGet()

    fun isFinished() = nonEmptyQueuesCnt.get() == 0 && waitingThreads.compareAndSet(workers, workers)
}