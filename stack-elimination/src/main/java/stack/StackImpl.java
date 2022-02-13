package stack;

import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicIntArray;
import kotlinx.atomicfu.AtomicRef;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class StackImpl implements Stack {
    private final static int ARRAY_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private final static int FIND_ATTEMPTS = 4;
    private final static int SPIN_WAIT_TIMES = 10_000;

    private final static int DONE = Integer.MIN_VALUE + 1;
    private final static int NULL = Integer.MIN_VALUE + 2;

    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicIntArray eliminationArray = new AtomicIntArray(ARRAY_SIZE);

    public StackImpl() {
        for (int i = 0; i < ARRAY_SIZE; i++) {
            eliminationArray.get(i).setValue(NULL);
        }
    }

    private int getRandomIndex() {
        return ThreadLocalRandom.current().nextInt(ARRAY_SIZE);
    }

    @Override
    public void push(int x) {
        if (x != DONE && x != NULL && eliminationPush(x)) {
            return;
        }
        basePush(x);
    }

    private boolean eliminationPush(int x) {
        int index = getRandomIndex();
        for (int attempt = 0; attempt < FIND_ATTEMPTS; attempt++) {
            int curIndex = (index + attempt) % ARRAY_SIZE;
            AtomicInt element = eliminationArray.get(curIndex);
            if (element.compareAndSet(NULL, x)) {
                return extracted(element);
            }
        }
        return false;
    }

    private boolean extracted(AtomicInt element) {
        for (int i = 0; i < SPIN_WAIT_TIMES; i++) ;
        return Objects.equals(element.getAndSet(NULL), DONE);
    }

    private void basePush(int x) {
        while (true) {
            Node curHead = head.getValue();
            Node newHead = new Node(x, curHead);
            if (head.compareAndSet(curHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        int index = getRandomIndex();
        for (int attempt = 0; attempt < FIND_ATTEMPTS; attempt++) {
            int curIndex = (index + attempt) % ARRAY_SIZE;
            AtomicInt element = eliminationArray.get(curIndex);
            int value = element.getValue();
            if (value != NULL && value != DONE && element.compareAndSet(value, DONE)) {
                return value;
            }
        }
        return basePop();
    }

    private int basePop() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curHead, curHead.next)) {
                return curHead.x;
            }
        }
    }

    private static class Node {
        final Node next;
        final int x;

        Node(int x, Node next) {
            this.next = next;
            this.x = x;
        }
    }
}
