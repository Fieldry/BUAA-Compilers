package midend.mir;

import utils.inodelist.IList;

public class BasicBlock extends Value {
    private final IList<Instruction> instList = new IList<>();
    private final String name;
    private final Function parent;
    private Instruction terminator;

    public BasicBlock(String name, Function parent) {
        this.name = name;
        this.parent = parent;
        terminator = null;
    }

    public IList<Instruction> getInstList() { return instList; }

    public String getName() { return name; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }

    public void addInst(Instruction instruction) { instList.addBack(instruction); }

    public void setTerminator(Instruction terminator) { this.terminator = terminator; }

    public boolean needTerminator() { return terminator == null; }

    @Override
    public String toString() {
        return name;
    }
}
