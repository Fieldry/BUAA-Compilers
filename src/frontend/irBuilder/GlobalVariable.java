package frontend.irBuilder;

public class GlobalVariable extends GlobalValue {
    private final boolean isConst;
    private final String name;
    private final Value value;

    public GlobalVariable(boolean isConst, String name, Value value) {
        this.isConst = isConst;
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() { return name; }

    public Value getValue() { return value; }

    public boolean isConst() { return isConst; }

    @Override
    public String toString() {
        return "@" + name + " = dso_local global " + value.getType() + " " + ((ConstantInt) value).getValue();
    }
}
