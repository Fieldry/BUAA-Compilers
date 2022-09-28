package frontend.irBuilder;

import java.util.List;

public class BasicBlock extends Value {
    private List<Instruction> instList;
    private Function parent;
    private Instruction terminator;

    public List<Instruction> getInstList() { return instList; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }
}
