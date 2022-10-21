package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;
import frontend.irBuilder.Initial.*;
import frontend.token.Tokens;
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

    public enum IdentKind {
        GLOBAL("@"),
        LOCAL("*"),
        FUNC_PARAM("$");
        private final String prefix;

        IdentKind(String prefix) { this.prefix = prefix; }

        @Override
        public String toString() {
            return prefix;
        }
    }

    public IdentKind getKindOfValue(String name) {
        if (curTable.findSymbolInAll("*" + name) != null) return IdentKind.LOCAL;
        else if (curTable.findSymbolInAll("$" + name) != null) return IdentKind.FUNC_PARAM;
        else if (curTable.findSymbolInAll("@" + name) != null) return IdentKind.GLOBAL;
        else return null;
    }

    public Value getValueFromTable(String name) {
        Value from = curTable.findSymbolInAll("*" + name);
        if (from == null) from = curTable.findSymbolInAll("$" + name);
        if (from == null) from = curTable.findSymbolInAll("@" + name);
        return from;
    }

    public void createSymbolTable() { curTable = new SymbolValueTable(curTable); }

    public void recallSymbolTable() { curTable = curTable.getParent(); }

    public GlobalVariable createGlobalVar(boolean isConst, Type type, String name, Initial initial) {
        name = "@" + name;
        Value value = isConst && type.isInt32Type() ? ((ValueInitial) initial).getValue()
                : new Value(new PointerType(type), name);
        curTable.addSymbol(name, value);
        return new GlobalVariable(isConst, type, name, initial);
    }

    public void addFunctionToTable(LibFunction function) {
        curTable.addSymbol(function.getName(), function);
    }

    public Function createFunction(boolean returnInt, String name, Module parent) {
        name = "@" + name;
        labelCount = -1;
        Type type = returnInt ? IntType.INT32_TYPE : VoidType.VOID_TYPE;
        Function function = new Function(type, name, parent);
        curTable.addSymbol(name, function);
        return function;
    }

    public Value createFParam(String name, int dimension, Value second) {
        Value value;
        if (dimension == 0) {
            value = new Value(IntType.INT32_TYPE, getRegName());
        } else if (dimension == 1) {
            value = new Value(new PointerType(IntType.INT32_TYPE), getRegName());
        } else {
            int size = ((ConstantInt) second).getValue();
            value = new Value(new PointerType(new ArrayType(size, IntType.INT32_TYPE)), getRegName());
        }
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

    public AllocInst createAllocInst(Type type, String prefix, String name) {
        String regName = getRegName();
        AllocInst inst;

        curTable.addSymbol(prefix + name, new Value(new PointerType(type), regName));

        inst = new AllocInst(block, new Value(type, regName));
        block.addInst(inst);
        return inst;
    }

    public MemoryInst createStrInst(String name, Value from) {
        return createStrInst(from, getValueFromTable(name));
    }

    public MemoryInst createStrInst(Value from, Value to) {
        MemoryInst inst = new MemoryInst(block, 0, from, to);
        block.addInst(inst);
        return inst;
    }

    public MemoryInst createLdInst(Value from) {
        Type type = ((PointerType) from.getType()).getInnerType();
        Value to = new Value(type, getRegName());
        MemoryInst inst = new MemoryInst(block, 1, from, to);
        block.addInst(inst);
        return inst;
    }

    public GEPInst createGEPInst(Value from, Type toValueType, Value index, boolean flag) {
        Value to = new Value(toValueType, getRegName());
        GEPInst inst = new GEPInst(block, from, to, index, flag);
        if(block != null) block.addInst(inst);

        return inst;
    }

    public RetInst createRetInst(Value value) {
        RetInst inst = value == null ?
                new RetInst(VoidType.VOID_TYPE, null) : new RetInst(IntType.INT32_TYPE, value);
        block.setTerminator(inst);
        return inst;
    }

    public FuncCallInst createFuncCallInst(String name, ArrayList<Value> params) {
        Function function = (Function) curTable.findSymbolInAll("@" + name);

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

    public BinaryInst createBinaryInst(Tokens.TokenKind token, Value lValue, Value rValue) {
        String regName = getRegName();
        User user;
        BinaryInst res;
        switch (token) {
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
                res = new BinaryInst(block, BinaryOp.SREM, lValue, rValue, user);
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
            case AND: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.AND, lValue, rValue, user);
                break;
            }
            case OR: {
                user = new User(IntType.INT32_TYPE, regName, lValue, rValue);
                res = new BinaryInst(block, BinaryOp.OR, lValue, rValue, user);
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

    public ZExtInst createZExtInst(Value from) {
        Value to = new Value(IntType.INT32_TYPE, getRegName());
        ZExtInst inst = new ZExtInst(block, from, to);
        block.addInst(inst);
        return inst;
    }

    public Value createConst(int value) {
        return new ConstantInt(value);
    }
}
