package frontend.irBuilder;

import java.util.ArrayList;

public abstract class Type {

    public boolean isVoidType() { return this instanceof VoidType; }

    public boolean isInt1Type() { return this == IntType.INT1_TYPE; }

    public boolean isInt32Type() { return this == IntType.INT32_TYPE; }

    public boolean isIntType() { return isInt1Type() || isInt32Type(); }

    public boolean isPointerType() { return this instanceof PointerType; }

    public boolean isArrayType() { return this instanceof ArrayType; }

    public static class VoidType extends Type {
        public static final VoidType VOID_TYPE = new VoidType();

        @Override
        public String toString() {
            return "void";
        }
    }

    public static class IntType extends Type {
        private final int bit;
        public static final IntType INT32_TYPE = new IntType(32);
        public static final IntType INT1_TYPE = new IntType(1);

        public IntType(int bit) { this.bit = bit; }

        @Override
        public String toString() { return "i" + bit; }
    }

    public static class PointerType extends Type {
        private final Type innerType;

        public PointerType(Type innerType) {
            this.innerType = innerType;
        }

        public Type getInnerType() {
            return innerType;
        }

        @Override
        public String toString() { return innerType + "*"; }
    }

    public static class ArrayType extends Type {
        private final int size;
        private final Type baseType;
        private final ArrayList<Integer> dims = new ArrayList<>();

        public ArrayType(int size, Type type) {
            this.size = size;
            baseType = type;
            dims.add(size);
            if (baseType.isArrayType()) {
                dims.addAll(((ArrayType) baseType).getDims());
            }
        }

        public int getSize() { return size; }

        public Type getBaseType() { return baseType; }

        public ArrayList<Integer> getDims() { return dims; }

        @Override
        public String toString() {
            return "[" + size + " x " + baseType + "]";
        }
    }
}
