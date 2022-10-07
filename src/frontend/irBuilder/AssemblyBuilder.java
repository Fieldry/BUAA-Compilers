package frontend.irBuilder;

import frontend.tree.SysYTree.*;
import frontend.irBuilder.Instruction.*;
import io.Writer;

public class AssemblyBuilder {
    private final Writer writer;

    private final Module module;
    private final IRBuilder builder = new IRBuilder();

    private Value value;
    private int constIntValue;
    private boolean isResultInt;

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
        for (Function function : module.getFunctionList()) {
            writer.writeln("define i32 @" + function.getName() + "(){");
            for (BasicBlock bBlock : function.getBBlockList()) {
                writer.writeln(bBlock.getName() + ":");
                for (Instruction inst : bBlock.getInstList()) {
                    writer.writeln("\t" + inst);
                }
            }
            writer.writeln("}");
        }
    }

    public void visit(SysYCompilationUnit node) {
        visit((SysYMainFuncDef) node.getMainFuncDef());
    }

    public void visit(SysYMainFuncDef node) {

        curFunction = new Function("main", module);
        module.addFunction(curFunction);
        curBBlock = new BasicBlock("main", curFunction);
        curFunction.addBBlock(curBBlock);
        visit((SysYBlock) node.getBlock());

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
        Instruction inst = null;
        if (node.isConst) {

        } else {
            switch (node.getDimensions()) {
                case 0: {
                    inst = builder.createAllocInst(node.getName());
                    curBBlock.addInst(inst);
                    // writer.writeln("\t" + inst.toString());
                    break;
                }
            }
        }
        Value value = visit(node.getInit());
        inst = builder.createStrInst(node.getName(), value);
        curBBlock.addInst(inst);
        // writer.writeln("\t" + inst);
    }

    public void visit(SysYStatement node) {
        if (node instanceof SysYBlock) {
            curBBlock = builder.createBlock(curFunction);
            visit((SysYBlock) node);
        } else if (node instanceof SysYReturn) {
            visit((SysYReturn) node);
        } else if (node instanceof SysYIf) {
            visit((SysYIf) node);
        }
    }

    public void visit(SysYIf node) {
        Value cond = visit(node.getCond());
        BranchInst inst = builder.createBranchInst(cond);
        curBBlock.addInst(inst);

        visit(node.getThenStmt());
        curFunction.addBBlock(curBBlock);
        inst.setThenBlock(curBBlock);

        if (node.getElseStmt() != null) {
            visit(node.getElseStmt());
            curFunction.addBBlock(curBBlock);
            inst.setElseBlock(curBBlock);
        }
        // writer.writeln("\t" + inst);
    }

    public void visit(SysYReturn node) {
        RetInst inst = builder.createRetInst(visit(node.getExpression()));
        curBBlock.addInst(inst);
        // writer.writeln("\t" + inst.toString());
    }

    public Value visit(SysYExpression node) {
        if (node instanceof SysYUnaryExp) {
            return visit((SysYUnaryExp) node);
        } else if (node instanceof SysYBinaryExp) {
            return visit((SysYBinaryExp) node);
        } else if(node instanceof SysYIntC) {
            return visit((SysYIntC) node);
        } else if (node instanceof SysYInit) {
            return visit((SysYInit) node);
        } else if (node instanceof SysYCond) {
            return visit((SysYCond) node);
        } else if (node instanceof SysYLVal) {
            return visit((SysYLVal) node, true);
        }
        return null;
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
        assert inst != null;
        // writer.writeln("\t" + inst);
        return inst.getResValue();
    }

    public Value visit(SysYBinaryExp node) {
        BinaryInst inst = null;
        if (node.getToken() == null) {
            return visit(node.getLeftExp());
        } else {
            Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
            inst = builder.createBinaryInst(node.getToken(), lValue, rValue);
        }
        curBBlock.addInst(inst);
        assert inst != null;
        // writer.writeln("\t" + inst);
        return inst.getResValue();
    }

    public Value visit(SysYIntC node) {
        return builder.createConst(node.getValue());
    }

    public Value visit(SysYInit node) {
        return visit(node.getExpression().get(0));
    }

    public Value visit(SysYLVal node, boolean load) {
        MemoryInst inst = null;
        switch (node.getDimensions()) {
            case 0: {
                if (load) {
                    inst = builder.createLdInst(node.getName());
                    curBBlock.addInst(inst);
                }
                return builder.getLValueByName(node.getName());
            }
        }
        return inst.getTo();
    }

    public Value visit(SysYCond node) {
        return visit((SysYBinaryExp) node.getCond());
    }
}
