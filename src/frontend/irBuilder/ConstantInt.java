package frontend.irBuilder;

public class ConstantInt extends Value {
    private final int value;
    private static final ConstantInt zero = new ConstantInt(0);

    public ConstantInt(int value) { this.value = value; }

    public int getValue() { return value; }

    public static ConstantInt getZero() { return zero; }

    @Override
    public Type getType() {
        return Type.IntType.INT32_TYPE;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
