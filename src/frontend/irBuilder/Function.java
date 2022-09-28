package frontend.irBuilder;

import java.util.List;

public class Function extends GlobalValue {
    private List<BasicBlock> basicBlockList;
    private Module parent;

    public List<BasicBlock> getBasicBlockList() { return basicBlockList; }

    public Module getParent() { return parent; }
}
