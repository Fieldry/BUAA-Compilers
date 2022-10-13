package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.token.Tokens.Token;
import frontend.symbolTable.SymbolValueTable;
import frontend.tree.SysYTree;

import java.util.List;

public class IRBuilder {
    /** The count of register used.
     */
    private int regCount = 0;
    private BasicBlock block;
    private SymbolValueTable curTable = new SymbolValueTable(null);

    public IRBuilder() {}

    private String getRegName() { return "%" + (++regCount); }

    public Value getLValueByName(String name) { return curTable.findSymbol(name); }

    public void createSymbolTable() { curTable = new SymbolValueTable(curTable); }

    public void recallSymbolTable() { curTable = curTable.getParent(); }

    public GlobalVariable createGlobalVar(boolean isConst, String name, Value value) {
        curTable.addSymbol('@' + name, new Value(PointerType.i32, "@" + name));
        return new GlobalVariable(isConst, name, value);
    }

    public Function createFunction(boolean returnInt, String name, Module parent) {
        name = '@' + name;
        regCount = -1;
        Type type = returnInt ? IntType.i32 : VoidType.vd;
        Function function = new Function(type, name, parent);
        curTable.addSymbol(name, function);
        return function;
    }

    public Value createFParam(String name, int dimension) {
        Value value = new Value(IntType.i32, getRegName());
        curTable.addSymbol(name, value);
        return value;
    }

    public BasicBlock createBlock(Function parent) {
        block = new BasicBlock(getRegName(), parent);
        return block;
    }

    public BranchInst createBranchInst(Value cond) {
        return new BranchInst(block, cond);
    }

    public BranchInst createBranchInst(BasicBlock target) { return new BranchInst(block, target); }

    public AllocInst createAllocInst(String name) {
        String regName = getRegName();
        Value value = new Value(PointerType.i32, regName);
        curTable.addSymbol('*' + name, value);
        return new AllocInst(block, value);
    }

    public MemoryInst createStrInst(String name, Value from) {
        Value to = curTable.findSymbolInAll('*' + name);
        return new MemoryInst(block, 0, from, to);
    }

    public MemoryInst createLdInst(String name) {
        String regName = getRegName();
        Value from = curTable.findSymbolInAll('*' + name);
        if (from == null) from = curTable.findSymbolInAll('@' + name);
        Value to = new Value(IntType.i32, regName);
        curTable.addSymbol(name, to);
        return new MemoryInst(block, 1, from, to);
    }

    public RetInst createRetInst(Value value) {
        return new RetInst(IntType.i32, value);
    }

    public FuncCallInst createFuncCallInst(String name) {
        Function function = (Function) curTable.findSymbolInAll('@' + name);

        User user;
        if (function.getType().equals(IntType.i32)) {
            user = new User(IntType.i32, getRegName());
            user.addOperand(function);
            function.addUse(user);
        } else user = null;
        return new FuncCallInst(block, function, user);
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
            case EQL: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.EQ, lValue, rValue, user);
                break;
            }
            case NEQ: {
                user = new User(IntType.i1, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.NE, lValue, rValue, user);
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
