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

    public void visit(SysYStatement node) {
        if (node instanceof SysYBlock) {
            curBBlock = new BasicBlock();
            visit((SysYBlock) node);
        } else if (node instanceof SysYReturn) {
            visit((SysYReturn) node);
        }
    }

    public void visit(SysYBlock node) {
        for (int i = 0, len = node.getBlock().size(); i < len; i++) {
            visit((SysYStatement) node.getBlock().get(i));
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
}
