package backend;

import backend.MIPSCode.*;
import midend.mir.BasicBlock;
import midend.mir.Function;
import midend.mir.Module;
import utils.Pair;
import utils.inodelist.INode;
import backend.Registers.*;

public class MulDivOptimizer {
    private static final int N = 32;
    public static class Factor {
        private long mulFactor;
        private int srlFactor;

        public Factor(long mulFactor, int srlFactor) {
            this.mulFactor = mulFactor;
            this.srlFactor = srlFactor;
        }
    }

    public static int clz(long x) {
        assert x != 0;
        int res = 0, high = 0x80000000;
        while ((x & high) == 0) {
            res++;
            x <<= 1;
        }
        return res;
    }
    public static int crz(long x) {
        assert x != 0;
        int res = 0, low = 1;
        while ((x & low) == 0) {
            res++;
            x >>= 1;
        }
        return res;
    }
    public static Pair<Integer, Integer> bit(long x) {
        int first = -1, second = -1, res = 0;
        while (x > 0) {
            if ((x & 1) > 0) {
                if (first == -1) first = res;
                else if (second == -1) second = res;
                else return null;
            }
            res++;
            x >>= 1;
        }
        return Pair.of(first, second);
    }

    public static Factor getFactor(long d, int p) {
        assert d != 0;
        assert (p >= 1 && p <= N);
        int l = N - clz(d - 1);
        long low = (1L << (N + l)) / d;
        long high = (((1L << (N + l)) + (1L << (N + l - p)))) / d;
        while ((low >> 1) < (high >> 1) && l > 0) {
            low >>= 1;
            high >>= 1;
            l--;
        }
        return new Factor(high, l);
    }

    public static void optDiv(BinaryRegImmCode inst) {
        long value = inst.getImm().getValue();
        Register rt = inst.getRt(), rs = inst.getRs();
        ImmNum imm = inst.getImm();
        assert value != 0;
        {
            int lo = crz(value);
            if (inst.isMod()) {
                inst.insertAfter(BinaryRegRegCode.subCode(rt, rs, Register.R1));
                inst.insertAfter(BinaryRegImmCode.mulCode(Register.R1, rt, imm));
            }
            if (value == (1L << lo)) {
                if(lo > 0) inst.insertAfter(BinaryRegImmCode.srlCode(rt, rs, new ImmNum(lo)));
                else inst.insertAfter(new MoveCode(rt, rs));
            } else {
                Factor factor = getFactor(value, N);
                if (factor.mulFactor < (1L << N)) lo = 0;
                else factor = getFactor(value >> lo, N - lo);
                if (factor.mulFactor < (1L << N)) {
                    if (factor.srlFactor > 0)
                        inst.insertAfter(BinaryRegImmCode.srlCode(rt, rt, new ImmNum(factor.srlFactor)));
                    inst.insertAfter(new MoveFromCode("hi", rt));
                    if (lo > 0) {
                        inst.insertAfter(BinaryRegImmCode.mulCode(rt, rt, new ImmNum(factor.mulFactor)));
                        inst.insertAfter(BinaryRegImmCode.srlCode(rt, rs, new ImmNum(lo)));
                    } else inst.insertAfter(BinaryRegImmCode.mulCode(rt, rs, new ImmNum(factor.mulFactor)));
                } else {
                    inst.insertAfter(BinaryRegImmCode.srlCode(rt, rt, new ImmNum(factor.srlFactor - 1)));
                    inst.insertAfter(BinaryRegRegCode.addCode(rt, rt, Register.R1));
                    inst.insertAfter(BinaryRegImmCode.srlCode(rt, rt, ImmNum.OneImm));
                    inst.insertAfter(BinaryRegRegCode.subCode(rt, rs, Register.R1));
                    inst.insertAfter(new MoveFromCode("hi", Register.R1));
                    inst.insertAfter(BinaryRegImmCode.mulCode(Register.R1, rs,
                            new ImmNum(factor.mulFactor - (1L << N))));
                }
            }
            inst.remove();
        }
    }

    public static void optMul(BinaryRegImmCode inst) {
        long value = inst.getImm().getValue();
        Register rt = inst.getRt(), rs = inst.getRs();
        if (value == 0) {
            inst.insertAfter(new LoadImmCode(rt, ImmNum.ZeroImm));
        } else {
            Pair<Integer, Integer> pair = bit(value);
            if (pair == null) {
                pair = bit(value + 1);
                if (pair != null && pair.getSecond() == -1) {
                    inst.insertAfter(BinaryRegRegCode.subCode(rt, Register.R1, rs));
                    inst.insertAfter(BinaryRegImmCode.sllCode(Register.R1, rs, new ImmNum(pair.getFirst())));
                } else {
                    return;
                }
            } else {
                if (pair.getSecond() != -1) {
                    int first = pair.getFirst(), second = pair.getSecond();
                    if (first != 0) {
                        inst.insertAfter(BinaryRegRegCode.addCode(rt, rt, Register.R1));
                        inst.insertAfter(BinaryRegImmCode.sllCode(rt, rs, new ImmNum(second)));
                        inst.insertAfter(BinaryRegImmCode.sllCode(Register.R1, rs, new ImmNum(first)));
                    } else {
                        inst.insertAfter(BinaryRegRegCode.addCode(rt, rt, rs));
                        inst.insertAfter(BinaryRegImmCode.sllCode(rt, rs, new ImmNum(second)));
                    }
                } else if (pair.getFirst() != -1) {
                    int first = pair.getFirst();
                    if (first != 0) inst.insertAfter(BinaryRegImmCode.sllCode(rt, rs, new ImmNum(first)));
                    else inst.insertAfter(new MoveCode(rt, rs));
                }
            }
        }
        inst.remove();
    }

    public static void optimize(Module module) {
        for (Function function : module.getFunctionList()) {
            for (BasicBlock block : function.getBBlockList()) {
                for (INode iNode : block.getInstList()) {
                    if (iNode instanceof BinaryRegImmCode) {
                        BinaryRegImmCode inst = (BinaryRegImmCode) iNode;
                        if (inst.isMul()) {
                            optMul(inst);
                        } else if (inst.isDiv()) {
                            optDiv(inst);
                        }
                    }
                }
            }
        }
    }
}
