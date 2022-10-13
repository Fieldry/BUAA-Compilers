package frontend.irBuilder;

public abstract class Type {
    public static class VoidType extends Type {
        public static final VoidType vd = new VoidType();

        @Override
        public String toString() {
            return "void";
        }
    }
    public static class IntType extends Type {
        private final int bit;
        public static final IntType i32 = new IntType();
        public static final IntType i1 = new IntType(1);

        public IntType(int bit) { this.bit = bit; }

        public IntType() { this.bit = 32; }

        @Override
        public String toString() { return "i" + bit; }
    }

    public static class PointerType extends Type {
        private final int bit;
        public static final PointerType i32 = new PointerType();

        public PointerType() { this.bit = 32; }

        @Override
        public String toString() { return "i" + bit + "*"; }
    }

    public static class LabelType extends Type {

    }
    public static class ArrayType extends Type {

    }
}
