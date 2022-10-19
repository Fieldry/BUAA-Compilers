package backend;

public class ImmNum {
    public static ImmNum Zero_Imm = new ImmNum(0);

    private final int value;

    public ImmNum(int value) { this.value = value; }

    public int getValue() { return value; }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
