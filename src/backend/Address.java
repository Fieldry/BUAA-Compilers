package backend;

import backend.Registers.*;

public class Address {
    public static class BaseAddress extends Address {
        private final Register base;
        private final ImmNum imm;

        public BaseAddress(Register base, ImmNum imm) {
            this.base = base;
            this.imm = imm;
        }

        @Override
        public String toString() {
            return imm + "(" + base + ")";
        }
    }

    public static class LabelAddress extends Address {
        private final Label label;
        private final Register reg;

        public LabelAddress(Label label, Register reg) {
            this.label = label;
            this.reg = reg;
        }

        @Override
        public String toString() {
            return label + "(" + reg + ")";
        }
    }
}
