package frontend.irBuilder;

import frontend.irBuilder.Type.*;

import java.util.Locale;

public abstract class Instruction extends User {
    private BasicBlock parent;

    public Instruction() {}

    public Instruction(BasicBlock parent) { this.parent = parent; }

    public BasicBlock getParent() { return parent; }

    public static class UnaryInst extends Instruction {
        public enum UnaryOp {
            POS,
            NEG,
            NOT
        }
        private UnaryOp op;
        private Value value;
        private Value resValue;
    }

    public static class BinaryInst extends Instruction {
        public enum BinaryOp {
            MUL, SDIV, REM,
            ADD, SUB,
            EQUAL, NEQ,
            GT, GE, LT, LE,
            LAND,
            LOR;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }
        private BinaryOp op;
        private Value lValue;
        private Value rValue;
        private Value resValue;

        public BinaryInst(BasicBlock parent, BinaryOp op, Value lValue, Value rValue, Value resValue) {
            super(parent);
            this.op = op;
            this.lValue = lValue;
            this.rValue = rValue;
            this.resValue = resValue;
        }

        public BinaryOp getOp() { return op; }

        public Value getLValue() { return lValue; }

        public Value getRValue() { return rValue; }

        public Value getResValue() { return resValue; }

        @Override
        public String toString() {
            return resValue + " = " + op + " " + resValue.getType() + " " + lValue + ", " + rValue;
        }
    }

    public static class AllocInst extends Instruction {
        IntType type;

        public AllocInst(int bit) { type = new IntType(bit); }
    }

    public static class RetInst extends Instruction {
        private final Type type;
        private final Value value;

        public RetInst(Type type, Value value) {
            this.type = type;
            this.value = value;
        }

        public Value getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "ret " + type + " " + value;
        }
    }
}
