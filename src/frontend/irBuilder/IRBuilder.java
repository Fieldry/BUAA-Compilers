package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.tree.SysYTree;

import java.util.HashMap;
import java.util.Map;

public class IRBuilder {
    /** The count of register used.
     */
    private int regCount = 0;
    private BasicBlock block;
    private final Map<String, Value> symbolTable = new HashMap<>();

    public IRBuilder() {}

    private String getRegName() { return "%" + (++regCount); }

    public AllocInst createAllocInst(String name) {
        String regName = getRegName();
        Value value = new Value(IntType.i32, regName);
        symbolTable.put(name, value);
        return new AllocInst(block, value);
    }

    public MemoryInst createStrInst(String name, Value from) {
        Value to = symbolTable.get(name);
        return new MemoryInst(block, 0, from, to);
    }

    public MemoryInst createLdInst(String name) {
        String regName = getRegName();
        Value from = symbolTable.get(name);
        Value to = new Value(IntType.i32, regName);
        symbolTable.put(name, to);
        return new MemoryInst(block, 1, from, to);
    }

    public RetInst createRetInst(Value value) {
        return new RetInst(IntType.i32, value);
    }

    public BinaryInst createAdd(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.ADD, lValue, rValue, user);
    }

    public BinaryInst createSub(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
    }

    public BinaryInst createMul(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.MUL, lValue, rValue, user);
    }

    public BinaryInst createDiv(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SDIV, lValue, rValue, user);
    }

    public BinaryInst createMod(Value lValue, Value rValue) {

        return null;
    }

    public Value createConst(int value) {
        return new ConstantInt(value);
    }
}
