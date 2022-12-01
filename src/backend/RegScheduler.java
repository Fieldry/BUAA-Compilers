package backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import backend.Registers.Register;
import midend.mir.Value;

public class RegScheduler {
    private final ArrayList<Register> tempPool = new ArrayList<>(Registers.getTempRegisters());
    private final ArrayList<Register> globalPool = new ArrayList<>(Registers.getGlobalRegisters());
    // private final ArrayList<Register> paramPool = new ArrayList<>(Registers.getParamRegisters());
    private final LinkedHashMap<Register, String> tempMap = new LinkedHashMap<>();
    private final LinkedHashMap<Register, String> globalMap = new LinkedHashMap<>();

    public Register allocGlobal(Value value) {
        if (!globalPool.isEmpty()) {
            Register r = globalPool.remove(0);
            globalMap.put(r, value.getName());
            return r;
        } else return null;
    }

    public boolean usedGlobal(Register register) {
        return globalMap.containsKey(register);
    }

    public Register allocTemp(Value value) {
        Register r;
        if (!tempPool.isEmpty()) {
            r = tempPool.remove(0);
        } else {
            r = overflowTemp(value);
        }
        tempMap.put(r, value.getName());
        return r;
    }

    public Register allocTemp() {
        Register r;
        if (!tempPool.isEmpty()) {
            r = tempPool.remove(0);
        } else {
            r = overflowTemp(null);
        }
        tempMap.put(r, null);
        return r;
    }

    public Register overflowTemp(Value value) {
        tempPool.addAll(Registers.getTempRegisters());
        return tempPool.remove(0);
    }

//    public Register allocParam(Value value) {
//        if (!paramPool.isEmpty()) {
//            Register r = paramPool.remove(0);
//            map.put(value.getName(), r);
//            return r;
//        } else return null;
//    }
//
//    public void clearParam() {
//        paramPool.clear();
//        paramPool.addAll(Registers.getParamRegisters());
//    }

    public Register find(Value value) {
        if (value.getName() == null) return null;
        Register reg;
        for (Map.Entry<Register, String> entry : globalMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(value.getName())) {
                reg = entry.getKey();
                // globalPool.add(Math.max(globalPool.size() - 1, 0), reg);
                return reg;
            }
        }
        for (Map.Entry<Register, String> entry : tempMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().equals(value.getName())) {
                reg = entry.getKey();
                // tempPool.add(Math.max(tempPool.size() - 1, 0), reg);
                return reg;
            }
        }
        return null;
    }

    public void freeTemp(Register register) {
        if (register.isTemp()) tempPool.add(Math.max(tempPool.size() - 1, 0), register);
    }
}
