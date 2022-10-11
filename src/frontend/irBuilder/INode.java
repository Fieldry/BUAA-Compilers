package frontend.irBuilder;

public class INode {
    private INode prev;
    private INode next;

    public boolean hasPrev() { return prev != null; }

    public boolean hasNext() { return next != null; }

    public void setPrev(INode node) { prev = node; }

    public void setNext(INode node) { next = node; }

    public void insertAfter(INode node) {
        node.prev = this;
        node.next = next;
        if(hasNext()) next.prev = node;
        next = node;
    }

    public void insertBefore(INode node) {
        node.prev = prev;
        node.next = this;
        if(hasPrev()) prev.next = node;
        prev = node;
    }

    public void remove() {
        
    }
}
