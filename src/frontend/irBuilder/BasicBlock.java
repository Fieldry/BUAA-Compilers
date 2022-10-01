package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<Instruction> instList = new ArrayList<>();
    private Function parent;
    private Instruction terminator;

    public BasicBlock() {}

    public List<Instruction> getInstList() { return instList; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }

    public void addInst(Instruction instruction) { instList.add(instruction); }
}
