package frontend.irBuilder;

import java.util.ArrayList;

public class Initial {
    protected final Type type;

    public Initial(Type type) { this.type = type; }

    public boolean isArrayInit() { return this instanceof ArrayInitial; }

    public boolean isValueInit() { return this instanceof ValueInitial; }

    public Type getType() { return type; }

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
    }

    public static class ZeroInitial extends Initial {
        public ZeroInitial(Type type) { super(type); }

        @Override
        public String toString() {
            return "zeroinitializer";
        }
    }
}
