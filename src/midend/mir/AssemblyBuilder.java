package midend.mir;

import frontend.token.Tokens;
import frontend.tree.SysYTree;
import frontend.tree.SysYTree.*;
import midend.mir.Instruction.*;
import midend.mir.LoopRecord.Pair;
import midend.mir.Type.*;
import midend.mir.Initial.*;
import utils.Writer;
import utils.inodelist.INode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class AssemblyBuilder {
    private final Writer writer;

    private final Module module;
    private final IRBuilder builder = new IRBuilder();

    private final Stack<LoopRecord> loopStack = new Stack<>();
    /** Current function.
     */
    private Function curFunction;
    /** Current basic block.
     */
    private BasicBlock curBBlock;
    private boolean inGlobal;
    private GEPInst secondGet = null;

    public AssemblyBuilder(Writer writer, Module module) {
        this.writer = writer;
        this.module = module;
    }

    public void generateLLVM(SysYCompilationUnit node) {
        boolean debug = false;
        if (debug) writer.setStdOut();
        else writer.setLlvmBw();
        declareLibFunc();
        visit(node);
        if (!module.getGlobalList().isEmpty()) {
            for (GlobalVariable var : module.getGlobalList()) {
                writer.writeln(var.toString());
            }
            writer.writeln("");
        }
        for (Function function : module.getFunctionList()) {
            writer.write(function + "{");
            writer.writeln("");
            for (INode inst : function.getParamFetchList()) {
                writer.writeln("\t" + inst);
            }
            for (BasicBlock bBlock : function.getBBlockList()) {
                writer.writeln(";<label>:" + bBlock.getName() + ":");
                for (INode inst : bBlock.getInstList()) {
                    writer.writeln("\t" + inst);
                }
                writer.writeln("\t" + bBlock.getTerminator());
            }
            writer.writeln("}");
            writer.writeln("");
        }
    }

    /*------------------------------
        Helper functions
     -----------------------------*/

    private void declareLibFunc() {
        writer.writeln(LibFunction.GET_INT);
        writer.writeln(LibFunction.PUT_CH);
        writer.writeln(LibFunction.PUT_INT);
        builder.addFunctionToTable(LibFunction.GET_INT);
        builder.addFunctionToTable(LibFunction.PUT_CH);
        builder.addFunctionToTable(LibFunction.PUT_INT);
        writer.writeln("");
    }

    private BasicBlock createNewBlock(boolean set) {
        BasicBlock temp = curBBlock;
        // 减少不必要的基本块
        // if (curBBlock.getInstList().isEmpty()) return curBBlock;
        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);
        if (set && temp.getTerminator() == null) {
            temp.setTerminator(builder.createBranchInst(curBBlock));
        }
        return temp;
    }

    private ConstantInt evaluate(Tokens.Token token, Value l, Value r) {
        int lValue = ((ConstantInt) l).getValue();
        int rValue = ((ConstantInt) r).getValue();
        int res = 0;
        switch (token.getTokenKind()) {
            case PLUS: {
                res = lValue + rValue;
                break;
            }
            case MINUS: {
                res = lValue - rValue;
                break;
            }
            case STAR: {
                res = lValue * rValue;
                break;
            }
            case DIV: {
                res = lValue / rValue;
                break;
            }
            case MOD: {
                res = lValue % rValue;
                break;
            }
        }
        return new ConstantInt(res);
    }

    private ConstantInt evaluate(Tokens.Token token, Value v) {
        int value = ((ConstantInt) v).getValue();
        int res = 0;
        switch (token.getTokenKind()) {
            case PLUS: {
                res = value;
                break;
            }
            case MINUS: {
                res = -value;
                break;
            }
            case NOT: {
                res = (value == 0) ? 1 : 0;
            }
        }
        return new ConstantInt(res);
    }

    private void initLocalVar(Value pointer, Initial init) {
        PointerType pointerType = (PointerType) pointer.getType();
        Type innerType = pointerType.getInnerType();
        if (innerType.isInt32Type()) {
            builder.createStrInst(((ValueInitial) init).getValue(), pointer,true);
        } else {
            ArrayType arrayType = (ArrayType) innerType;
            PointerType toType = new PointerType(arrayType.getBaseType());
            ArrayInitial arrayInitial = (ArrayInitial) init;
            /* new */
            int first = arrayType.getSize();
            if (arrayType.getBaseType().isArrayType()) {
                ArrayType arrayType1 =(ArrayType) arrayType.getBaseType();
                PointerType toType2 = new PointerType(arrayType1.getBaseType());
                int second = arrayType1.getSize();
                for (int i = 0; i < first; i++) {
                    ArrayInitial arrayInitial1 = (ArrayInitial) arrayInitial.getInitValues().get(i);
                    Initial temp;
                    Value pointer1;
                    for (int j = 0; j < second; j++) {
                        pointer1 = builder.createGEPInst(pointer, toType, new ConstantInt(i), true).getTo();
                        temp = arrayInitial1.getInitValues().get(j);
                        initLocalVar(
                                builder.createGEPInst(pointer1, toType2, new ConstantInt(j), true).getTo(), temp);
                    }
                }
            }
            /* end */
            else {
                for (int i = 0; i < first; i++) {
                    initLocalVar(builder.createGEPInst(pointer, toType, new ConstantInt(i), true).getTo(),
                            arrayInitial.getInitValues().get(i));
                }
            }
        }
    }

    public void visit(SysYCompilationUnit node) {
        inGlobal = true;
        for (SysYBlockItem item : node.getDecls()) {
            visit((SysYDecl) item);
        }
        inGlobal = false;
        for (SysYSymbol symbol : node.getFuncDefs()) {
            visit((SysYFuncDef) symbol);
        }
        visit((SysYMainFuncDef) node.getMainFuncDef());
    }

    public void visit(SysYFuncDef node) {
        curFunction = builder.createFunction(node.isReturnInt(), node.getName(), module);
        module.addFunction(curFunction);

        builder.createSymbolTable();
        for (SysYTree.SysYSymbol symbol : node.getFuncParams()) {
            Value value = builder.createFParam(symbol.getName(),
                    ((SysYFuncParam) symbol).getDimensions(), visit(((SysYFuncParam) symbol).getSecondExp()));
            curFunction.addParam(value);
        }
        for (int i = 0, len = node.getFuncParams().size(); i < len; i++) {
            SysYSymbol symbol = node.getFuncParams().get(i);
            Value value = curFunction.getParams().get(i);
            curFunction.addParam(builder.createAllocInst(value.getType(), "$", symbol.getName(), false));
        }
        for (int i = 0, len = node.getFuncParams().size(); i < len; i++) {
            SysYSymbol symbol = node.getFuncParams().get(i);
            Value value = curFunction.getParams().get(i);
            curFunction.addParam(builder.createStrInst(symbol.getName(), value, false));
        }

        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);
        visit((SysYBlock) node.getBlock());
        if (curBBlock.needTerminator()) {
            if (node.isReturnInt()) {
                builder.createRetInst(ConstantInt.getZero());
            } else {
                builder.createRetInst(null);
            }
        }
        builder.recallSymbolTable();
    }

    public void visit(SysYMainFuncDef node) {
        curFunction = builder.createFunction(true, "main", module);
        module.addMainFunction(curFunction);
        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);

        builder.createSymbolTable();
        visit((SysYBlock) node.getBlock());
        if (curBBlock.needTerminator()) builder.createRetInst(ConstantInt.getZero());
        builder.recallSymbolTable();
    }

    public void visit(SysYBlock node) {
        for (int i = 0, len = node.getBlock().size(); i < len; i++) {
            SysYBlockItem blockItem = node.getBlock().get(i);
            if (blockItem instanceof SysYStatement) {
                visit((SysYStatement) blockItem);
                if (blockItem instanceof SysYContinue || blockItem instanceof SysYBreak) return;
            }
            else visit((SysYDecl) blockItem);
        }
    }

    public void visit(SysYDecl node) {
        for (SysYSymbol symbol : node.getDefs()) {
            visit((SysYDef) symbol);
        }
    }

    public void visit(SysYDef node) {
        boolean isConst = node.isConst();
        SysYInit init = (SysYInit) node.getInit();
        Type type;
        switch (node.getDimensions()) {
            case 0: {
                type = IntType.INT32_TYPE;
                break;
            }
            case 1: {
                int size = ((ConstantInt) visit(node.getFirstExp())).getValue();
                type = new ArrayType(isConst, size, IntType.INT32_TYPE);
                break;
            }
            case 2: {
                int firstSize = ((ConstantInt) visit(node.getFirstExp())).getValue();
                int secondSize = ((ConstantInt) visit(node.getSecondExp())).getValue();
                ArrayType secondArrayType = new ArrayType(isConst, secondSize, IntType.INT32_TYPE);
                type = new ArrayType(isConst, firstSize, secondArrayType);
                break;
            }
            default: {
                return;
            }
        }
        if (inGlobal) {
            // initial global variables and constants
            Initial initValue;
            if (init == null) {
                // 隐式初始化为0
                initValue = type.isInt32Type() ? new ValueInitial(type, ConstantInt.getZero())
                        : new ZeroInitial(type);
            } else initValue = visit(init, type);
            module.addGlobal(builder.createGlobalVar(node.isConst(), type, node.getName(), initValue));
        } else {
            // 初始化局部变量
            if (isConst) {
                builder.addLocalConstToTable(node.getName(), visit(init, type));
            } else {
                builder.createAllocInst(type, "*", node.getName(), true);
                if (init != null) initLocalVar(builder.getValueFromTable(node.getName()), visit(init, type));
            }
        }
    }

    public void visit(SysYStatement node) {
        if (node instanceof SysYBlock) {
            builder.createSymbolTable();
            visit((SysYBlock) node);
            builder.recallSymbolTable();
        } else if (node instanceof SysYReturn) {
            visit((SysYReturn) node);
        } else if (node instanceof SysYIf) {
            visit((SysYIf) node);
        } else if (node instanceof SysYWhile) {
            visit((SysYWhile) node);
        } else if (node instanceof SysYAssign) {
            visit((SysYAssign) node);
        } else if (node instanceof SysYContinue) {
            visit((SysYContinue) node);
        } else if (node instanceof SysYBreak) {
            visit((SysYBreak) node);
        } else if (node instanceof SysYExpressionStatement) {
            visit((SysYExpressionStatement) node);
        } else if (node instanceof SysYPrintf) {
            visit((SysYPrintf) node);
        }
    }

    public void visit(SysYExpressionStatement node) {
        if (!node.isEmpty()) visit(node.getExp());
    }

    public void visit(SysYAssign node) {
        Value value = visit(node.getExpression());
        Value left = visit(node.getlVal(), true);
        builder.createStrInst(value, left, true);
    }

    private void visitEqHelper(SysYEqExp cond, SysYStatement trueBlock, SysYStatement falseBlock) {
        // main block
        Value condValue = visit(cond);
        if (!condValue.getType().isInt1Type())
            condValue = builder.createBinaryInst(Tokens.TokenKind.NEQ, condValue, ConstantInt.getZero()).getResValue();
        BranchInst inst = builder.createBranchInst(condValue);
        curBBlock.setTerminator(inst);

        // save while cond block
        if (!loopStack.empty() && loopStack.peek().getCondBlock() == null)
            loopStack.peek().setCondBlock(curBBlock);

        // It's certain that the cond block has terminator.
        // first if block -- then block
        createNewBlock(false);
        inst.setThenBlock(curBBlock);
        visit(trueBlock);

        BasicBlock thenTemp = createNewBlock(false);
        inst.setElseBlock(curBBlock);
        if (falseBlock != null) {
            visit(falseBlock);
            BasicBlock elseTemp = createNewBlock(false);
            if (elseTemp.needTerminator())
                elseTemp.setTerminator(builder.createBranchInst(curBBlock));
        }
        if (thenTemp.needTerminator())
            thenTemp.setTerminator(builder.createBranchInst(curBBlock));
    }

    private void visitLAndHelper(SysYLAndExp cond, SysYStatement trueBlock, SysYStatement falseBlock) {
        if (cond.getToken() == null) {
            visitEqHelper((SysYEqExp) cond.getLeftExp(), trueBlock, falseBlock);
        } else {
            visitLAndHelper((SysYLAndExp) cond.getLeftExp(),
                    new SysYIf(cond.getRightExp(), trueBlock, falseBlock), falseBlock);
        }
    }

    private void visitLOrHelper(SysYLOrExp cond, SysYStatement trueBlock, SysYStatement falseBlock) {
        if (cond.getToken() == null) {
            visitLAndHelper((SysYLAndExp) cond.getLeftExp(), trueBlock, falseBlock);
        } else {
            visitLOrHelper((SysYLOrExp) cond.getLeftExp(), trueBlock,
                    new SysYIf(cond.getRightExp(), trueBlock, falseBlock));
        }
    }

    private void visitIfHelper(SysYExpression cond, SysYStatement trueBlock, SysYStatement falseBlock) {
        if (cond instanceof SysYCond)
            visitLOrHelper((SysYLOrExp) ((SysYCond) cond).getCond(), trueBlock, falseBlock);
        else if (cond instanceof SysYLOrExp)
            visitLOrHelper((SysYLOrExp) cond, trueBlock, falseBlock);
        else if (cond instanceof SysYLAndExp)
            visitLAndHelper((SysYLAndExp) cond, trueBlock, falseBlock);
        else visitEqHelper((SysYEqExp) cond, trueBlock, falseBlock);
    }

    public void visit(SysYIf node) {
        visitIfHelper(node.getCond(), node.getThenStmt(), node.getElseStmt());
    }

    private void visitWhileHelper(SysYExpression cond, SysYStatement trueBlock) {
        createNewBlock(true);

        // push loop block into stack
        loopStack.push(new LoopRecord());

        visitIfHelper(cond, trueBlock, new SysYBreak());

        BasicBlock condBlock = loopStack.peek().getCondBlock();
        curBBlock.setTerminator(builder.createBranchInst(condBlock));
        createNewBlock(false);

        // handle continue and break
        for (Pair pair : loopStack.peek().getRecords()) {
            if (pair.getString().equals("Continue")) {
                pair.getBlock().setTerminator(builder.createBranchInst(condBlock));
            } else {
                pair.getBlock().setTerminator(builder.createBranchInst(curBBlock));
            }
        }

        // pop stack
        loopStack.pop();
    }

    public void visit(SysYWhile node) {
        visitWhileHelper(node.getCond(), node.getStmt());
    }

    public void visit(SysYContinue node) {
        loopStack.peek().add("Continue", curBBlock);
    }

    public void visit(SysYBreak node) {
        loopStack.peek().add("Break", curBBlock);
    }

    public void visit(SysYReturn node) {
        builder.createRetInst(visit(node.getExpression()));
    }

    public Value visit(SysYExpression node) {
        if (node == null) return null;
        else if (node instanceof SysYUnaryExp) {
            return visit((SysYUnaryExp) node);
        } else if (node instanceof SysYBinaryExp) {
            return visit((SysYBinaryExp) node);
        } else if(node instanceof SysYIntC) {
            return visit((SysYIntC) node);
        } else if (node instanceof SysYCond) {
            return visit((SysYCond) node);
        } else if (node instanceof SysYLVal) {
            return visit((SysYLVal) node, false);
        } else if (node instanceof SysYFuncCall) {
            return visit((SysYFuncCall) node);
        } else if (node instanceof SysYGetInt) {
            return visit((SysYGetInt) node);
        } else if (node instanceof SysYPrintf) {
            return visit((SysYPrintf) node);
        }
        return null;
    }

    public Value visit(SysYGetInt node) {
        return builder.createFuncCallInst("getint", new ArrayList<>()).getResValue();
    }

    public Value visit(SysYPrintf node) {
        Object[] strings = Arrays.stream(node.getFormat().replace("\\n", "\n").split("%d"))
                .map(s -> s.replace("\"", "")).toArray();
        List<SysYExpression> exps = node.getExps();
        for (int i = 0, len = strings.length; i < len; i++) {
            for (char ch : ((String) strings[i]).toCharArray()) {
                builder.createFuncCallInst("putch", new ArrayList<Value>() {{
                    add(new ConstantInt(ch));
                }});
            }
            if (i < len - 1) {
                int finalI = i;
                builder.createFuncCallInst("putint", new ArrayList<Value>() {{
                    add(visit(exps.get(finalI)));
                }});
            }
        }
        return null;
    }

    public Value visit(SysYFuncCall node) {
        ArrayList<Value> params = new ArrayList<>();
        for (SysYExpression exp : node.getFuncRParams()) {
            if (exp.isFuncCall())
                params.add(visit(exp));
        }
        for (int i = 0, len = node.getFuncRParams().size(); i < len; i++) {
            SysYExpression exp = node.getFuncRParams().get(i);
            if (!exp.isFuncCall())
                params.add(i, visit(exp));
        }
        return builder.createFuncCallInst(node.getName(), params).getResValue();
    }

    public Value visit(SysYUnaryExp node) {
        Value value = visit(node.getUnaryExp());
        if (value instanceof ConstantInt) {
            return evaluate(node.getUnaryOp(), value);
        } else if (node.getUnaryOp().getTokenKind() == Tokens.TokenKind.MINUS){
            return builder.createBinaryInst(node.getUnaryOp().getTokenKind(),
                    ConstantInt.getZero(), value).getResValue();
        } else if (node.getUnaryOp().getTokenKind() == Tokens.TokenKind.NOT) {
            return builder.createBinaryInst(Tokens.TokenKind.EQL, value, ConstantInt.getZero()).getResValue();
        } else {
            return value;
        }
    }

    public Value visit(SysYBinaryExp node) {
        if (node.getToken() == null) {
            return visit(node.getLeftExp());
        } else {
            Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
            if (lValue instanceof ConstantInt && rValue instanceof ConstantInt
                    && node.getToken().getTokenKind().isALUOp())
                return evaluate(node.getToken(), lValue, rValue);
            else {
                if (lValue.getType().isInt1Type()) lValue = builder.createZExtInst(lValue).getTo();
                if (rValue.getType().isInt1Type()) rValue = builder.createZExtInst(rValue).getTo();
                return builder.createBinaryInst(node.getToken().getTokenKind(), lValue, rValue).getResValue();
            }
        }
    }

    public Value visit(SysYIntC node) {
        return builder.createConst(node.getValue());
    }

    public Initial visit(SysYInit node, Type type) {
        if (type.isArrayType()) {
            ArrayList<Initial> values = new ArrayList<>();
            for (SysYExpression exp : node.getExpression()) {
                values.add(visit((SysYInit) exp, ((ArrayType) type).getBaseType()));
            }
            return new ArrayInitial(type, values);
        } else {
            return new ValueInitial(type, visit(node.getExpression().get(0)));
        }
    }

    public Value visit(SysYLVal node, boolean needPointer) {
        Value pointer = builder.getValueFromTable(node.getName());
        ArrayList<Value> indexes = new ArrayList<>();
        boolean flag = true;
        if (node.getFirstExp() != null) {
            indexes.add(visit(node.getFirstExp()));
            if (node.getSecondExp() != null) indexes.add(visit(node.getSecondExp()));
        }

        for (Value value : indexes) {
            flag &= (value instanceof ConstantInt);
        }

        if (pointer instanceof Initial) {
            if (flag) {
                for (Value value : indexes) {
                    pointer = ((ArrayInitial) pointer).getInitValues().get(((ConstantInt) value).getValue());
                }
                return ((ValueInitial) pointer).getValue();
            } else {
                pointer = builder.getValueFromTable("@" + node.getName());
            }
        }

        Type innerType = ((PointerType) pointer.getType()).getInnerType();
        for (Value value : indexes) {
            if (innerType instanceof PointerType) {
                pointer = builder.createGEPInst(builder.createLdInst(pointer).getTo(), innerType, value, false).getTo();
                innerType = ((PointerType) innerType).getInnerType();
            } else if (innerType instanceof ArrayType) {
                pointer = builder.createGEPInst(pointer, new PointerType(((ArrayType) innerType).getBaseType()),
                        value, true).getTo();
                innerType = ((ArrayType) innerType).getBaseType();
            }
        }
        if (needPointer) return pointer;
        else {
            if (innerType instanceof ArrayType) {
                Value value = builder.createGEPInst(pointer, new PointerType(((ArrayType) innerType).getBaseType()),
                        new ConstantInt(0), true).getTo();
                return new Value(new PointerType(((ArrayType) innerType).getBaseType()), value.getName());
            }
            else return builder.createLdInst(pointer).getTo();
        }
    }

    public Value visit(SysYCond node) {
        return visit((SysYBinaryExp) node.getCond());
    }
}
