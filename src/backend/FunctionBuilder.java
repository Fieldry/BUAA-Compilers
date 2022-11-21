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
import utils.Writer;
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
    private final LinkedHashMap<String, Pair<Register, Address>> paramPos = new LinkedHashMap<>();

    private int stackSize;
    private boolean isMain;
    private Function mirFunction;
    private Function curFunction;
    private BasicBlock curBBlock;

    public FunctionBuilder(Function mirFunction, boolean isMain) {
        this.mirFunction = mirFunction;
        this.isMain = isMain;
    }

    private Register findRegForSymOrInt(Value value) {
        Register reg = null;
        /* param */
        if (paramPos.containsKey(value.getName())) reg = paramPos.get(value.getName()).getFirst();
        /* global or temp register */
        if (reg == null) reg = regScheduler.find(value);
        if (reg == null) reg = allocRegForSymOrInt(value);
        if (value instanceof ConstantInt) {
            curBBlock.addMipsCode(new LoadImmCode(reg, ImmNum.toImmNum(value)));
        }
        return reg;
    }
    private Register allocRegForSymOrInt(Value value) {
        return regScheduler.allocTemp(value);
    }
    private Address findAddress(Value value) {
        String name = value.getName();
        if (paramPos.containsKey(name)) return paramPos.get(name).getSecond();
        else if (stackMem.containsKey(name)) return stackMem.get(name);
        else return globalMem.getOrDefault(name, null);
    }

    public Function firstPass(Module lirModule, Function function) {
        curFunction = new Function(function.getType(), function.getName().replace("@", ""), lirModule);
        HashMap<String, Integer> map = new HashMap<>();
        int total;
        /* params */
        if (isMain) total = 0;
        else total = Math.max(0, function.getParams().size() - 4) * 4;

        /* local var */
        for (BasicBlock block : function.getBBlockList()) {
            for (INode inst : block.getInstList()) {
                if (inst instanceof Instruction.AllocInst) {
                    int size = visit((Instruction.AllocInst) inst);
                    if (size != 0) {
                        total += size;
                        map.put(((AllocInst) inst).getValue().getName(), size);
                    }
                } else if (inst instanceof FuncCallInst) {
                    ArrayList<Value> params = ((FuncCallInst) inst).getParams();
                    Value value;
                    Register reg;
                    int pos = 0;
                    regScheduler.clearParam();
                    for (int i = 0, len = params.size(); i < len; i++) {
                        value = params.get(i);
                        reg = regScheduler.allocParam(value);
                        if (reg == null) {
                            pos -= 4;
                            Address address = new BaseAddress(Register.R29, new ImmNum(pos));
                            paramPos.put(value.getName(), Pair.of(null, address));
                        } else {
                            paramPos.put(value.getName(), Pair.of(reg, null));
                        }
                    }
                }
            }
        }

        /* temp registers */
        total += Registers.getTempRegisters().size() * 4;

        stackSize = total;
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
        int size = 1;
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
        for (BasicBlock block : mirFunction.getBBlockList()) {
            curBBlock = new BasicBlock(curFunction.getName() + block.getName(), curFunction);
            curFunction.addBBlock(curBBlock);
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
            Register from = findRegForSymOrInt(inst.getFrom());
            Register to = allocRegForSymOrInt(inst.getTo());
            if (from != null) {
                return Pair.of(new MoveCode(to, from), new NopCode());
            } else {
                Address address = findAddress(inst.getFrom());
                return Pair.of(new LoadWordCode(to, address), new NopCode());
            }
        } else {
            /* 0 for store */
            Register from;
            Register to = findRegForSymOrInt(inst.getTo());
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
                    first = new LoadImmCode(from, immNum);
                    second = new StoreWordCode(from, address);
                }
            } else {
                from = findRegForSymOrInt(value);
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
            BnezCode first = new BnezCode(allocRegForSymOrInt(inst.getCond()), new Label(curFunction.getName() +
                    inst.getThenBlock().getName()));
            JumpCode second = new JumpCode(new Label(curFunction.getName() + inst.getElseBlock().getName()));
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
                Register rt = findRegForSymOrInt(inst.getValue());
                first = new MoveCode(Register.R2, rt);
            }
            return Pair.of(first, jr);
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
                    reg = findRegForSymOrInt(index);
                    curBBlock.addMipsCode(new BinaryRegImmCode(BinaryRegImmCode.toOp(BinaryOp.MUL),
                            reg, reg, new ImmNum(((ArrayType) innerType).getSize())));
                }
            } else  {
                if (index instanceof ConstantInt) {
                    size =  ((ConstantInt) index).getValue();
                } else {
                    reg = findRegForSymOrInt(index);
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

    private Pair<MIPSCode, MIPSCode> visit(FuncCallInst inst) {
        // TODO: Save temp registers.
        // save(SAVE_TEMP);

        JumpLinkCode jal = new JumpLinkCode(
                new Label("Function_" + inst.getFunction().getName().replace("@", "")));
        if (inst.getFunction().getType().isInt32Type()) {
            return Pair.of(jal, new MoveCode(allocRegForSymOrInt(inst.getResValue()), Register.R2));
        } else return Pair.of(jal, new NopCode());

        // TODO: Restore temp registers.
    }
}
