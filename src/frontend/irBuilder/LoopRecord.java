package frontend.irBuilder;

import java.util.ArrayList;

public class LoopRecord {
    public static class Pair {
        private final String string;
        private final BasicBlock block;

        public Pair(String s, BasicBlock b) {
            string = s;
            block = b;
        }

        public String getString() {
            return string;
        }

        public BasicBlock getBlock() {
            return block;
        }
    }

    private final ArrayList<Pair> records = new ArrayList<>();

    private BasicBlock condBlock;

    public void add(String string, BasicBlock block) {
        records.add(new Pair(string, block));
    }

    public ArrayList<Pair> getRecords() {
        return records;
    }

    public void setCondBlock(BasicBlock condBlock) {
        this.condBlock = condBlock;
    }

    public BasicBlock getCondBlock() {
        return condBlock;
    }
}
