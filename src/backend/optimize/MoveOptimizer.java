package backend.optimize;

import backend.MIPSCode;
import midend.mir.BasicBlock;
import midend.mir.Function;
import midend.mir.Module;
import backend.MIPSCode.*;
import utils.inodelist.INode;

public class MoveOptimizer {
    public static void optimize(Module module) {
        for (Function function : module.getFunctionList()) {
            for (BasicBlock block : function.getBBlockList()) {
                for (INode iNode : block.getInstList()) {
                    if (iNode instanceof MoveCode) {
                        MIPSCode inst = (MIPSCode) iNode.getPrev();
                        if (inst != null && inst.optMove(((MoveCode) iNode).getRt(), ((MoveCode) iNode).getRs())) {
                            iNode.remove();
                        }
                    }
                }
            }
        }
    }
}
