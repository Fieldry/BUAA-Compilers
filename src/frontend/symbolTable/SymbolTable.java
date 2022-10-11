package frontend.symbolTable;

public interface SymbolTable<T> {
    void addSymbol(String name, T symbol) throws Exception;

    T findSymbol(String name);

    T findSymbolInAll(String name);
}
