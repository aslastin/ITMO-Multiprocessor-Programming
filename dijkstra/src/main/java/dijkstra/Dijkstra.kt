package dijkstra

import java.util.concurrent.Phaser
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                val cur: Node? = q.poll()
                if (cur == null) {
                    if (q.isFinished()) break
                    continue
                }
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val toDistance = e.to.distance
                        if (toDistance <= cur.distance + e.weight) break
                        if (e.to.casDistance(toDistance, cur.distance + e.weight)) {
                            q.add(e.to)
                            break
                        }
                    }
                }
                q.notifyDone()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}