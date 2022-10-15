package frontend.irBuilder;

public class GlobalVariable extends GlobalValue {
    private final boolean isConst;
    private final Initial initValue;

    public GlobalVariable(boolean isConst, Type type, String name, Initial value) {
        super(type, name);
        this.isConst = isConst;
        initValue = value;
    }

    public Initial getValue() { return initValue; }

    public boolean isConst() { return isConst; }

    @Override
    public String toString() {
        return name + " = dso_local " +
                (isConst ? "const" : "global")
                + " " + type + " " + initValue;
    }
}
