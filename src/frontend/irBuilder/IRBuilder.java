package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.token.Tokens.Token;
import frontend.symbolTable.SymbolValueTable;

import java.util.ArrayList;

public class IRBuilder {
    /** The count of register used.
     */
    private int labelCount = 0;
    private BasicBlock block;
    private SymbolValueTable curTable = new SymbolValueTable(null);

    public IRBuilder() {}

    private String getRegName() { return "%" + (++labelCount); }

    private String getBlockName() { return "" + (++labelCount); }

    public void createSymbolTable() { curTable = new SymbolValueTable(curTable); }

    public void recallSymbolTable() { curTable = curTable.getParent(); }

    public GlobalVariable createGlobalVar(boolean isConst, Type type, String name, Initial initial) {
        name = "@" + name;
        curTable.addSymbol(name, new Value(new PointerType(type), name));
        return new GlobalVariable(isConst, type, name, initial);
    }

    public Function createFunction(boolean returnInt, String name, Module parent) {
        name = "@" + name;
        labelCount = -1;
        Type type = returnInt ? IntType.INT32_TYPE : VoidType.VOID_TYPE;
        Function function = new Function(type, name, parent);
        curTable.addSymbol(name, function);
        return function;
    }

    public Value createFParam(String name, int dimension) {
        Value value = new Value(IntType.INT32_TYPE, getRegName());
        curTable.addSymbol(name, value);
        return value;
    }

    public BasicBlock createBlock(Function parent) {
        block = new BasicBlock(getBlockName(), parent);
        return block;
    }

    public BranchInst createBranchInst(Value cond) {
        return new BranchInst(block, cond);
    }

    public BranchInst createBranchInst(BasicBlock target) { return new BranchInst(block, target); }

    public AllocInst createAllocInst(Type type, String name) {
        String regName = getRegName();
        AllocInst inst;

        curTable.addSymbol('*' + name, new Value(new PointerType(type), regName));

        inst = new AllocInst(block, new Value(type, regName));
        block.addInst(inst);
        return inst;
    }

    public MemoryInst createStrInst(String name, Value from) {
        Value to = curTable.findSymbolInAll('*' + name);
        if (to == null) to = curTable.findSymbolInAll('@' + name);
        MemoryInst inst = new MemoryInst(block, 0, from, to);
        block.addInst(inst);
        return inst;
    }

    public MemoryInst createLdInst(String name) {
        MemoryInst inst;
        Value from = curTable.findSymbolInAll('*' + name);
        if (from == null) from = curTable.findSymbolInAll('@' + name);
        if (from instanceof ConstantInt) {
            inst = new MemoryInst(null, 0, null, from);
        } else {
            Value to = new Value(IntType.INT32_TYPE, getRegName());
            curTable.addSymbol(name, to);
            inst = new MemoryInst(block, 1, from, to);
            block.addInst(inst);
        }
        return inst;
    }

    public RetInst createRetInst(Value value) {
        RetInst inst = value == null ?
                new RetInst(VoidType.VOID_TYPE, null) : new RetInst(IntType.INT32_TYPE, value);
        block.setTerminator(inst);
        return inst;
    }

    public FuncCallInst createFuncCallInst(String name, ArrayList<Value> params) {
        Function function = (Function) curTable.findSymbolInAll('@' + name);

        User user;
        if (function.getType().equals(IntType.INT32_TYPE)) {
            user = new User(IntType.INT32_TYPE, getRegName());
            user.addOperand(function);
            function.addUse(user);
        } else user = null;

        FuncCallInst inst = new FuncCallInst(block, function, params, user);
        block.addInst(inst);
        return inst;
    }

    public BinaryInst createBinaryInst(Token token, Value lValue, Value rValue) {
        String regName = getRegName();
        User user;
        BinaryInst res;
        switch (token.getTokenKind()) {
            case PLUS: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.ADD, lValue, rValue, user);
                break;
            }
            case MINUS: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
                break;
            }
            case STAR: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.MUL, lValue, rValue, user);
                break;
            }
            case DIV: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SDIV, lValue, rValue, user);
                break;
            }
            case MOD: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.REM, lValue, rValue, user);
                break;
            }
            case GEQ: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SGE, lValue, rValue, user);
                break;
            }
            case GRE: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SGT, lValue, rValue, user);
                break;
            }
            case LEQ: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SLE, lValue, rValue, user);
                break;
            }
            case LSS: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.SLT, lValue, rValue, user);
                break;
            }
            case EQL: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.EQ, lValue, rValue, user);
                break;
            }
            case NEQ: {
                user = new User(IntType.INT1_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.NE, lValue, rValue, user);
                break;
            }
            default: {
                return null;
            }
        }
        lValue.addUse(user);
        rValue.addUse(user);

        block.addInst(res);
        return res;
    }

    public BinaryInst createSub(Value lValue, Value rValue) {
        String regName = getRegName();
        User user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
    }

    public Value createConst(int value) {
        return new ConstantInt(value);
    }
}
