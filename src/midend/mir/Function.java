package midend.mir;

import backend.MIPSCode;
import utils.inodelist.IList;
import utils.inodelist.INode;

import java.util.ArrayList;

public class Function extends GlobalValue {
    private final IList<INode> paramFetchList = new IList<>();
    private final IList<BasicBlock> basicBlockList = new IList<>();
    private final ArrayList<Value> params = new ArrayList<>();
    private Module parent;

    public Function() {}

    public Function(Type type, String name, Module parent) {
        super(type, name);
        this.parent = parent;
    }

    public IList<BasicBlock> getBBlockList() { return basicBlockList; }

    public ArrayList<Value> getParams() { return params; }

    public IList<INode> getParamFetchList() { return paramFetchList; }

    public String getName() { return name; }

    public Module getParent() { return parent; }

    public void addBBlock(BasicBlock block) { basicBlockList.addBack(block); }

    public void addParam(Value value) { params.add(value); }

    public void addParam(Instruction inst) { paramFetchList.addBack(inst); }

    public void addParam(MIPSCode inst) { paramFetchList.addBack(inst); }

    @Override
    public String toString() {
        return  "define dso_local " + type + " " + name + "(" +
                params.stream().map(value -> value.getType().toString())
                        .reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }
}
