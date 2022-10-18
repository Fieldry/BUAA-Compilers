package frontend.irBuilder;

import frontend.inodelist.IList;

import java.util.ArrayList;

public class Function extends GlobalValue {
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

    public String getName() { return name; }

    public Module getParent() { return parent; }

    public void addBBlock(BasicBlock block) { basicBlockList.addBack(block); }

    public void addParam(Value value) { params.add(value); }

    @Override
    public String toString() {
        return  "define dso_local " + type + " " + name + "(" +
                params.stream().map(value -> value.getType() + " " + value)
                        .reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }
}
