package frontend.irBuilder;

import frontend.tree.SysYTree;
import frontend.tree.SysYTree.*;
import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.LoopRecord.Pair;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Initial.*;
import io.Writer;

import java.util.ArrayList;
import java.util.Stack;

public class AssemblyBuilder {
    private final Writer writer;

    private final Module module;
    private final IRBuilder builder = new IRBuilder();

    private final Stack<LoopRecord> loopStack = new Stack<>();
    /** Current function.
     */
    private Function curFunction;
    /** The count of basic block in current function.
     */
    private int bbCount;
    /** Current basic block.
     */
    private BasicBlock curBBlock;
    private boolean inGlobal;

    public AssemblyBuilder(Writer writer, Module module) {
        this.writer = writer;
        this.module = module;
    }

    public void generateLLVM(SysYCompilationUnit node) {
        visit(node);
        for (GlobalVariable var : module.getGlobalList()) {
            writer.writeln(var.toString());
        }
        writer.writeln("");
        for (Function function : module.getFunctionList()) {
            writer.writeln(function + "{");
            for (BasicBlock bBlock : function.getBBlockList()) {
                writer.writeln(bBlock.getName() + ":");
                for (Instruction inst : bBlock.getInstList()) {
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

    private BasicBlock createNewBlock(boolean set) {
        BasicBlock temp = curBBlock;
        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);
        if (set && temp.getTerminator() == null) {
            temp.setTerminator(builder.createBranchInst(curBBlock));
        }
        return temp;
    }

    public void visit(SysYCompilationUnit node) {
        inGlobal = true;
        for (SysYBlockItem item : node.getDecls()) {
            visit((SysYDecl) item);
        }
        for (SysYSymbol symbol : node.getFuncDefs()) {
            visit((SysYFuncDef) symbol);
        }
        inGlobal = false;
        visit((SysYMainFuncDef) node.getMainFuncDef());
    }

    public void visit(SysYFuncDef node) {
        curFunction = builder.createFunction(node.isReturnInt(), node.getName(), module);
        module.addFunction(curFunction);

        builder.createSymbolTable();
        for (SysYTree.SysYSymbol symbol : node.getFuncParams()) {
            Value value = builder.createFParam(symbol.getName(), ((SysYFuncParam) symbol).getDimensions());
            curFunction.addParam(value);
        }
        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);
        for (int i = 0, len = node.getFuncParams().size(); i < len; i++) {
            SysYSymbol symbol = node.getFuncParams().get(i);
            Value value = curFunction.getParams().get(i);
            curBBlock.addInst(builder.createAllocInst(value.getType(), symbol.getName()));
        }
        for (int i = 0, len = node.getFuncParams().size(); i < len; i++) {
            SysYSymbol symbol = node.getFuncParams().get(i);
            Value value = curFunction.getParams().get(i);
            builder.createStrInst(symbol.getName(), value);
        }
        visit((SysYBlock) node.getBlock());
        builder.recallSymbolTable();
    }

    public void visit(SysYMainFuncDef node) {
        curFunction = builder.createFunction(true, "main", module);
        module.addFunction(curFunction);
        curBBlock = builder.createBlock(curFunction);
        curFunction.addBBlock(curBBlock);

        builder.createSymbolTable();
        visit((SysYBlock) node.getBlock());
        builder.recallSymbolTable();
    }

    public void visit(SysYBlock node) {
        for (int i = 0, len = node.getBlock().size(); i < len; i++) {
            SysYBlockItem blockItem = node.getBlock().get(i);
            if (blockItem instanceof SysYStatement) visit((SysYStatement) blockItem);
            else visit((SysYDecl) blockItem);
        }
    }

    public void visit(SysYDecl node) {
        for (SysYSymbol symbol : node.getDefs()) {
            visit((SysYDef) symbol);
        }
    }

    public void visit(SysYDef node) {
        SysYInit init = (SysYInit) node.getInit();
        Type type;
        switch (node.getDimensions()) {
            case 0: {
                type = IntType.INT32_TYPE;
                break;
            }
            case 1: {
                int size = ((ConstantInt) visit(node.getFirstExp())).getValue();
                type = new ArrayType(size, IntType.INT32_TYPE);
                break;
            }
            case 2: {
                int firstSize = ((ConstantInt) visit(node.getFirstExp())).getValue();
                int secondSize = ((ConstantInt) visit(node.getSecondExp())).getValue();
                ArrayType secondArrayType = new ArrayType(secondSize, IntType.INT32_TYPE);
                type = new ArrayType(firstSize, secondArrayType);
                break;
            }
            default: {
                return;
            }
        }
        if (inGlobal) {
            Initial initValue = visit(init, type);
            module.addGlobal(builder.createGlobalVar(node.isConst(), type, node.getName(), initValue));
        } else {
            builder.createAllocInst(type, node.getName());
            Initial initValue = visit(init, type);
            if (type.isInt32Type()) builder.createStrInst(node.getName(), ((ValueInitial) initValue).getValue());
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
        }
    }

    public void visit(SysYExpressionStatement node) {
        if (!node.isEmpty()) visit(node.getExp());
    }

    public void visit(SysYAssign node) {
        Value value = visit(node.getExpression());
        Value left = visit(node.getlVal(), true);
        builder.createStrInst(value, left);
    }

    public void visit(SysYIf node) {
        // main block
        Value cond = visit(node.getCond());
        BranchInst inst = builder.createBranchInst(cond);
        curBBlock.setTerminator(inst);

        // first if block
        createNewBlock(false);
        inst.setThenBlock(curBBlock);
        visit(node.getThenStmt());


        createNewBlock(false);
        inst.setElseBlock(curBBlock);
        if (node.getElseStmt() != null) {
            // second if block
            visit(node.getElseStmt());

            // block after if statement
            createNewBlock(true);
            inst.getThenBlock().setTerminator(builder.createBranchInst(curBBlock));
            if (inst.getElseBlock().getTerminator() == null)
                inst.getElseBlock().setTerminator(builder.createBranchInst(curBBlock));
        } else {
            // block after if statement
            inst.getThenBlock().setTerminator(builder.createBranchInst(curBBlock));
        }
    }

    public void visit(SysYWhile node) {
        createNewBlock(true);

        // condition block
        Value cond = visit(node.getCond());
        BranchInst inst = builder.createBranchInst(cond);
        curBBlock.setTerminator(inst);

        // push loop block into stack
        loopStack.push(new LoopRecord());

        // loop body
        BasicBlock condBlock = createNewBlock(false);
        inst.setThenBlock(curBBlock);
        visit(node.getStmt());
        curBBlock.setTerminator(builder.createBranchInst(condBlock));

        // block after loop
        createNewBlock(false);
        inst.setElseBlock(curBBlock);

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
        }
        return null;
    }

    public Value visit(SysYFuncCall node) {
        ArrayList<Value> params = new ArrayList<>();
        for (SysYExpression exp : node.getFuncRParams()) {
            params.add(visit(exp));
        }
        FuncCallInst inst = builder.createFuncCallInst(node.getName(), params);
        return inst.getResValue();
    }

    public Value visit(SysYUnaryExp node) {
        BinaryInst inst = null;
        switch (node.getUnaryOp().getTokenKind()) {
            case PLUS: {
                return visit(node.getUnaryExp());
            }
            case MINUS: {
                inst = builder.createSub(ConstantInt.getZero(), visit(node.getUnaryExp()));
            }
        }
        curBBlock.addInst(inst);
        return inst.getResValue();
    }

    public Value visit(SysYBinaryExp node) {
        BinaryInst inst;
        if (node.getToken() == null) {
            return visit(node.getLeftExp());
        } else {
            Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
            inst = builder.createBinaryInst(node.getToken(), lValue, rValue);
        }
        return inst.getResValue();
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

    public Value visit(SysYLVal node, boolean pointer) {
        switch (node.getDimensions()) {
            case 0: {
                if (pointer) return builder.getValueFromTable(node.getName());
                else return builder.createLdInst(node.getName()).getTo();
            }
            case 1: {
                GEPInst inst = builder.createGEPInst(node.getName(), node.getDimensions(), visit(node.getFirstExp()));
                if (pointer) return inst.getTo();
                else return builder.createLdInst(inst.getTo()).getTo();
            }
            case 2: {
                GEPInst inst = builder.createGEPInst(node.getName(), node.getDimensions(),
                        visit(node.getFirstExp()), visit(node.getSecondExp()));
                if (pointer) return inst.getTo();
                else return builder.createLdInst(inst.getTo()).getTo();
            }
            default: {
                return null;
            }
        }
    }

    public Value visit(SysYCond node) {
        return visit((SysYBinaryExp) node.getCond());
    }
}
