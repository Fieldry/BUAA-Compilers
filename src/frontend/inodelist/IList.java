package frontend.inodelist;

import java.util.Iterator;

public class IList<T extends INode> implements Iterable<T> {
    private T begin;
    private T end;

    public IList() {
        begin = end = null;
    }

    public void addBack(T node) {
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
        if (node != null) node.remove();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private T cursor = getBegin();

            @Override
            public boolean hasNext() {
                return cursor != null;
            }

            @Override
            public T next() {
                T temp = cursor;
                cursor = (T) cursor.getNext();
                return temp;
            }
        };
    }
}
