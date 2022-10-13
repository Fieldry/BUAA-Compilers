package frontend.irBuilder;

import frontend.inodelist.IList;

import java.util.ArrayList;

public class Function extends GlobalValue {
    private final IList<BasicBlock> basicBlockList = new IList<>();
    private final ArrayList<Value> params = new ArrayList<>();
    private final String name;
    private final Module parent;

    public Function(Type type, String name, Module parent) {
        super(type);
        this.name = name;
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
        String res = "define dso_local " + type + " " + name + "(";
        if (!params.isEmpty()) {
            Value value = params.get(0);
            res = res + value.getType() + " " + value;
            if (params.size() > 1) for (int i = 1, len = params.size(); i < len; i++) {
                res = res + ", " + value.getType() + " " + value;
            }
        }
        return  res + ") ";
    }
}
