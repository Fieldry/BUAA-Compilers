package midend.mir;

import backend.MIPSCode;
import utils.inodelist.IList;
import utils.inodelist.INode;
import utils.Pair;

public class BasicBlock extends Value {
    private final IList<INode> instList = new IList<>();
    private final String name;
    private final Function parent;
    private Instruction terminator;

    public BasicBlock(String name, Function parent) {
        this.name = name;
        this.parent = parent;
        terminator = null;
    }

    public IList<INode> getInstList() { return instList; }

    public String getName() { return name; }

    public Function getParent() { return parent; }

    public Instruction getTerminator() { return terminator; }

    public void addInst(Instruction instruction) { instList.addBack(instruction); }

    public void addMipsCode(MIPSCode code) { instList.addBack(code);}

    public void addMipsCode(Pair<MIPSCode, MIPSCode> code) {
        instList.addBack(code.getFirst());
        instList.addBack(code.getSecond());
    }

    public void setTerminator(Instruction terminator) { this.terminator = terminator; }

    public boolean needTerminator() { return terminator == null; }

    @Override
    public String toString() {
        return name;
    }
}
