package backend;

import utils.inodelist.INode;
import midend.mir.Instruction.BinaryInst.BinaryOp;
import backend.Registers.*;

import java.util.Locale;

public abstract class MIPSCode extends INode {
    public abstract boolean optMove(Register to, Register from);
    public static class BinaryRegImmCode extends MIPSCode {
        public static BinaryRegImmCode sllCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.SLL, rt, rs, imm);
        }
        public static BinaryRegImmCode srlCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.SRL, rt, rs, imm);
        }
        public static BinaryRegImmCode sgeCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.SGE, rt, rs, imm);
        }
        public static BinaryRegImmCode mulCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.MUL, rt, rs, imm);
        }
        public static BinaryRegImmCode subCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.SUBIU, rt, rs, imm);
        }
        public static BinaryRegImmCode addCode(Register rt, Register rs, ImmNum imm) {
            return new BinaryRegImmCode(Op.ADDIU, rt, rs, imm);
        }
        private enum Op {
            MUL, DIV, REM, ADDIU, SUBIU,
            SGT, SGE, SLTI, SLE, SEQ, SNE,
            ANDI, ORI,
            SLL, SRL;

            @Override
            public String toString() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }

        public static Op toOp(BinaryOp op) {
            return Op.values()[op.ordinal()];
        }
        private final Op op;
        private Register rs;
        private Register rt;
        private final ImmNum imm;
        public BinaryRegImmCode(Op op, Register rt, Register rs, ImmNum imm) {
            this.op = op;
            this.rs = rs;
            this.rt = rt;
            this.imm = imm;
        }
        public boolean isMul() { return op.equals(Op.MUL); }
        public boolean isDiv() { return op.equals(Op.DIV); }
        public boolean isMod() { return op.equals(Op.REM); }
        public Register getRs() { return rs; }
        public Register getRt() { return rt; }
        public ImmNum getImm() { return imm; }

        @Override
        public String toString() {
            return op + " " + rt + ", " + rs + ", " + imm;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (rt.equals(from)) {
                rt = to;
                return true;
            } else return false;
        }
    }

    public static class BinaryRegRegCode extends MIPSCode {
        public static BinaryRegRegCode addCode(Register rd, Register rs, Register rt) {
            return new BinaryRegRegCode(Op.ADDU, rd, rs, rt);
        }
        public static BinaryRegRegCode subCode(Register rd, Register rs, Register rt) {
            return new BinaryRegRegCode(Op.SUBU, rd, rs, rt);
        }
        public static BinaryRegRegCode mulCode(Register rd, Register rs, Register rt) {
            return new BinaryRegRegCode(Op.MUL, rd, rs, rt);
        }
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
        private Register rs;
        private Register rt;
        private Register rd;

        public BinaryRegRegCode(Op op, Register rd, Register rs, Register rt) {
            this.op = op;
            this.rs = rs;
            this.rt = rt;
            this.rd = rd;
        }

        @Override
        public String toString() {
            return op + " " + rd + ", " + rs + ", " + rt;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (rd.equals(from)) {
                rd = to;
                return true;
            } else return false;
        }
    }

    public static class JumpCode extends MIPSCode {
        private final Label label;

        public JumpCode(Label tag) { label = tag; }

        public Label getLabel() { return label; }

        @Override
        public String toString() {
            return "j " + label;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class JumpLinkCode extends MIPSCode {
        private final Label label;

        public JumpLinkCode(Label tag) { label = tag; }

        public Label getLabel() { return label; }

        @Override
        public String toString() {
            return "jal " + label;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
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

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class StoreWordCode extends MIPSCode {
        private Register rs;
        private final Address addr;

        public StoreWordCode(Register rs, Address addr) {
            this.rs = rs;
            this.addr = addr;
        }

        @Override
        public String toString() {
            return "sw " + rs + ", " + addr;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class LoadWordCode extends MIPSCode {
        private Register rt;
        private final Address addr;

        public LoadWordCode(Register rt, Address addr) {
            this.rt = rt;
            this.addr = addr;
        }

        @Override
        public String toString() {
            return "lw " + rt + ", " + addr;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (rt.equals(from)) {
                rt = to;
                return true;
            }
            return false;
        }
    }

    public static class LoadImmCode extends MIPSCode {
        private Register reg;
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

        @Override
        public boolean optMove(Register to, Register from) {
            if (reg.equals(from)) {
                reg = to;
                return true;
            } else return false;
        }
    }

    public static class LoadAddressCode extends MIPSCode {
        private Register reg;
        private final Address address;

        public LoadAddressCode(Register reg, Address address) {
            this.reg = reg;
            this.address = address;
        }

        public Register getReg() { return reg; }

        public Address getAddress() { return address; }

        @Override
        public String toString() {
            return "la " + reg + ", " + address;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (reg.equals(from)) {
                reg = to;
                return true;
            } else return false;
        }
    }

    public static class MoveCode extends MIPSCode {
        private Register rs;
        private Register rt;

        public MoveCode(Register rt, Register rs) {
            this.rs = rs;
            this.rt = rt;
        }

        public Register getRs() { return rs; }

        public Register getRt() { return rt; }

        @Override
        public String toString() {
            return "move " + rt + ", " + rs;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (rt.equals(from)) {
                rt = to;
                return true;
            } else return false;
        }
    }

    public static class SysCallCode extends MIPSCode {
        public static SysCallCode sysCall = new SysCallCode();

        @Override
        public String toString() {
            return "syscall";
        }

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class NopCode extends MIPSCode {
        public static NopCode nop = new NopCode();

        @Override
        public String toString() {
            return "nop";
        }

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class BnezCode extends MIPSCode {
        private Register reg;
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

        @Override
        public boolean optMove(Register to, Register from) {
            return false;
        }
    }

    public static class MoveFromCode extends MIPSCode {
        private final String string;
        private Register rt;

        public MoveFromCode(String string, Register rt) {
            this.string = string;
            this.rt = rt;
        }

        @Override
        public String toString() {
            return "mf" + string + ' ' + rt;
        }

        @Override
        public boolean optMove(Register to, Register from) {
            if (rt.equals(from)) {
                rt = to;
                return true;
            } else return false;
        }
    }
}
