package backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import backend.Registers.Register;
import midend.mir.ConstantInt;
import midend.mir.Value;

public class RegScheduler {
    private final ArrayList<Register> tempPool = new ArrayList<>(Registers.getTempRegisters());
    private final ArrayList<Register> globalPool = new ArrayList<>(Registers.getGlobalRegisters());
    private final ArrayList<Register> paramPool = new ArrayList<>(Registers.getParamRegisters());
    private final LinkedHashMap<String, Register> map = new LinkedHashMap<>();

    public Register allocGlobal(Value value) {
        if (!globalPool.isEmpty()) {
            Register r = globalPool.remove(0);
            map.put(value.getName(), r);
            return r;
        } else return null;
    }

    public Register allocTemp(Value value) {
        if (value instanceof ConstantInt) return Register.R1;
        Register r;
        if (!tempPool.isEmpty()) {
            r = tempPool.remove(0);
        } else {
            r = overflowTemp(value);
        }
        map.put(value.getName(), r);
        return r;
    }

    public Register overflowTemp(Value value) {
        tempPool.addAll(Registers.getTempRegisters());
        return tempPool.remove(0);
    }

    public Register allocParam() {
        if (!paramPool.isEmpty()) {
            return paramPool.remove(0);
        } else return null;
    }

    public Register find(Value value) {
        return map.get(value.getName());
    }
}
