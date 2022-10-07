package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class Function extends GlobalValue {
    private final List<BasicBlock> basicBlockList = new ArrayList<>();;
    private final String name;
    private final Module parent;

    public Function(String name, Module parent) {
        this.name = name;
        this.parent = parent;
    }

    public List<BasicBlock> getBBlockList() { return basicBlockList; }

    @Override
    public String getName() { return name; }

    public Module getParent() { return parent; }

    public void addBBlock(BasicBlock block) { basicBlockList.add(block); }
}
