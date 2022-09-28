package frontend.irBuilder;

public abstract class Instruction extends User {
    private BasicBlock parent;

    public BasicBlock getParent() { return parent; }
}
