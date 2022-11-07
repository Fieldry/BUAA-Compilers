package backend;

import midend.mir.ConstantInt;
import midend.mir.Value;

public class ImmNum {
    public static ImmNum Zero_Imm = new ImmNum(0);

    public static ImmNum toImmNum(Value value) {
        ConstantInt from = (ConstantInt) value;
        return new ImmNum(from.getValue());
    }

    private final int value;

    public ImmNum(int value) { this.value = value; }

    public int getValue() { return value; }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
