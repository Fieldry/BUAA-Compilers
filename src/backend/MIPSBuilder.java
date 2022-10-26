package backend;

import frontend.irBuilder.ConstantInt;
import frontend.irBuilder.Instruction.*;
import backend.MIPSCode.*;
import backend.Registers.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.irBuilder.Value;
import utils.Pair;

public class MIPSBuilder {

    private Register getRegister(Value value) {
        return null;
    }

    private MIPSCode visit(BinaryInst inst) {
        Value lValue = inst.getLValue(), rValue = inst.getRValue();
        Object l = null, r = null;
        Register res = Registers.getRegister();
        if (lValue instanceof ConstantInt) {
            l = new ImmNum(((ConstantInt) lValue).getValue());
            r = getRegister(rValue);
            return new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), (Register) r, res, (ImmNum) l);
        }
        else if (rValue instanceof ConstantInt) {
            r = new ImmNum(((ConstantInt) rValue).getValue());
            l = getRegister(lValue);
            return new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), (Register) l, res, (ImmNum) r);
        }
        else {
            l = getRegister(lValue);
            r = getRegister(rValue);
            return new BinaryRegRegCode(BinaryRegRegCode.toOp(inst.getOp()), (Register) l, (Register) r, res);
        }
    }

    private MIPSCode visit(UnaryInst inst) {
        return null;
    }

    private MIPSCode visit(BranchInst inst) {
        if (inst.getCond() == null) {
            return new JumpCode(inst.getThenBlock().getName());
        } else {
            return new BnezCode(getRegister(inst.getCond()), new Label(inst.getThenBlock().getName()));
        }
    }

    private MIPSCode visit(AllocInst inst) {
        return null;
    }

    private Pair<MIPSCode, MIPSCode> visit(RetInst inst) {
        JumpRegCode jr = new JumpRegCode(Register.R31);
        if (inst.getType().isVoidType()) {
            return new Pair<>(new NopCode(), jr);
        } else {
            MIPSCode first;
            if (inst.getValue() instanceof ConstantInt)
                first = new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.ADD),
                        Register.R0, Register.R2, ImmNum.toImmNum(inst.getValue()));
            else {
                Register rt = getRegister(inst.getValue());
                first = new BinaryRegRegCode(BinaryRegRegCode.toOp(BinaryOp.ADD),
                        Register.R2, Register.R0, rt);
            }
            return new Pair<>(first, jr);
        }
    }
}
