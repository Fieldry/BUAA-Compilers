package backend;

import frontend.inodelist.INode;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import backend.Registers.*;

import java.util.Locale;

public class MIPSCode extends INode {
    public static class BinaryRegImmCode extends MIPSCode {
        private enum Op {
            MUL, DIV, REM, ADDIU, SUBIU,
            SGT, SGE, SLTI, SLE, SEQ, SNE,
            ANDI, ORI;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }

        public static Op toOp(BinaryOp op) {
            return Op.values()[op.ordinal()];
        }

        private final Op op;
        private final Register rs;
        private final Register rt;
        private final ImmNum imm;

        public BinaryRegImmCode(Op op, Register rs, Register rt, ImmNum imm) {
            this.op = op;
            this.rs = rs;
            this.rt = rt;
            this.imm = imm;
        }

        @Override
        public String toString() {
            return op + " " + rt + ", " + rs + ", " + imm;
        }
    }

    public static class BinaryRegRegCode extends MIPSCode {
        private enum Op {
            MUL, DIV, REM, ADDU, SUBU,
            SGT, SGE, SLT, SLE, SEQ, SNE,
            AND, OR;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }

        public static Op toOp(BinaryOp op) {
            return Op.values()[op.ordinal()];
        }

        private final Op op;
        private final Register rs;
        private final Register rt;
        private final Register rd;

        public BinaryRegRegCode(Op op, Register rs, Register rt, Register rd) {
            this.op = op;
            this.rs = rs;
            this.rt = rt;
            this.rd = rd;
        }

        @Override
        public String toString() {
            return op + " " + rd + ", " + rs + ", " + rt;
        }
    }

    public static class JumpCode extends MIPSCode {
        private final String tag;

        public JumpCode(String tag) { this.tag = tag; }

        public String getTag() { return tag; }

        @Override
        public String toString() {
            return "j " + tag;
        }
    }

    public static class JumpLinkCode extends MIPSCode {
        private final String tag;

        public JumpLinkCode(String tag) { this.tag = tag; }

        public String getTag() { return tag; }

        @Override
        public String toString() {
            return "jal " + tag;
        }
    }

    public static class JumpRegCode extends MIPSCode {
        private final Register register;

        public JumpRegCode(Register register) { this.register = register; }

        public Register getRegister() { return register; }

        @Override
        public String toString() {
            return "jr " + register;
        }
    }

    public static class StoreWordCode extends MIPSCode {
        private final Register rt;
        private final Address addr;

        public StoreWordCode(Register rt, Address addr) {
            this.rt = rt;
            this.addr = addr;
        }

        @Override
        public String toString() {
            return "sw " + rt + ", " + addr;
        }
    }

    public static class LoadWordCode extends MIPSCode {
        private final Register rt;
        private final Address addr;

        public LoadWordCode(Register rt, Address addr) {
            this.rt = rt;
            this.addr = addr;
        }

        @Override
        public String toString() {
            return "lw " + rt + ", " + addr;
        }
    }

    public static class LoadImmCode extends MIPSCode {
        private final Register reg;
        private final ImmNum imm;

        public LoadImmCode(Register reg, ImmNum imm) { 
            this.reg = reg;
            this.imm = imm;
        }

        public ImmNum getImm() { return imm; }

        @Override
        public String toString() {
            return "li " + reg + ", " + imm;
        }
    }

    public static class LoadAddressCode extends MIPSCode {
        private final Register reg;
        private final Label label;

        public LoadAddressCode(Register reg, Label label) {
            this.reg = reg;
            this.label = label;
        }

        public Register getReg() { return reg; }

        public Label getLabel() { return label; }

        @Override
        public String toString() {
            return "la " + reg + ", " + label;
        }
    }

    public static class MoveCode extends MIPSCode {
        private final Register rs;
        private final Register rt;

        public MoveCode(Register rs, Register rt) {
            this.rs = rs;
            this.rt = rt;
        }

        public Register getRs() { return rs; }

        public Register getRt() { return rt; }

        @Override
        public String toString() {
            return "move " + rt + ", " + rs;
        }
    }

    public static class SysCallCode extends MIPSCode {
        public static SysCallCode sysCall = new SysCallCode();

        @Override
        public String toString() {
            return "syscall";
        }
    }

    public static class NopCode extends MIPSCode {
        public static NopCode nop = new NopCode();

        @Override
        public String toString() {
            return "nop";
        }
    }

    public static class BnezCode extends MIPSCode {
        private final Register reg;
        private final Label label;

        public BnezCode(Register reg, Label label) {
            this.reg = reg;
            this.label = label;
        }

        public Register getReg() { return reg; }

        public Label getLabel() { return label; }

        @Override
        public String toString() {
            return "bnez " + reg + ", " + label;
        }
    }
}
