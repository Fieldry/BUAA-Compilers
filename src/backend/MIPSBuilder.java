package backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import midend.mir.*;
import midend.mir.Instruction.*;
import backend.MIPSCode.*;
import backend.Registers.*;
import backend.Address.*;
import midend.mir.Instruction.BinaryInst.BinaryOp;
import midend.mir.Module;
import midend.mir.Type.*;
import utils.Pair;
import utils.Writer;
import utils.inodelist.INode;

public class MIPSBuilder {
    private final TempRegScheduler tempRegScheduler = new TempRegScheduler();
    private final GlobalRegScheduler globalRegScheduler = new GlobalRegScheduler();

    private final LinkedHashMap<String, Address> memoryAddress = new LinkedHashMap<>();
    private int sp = 0;
    private Register getRegister(Value value) {
        return tempRegScheduler.find(value);
    }

    private final Module mirModule;
    private final Module lirModule = new Module();

    private final Writer writer;

    public MIPSBuilder(Writer writer, Module module) {
        mirModule = module;
        this.writer = writer;
    }

    public void genModule() {
        writer.setMipsBw();
        for (Function function : mirModule.getFunctionList()) {
            Function lir = new Function();
            for (BasicBlock block : function.getBBlockList()) {
                lir.addBBlock(genBBlock(block));
            }
            lirModule.addFunction(function);
        }
    }

    private BasicBlock genBBlock(BasicBlock block) {
        BasicBlock lir = new BasicBlock("Label" + block.getName());
        writer.writeln(lir.getName() + ":");
        Pair<MIPSCode, MIPSCode> pair;
        for (INode inst : block.getInstList()) {
            if (inst instanceof BinaryInst) {
                pair = visit((BinaryInst) inst);
            } else if (inst instanceof AllocInst) {
                pair = visit((AllocInst) inst);
            } else if (inst instanceof MemoryInst) {
                pair = visit((MemoryInst) inst);
            } else {
                pair = Pair.of(new NopCode(), new NopCode());
            }
            lir.addMipsCode(pair);
            writer.writeln("\t" + pair);
        }
        Instruction inst = block.getTerminator();
        if (inst instanceof BranchInst) {
            pair = visit((BranchInst) inst);
        } else {
            pair = visit((RetInst) inst);
        }
        lir.addMipsCode(pair);
        writer.writeln("\t" + pair);
        return lir;
    }

    private Pair<MIPSCode, MIPSCode> visit(AllocInst inst) {
        Type type = inst.getValue().getType();
        int size = 1;
        while (type.isArrayType()) {
            size *= ((ArrayType) type).getSize();
            type = ((ArrayType) type).getBaseType();
        }
        sp -= size * 4;
        memoryAddress.put(inst.getValue().getName(), new BaseAddress(Register.R29, new ImmNum(sp)));
        return Pair.of(new NopCode(), new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(BinaryInst inst) {
        Value lValue = inst.getLValue(), rValue = inst.getRValue();
        Object l, r;
        Register res = tempRegScheduler.allocReg(inst.getResValue());
        MIPSCode code;
        if (lValue instanceof ConstantInt) {
            l = ImmNum.toImmNum(lValue);
            r = getRegister(rValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) r, (ImmNum) l);
        } else if (rValue instanceof ConstantInt) {
            r = ImmNum.toImmNum(rValue);
            l = getRegister(lValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) l, (ImmNum) r);
        } else {
            l = getRegister(lValue);
            r = getRegister(rValue);
            code = new BinaryRegRegCode(BinaryRegRegCode.toOp(inst.getOp()), res, (Register) l, (Register) r);
        }
        return Pair.of(code, new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(MemoryInst inst) {
        if (inst.getFlag() == 1) {
            /* 1 for load */
            Address address = memoryAddress.get(inst.getFrom().getName());
            Register reg = tempRegScheduler.allocReg(inst.getTo());
            return Pair.of(new LoadWordCode(reg, address), new NopCode());
        } else {
            /* 0 for store */
            Address address = memoryAddress.get(inst.getTo().getName());
            Value value = inst.getFrom();
            Register reg;
            MIPSCode first;
            if (value instanceof ConstantInt) {
                ImmNum immNum = ImmNum.toImmNum(value);
                reg = tempRegScheduler.allocReg(value);
                first = new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.ADD), reg, Register.R0, immNum);
            } else {
                reg = tempRegScheduler.find(inst.getFrom());
                first = new NopCode();
            }
            return Pair.of(first, new StoreWordCode(reg, address));
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(BranchInst inst) {
        if (inst.getCond() == null) {
            return Pair.of(new JumpCode(inst.getThenBlock().getName()), new NopCode());
        } else {
            BnezCode first = new BnezCode(getRegister(inst.getCond()), new Label(inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(inst.getElseBlock().getName());
            return Pair.of(first, second);
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(RetInst inst) {
        JumpRegCode jr = new JumpRegCode(Register.R31);
        if (inst.getType().isVoidType()) {
            return Pair.of(new NopCode(), jr);
        } else {
            MIPSCode first;
            if (inst.getValue() instanceof ConstantInt)
                first = new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.ADD),
                        Register.R2, Register.R0, ImmNum.toImmNum(inst.getValue()));
            else {
                Register rt = tempRegScheduler.allocReg(inst.getValue());
                first = new BinaryRegRegCode(BinaryRegRegCode.toOp(BinaryOp.ADD),
                        Register.R2, Register.R0, rt);
            }
            return Pair.of(first, jr);
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
