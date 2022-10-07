package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<Instruction> instList = new ArrayList<>();
    private final String name;
    private final Function parent;
    private Instruction terminator;

    public BasicBlock(String name, Function parent) {
        this.name = name;
        this.parent = parent;
    }

    public List<Instruction> getInstList() { return instList; }

    @Override
    public String getName() { return name; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }

    public void addInst(Instruction instruction) { instList.add(instruction); }

    @Override
    public String toString() {
        return name;
    }
}
