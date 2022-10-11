package frontend.irBuilder;

import frontend.inodelist.IList;
import frontend.inodelist.INode;

public class Function extends INode {
    private final IList<BasicBlock> basicBlockList = new IList<>();;
    private final String name;
    private final Module parent;

    public Function(String name, Module parent) {
        this.name = name;
        this.parent = parent;
    }

    public IList<BasicBlock> getBBlockList() { return basicBlockList; }

    public String getName() { return name; }

    public Module getParent() { return parent; }

    public void addBBlock(BasicBlock block) { basicBlockList.addBack(block); }
}
