/**
 * @author : Slastin Aleksandr
 */
class Solution : AtomicCounter {
    private val root = Node(0)
    private val last = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        var old: Int
        do {
            old = last.get().x
            val node = Node(old + x)
            last.set(last.get().next.decide(node))
        } while (last.get() != node)
        return old
    }

    private class Node(val x: Int, val next: Consensus<Node> = Consensus())
}
