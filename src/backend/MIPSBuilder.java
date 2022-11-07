package backend;

import java.util.ArrayList;

import midend.mir.ConstantInt;
import midend.mir.Instruction.*;
import backend.MIPSCode.*;
import backend.Registers.*;
import midend.mir.Instruction.BinaryInst.BinaryOp;
import midend.mir.Value;
import utils.Pair;

public class MIPSBuilder {
    private final TempRegScheduler tempRegScheduler = new TempRegScheduler();
    private final GlobalRegScheduler globalRegScheduler = new GlobalRegScheduler();
    private Register getRegister(Value value) {
        return tempRegScheduler.find(value);
    }



    private MIPSCode visit(BinaryInst inst) {
        Value lValue = inst.getLValue(), rValue = inst.getRValue();
        Object l, r;
        Register res = tempRegScheduler.allocReg(inst.getResValue());
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

    private MIPSCode visit(BranchInst inst) {
        if (inst.getCond() == null) {
            return new JumpCode(inst.getThenBlock().getName());
        } else {
            return new BnezCode(getRegister(inst.getCond()), new Label(inst.getThenBlock().getName()));
        }
    }

    private MIPSCode visit(MemoryInst inst) {
        if (inst.getFlag() == 1) {
            /* 1 for load */
            Register res = globalRegScheduler.find(inst.getFrom());
            return new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.ADD), res, Register.R0, new ImmNum(1));
        } else {
            return null;
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

    private void pushArguments(ArrayList<Value> params) {

    }

    private MIPSCode visit(FuncCallInst inst) {
        pushArguments(inst.getParams());
        JumpLinkCode jal = new JumpLinkCode(inst.getFunction().getName());

        return null;
    }
}
