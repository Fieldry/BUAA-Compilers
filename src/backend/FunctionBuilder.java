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
    private final RegScheduler regScheduler;
    private final LinkedHashMap<String, Address> globalMem;
    /**
     * -------------- 0
     * params
     * local var
     * -------------- temp pointer (< 0)
     * temp register
     * save temp registers and ra(context)
     * -------------- stack pointer
     */
    private final LinkedHashMap<String, Address> stackMem = new LinkedHashMap<>();
    /**
     * Parse parameters.
     */
    private final LinkedHashMap<String, Pair<Register, Address>> paramPos;
    /** All used stack memory in this function. */
    private int stackSize;
    /** Used to save temp register when overFlow, every time sub 4. */
    private int tempStartPointer;
    private int tempPointer;
    /** Used stack memory to save context, the same to every function. */
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
        regScheduler = new RegScheduler(this);
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
    /** Find or alloc temp Register for Right value.*/
    private Register getRegForRight(Value value) {
        Register reg = findRegOrAddrForSym(value);
        if (reg != null) return reg;
        else return allocRegForSymOrInt(value);
    }
    /** Find or alloc Register for Left value.*/
    private Register getRegForLeft(Value value) {
        Register reg = regScheduler.find(value);
        if (reg == null) return regScheduler.allocTemp(value);
        else return reg;
    }
    /** Find Address for value in stack memory or global memory.*/
    private Address findAddress(Value value) {
        String name = value.getName();
        if (stackMem.containsKey(name)) return stackMem.get(name);
        else return globalMem.getOrDefault(name, null);
    }
    /** Store value into the memory. */
    private MIPSCode storeWord(Register res, Value value) {
        Address address = null;
        if (paramPos.containsKey(value.getName())) address = paramPos.get(value.getName()).getSecond();
        if (address == null) address = findAddress(value);
        if (address != null) return genSWCode(res, address);
        else return new NopCode();
    }
    /** Store a temp register into stack. */
    public void saveOverFlowTemp(Register register, String value) {
        tempPointer -= 4;
        if (stackSize + tempPointer < saveSize) {
            System.out.println("G");
        };
        BaseAddress address = new BaseAddress(Register.R30, new ImmNum(tempPointer));
        stackMem.put(value, address);
        curBBlock.addMipsCode(new StoreWordCode(register, address));
    }
    public void freeOverFlowTempInMem() { tempPointer = tempStartPointer; }

    // ----------------------------------------------------------------------

    private StoreWordCode genSWCode(Register reg, Address address) {
        StoreWordCode code = new StoreWordCode(reg, address);
        regScheduler.freeTemp(reg);
        return code;
    }
    /** generate a move code and free from register. */
    private MoveCode genMoveCode(Register rt, Register rs) {
        MoveCode code = new MoveCode(rt, rs);
        regScheduler.freeTemp(rs);
        return code;
    }

    // ----------------------------------------------------------------------

    public Function firstPass(Module lirModule) {
        curFunction = new Function(mirFunction.getType(), mirFunction.getName().replace("@", ""), lirModule);
        HashMap<String, Integer> map = new HashMap<>();
        /* params */
        if (isMain) stackSize = 0;
        else stackSize = mirFunction.getParams().size() * 4;
        // total = Math.max(0, mirFunction.getParams().size() - 4) * 4;

        /* local var */
        for (BasicBlock block : mirFunction.getBBlockList()) {
            for (INode inst : block.getInstList()) {
                if (inst instanceof AllocInst) {
                    int size = visit((AllocInst) inst);
                    if (size != 0) {
                        stackSize += size;
                        map.put(((AllocInst) inst).getValue().getName(), size);
                    } else stackSize += 4;
                } else if (inst instanceof BinaryRegImmCode || inst instanceof BinaryRegRegCode) {
                    stackSize += 3 * 4;
                } else if (inst instanceof GEPInst) {
                    stackSize += 2 * 4;
                } else if (inst instanceof FuncCallInst) {
                    ArrayList<Value> params = ((FuncCallInst) inst).getParams();
                    Value value;
                    Address address;
                    String name = ((FuncCallInst) inst).getFunction().getName().replace("@", "");
                    if (isLibFunction(name)) continue;
                    if (((FuncCallInst) inst).getResValue() != null) stackSize += 4;
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

        /* Save temp registers. */
        saveSize += Registers.getTempRegisters().size() * 4;
        /* Save ra register. */
        saveSize += 4;
        stackSize += saveSize;

        /* Save parameters in stack.*/
        for (Value value : mirFunction.getParams()) {
            tempPointer -= 4;
            stackMem.put(value.getName(), new BaseAddress(Register.R30, new ImmNum(tempPointer)));
        }
        /* Save local parameters. */
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String name = entry.getKey();
            Integer size = entry.getValue();
            tempPointer -= size;
            stackMem.put(name, new BaseAddress(Register.R30, new ImmNum(tempPointer)));
        }
        tempStartPointer = tempPointer;

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
            curBBlock = new BasicBlock(curFunction.getName() + "_" + block.getName(), curFunction);
            curFunction.addBBlock(curBBlock);
            regScheduler.freeAllTemp();
            if (mirFunction.getBBlockList().getBegin().equals(block) && stackSize > 0) {
                curBBlock.addMipsCode(new MoveCode(Register.R30, Register.R29));
                curBBlock.addMipsCode(BinaryRegImmCode.subCode(Register.R29, Register.R29, new ImmNum(stackSize)));
            }
            genBBlock(block);
        }
    }

    private void genBBlock(BasicBlock block) {
        Pair<MIPSCode, MIPSCode> pair;
        for (INode inst : block.getInstList()) {
            if (inst instanceof BinaryInst) {
                pair = visit((BinaryInst) inst);
            } else if (inst instanceof ZExtInst) {
                pair = visit((ZExtInst) inst);
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
        regScheduler.freeTemp(l);
        if(r instanceof Register) regScheduler.freeTemp((Register) r);
        return Pair.of(code, storeWord(res, inst.getResValue()));
    }

    private Pair<MIPSCode, MIPSCode> visit(ZExtInst inst) {
        Register from = getRegForRight(inst.getFrom());
        Register to = getRegForLeft(inst.getTo());
        MIPSCode code = genMoveCode(to, from);
        return Pair.of(code, new NopCode());
    }

    private Pair<MIPSCode, MIPSCode> visit(MemoryInst inst) {
        Register from = getRegForRight(inst.getFrom());
        Register to = getRegForLeft(inst.getTo());
        assert from != null;
        assert to != null;
        // TODO: Free from.
        MIPSCode code = genMoveCode(to, from);
        return Pair.of(code, storeWord(to, inst.getTo()));
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
            return Pair.of(new JumpCode(new Label(curFunction.getName() + "_" + inst.getThenBlock())), new NopCode());
        } else {
            BnezCode first = new BnezCode(findRegOrAddrForSym(inst.getCond()), new Label(curFunction.getName() +
                    "_" + inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(new Label(curFunction.getName() + "_" + inst.getElseBlock().getName()));
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
                    first = genMoveCode(Register.R2, rt);
                }
                curBBlock.addMipsCode(first);
            }
            curBBlock.addMipsCode(BinaryRegImmCode.addCode(Register.R29, Register.R29, new ImmNum(stackSize)));
            return Pair.of(new NopCode(), new JumpRegCode(Register.R31));
        }
    }

    private Pair<MIPSCode, MIPSCode> visit(GEPInst inst) {
        Register reg;
        Value from, to, index;
        Address address, newAddress;
        Type innerType;
        from = inst.getFrom();
        to = inst.getTo();
        index = inst.getIndex();
        address = findAddress(from);
        innerType = ((PointerType) to.getType()).getInnerType();   // [i * i32] or i32
//            if (index instanceof ConstantInt) {
//                if (innerType.isArrayType()) {
//                    size = ((ConstantInt) index).getValue() * ((ArrayType) innerType).getSize() * 4;
//                } else {
//                    size =  ((ConstantInt) index).getValue() * 4;
//                }
//            } else {
//                reg = getRegForRight(index);
//                if (innerType.isArrayType()) {
//                    curBBlock.addMipsCode(new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.MUL),
//                            reg, reg, new ImmNum(((ArrayType) innerType).getSize() * 4)));
//                }
//            }
        reg = getRegForRight(index);
        if (innerType.isArrayType()) {
            curBBlock.addMipsCode(BinaryRegImmCode.mulCode(
                    reg, reg, new ImmNum(((ArrayType) innerType).getSize() * 4L)));
        } else {
            curBBlock.addMipsCode(BinaryRegImmCode.sllCode(reg, reg, new ImmNum(2)));
        }
        if (address instanceof BaseAddress) {
            /* local array */
//                if (index instanceof ConstantInt) {
//                    newAddress = new BaseAddress(Register.R29, new ImmNum(size + ((BaseAddress) address).getImm().getValue()));
//                } else {
//                    curBBlock.addMipsCode(new BinaryRegRegCode(BinaryRegRegCode.toOp(BinaryOp.ADD), reg, ((BaseAddress) address).getReg(), reg));
//                    newAddress = new BaseAddress(reg, ((BaseAddress) address).getImm());
//                }
            curBBlock.addMipsCode(BinaryRegRegCode.addCode(reg, ((BaseAddress) address).getReg(), reg));
            newAddress = new BaseAddress(reg, ((BaseAddress) address).getImm());
        } else if (address instanceof LabelAddress){
            /* global array */
            curBBlock.addMipsCode(BinaryRegRegCode.addCode(reg, ((LabelAddress) address).getReg(), reg));
            newAddress = new LabelAddress(((LabelAddress) address).getLabel(), reg);
        } else {
            assert address == null;
            Register temp = getRegForRight(from);
            curBBlock.addMipsCode(BinaryRegRegCode.addCode(temp, temp, reg));
            newAddress = new BaseAddress(temp, ImmNum.ZeroImm);
        }
        stackMem.put(to.getName(), newAddress);
        if (paramPos.containsKey(to.getName())) storeAddress(newAddress, to);

        return Pair.of(new NopCode(), new NopCode());
    }

    private void storeAddress(Address addr, Value value) {
        Address address = paramPos.get(value.getName()).getSecond();
        if (addr instanceof BaseAddress) {
            Register reg = ((BaseAddress) addr).getReg();
            ImmNum imm = ((BaseAddress) addr).getImm();
            curBBlock.addMipsCode(BinaryRegImmCode.addCode(reg, reg, imm));
            curBBlock.addMipsCode(genSWCode(reg, address));
        } else {
            Register temp = regScheduler.allocTemp();
            curBBlock.addMipsCode(new LoadAddressCode(temp, addr));
            curBBlock.addMipsCode(genSWCode(temp, address));
        }
    }

    private boolean libFuncHelper(FuncCallInst inst) {
        Function function = inst.getFunction();
        switch (function.getName()) {
            case "@getint": {
                Register reg = getRegForRight(inst.getResValue());
                curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.GETINT));
                curBBlock.addMipsCode(new SysCallCode());
                curBBlock.addMipsCode(new MoveCode(reg, Register.R2));
                return true;
            }
            case "@putch": {
                curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.PUTCHAR));
                curBBlock.addMipsCode(new LoadImmCode(Register.R4, ImmNum.toImmNum(inst.getParams().get(0))));
                curBBlock.addMipsCode(new SysCallCode());
                return true;
            }
            case "@putint": {
                curBBlock.addMipsCode(new LoadImmCode(Register.R2, ImmNum.PUTINT));
                Register reg = getRegForRight(inst.getParams().get(0));
                curBBlock.addMipsCode(genMoveCode(Register.R4, reg));
                curBBlock.addMipsCode(new SysCallCode());
                return true;
            }
            default:
                return false;
        }
    }
    private void save() {
        int pos = 0;
        for (Register register : Registers.getGlobalRegisters()) {
            if (regScheduler.usedGlobal(register)) {
                pos += 4;
                curBBlock.addMipsCode(new StoreWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos))));
            }
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
            if (regScheduler.usedGlobal(register)) {
                pos += 4;
                curBBlock.addMipsCode(new LoadWordCode(register, new BaseAddress(Register.R29, new ImmNum(pos))));
            }
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
                curBBlock.addMipsCode(genSWCode(reg, address));
            } else if (findAddress(value) == null
                && (reg = regScheduler.find(value)) != null) {
                address = paramPos.get(value.getName()).getSecond();
                curBBlock.addMipsCode(genSWCode(reg, address));
            }
        }

        curBBlock.addMipsCode(new JumpLinkCode(
                new Label("Function_" + inst.getFunction().getName().replace("@", ""))));
        curBBlock.addMipsCode(BinaryRegImmCode.addCode(Register.R30, Register.R30, new ImmNum(stackSize)));

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
