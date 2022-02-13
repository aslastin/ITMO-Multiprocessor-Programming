package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        head = new AtomicRef<>(dummy);
        tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node curTail = tail.getValue();
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail);
                return;
            } else {
                tail.compareAndSet(curTail, curTail.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == tail.getValue()) {
                return Integer.MIN_VALUE;
            }
            Node next = curHead.next.getValue();
            if (head.compareAndSet(curHead, next)) {
                return next.x;
            }
        }
    }

    @Override
    public int peek() {
        Node curHead = head.getValue();
        return curHead == tail.getValue() ? Integer.MIN_VALUE : curHead.next.getValue().x;
    }

    private static class Node {
        final int x;
        AtomicRef<Node> next = new AtomicRef<>(null);

        Node(int x) {
            this.x = x;
        }
    }
}
