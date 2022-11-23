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
    private final LinkedHashMap<String, Address> globalMem = new LinkedHashMap<>();
    /**
     * params
     * local var
     * temp registers
     */
    private final LinkedHashMap<String, Address> stackMem = new LinkedHashMap<>();
    /**
     * Parse parameters.
     */
    private final LinkedHashMap<String, Pair<Register, Address>> paramPos = new LinkedHashMap<>();

    private int stackSize;
    private final boolean isMain;
    private final Function mirFunction;
    private Function curFunction;
    private BasicBlock curBBlock;

    public FunctionBuilder(Function mirFunction, boolean isMain) {
        this.mirFunction = mirFunction;
        this.isMain = isMain;
    }

    private Register findRegForSym(Value value) {
        Register reg = null;
        /* param */
        if (paramPos.containsKey(value.getName())) reg = paramPos.get(value.getName()).getFirst();
        /* global or temp register */
        if (reg == null) return regScheduler.find(value);
        else return reg;
    }
    private Register allocRegForSymOrInt(Value value) {
        Register reg = regScheduler.allocTemp(value);
        if (value instanceof ConstantInt) {
            curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
        } else if (findAddress(value) != null) {
            curBBlock.addMipsCode(new LoadWordCode(reg, findAddress(value)));
        }
        return reg;
    }
    private Register findOrAllocReg(Value value) {
        Register reg;
        if ((reg = findRegForSym(value)) != null) return reg;
        else return allocRegForSymOrInt(value);
    }
    private Address findAddress(Value value) {
        String name = value.getName();
        if (paramPos.containsKey(name) && paramPos.get(name).getSecond() != null) return paramPos.get(name).getSecond();
        else if (stackMem.containsKey(name)) return stackMem.get(name);
        else return globalMem.getOrDefault(name, null);
    }

    public Function firstPass(Module lirModule) {
        curFunction = new Function(mirFunction.getType(), mirFunction.getName().replace("@", ""), lirModule);
        HashMap<String, Integer> map = new HashMap<>();
        int total;
        /* params */
        if (isMain) total = 0;
        else total = Math.max(0, mirFunction.getParams().size() - 4) * 4;

        /* local var */
        for (BasicBlock block : mirFunction.getBBlockList()) {
            for (INode inst : block.getInstList()) {
                if (inst instanceof AllocInst) {
                    int size = visit((AllocInst) inst);
                    if (size != 0) {
                        total += size;
                        map.put(((AllocInst) inst).getValue().getName(), size);
                    }
                } else if (inst instanceof FuncCallInst) {
                    ArrayList<Value> params = ((FuncCallInst) inst).getParams();
                    Register reg;
                    Value value;
                    String name = ((FuncCallInst) inst).getFunction().getName().replace("@", "");
                    int pos = 0;
                    regScheduler.clearParam();
                    for (int i = 0, len = params.size(); i < len; i++) {
                        value = params.get(i);
                        reg = regScheduler.allocParam(value);
                        if (reg == null) {
                            pos -= 4;
                            Address address = new BaseAddress(Register.R29, new ImmNum(pos));
                            paramPos.put(name + "_param" + i, Pair.of(null, address));
                        } else {
                            paramPos.put(name + "_param" + i, Pair.of(reg, null));
                        }
                    }
                }
            }
        }

        /* temp registers */
        // total += Registers.getTempRegisters().size() * 4;
        /* ra register */
        total += 4;

        stackSize = total;

        for (int i = 4, len = mirFunction.getParams().size(); i < len; i++) {
            total -= 4;
            stackMem.put(curFunction.getName() + "_param" + i, new BaseAddress(Register.R29, new ImmNum(total)));
        }
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer size = entry.getValue();
            total -= size;
            stackMem.put(name, new BaseAddress(Register.R29, new ImmNum(total)));
        }
        return curFunction;
    }

    private int visit(AllocInst inst) {
        Type type = inst.getValue().getType();
        int size = 4;
        if (type.isInt32Type()) {
            Register reg = regScheduler.allocGlobal(inst.getValue());
            if (reg != null) {
                return 0;
            }
        } else while (type.isArrayType()) {
            size *= ((ArrayType) type).getSize();
            type = ((ArrayType) type).getBaseType();
        }
        return size;
    }

    public void secondPass() {
        regScheduler.clearParam();
        for (INode inst : mirFunction.getParamFetchList()) {
            if (inst instanceof MemoryInst) {
                String from = ((MemoryInst) inst).getFrom().getName().replace("%", "");
                Value to = ((MemoryInst) inst).getTo();
                Register reg = regScheduler.allocParam(to);
                if (reg == null) stackMem.put(to.getName(), stackMem.get(from));
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

        if (mirFunction.getBBlockList().getEnd().equals(block) && stackSize > 0 && !isMain) {
            curBBlock.addMipsCode(new BinaryRegImmCode(
                    BinaryRegImmCode.toOp(BinaryOp.ADD), Register.R29, Register.R29, new ImmNum(stackSize)
            ));
        }

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
        Object l, r;
        Register res = allocRegForSymOrInt(inst.getResValue());
        MIPSCode code;
        if (lValue instanceof ConstantInt) {
            l = ImmNum.toImmNum(lValue);
            r = findOrAllocReg(rValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) r, (ImmNum) l);
        } else if (rValue instanceof ConstantInt) {
            r = ImmNum.toImmNum(rValue);
            l = findOrAllocReg(lValue);
            code = new BinaryRegImmCode(BinaryRegImmCode.toOp(inst.getOp()), res, (Register) l, (ImmNum) r);
        } else {
            l = findOrAllocReg(lValue);
            r = findOrAllocReg(rValue);
            code = new BinaryRegRegCode(BinaryRegRegCode.toOp(inst.getOp()), res, (Register) l, (Register) r);
        }
        return Pair.of(code, new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(MemoryInst inst) {
        if (inst.getFlag() == 1) {
            /* 1 for load */
            Register from = findRegForSym(inst.getFrom());
            Register to = findRegForSym(inst.getTo());
            assert to != null;
            if (from != null) {
                return Pair.of(new MoveCode(to, from), new NopCode());
            } else {
                Address address = findAddress(inst.getFrom());
                return Pair.of(new LoadWordCode(to, address), new NopCode());
            }
        } else {
            /* 0 for store */
            Register from;
            Register to = findRegForSym(inst.getTo());
            Address address = findAddress(inst.getTo());
            Value value = inst.getFrom();
            MIPSCode first, second;
            if (value instanceof ConstantInt) {
                ImmNum immNum = ImmNum.toImmNum(value);
                if (to != null) {
                    first = new LoadImmCode(to, immNum);
                    second = new NopCode();
                } else {
                    from = allocRegForSymOrInt(value);
                    first = new NopCode();
                    second = new StoreWordCode(from, address);
                }
            } else {
                from = findRegForSym(value);
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
            return Pair.of(new JumpCode(new Label(curFunction.getName() + inst.getThenBlock())), new NopCode());
        } else {
            BnezCode first = new BnezCode(findRegForSym(inst.getCond()), new Label(curFunction.getName() +
                    inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(new Label(curFunction.getName() + inst.getElseBlock().getName()));
            return Pair.of(first, second);
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(RetInst inst) {
        if (isMain) {
            return Pair.of(new LoadImmCode(Register.R2, new ImmNum(10)), new SysCallCode());
        } else if (inst.getType().isVoidType()) {
            return Pair.of(new NopCode(), new JumpRegCode(Register.R31));
        } else {
            MIPSCode first;
            if (inst.getValue() instanceof ConstantInt)
                first = new LoadImmCode(Register.R2, ImmNum.toImmNum(inst.getValue()));
            else {
                Register rt = findRegForSym(inst.getValue());
                first = new MoveCode(Register.R2, rt);
            }
            return Pair.of(first, new JumpRegCode(Register.R31));
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
                    reg = findRegForSym(index);
                    curBBlock.addMipsCode(new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.MUL),
                            reg, reg, new ImmNum(((ArrayType) innerType).getSize())));
                }
            } else  {
                if (index instanceof ConstantInt) {
                    size =  ((ConstantInt) index).getValue();
                } else {
                    reg = findRegForSym(index);
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

    private Pair<MIPSCode, MIPSCode> libFuncHelper(LibFunction function) {

        return Pair.of(new NopCode(), new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(FuncCallInst inst) {
        // TODO: Save temp registers and ra.
        // save(SAVE_TEMP);
        if (!isMain)
            curBBlock.addMipsCode(new StoreWordCode(Register.R31, new BaseAddress(Register.R29, ImmNum.ZeroImm)));

        if (inst.getFunction() instanceof LibFunction) {
            return libFuncHelper((LibFunction) inst.getFunction());
        }

        /* Parse imm. */
        ArrayList<Value> params = inst.getParams();
        Register reg;
        Value value;
        Address address;
        Pair<Register, Address> pair;
        String name = inst.getFunction().getName().replace("@", "");
        for (int i = 0, len = params.size(); i < len; i++) {
            value = params.get(i);
            if (value instanceof ConstantInt) {
                pair = paramPos.get(name + "_param" + i);
                if ((reg = pair.getFirst()) != null) {
                    curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
                } else {
                    address = pair.getSecond();
                    reg = findRegForSym(value);
                    curBBlock.addMipsCode(new StoreWordCode(reg, address));
                }
            }
        }

        Pair<MIPSCode, MIPSCode> codes;
        JumpLinkCode jal = new JumpLinkCode(
                new Label("Function_" + inst.getFunction().getName().replace("@", "")));
        if (inst.getFunction().getType().isInt32Type()) {
            codes = Pair.of(jal, new MoveCode(findRegForSym(inst.getResValue()), Register.R2));
        } else codes = Pair.of(jal, new NopCode());

        // TODO: Restore temp registers and ra.
        if(!isMain)
            curBBlock.addMipsCode(new LoadWordCode(Register.R31, new BaseAddress(Register.R29, ImmNum.ZeroImm)));

        return codes;
    }
}
