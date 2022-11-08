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
    private final RegScheduler regScheduler = new RegScheduler();
    private final LinkedHashMap<String, Address> memoryAddress = new LinkedHashMap<>();
    private int sp = 0;
    private boolean inMain = false;
    private Function curFunction;
    private BasicBlock curBBlock;

    private Register findRegForSymOrInt(Value value) {
        Register reg = allocRegForSymOrInt(value);
        if (value instanceof ConstantInt) {
            curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
        }
        return reg;
    }
    private Register allocRegForSymOrInt(Value value) {
        return regScheduler.allocTemp(value);
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

        writer.writeln(".text:");
        for (Function function : mirModule.getFunctionList()) {
            curFunction = new Function(function.getType(), function.getName(), lirModule);
            lirModule.addFunction(curFunction);
            writer.writeln("Function_" + curFunction.getName() + ":");
            if (function.getName().equals("main")) {
                inMain = true;
            } else {
                functionHelper();
            }
            for (BasicBlock block : function.getBBlockList()) {
                curBBlock = new BasicBlock(curFunction.getName() + block.getName(), curFunction);
                curFunction.addBBlock(curBBlock);
                genBBlock(block);
            }
        }
    }

    private void functionHelper() {}

    private void genBBlock(BasicBlock block) {
        writer.writeln(curBBlock.getName() + ":");
        Pair<MIPSCode, MIPSCode> pair;
        for (INode inst : block.getInstList()) {
            if (inst instanceof BinaryInst) {
                pair = visit((BinaryInst) inst);
            } else if (inst instanceof AllocInst) {
                pair = visit((AllocInst) inst);
            } else if (inst instanceof MemoryInst) {
                pair = visit((MemoryInst) inst);
            } else if (inst instanceof FuncCallInst) {
                pair = visit((FuncCallInst) inst);
            } else {
                pair = Pair.of(new NopCode(), new NopCode());
            }
            curBBlock.addMipsCode(pair);
        }
        Instruction inst = block.getTerminator();
        if (inst instanceof BranchInst) {
            pair = visit((BranchInst) inst);
        } else {
            pair = visit((RetInst) inst);
        }
        curBBlock.addMipsCode(pair);

        for (INode iNode : curBBlock.getInstList()) {
            // writer.writeln("\t" + iNode);
            if (iNode instanceof NopCode) {
                iNode.remove();
            } else {
                writer.writeln("\t" + iNode);
            }
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(AllocInst inst) {
        Type type = inst.getValue().getType();
        int size = 1;
        if (type.isInt32Type()) {
            Register reg = regScheduler.allocGlobal(inst.getValue());
            if (reg != null) {
                return Pair.of(new NopCode(), new NopCode());
            }
        } else while (type.isArrayType()) {
            size *= ((ArrayType) type).getSize();
            type = ((ArrayType) type).getBaseType();
        }
        sp -= size * 4;
        memoryAddress.put(inst.getValue().getIdent(), new BaseAddress(Register.R29, new ImmNum(sp)));
        return Pair.of(new NopCode(), new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(BinaryInst inst) {
        Value lValue = inst.getLValue(), rValue = inst.getRValue();
        Object l, r;
        Register res = regScheduler.allocGlobal(inst.getResValue());
        MIPSCode code;
        if (lValue instanceof ConstantInt) {
            l = ImmNum.toImmNum(lValue);
            r = allocRegForSymOrInt(rValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) r, (ImmNum) l);
        } else if (rValue instanceof ConstantInt) {
            r = ImmNum.toImmNum(rValue);
            l = allocRegForSymOrInt(lValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) l, (ImmNum) r);
        } else {
            l = allocRegForSymOrInt(lValue);
            r = allocRegForSymOrInt(rValue);
            code = new BinaryRegRegCode(BinaryRegRegCode.toOp(inst.getOp()), res, (Register) l, (Register) r);
        }
        return Pair.of(code, new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(MemoryInst inst) {
        if (inst.getFlag() == 1) {
            /* 1 for load */
            Register from = regScheduler.find(inst.getFrom());
            Register to = regScheduler.allocTemp(inst.getTo());
            if (from != null) {
                return Pair.of(new MoveCode(to, from), new NopCode());
            } else {
                Address address = memoryAddress.get(inst.getFrom().getIdent());
                return Pair.of(new LoadWordCode(to, address), new NopCode());
            }
        } else {
            /* 0 for store */
            Register from;
            Register to = regScheduler.find(inst.getTo());
            Address address = memoryAddress.get(inst.getTo().getIdent());
            Value value = inst.getFrom();
            MIPSCode first, second;
            if (value instanceof ConstantInt) {
                ImmNum immNum = ImmNum.toImmNum(value);
                if (to != null) {
                    first = new LoadImmCode(to, immNum);
                    second = new NopCode();
                } else {
                    from = regScheduler.allocTemp(value);
                    first = new LoadImmCode(from, immNum);
                    second = new StoreWordCode(from, address);
                }
            } else {
                from = regScheduler.find(value);
                if (to != null) {
                    first = new MoveCode(to, from);
                } else {
                    first = new StoreWordCode(from, address);
                }
                second = new NopCode();
            }
            return Pair.of(first, second);
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(BranchInst inst) {
        if (inst.getCond() == null) {
            return Pair.of(new JumpCode(new Label(inst.getThenBlock().getName())), new NopCode());
        } else {
            BnezCode first = new BnezCode(allocRegForSymOrInt(inst.getCond()), new Label(inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(new Label(inst.getElseBlock().getName()));
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
                first = new LoadImmCode(Register.R2, ImmNum.toImmNum(inst.getValue()));
            else {
                Register rt = regScheduler.find(inst.getValue());
                first = new MoveCode(Register.R2, rt);
            }
            return Pair.of(first, jr);
        }
    }

    private void pushArguments(String name, ArrayList<Value> params) {
        Value value;
        Register reg;
        for (int i = 0, len = params.size(); i < len; i++) {
            value = params.get(i);
            if ((reg = regScheduler.allocParam()) == null) {
                sp -= 4;
                Address address = new BaseAddress(Register.R29, new ImmNum(sp));
                memoryAddress.put(String.format("%s_param%d", name, i), address);
                curBBlock.addMipsCode(new StoreWordCode(findRegForSymOrInt(value), address));
            } else {
                if (value instanceof ConstantInt) {
                    curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
                } else {
                    curBBlock.addMipsCode(new MoveCode(reg, regScheduler.find(value)));
                }
            }
        }
        curBBlock.addMipsCode(new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.ADD),
                Register.R29, Register.R29, new ImmNum(sp)));
    }

    private Pair<MIPSCode, MIPSCode> visit(FuncCallInst inst) {
        pushArguments(inst.getFunction().getName(), inst.getParams());
        JumpLinkCode jal = new JumpLinkCode(new Label(inst.getFunction().getName()));
        if (inst.getFunction().getType().isInt32Type()) {
            return Pair.of(jal, new MoveCode(regScheduler.allocTemp(inst.getResValue()), Register.R2));
        } else return Pair.of(jal, new NopCode());
    }
}
