package frontend.symbolTable;

import java.util.Map;
import java.util.HashMap;

import frontend.irBuilder.Value;

public class SymbolValueTable implements SymbolTable<Value> {
    private final Map<String, Value> symbolMap = new HashMap<>();
    private final SymbolValueTable parent;

    public SymbolValueTable(SymbolValueTable table) {
        parent = table;
    }

    public void addSymbol(String name, Value value) {
        symbolMap.put(name, value);
    }

    public Value findSymbol(String name) { return symbolMap.get(name); }

    public Value findSymbolInAll(String name) {
        SymbolValueTable temp = this;
        Value value = null;
        while (temp != null && value == null) {
            value = temp.findSymbol(name);
            temp = temp.parent;
        }
        return value;
    }

    public SymbolValueTable getParent() {
        return parent;
    }
}