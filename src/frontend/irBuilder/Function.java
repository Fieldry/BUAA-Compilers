package frontend.irBuilder;

import java.util.List;

public class Function extends GlobalValue {
    private List<BasicBlock> basicBlockList;
    private Module parent;

    public List<BasicBlock> getBBlockList() { return basicBlockList; }

    public Module getParent() { return parent; }

    public void addBBlock(BasicBlock block) { basicBlockList.add(block); }
}
