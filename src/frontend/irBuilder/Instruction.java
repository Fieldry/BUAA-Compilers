package frontend.irBuilder;

import frontend.irBuilder.Type.*;

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
        private Value lValue;
        private Value rValue;
    }

    public static class BinaryInst extends Instruction {
        public enum BinaryOp {
            MULTIPLE, DIVIDE, MOD,
            PLUS, MINUS,
            EQUAL, NEQ,
            GT, GE, LT, LE,
            LAND,
            LOR
        }
        private BinaryOp op;
        private Value lValue;
        private Value rValue;
    }

    public static class AllocInst extends Instruction {
        IntType type;

        public AllocInst(int bit) { type = new IntType(bit); }
    }

    public static class RetInst extends Instruction {
        private final Type type;
        private Value value;
        private int number;

        public RetInst(Type type, Value value) {
            this.type = type;
            this.value = value;
        }

        public RetInst(Type type, int number) {
            this.type = type;
            this.number = number;
        }

        public Value getValue() {
            return value;
        }

        public int getNumber() {
            return number;
        }
    }
}
