package frontend.irBuilder;

public class GlobalVariable extends GlobalValue {
    private final boolean isConst;
    private final String name;
    private int value;

    public GlobalVariable(boolean isConst, String name, int value) {
        this.isConst = isConst;
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() { return name; }

    public int getValue() { return value; }

    public boolean isConst() { return isConst; }
}
