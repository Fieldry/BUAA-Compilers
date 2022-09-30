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
        RetInst inst = builder.createRetInst(node.getExpression());
        curBBlock.addInst(inst);
        writer.write("ret " + inst.getType() + " " + inst.getNumber());
    }

    public Value visit(SysYExpression node) {

        return null;
    }

    public void visit(SysYBinaryExp node) {
        switch (node.getToken().getTokenKind()) {
            case PLUS: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                builder.createAdd(lValue, rValue);
                break;
            }
            case MINUS: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                builder.createSub(lValue, rValue);
                break;
            }
            case STAR: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                builder.createMul(lValue, rValue);
                break;
            }
            case DIV: {
                Value lValue = visit(node.getLeftExp()), rValue = visit(node.getRightExp());
                builder.createDiv(lValue, rValue);
                break;
            }
        }
    }
}
