package midend;

import midend.mir.BasicBlock;
import midend.mir.Function;
import midend.mir.Instruction.*;
import midend.mir.Module;

import java.util.ArrayList;

public class JumpOptimizer {
    public static void optimize(Module module) {
        for (Function function : module.getFunctionList()) {
            ArrayList<BasicBlock> removeBlocks = new ArrayList<>();
            for (BasicBlock block = function.getBBlockList().getEnd(); block != null;
                 block = (BasicBlock) block.getPrev()) {
                if (block.getTerminator() instanceof BranchInst) {
                    BranchInst inst = (BranchInst) block.getTerminator();
                    BasicBlock tmpBlock = inst.getThenBlock();
                    if (tmpBlock.getInstList().getBegin() == null && tmpBlock.getTerminator() instanceof BranchInst) {
                          BasicBlock jumpBlock = ((BranchInst) tmpBlock.getTerminator()).getThenBlock();
                          inst.setThenBlock(jumpBlock);
                          removeBlocks.add(tmpBlock);
                    }
                    tmpBlock = inst.getElseBlock();
                    if (tmpBlock != null && tmpBlock.getInstList().getBegin() == null
                            && tmpBlock.getTerminator() instanceof BranchInst) {
                        BasicBlock jumpBlock = ((BranchInst) tmpBlock.getTerminator()).getThenBlock();
                        inst.setElseBlock(jumpBlock);
                        removeBlocks.add(tmpBlock);
                    }
                }
            }
            for (BasicBlock block : removeBlocks) {
                block.remove();
            }
        }
    }
}
