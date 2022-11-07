package midend;

import midend.mir.BasicBlock;
import midend.mir.Function;
import midend.mir.Instruction;
import midend.mir.Instruction.*;
import midend.mir.Value;
import utils.inodelist.INode;

import java.util.HashMap;
import java.util.HashSet;

public class LiveAnalysis {
    private final HashMap<BasicBlock, HashSet<Value>> liveIn = new HashMap<>();
    private final HashMap<BasicBlock, HashSet<Value>> liveOut = new HashMap<>();
    private final HashMap<BasicBlock, HashSet<Value>> liveUse = new HashMap<>();
    private final HashMap<BasicBlock, HashSet<Value>> liveDef = new HashMap<>();

    private void liveUseDef(BasicBlock block) {
        if (!liveUse.containsKey(block)) liveUse.put(block, new HashSet<>());
        if (!liveDef.containsKey(block)) liveDef.put(block, new HashSet<>());
        for (INode inst : block.getInstList()) {
            if (inst instanceof AllocInst) {
                liveDef.get(block).add(((AllocInst) inst).getValue());
            } else if (inst instanceof MemoryInst) {
                MemoryInst memoryInst = (MemoryInst) inst;
                if (memoryInst.getFlag() == 0) {
                    liveDef.get(block).add(memoryInst.getTo());
                } else {
                    liveUse.get(block).add(memoryInst.getFrom());
                }
            }
        }
    }

    private void liveInOut(FlowGraph flowGraph, Function function) {
        for (BasicBlock block : function.getBBlockList()) {
            liveIn.put(block, new HashSet<>());
            liveOut.put(block, new HashSet<>());
        }
        boolean flag = false;
        do {
            BasicBlock block = function.getBBlockList().getEnd();
            while (block != null) {
                int size = liveOut.get(block).size();
                for (BasicBlock nextBlock : flowGraph.nextOf(block)) {
                    liveOut.get(block).addAll(liveIn.get(nextBlock));
                }
                if (size != liveOut.get(block).size()) flag = true;
                for (Value value : liveOut.get(block)) {
                    if (!liveDef.get(block).contains(value)) {
                        liveIn.get(block).add(value);
                    }
                }
                liveIn.get(block).addAll(liveUse.get(block));
                block = (BasicBlock) block.getPrev();
            }
        } while (flag);
    }

    public HashMap<BasicBlock, HashSet<Value>> getLiveIn() { return liveIn; }

    public HashMap<BasicBlock, HashSet<Value>> getLiveOut() { return liveOut; }
}
