package frontend.irBuilder;

import frontend.tree.SysYTree.*;
import frontend.irBuilder.Instruction.*;
import io.Writer;

public class AssemblyBuilder {
    private final Writer writer;

    private final Module module;
    private IRBuilder builder = new IRBuilder();

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

    public void visit(SysYCompilationUnit node) {
        visit((SysYMainFuncDef) node.getMainFuncDef());
    }

    public void visit(SysYMainFuncDef node) {
        writer.write("define i32 @main()");
        writer.writeln("{");

        curFunction = new Function();
        module.addFunction(curFunction);
        visit(node.getBlock());

        writer.writeln("}");

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
                    writer.writeln("\t" + inst.toString());
                    break;
                }
            }
        }
        Value value = visit(node.getInit());
        inst = builder.createStrInst(node.getName(), value);
        curBBlock.addInst(inst);
        writer.writeln("\t" + inst);
    }

    public void visit(SysYStatement node) {
        if (node instanceof SysYBlock) {
            curBBlock = new BasicBlock();
            visit((SysYBlock) node);
        } else if (node instanceof SysYReturn) {
            visit((SysYReturn) node);
        }
    }

    public void visit(SysYReturn node) {
        RetInst inst = builder.createRetInst(visit(node.getExpression()));
        curBBlock.addInst(inst);
        writer.writeln("\t" + inst.toString());
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
        writer.writeln("\t" + inst);
        return inst.getResValue();
    }

    public Value visit(SysYBinaryExp node) {
        BinaryInst inst = null;
        if (node.getToken() == null) {
            return visit(node.getLeftExp());
        } else switch (node.getToken().getTokenKind()) {
            case PLUS: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                inst = builder.createAdd(lValue, rValue);
                break;
            }
            case MINUS: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                inst = builder.createSub(lValue, rValue);
                break;
            }
            case STAR: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                inst = builder.createMul(lValue, rValue);
                break;
            }
            case DIV: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                inst = builder.createDiv(lValue, rValue);
                break;
            }
            case MOD: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                inst = builder.createMod(lValue, rValue);
                break;
            }
        }
        curBBlock.addInst(inst);
        assert inst != null;
        writer.writeln("\t" + inst);
        return inst.getResValue();
    }

    public Value visit(SysYIntC node) {
        return builder.createConst(node.getValue());
    }

    public Value visit(SysYInit node) {
        return visit(node.getExpression().get(0));
    }

    public Value visit(SysYLVal node) {
        Instruction inst = null;
        switch (node.getDimensions()) {
            case 0: {
                inst = builder.createLdInst(node.getName());
            }
        }
        assert inst != null;
        return ((MemoryInst)inst).getTo();
    }
}
