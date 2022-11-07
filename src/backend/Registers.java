package backend;

import midend.mir.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Registers {
    public enum Register {
        R0("zero"), R1("at"),
        R2("v0"), R3("v1"),
        R4("a0"), R5("a1"), R6("a2"), R7("a3"),
        R8("t0"), R9("t1"), R10("t2"), R11("t3"),
        R12("t4"), R13("t5"), R14("t6"), R15("t7"),
        R16("s0"), R17("s1"), R18("s2"), R19("s3"),
        R20("s4"), R21("s5"), R22("s6"), R23("s7"),
        R24("t8"), R25("t9"),
        R26("k0"), R27("k1"),
        R28("gp"), R29("sp"), R30("fp"), R31("ra");

        private final String name;

        Register(String name) {
            this.name = name;
        }

        public String getName() { return name; }
    }

    private final static ArrayList<Register> globalRegisters = (ArrayList<Register>) Arrays.stream(Register.values())
            .filter(register -> register.name.contains("s")).toList();
    private final static ArrayList<Register> tempRegisters = (ArrayList<Register>) Arrays.stream(Register.values())
            .filter(register -> register.name.contains("t")).toList();
    public static ArrayList<Register> getGlobalRegisters() { return globalRegisters; }

    public static ArrayList<Register> getTempRegisters() { return tempRegisters; }

    public static class TempRegScheduler {
        private final ArrayList<Register> pool = new ArrayList<>(tempRegisters);
        private final ArrayList<Register> used = new ArrayList<>();
        private final HashMap<Register, Value> allocated = new HashMap<>();


        public Register allocReg(Value value) {
            Register r;
            if (!pool.isEmpty()) {
                r = pool.remove(0);
                used.add(0, r);
            } else {
                r = overflow(value);
            }
            allocated.put(r, value);
            return r;
        }

        public Register overflow(Value value) {

            return null;
        }

        public void clear() {
            allocated.clear();
            used.clear();
            pool.addAll(tempRegisters);
        }

        public Register find(Value value) {
            for (Register r : used) {
                if (allocated.get(r).equals(value)) {
                    return r;
                }
            }
            return null;
        }
    }

    public static class GlobalRegScheduler {
        private final ArrayList<Register> pool = new ArrayList<>(globalRegisters);
        private final ArrayList<Register> used = new ArrayList<>();
        private final HashMap<String, Register> inReg = new HashMap<>();
        private final HashMap<String, Address> inMem = new HashMap<>();

        public void allocReg(Value value) {

        }

        public Register overflow(Value value) {

            return null;
        }

        public void clear() {

        }

        public Register find(Value value) {
            return inReg.get(value.getName());
        }
    }
}
