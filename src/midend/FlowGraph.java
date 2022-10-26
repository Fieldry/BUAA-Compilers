package midend;

import frontend.irBuilder.BasicBlock;
import frontend.irBuilder.Function;
import frontend.irBuilder.Module;
import frontend.irBuilder.Instruction.BranchInst;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FlowGraph {
    private final HashMap<BasicBlock, List<BasicBlock>> nextBlocks = new HashMap<>();
    private final HashMap<BasicBlock, List<BasicBlock>> prevBlocks = new HashMap<>();

    public static FlowGraph getFlowGraph(Module module) {
        FlowGraph flowGraph = new FlowGraph();
        for (Function function : module.getFunctionList()) {
            for (BasicBlock block : function.getBBlockList()) {
                if (block.getTerminator() instanceof BranchInst) {
                    BranchInst inst = (BranchInst) block.getTerminator();
                    flowGraph.addToGraph(Pair.of(block, inst.getThenBlock()));
                    if (inst.getCond() != null) flowGraph.addToGraph(Pair.of(block, inst.getElseBlock()));
                }
            }
        }
        return flowGraph;
    }

    private void addToGraph(Pair<BasicBlock, BasicBlock> pair) {
        BasicBlock f = pair.getFirst(), s = pair.getSecond();
        if (!nextBlocks.containsKey(f)) nextBlocks.put(f, new ArrayList<>());
        nextBlocks.get(f).add(s);
        if (!prevBlocks.containsKey(s)) prevBlocks.put(s, new ArrayList<>());
        prevBlocks.get(s).add(f);
    }

    public List<BasicBlock> nextOf(BasicBlock block) {
        return nextBlocks.getOrDefault(block, null);
    }

    public List<BasicBlock> prevOf(BasicBlock block) {
        return prevBlocks.getOrDefault(block, null);
    }
}
