package frontend.irBuilder;

import frontend.inodelist.IList;
import frontend.inodelist.INode;

public class BasicBlock extends INode {
    private final IList<Instruction> instList = new IList<>();
    private final String name;
    private final Function parent;
    private Instruction terminator;

    public BasicBlock(String name, Function parent) {
        this.name = name;
        this.parent = parent;
    }

    public IList<Instruction> getInstList() { return instList; }

    public String getName() { return name; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }

    public void addInst(Instruction instruction) { instList.addBack(instruction); }

    @Override
    public String toString() {
        return name;
    }
}
