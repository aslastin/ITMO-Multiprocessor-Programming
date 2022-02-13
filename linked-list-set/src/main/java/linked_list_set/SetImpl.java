package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            Window w = new Window();
            w.cur = head;
            w.next = (Node) w.cur.next.getValue();
            while (true) {
                Object node = w.next.next.getValue();
                if (node == null) {
                    return w;
                }
                if (node instanceof Removed) {
                    Node nodeValue = ((Removed) node).next;
                    if (!w.cur.next.compareAndSet(w.next, nodeValue)) {
                        break;
                    }
                    w.next = nodeValue;
                } else {
                    if (w.next.x >= x) {
                        return w;
                    }
                    w.cur = w.next;
                    w.next = (Node) node;
                }
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            }
            if (w.cur.next.compareAndSet(w.next, new Node(x, w.next))) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            }
            Object node = w.next.next.getValue();
            if (!(node instanceof Removed)) {
                Removed newNode = new Removed((Node) node);
                if (w.next.next.compareAndSet(node, newNode)) {
                    w.cur.next.compareAndSet(w.next, node);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }

    private class Node {
        final AtomicRef<Object> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<Object>(next);
            this.x = x;
        }
    }

    private class Removed {
        final Node next;

        Removed(Node next) {
            this.next = next;
        }
    }

    private class Window {
        Node cur, next;
    }
}
