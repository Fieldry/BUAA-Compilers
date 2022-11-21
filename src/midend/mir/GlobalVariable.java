package midend.mir;

public class GlobalVariable extends GlobalValue {
    private final boolean isConst;
    private final Initial initValue;

    public GlobalVariable(boolean isConst, Type type, String name, String ident, Initial value) {
        super(type, name);
        this.ident = ident;
        this.isConst = isConst;
        initValue = value;
    }

    public Initial getValue() { return initValue; }

    public boolean isConst() { return isConst; }

    public Initial getInitValue() { return initValue; }

    @Override
    public String toString() {
        return name + " = dso_local " +
                (isConst ? "constant" : "global")
                + " " + type + " " + (initValue == null ? "" : initValue);
    }
}
