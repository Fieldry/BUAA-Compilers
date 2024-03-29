package midend.mir;

import utils.inodelist.IList;
import utils.inodelist.INode;

import java.util.ArrayList;
import java.util.List;

public class Module extends INode {
    private final IList<Function> functionList = new IList<>();
    private final List<GlobalVariable> globalList = new ArrayList<>();

    public Module() {}

    public void addFunction(Function function) { functionList.addBack(function); }

    public void addMainFunction(Function main) { functionList.addFront(main); }

    public void addGlobal(GlobalVariable global) { globalList.add(global); }

    public GlobalVariable getGlobal(int index) { return globalList.get(index); }

    public IList<Function> getFunctionList() { return functionList; }

    public List<GlobalVariable> getGlobalList() { return globalList; }
}
