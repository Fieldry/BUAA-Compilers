package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class Module extends Value {
    private final List<Function> functionList;
    private final List<GlobalVariable> globalList;

    public Module() {
        functionList = new ArrayList<>();
        globalList = new ArrayList<>();
    }

    public void addFunction(Function function) { functionList.add(function); }

    public void addGlobal(GlobalVariable global) { globalList.add(global); }

    public Function getFunction(int index) { return functionList.get(index); }

    public GlobalVariable getGlobal(int index) { return globalList.get(index); }

    public List<Function> getFunctionList() {
        return functionList;
    }

    public List<GlobalVariable> getGlobalList() {
        return globalList;
    }
}
