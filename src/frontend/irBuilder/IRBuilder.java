package frontend.irBuilder;

import frontend.irBuilder.Instruction.*;
import frontend.irBuilder.Type.*;
import frontend.irBuilder.Instruction.BinaryInst.BinaryOp;

public class IRBuilder {
    /** The count of register used.
     */
    private int regCount = 0;
    private BasicBlock block;

    public IRBuilder() {}

    public RetInst createRetInst(Value value) {
        return new RetInst(IntType.i32, value);
    }

    public BinaryInst createAdd(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.ADD, lValue, rValue, user);
    }

    public BinaryInst createSub(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SUB, lValue, rValue, user);
    }

    public BinaryInst createMul(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.MUL, lValue, rValue, user);
    }

    public BinaryInst createDiv(Value lValue, Value rValue) {
        String name = "%" + (++regCount);
        User user = new User(IntType.i32, name, lValue, rValue);
        lValue.addUse(user);
        rValue.addUse(user);
        return new BinaryInst(block, BinaryOp.SDIV, lValue, rValue, user);
    }

    public BinaryInst createMod(Value lValue, Value rValue) {

        return null;
    }

    public Value createConst(int value) {
        return new ConstantInt(value);
    }
}
