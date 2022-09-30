package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.tree.SysYTree.*;

public class IRBuilder {
    public IRBuilder() {}

    public RetInst createRetInst(SysYExpression expression) {
        if (expression instanceof SysYIntC) {
            return new RetInst(new IntType(), ((SysYIntC) expression).getValue());
        } else return null;
    }

    public BinaryInst createAdd(Value lValue, Value rValue) {

        return new BinaryInst();
    }

    public BinaryInst createSub(Value lValue, Value rValue) {

        return new BinaryInst();
    }

    public BinaryInst createMul(Value lValue, Value rValue) {

        return new BinaryInst();
    }

    public BinaryInst createDiv(Value lValue, Value rValue) {

        return new BinaryInst();
    }
}
