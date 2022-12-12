package backend;

import midend.mir.Value;

import java.util.ArrayList;
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

        Register(String name) { this.name = "$" + name; }

        public String getName() { return name; }

        public boolean isTemp() { return name.matches("\\$t[0-9]"); }   // 10

        public boolean isGlobal() {
            return name.matches("\\$s[0-7]") || name.matches("\\$k[0-1]")
                    || name.equals("fp") || name.equals("v1"); // 12
        }

        public boolean isParam() { return name.matches("\\$a[1-3]"); }  // 3

        @Override
        public String toString() { return name; }
    }

    private final static ArrayList<Register> globalRegisters = new ArrayList<Register>(){};
    private final static ArrayList<Register> tempRegisters = new ArrayList<Register>() {{
        for (Register register : Register.values())
            if (register.isTemp() || register.isGlobal() || register.isParam()) add(register);
    }};
    private final static ArrayList<Register> paramRegisters = new ArrayList<Register>() {{
        for (Register register : Register.values()) if (register.isParam()) add(register);
    }};
    public static ArrayList<Register> getGlobalRegisters() { return globalRegisters; }
    public static ArrayList<Register> getTempRegisters() { return tempRegisters; }
    public static ArrayList<Register> getParamRegisters() { return paramRegisters; }
}
