package midend.mir;

import java.util.ArrayList;
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
            MUL, SDIV, SREM,
            ADD, SUB,
            SGT, SGE, SLT, SLE,
            EQ, NE,
            AND,
            OR;

            @Override
            public String toString() {
                switch (this) {
                    case EQ: case NE:
                    case SGT: case SGE: case SLT: case SLE:
                        return "icmp " + this.name().toLowerCase(Locale.ROOT);
                    default: return this.name().toLowerCase(Locale.ROOT);
                }
            }
        }
        private final BinaryOp op;
        private final Value lValue;
        private final Value rValue;
        private final Value resValue;

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
            return resValue + " = " + op + " " + lValue.getType() + " " + lValue + ", " + rValue;
        }
    }

    public static class AllocInst extends Instruction {
        private final Value value;

        public AllocInst(BasicBlock parent, Value value) {
            super(parent);
            this.value = value;
        }

        public Value getValue() { return value; }

        @Override
        public String toString() {
            return value + " = alloca " + value.getType();
        }
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
        public Type getType() { return type; }

        @Override
        public String toString() {
            return "ret " + type + (value != null ? " " + value : "");
        }
    }

    public static class MemoryInst extends Instruction {
        /** 0 for store, 1 for load*/
        private final int flag;
        private final Value from;
        private final Value to;

        public MemoryInst(BasicBlock parent, int flag, Value from, Value to) {
            super(parent);
            this.flag = flag;
            this.from = from;
            this.to = to;
        }

        public int getFlag() {
            return flag;
        }

        public Value getFrom() {
            return from;
        }

        public Value getTo() {
            return to;
        }

        @Override
        public String toString() {
            if (flag == 1) return to + " = load " + to.getType() + ", " + from.getType() + " " + from;
            else return "store " + from.getType() + " " + from + ", " + to.getType() + " " + to;
        }
    }

    public static class ZExtInst extends Instruction {
        private final Value from;
        private final Value to;

        public ZExtInst(BasicBlock block, Value from, Value to) {
            super(block);
            this.from = from;
            this.to = to;
        }

        public Value getFrom() { return from; }

        public Value getTo() { return to; }

        @Override
        public String toString() {
            return to + " = zext " + from.getType() + " " + from + " to " + to.getType();
        }
    }

    public static class BranchInst extends Instruction {
        private final Value cond;
        private BasicBlock thenBlock;
        private BasicBlock elseBlock;

        public BranchInst(BasicBlock parent, Value cond) {
            super(parent);
            this.cond = cond;
        }

        public BranchInst(BasicBlock parent, BasicBlock block) {
            super(parent);
            this.cond = null;
            this.thenBlock = block;
            this.elseBlock = null;
        }

        public Value getCond() {
            return cond;
        }

        public BasicBlock getThenBlock() {
            return thenBlock;
        }

        public BasicBlock getElseBlock() {
            return elseBlock;
        }

        public void setThenBlock(BasicBlock thenBlock) {
            this.thenBlock = thenBlock;
        }

        public void setElseBlock(BasicBlock elseBlock) {
            this.elseBlock = elseBlock;
        }

        @Override
        public String toString() {
            if (cond != null)
                return "br " + cond.getType() + " " + cond + ", label %" + thenBlock + ", label %" + elseBlock;
            else return "br label %" + thenBlock;
        }
    }

    public static class FuncCallInst extends Instruction {
        private final Function function;
        private final ArrayList<Value> params;
        private final Value resValue;

        public FuncCallInst(BasicBlock parent, Function function, ArrayList<Value> params, Value resValue) {
            super(parent);
            this.function = function;
            this.params = params;
            this.resValue = resValue;
        }

        public Function getFunction() { return function; }

        public Value getResValue() { return resValue; }

        public ArrayList<Value> getParams() { return params; }

        @Override
        public String toString() {
            return (resValue == null ? "" : resValue + " = ") + "call " + function.getType() + " " + function.getName()
                    + "(" + params.stream().map(value -> value.getType() + " " + value)
                    .reduce((x, y) -> x + ", " + y).orElse("") + ")";
        }
    }

    public static class GEPInst extends Instruction {
        private final Value from;
        private final Value to;
        private final Value index;
        private final boolean flag;

        public GEPInst(BasicBlock parent, Value from, Value to, Value index, boolean flag) {
            super(parent);
            this.from = from;
            this.to = to;
            this.index = index;
            this.flag = flag;
        }

        public Value getFrom() {
            return from;
        }

        public Value getTo() {
            return to;
        }

        public Value getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return to + " = getelementptr " + ((Type.PointerType) from.getType()).getInnerType() + ", "
                    + from.getType() + " " + from + (flag ? ", i32 0, " : ", ") + index.getType() + " " + index;
        }
    }
}
