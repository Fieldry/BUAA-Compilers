package backend;

import midend.mir.*;
import midend.mir.Instruction.*;
import backend.MIPSCode.*;
import backend.Registers.*;
import backend.Address.*;
import midend.mir.Instruction.BinaryInst.BinaryOp;
import midend.mir.Module;
import midend.mir.Type.*;
import utils.Pair;
import utils.inodelist.INode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionBuilder {
    private final RegScheduler regScheduler = new RegScheduler();
    private final LinkedHashMap<String, Address> globalMem;
    /**
     * params
     * local var
     * temp registers
     */
    private final LinkedHashMap<String, Address> stackMem = new LinkedHashMap<>();
    /**
     * Parse parameters.
     */
    private final LinkedHashMap<String, Pair<Register, Address>> paramPos;

    private int stackSize;
    private int saveSize;
    private final boolean isMain;
    private final Function mirFunction;
    private Function curFunction;
    private BasicBlock curBBlock;

    public FunctionBuilder(Function mirFunction, boolean isMain, LinkedHashMap<String, Address> globalMem) {
        paramPos = new LinkedHashMap<>();
        this.mirFunction = mirFunction;
        this.isMain = isMain;
        this.globalMem = globalMem;
    }
    private boolean isLibFunction(String name) {
        if (name.contains("@")) name = name.replace("@", "");
        return name.equals("getint") || name.equals("putch") || name.equals("putint");
    }
    private Register findRegOrAddrForSym(Value value) {
        /* param */
        // if (paramPos.containsKey(value.getName())) reg = paramPos.get(value.getName()).getFirst();
        /* global or temp register */
        if (value instanceof ConstantInt) return allocRegForSymOrInt(value);
        Address address = findAddress(value);
        Register reg;
        if (address != null) {
            reg = regScheduler.allocTemp(value);
            curBBlock.addMipsCode(new LoadWordCode(reg, findAddress(value)));
            return reg;
        } else {
            return regScheduler.find(value);
        }
    }
    private Register allocRegForSymOrInt(Value value) {
        Register reg = regScheduler.allocTemp(value);
        if (value instanceof ConstantInt) {
            curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
        }
        return reg;
    }
    private Register findOrAllocReg(Value value) {
        Register reg = findRegOrAddrForSym(value);
        if (reg != null) return reg;
        else return allocRegForSymOrInt(value);
    }
    private Register getRegForRight(Value value) {
        return findOrAllocReg(value);
    }
    private Register getRegForLeft(Value value) {
        Register reg = regScheduler.find(value);
        if (reg == null) return regScheduler.allocTemp(value);
        else return reg;
    }
    private Address findAddress(Value value) {
        String name = value.getName();
        if (stackMem.containsKey(name)) return stackMem.get(name);
        else return globalMem.getOrDefault(name, null);
    }
    private MIPSCode storeWord(Register res, Value value) {
        Address address = null;
        if (paramPos.containsKey(value.getName())) address = paramPos.get(value.getName()).getSecond();
        if (address == null) address = findAddress(value);
        if (address != null) return new StoreWordCode(res, address);
        else return new NopCode();
    }

    public Function firstPass(Module lirModule) {
        curFunction = new Function(mirFunction.getType(), mirFunction.getName().replace("@", ""), lirModule);
        HashMap<String, Integer> map = new HashMap<>();
        int total;
        /* params */
        if (isMain) total = 0;
        else total = mirFunction.getParams().size() * 4;
        // total = Math.max(0, mirFunction.getParams().size() - 4) * 4;

        /* local var */
        for (BasicBlock block : mirFunction.getBBlockList()) {
            for (INode inst : block.getInstList()) {
                if (inst instanceof AllocInst) {
                    int size = visit((AllocInst) inst);
                    if (size != 0) {
                        total += size;
                        map.put(((AllocInst) inst).getValue().getName(), size);
                    } else total += 4;
                } else if (inst instanceof FuncCallInst) {
                    ArrayList<Value> params = ((FuncCallInst) inst).getParams();
                    Value value;
                    Address address;
                    String name = ((FuncCallInst) inst).getFunction().getName().replace("@", "");
                    if (isLibFunction(name)) continue;
                    int pos = 0;
                    // regScheduler.clearParam();
                    for (int i = 0, len = params.size(); i < len; i++) {
                        value = params.get(i);
                       //  reg = regScheduler.allocParam(value);
                        pos -= 4;
                        address = new BaseAddress(Register.R29, new ImmNum(pos));
                        if (value instanceof ConstantInt) {
                            paramPos.put(name + "_param" + i, Pair.of(null, address));
                        } else {
                            paramPos.put(value.getName(), Pair.of(null, address));
                        }
                    }
                }
            }
        }

        /* temp registers */
        total += Registers.getTempRegisters().size() * 4;
        /* ra register */
        total += 4;

        stackSize = total;

        for (Value value : mirFunction.getParams()) {
            total -= 4;
            stackMem.put(value.getName(), new BaseAddress(Register.R29, new ImmNum(total)));
        }
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer size = entry.getValue();
            total -= size;
            stackMem.put(name, new BaseAddress(Register.R29, new ImmNum(total)));
        }
        saveSize = total;
        return curFunction;
    }

    private int visit(AllocInst inst) {
        Type type = inst.getValue().getType();
        int size = 4;
        if (type.isInt32Type()) {
            if (regScheduler.allocGlobal(inst.getValue()) != null) size = 0;
        } else while (type.isArrayType()) {
            size *= ((ArrayType) type).getSize();
            type = ((ArrayType) type).getBaseType();
        }
        return size;
    }

    public void secondPass() {
        for (INode inst : mirFunction.getParamFetchList()) {
            if (inst instanceof MemoryInst) {
                Value from = ((MemoryInst) inst).getFrom();
                Value to = ((MemoryInst) inst).getTo();
                stackMem.put(to.getName(), stackMem.get(from.getName()));
            }
        }
        for (BasicBlock block : mirFunction.getBBlockList()) {
            curBBlock = new BasicBlock(curFunction.getName() + block.getName(), curFunction);
            curFunction.addBBlock(curBBlock);
            if (mirFunction.getBBlockList().getBegin().equals(block) && stackSize > 0) {
                curBBlock.addMipsCode(new BinaryRegImmCode(
                        BinaryRegImmCode.toOp(BinaryOp.SUB), Register.R29, Register.R29, new ImmNum(stackSize)
                ));
            }
            genBBlock(block);
        }
    }

    private void genBBlock(BasicBlock block) {
        Pair<MIPSCode, MIPSCode> pair;
        for (INode inst : block.getInstList()) {
            if (inst instanceof BinaryInst) {
                pair = visit((BinaryInst) inst);
            } else if (inst instanceof MemoryInst) {
                pair = visit((MemoryInst) inst);
            } else if (inst instanceof FuncCallInst) {
                pair = visit((FuncCallInst) inst);
            } else if (inst instanceof GEPInst) {
                pair = visit((GEPInst) inst);
            } else {
                pair = Pair.of(new NopCode(), new NopCode());
            }
            curBBlock.addMipsCode(pair);
        }

//        if (mirFunction.getBBlockList().getEnd().equals(block) && stackSize > 0 && !isMain) {
//            curBBlock.addMipsCode(new BinaryRegImmCode(
//                    BinaryRegImmCode.toOp(BinaryOp.ADD), Register.R29, Register.R29, new ImmNum(stackSize)
//            ));
//        }

        Instruction inst = block.getTerminator();
        if (inst instanceof BranchInst) {
            pair = visit((BranchInst) inst);
        } else {
            pair = visit((RetInst) inst);
        }
        curBBlock.addMipsCode(pair);
    }

    private Pair<MIPSCode, MIPSCode> visit(BinaryInst inst) {
        Value lValue = inst.getLValue(), rValue = inst.getRValue();
        Register l;
        Object r;
        Register res = getRegForLeft(inst.getResValue());
        MIPSCode code;
        if (rValue instanceof ConstantInt) {
            r = ImmNum.toImmNum(rValue);
            l = getRegForRight(lValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, l, (ImmNum) r);
        } else {
            l = getRegForRight(lValue);
            r = getRegForRight(rValue);
            code = new BinaryRegRegCode(BinaryRegRegCode.toOp(inst.getOp()), res, l, (Register) r);
        }
        return Pair.of(code, storeWord(res, inst.getResValue()));
    }

    private Pair<MIPSCode, MIPSCode> visit(MemoryInst inst) {
//        if (inst.getTo().getName() != null && inst.getTo().getName().equals("%44"))
//            System.out.println("break");
        Register from = getRegForRight(inst.getFrom());
        Register to = getRegForLeft(inst.getTo());
        assert from != null;
        assert to != null;
        return Pair.of(new MoveCode(to, from), storeWord(to, inst.getTo()));
//        if (inst.getFlag() == 1) {
//            /* 1 for load */
//            Register from = findOrAllocReg(inst.getFrom());
//            Register to = findOrAllocReg(inst.getTo());
//            assert from != null;
//            if (to != null)
//                return Pair.of(new MoveCode(to, from), new NopCode());
//            else
//                return Pair.of(new LoadWordCode(from, findAddress(inst.getTo())), new NopCode());
//        } else {
//            /* 0 for store */
//            Register from;
//            Register to = findOrAllocReg(inst.getTo());
//            Address address = findAddress(inst.getTo());
//            Value value = inst.getFrom();
//            MIPSCode first, second;
//            if (value instanceof ConstantInt) {
//                ImmNum immNum = ImmNum.toImmNum(value);
//                if (to != null) {
//                    first = new LoadImmCode(to, immNum);
//                    second = new NopCode();
//                } else {
//                    from = allocRegForSymOrInt(value);
//                    first = new NopCode();
//                    second = new StoreWordCode(from, address);
//                }
//            } else {
//                from = findRegForSym(value);
//                if (to != null) {
//                    first = new MoveCode(to, from);
//                } else {
//                    first = new StoreWordCode(from, address);
//                }
//                second = new NopCode();
//            }
//            return Pair.of(first, second);
//        }
    }

    private Pair<MIPSCode, MIPSCode> visit(BranchInst inst) {
        if (inst.getCond() == null) {
            return Pair.of(new JumpCode(new Label(curFunction.getName() + inst.getThenBlock())), new NopCode());
        } else {
            BnezCode first = new BnezCode(findRegOrAddrForSym(inst.getCond()), new Label(curFunction.getName() +
                    inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(new Label(curFunction.getName() + inst.getElseBlock().getName()));
            return Pair.of(first, second);
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(RetInst inst) {
        if (isMain) {
            return Pair.of(new LoadImmCode(Register.R2, new ImmNum(10)), new SysCallCode());
        } else {
            if (inst.getType().isInt32Type()) {
                MIPSCode first;
                if (inst.getValue() instanceof ConstantInt)
                    first = new LoadImmCode(Register.R2, ImmNum.toImmNum(inst.getValue()));
                else {
                    Register rt = getRegForRight(inst.getValue());
                    first = new MoveCode(Register.R2, rt);
                }
                curBBlock.addMipsCode(first);
            }
            curBBlock.addMipsCode(new BinaryRegImmCode(
                    BinaryRegImmCode.toOp(BinaryOp.ADD), Register.R29, Register.R29, new ImmNum(stackSize)
            ));
            return Pair.of(new NopCode(), new JumpRegCode(Register.R31));
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(GEPInst inst) {
        int size = 0;
        Register reg = null;
        Value from, to, index;
        Address address, newAddress;
        Type innerType;
        Label label = new Label(inst.getFrom().getIdent());
        while (true) {
            from = inst.getFrom();
            to = inst.getTo();
            index = inst.getIndex();
            address = findAddress(from);
            innerType = ((PointerType) to.getType()).getInnerType();   // [i * i32] or i32
            if (innerType.isArrayType()) {
                if (index instanceof ConstantInt) {
                    size = ((ConstantInt) index).getValue() * ((ArrayType) innerType).getSize();
                } else {
                    reg = findRegOrAddrForSym(index);
                    curBBlock.addMipsCode(new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.MUL),
                            reg, reg, new ImmNum(((ArrayType) innerType).getSize())));
                }
            } else  {
                if (index instanceof ConstantInt) {
                    size =  ((ConstantInt) index).getValue();
                } else {
                    reg = findRegOrAddrForSym(index);
                }
            }

            if (address != null) {
                /* local array */
                if (index instanceof ConstantInt) {
                    newAddress = new BaseAddress(Register.R29, new ImmNum(size + ((BaseAddress) address).getImm().getValue()));
                } else {
                    curBBlock.addMipsCode(new BinaryRegRegCode(BinaryRegRegCode.toOp(BinaryOp.ADD), reg, ((BaseAddress) address).getReg(), reg));
                    newAddress = new BaseAddress(reg, ((BaseAddress) address).getImm());
                }
            } else {
                /* global array */
                newAddress = new LabelAddress(label, reg);
            }
            stackMem.put(to.getName(), newAddress);

            if (inst.getNext() instanceof GEPInst) {
                inst = (GEPInst) inst.getNext();
                inst.remove();
            } else break;
        }
        return Pair.of(new NopCode(), new NopCode());
    }

    private boolean libFuncHelper(FuncCallInst inst) {
        Function function = inst.getFunction();
        if (function.getName().equals("@getint")) {
            Register reg = findOrAllocReg(inst.getResValue());
            curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.GETINT));
            curBBlock.addMipsCode(new SysCallCode());
            curBBlock.addMipsCode(new MoveCode(reg, Register.R2));
            return true;
        } else if (function.getName().equals("@putch")) {
            curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.PUTCHAR));
            curBBlock.addMipsCode(new LoadImmCode(Register.R4, ImmNum.toImmNum(inst.getParams().get(0))));
            curBBlock.addMipsCode(new SysCallCode());
            return true;
        } else if (function.getName().equals("@putint")) {
            curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.PUTINT));
            Register reg = getRegForRight(inst.getParams().get(0));
            curBBlock.addMipsCode(new MoveCode(Register.R4, reg));
            curBBlock.addMipsCode(new SysCallCode());
            return true;
        } else return false;
    }
    private void save() {
        int pos = 0;
        for (Register register : Registers.getGlobalRegisters()) {
            pos += 4;
            if (regScheduler.usedGlobal(register)) curBBlock.addMipsCode(
                    new StoreWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos)))
            );
        }
        for (Register register : Registers.getTempRegisters()) {
            pos += 4;
            curBBlock.addMipsCode(new StoreWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos))));
        }
        assert saveSize == pos + 4;
    }
    private void restore() {
        int pos = 0;
        for (Register register : Registers.getGlobalRegisters()) {
            pos += 4;
            if (regScheduler.usedGlobal(register)) curBBlock.addMipsCode(
                    new LoadWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos)))
            );
        }
        for (Register register : Registers.getTempRegisters()) {
            pos += 4;
            curBBlock.addMipsCode(new LoadWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos))));
        }
        assert saveSize == pos + 4;
    }
    private Pair<MIPSCode, MIPSCode> visit(FuncCallInst inst) {
        if (libFuncHelper(inst)) return Pair.of(new NopCode(), new NopCode());

        // TODO: Save global registers and ra.
        if (!isMain)
            curBBlock.addMipsCode(new StoreWordCode(Register.R31, new BaseAddress(Register.R29, ImmNum.ZeroImm)));
        save();

        /* Parse imm. */
        ArrayList<Value> params = inst.getParams();
        Register reg;
        Value value;
        Address address;
        String name = inst.getFunction().getName().replace("@", "");
        for (int i = 0, len = params.size(); i < len; i++) {
            value = params.get(i);
            if (value instanceof ConstantInt) {
                address = paramPos.get(name + "_param" + i).getSecond();
                reg = allocRegForSymOrInt(value);
                curBBlock.addMipsCode(new StoreWordCode(reg, address));
            }
        }

        JumpLinkCode jal = new JumpLinkCode(
                new Label("Function_" + inst.getFunction().getName().replace("@", "")));
        curBBlock.addMipsCode(jal);

        // TODO: Restore global registers and ra.
        if(!isMain)
            curBBlock.addMipsCode(new LoadWordCode(Register.R31, new BaseAddress(Register.R29, ImmNum.ZeroImm)));
        restore();

        Pair<MIPSCode, MIPSCode> codes;
        if (inst.getFunction().getType().isInt32Type()) {
            reg = getRegForLeft(inst.getResValue());
            codes = Pair.of(new MoveCode(reg, Register.R2), storeWord(reg, inst.getResValue()));
            curBBlock.addMipsCode(codes);
        }
        return Pair.of(new NopCode(), new NopCode());
    }
}