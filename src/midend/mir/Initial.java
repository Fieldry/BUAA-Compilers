package midend.mir;

import java.util.ArrayList;
import midend.mir.Type.ArrayType;

public abstract class Initial extends Value {
    protected final Type type;

    public Initial(Type type) { this.type = type; }

    public boolean isArrayInit() { return this instanceof ArrayInitial; }

    public boolean isValueInit() { return this instanceof ValueInitial; }

    public Type getType() { return type; }

    public abstract String toMIPS(boolean needPrefix);

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
        public String toMIPS(boolean needPrefix) {
            if (needPrefix)
                return ".word " + this;
            else
                return toString();
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

        public String toMIPS(boolean needPrefix) {
//            for (Initial initial : initValues) {
//                System.out.println(initial.toMIPS(true));
//            }
            String prefix = needPrefix ? ".word " : "";
            return prefix + initValues.stream().map(initial -> initial.toMIPS(false)).reduce((x, y) -> x + ", " + y).orElse("");
        }
    }

    public static class ZeroInitial extends Initial {
        public ZeroInitial(Type type) { super(type); }

        @Override
        public String toString() {
            return "zeroinitializer";
        }

        @Override
        public String toMIPS(boolean needPrefix) {
            int size = 1;
            for (Integer i : ((ArrayType) type).getDims()) size *= i;
            if (needPrefix) return ".space " + size * 4;
            else return "" + size;
        }
    }
}
