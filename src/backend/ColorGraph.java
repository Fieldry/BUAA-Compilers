package backend;

import java.util.*;

import midend.mir.Value;
import midend.LiveAnalysis;
import utils.Pair;
import backend.Registers.*;

public class ColorGraph {
    private final int K = 8;

    public static class ConflictGraph {
        private final HashMap<Value, HashSet<Value>> graph = new HashMap<>();
        private final HashMap<Value, Integer> degree = new HashMap<>();
    
        private void link(Pair<Value, Value> pair) {
            Value f = pair.getFirst(), s = pair.getSecond();
            if (!graph.containsKey(f)) graph.put(f, new HashSet<>());
            graph.get(f).add(s);
            if (!graph.containsKey(s)) graph.put(s, new HashSet<>());
            graph.get(s).add(f);
        }
    
        public static ConflictGraph getConflictGraph(LiveAnalysis liveAnalysis) {
            ConflictGraph conflictGraph = new ConflictGraph();
            Collection<HashSet<Value>> sets = liveAnalysis.getLiveIn().values();
        
            for (HashSet<Value> set : sets) {
                for (Value value : set) {
                    for (Value value2 : set) {
                        if (!value.equals(value2)) {
                            conflictGraph.link(Pair.of(value, value2));
                        }
                    }
                }
            }
            for (Value value : conflictGraph.getDegree().keySet()) {
                conflictGraph.getDegree().put(value, conflictGraph.getGraph().get(value).size());
            }
            return conflictGraph;
        }

        public void reduceDegree(Value value) {
            for (Value val : graph.get(value)) {
                degree.put(val, degree.get(val) - 1);
            }
            degree.remove(value);
        }

        public HashMap<Value, HashSet<Value>> getGraph() { return graph; }

        public HashMap<Value, Integer> getDegree() { return degree; }
    }

    private final ConflictGraph conflictGraph = ConflictGraph.getConflictGraph(null);
    private final Stack<Value> selectStack = new Stack<>();
    private final ArrayList<Value> spilledList = new ArrayList<>();
    private final ArrayList<Value> coloredList = new ArrayList<>();
    private final HashMap<Value, Register> allocate = new HashMap<>();

    public ColorGraph getColorGraph() {
        HashMap<Value, HashSet<Value>> graph = conflictGraph.getGraph();
        HashMap<Value, Integer> degree = conflictGraph.getDegree();
        boolean find;
        ArrayList<Register> registers = Registers.getGlobalRegisters();
        while (degree.size() > 0) {
            find = false;
            for (Value value : degree.keySet()) {
                if (degree.get(value) < K) {
                    selectStack.push(value);
                    conflictGraph.reduceDegree(value);
                    find = true;
                    break;
                }
            }
            if (!find) {
                for (Value value : degree.keySet()) {
                    spilledList.add(value);
                    conflictGraph.reduceDegree(value);
                    break;
                }
            }
        }

        while (!selectStack.empty()) {
            Value color = selectStack.pop();
            coloredList.add(color);
        }
        return null;
    }

}