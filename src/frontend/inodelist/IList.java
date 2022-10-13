package frontend.inodelist;

import java.util.Iterator;

public class IList<T extends INode> implements Iterable<T> {
    private T begin;
    private T end;
    private int size;

    public IList() {
        size = 0;
        begin = end = null;
    }

    public void addBack(T node) {
        ++size;
        if (end == null) {
            if (node != null) {
                node.setPrev(null);
                node.setNext(null);
            }
            begin = end = node;
        } else {
            end.insertAfter(node);
            end = node;
        }
    }

    public void addFront(T node) {
        ++size;
        if (begin == null) {
            if (node != null) {
                node.setPrev(null);
                node.setNext(null);
            }
            begin = end = node;
        } else {
            begin.insertBefore(node);
            begin = node;
        }
    }

    public T getBegin() {
        return begin;
    }

    public T getEnd() {
        return end;
    }

    public void remove(T node) {
        --size;
        if (node != null) node.remove();
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private T cursor = getBegin();

            @Override
            public boolean hasNext() {
                return cursor != null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                T temp = cursor;
                cursor = (T) cursor.getNext();
                return temp;
            }
        };
    }
}
