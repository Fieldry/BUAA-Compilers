package midend.mir;

import java.util.ArrayList;
import midend.mir.Type.ArrayType;

public abstract class Initial extends Value {
    protected final Type type;

    public Initial(Type type) { this.type = type; }

    public boolean isArrayInit() { return this instanceof ArrayInitial; }

    public boolean isValueInit() { return this instanceof ValueInitial; }

    public Type getType() { return type; }

    public abstract String toMIPS();

    public static class ValueInitial extends Initial {
        private final Value value;

        public ValueInitial(Type type, Value value) {
            super(type);
            this.value = value;
        }

        public Value getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public String toMIPS() {
            return ".word " + toString();
        }
    }

    public static class ArrayInitial extends Initial {
        private final ArrayList<Initial> initValues;

        public ArrayInitial(Type type, ArrayList<Initial> values) {
            super(type);
            initValues = values;
        }

        public void addInitValue(Initial value) { initValues.add(value); }

        public ArrayList<Initial> getInitValues () {
            return initValues;
        }

        @Override
        public String toString () {
            return "[" + initValues.stream().map(initial -> initial.getType() + " " + initial)
                    .reduce((x, y) -> x + ", " + y).orElse("") + "]";
        }

        public String toMIPS() {
            return ".word " + initValues.stream().map(Initial::toMIPS).reduce((x, y) -> x + ", " + y).orElse("");
        }
    }

    public static class ZeroInitial extends Initial {
        public ZeroInitial(Type type) { super(type); }

        @Override
        public String toString() {
            return "zeroinitializer";
        }

        @Override
        public String toMIPS() {
            int size = 1;
            for (Integer i : ((ArrayType) type).getDims()) size *= i;
            return ".space " + size;
        }
    }
}
