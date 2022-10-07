package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.token.Tokens.Token;

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

    public Value getLValueByName(String name) { return symbolTable.get(name); }

    public BasicBlock createBlock(Function parent) {
        block = new BasicBlock(getRegName(), parent);
        return block;
    }

    public BranchInst createBranchInst(Value cond) {
        return new BranchInst(block, cond);
    }

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

    public BinaryInst createBinaryInst(Token token, Value lValue, Value rValue) {
        String regName = getRegName();
        User user;
        BinaryInst res;
        switch (token.getTokenKind()) {
            case PLUS: {
                user = new User(IntType.i32, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.ADD, lValue, rValue, user);
                break;
            }
            case MINUS: {
                user = new User(IntType.i32, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
                break;
            }
            case STAR: {
                user = new User(IntType.i32, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.MUL, lValue, rValue, user);
                break;
            }
            case DIV: {
                user = new User(IntType.i32, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SDIV, lValue, rValue, user);
                break;
            }
            case MOD: {
                user = new User(IntType.i32, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.REM, lValue, rValue, user);
                break;
            }
            case GEQ: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SGE, lValue, rValue, user);
                break;
            }
            case GRE: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SGT, lValue, rValue, user);
                break;
            }
            case LEQ: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SLE, lValue, rValue, user);
                break;
            }
            case LSS: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SLT, lValue, rValue, user);
                break;
            }
            default: {
                return null;
            }
        }
        lValue.addUse(user);
        rValue.addUse(user);
        return res;
    }

    public BinaryInst createSub(Value lValue, Value rValue) {
        String regName = getRegName();
        User user = new User(IntType.i32, regName, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
    }

    public Value createConst(int value) {
        return new ConstantInt(value);
    }
}
