package frontend.symbolTable;

import frontend.exception.SysYException;
import frontend.tree.SysYTree.SysYSymbol;

import java.util.HashMap;
import java.util.Map;

public class SymbolSysYTable implements SymbolTable<SysYSymbol>{
    public enum STKind {
        INT_FUNC,
        VOID_FUNC
    }

    private final Map<String, SysYSymbol> symbolMap;
    private final SymbolSysYTable parent;
    private STKind kind;

    public SymbolSysYTable(SymbolSysYTable parent, STKind kind) {
        this.parent = parent;
        symbolMap = new HashMap<>();
        this.kind = kind;
    }

    public SymbolSysYTable(SymbolSysYTable parent) {
        this.parent = parent;
        symbolMap = new HashMap<>();
    }

    public void addSymbol(String name, SysYSymbol symbol) throws SysYException {
        if (this.symbolMap.containsKey(name)) {
            throw new SysYException(SysYException.EKind.b);
        } else {
            this.symbolMap.put(name, symbol);
        }
    }

    public SysYSymbol findSymbol(String name) { return this.symbolMap.get(name); }

    public SysYSymbol findSymbolInAll(String name) {
        SymbolSysYTable temp = this;
        SysYSymbol symbol = null;
        while (temp != null && symbol == null) {
            symbol = temp.findSymbol(name);
            temp = temp.parent;
        }
        return symbol;
    }

    public SymbolSysYTable getParent() {
        return parent;
    }

    public STKind getKind() {
        return kind;
    }
}
