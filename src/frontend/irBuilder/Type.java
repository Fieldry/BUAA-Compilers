package frontend.irBuilder;

public abstract class Type {
    public static class VoidType extends Type {

    }
    public static class IntType extends Type {
        private final int bit;

        public IntType(int bit) { this.bit = bit; }

        public IntType() { this.bit = 32; }

        public int getBit() { return this.bit; }

        @Override
        public String toString() {
            return String.format("i%d", bit);
        }
    }
    public static class FuncType extends Type {

    }
    public static class PointerType extends Type {

    }
    public static class ArrayType extends Type {

    }
}
