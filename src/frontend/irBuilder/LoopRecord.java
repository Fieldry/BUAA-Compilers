package frontend.irBuilder;

import java.util.ArrayList;

public class LoopRecord {
    public record Pair(String string, BasicBlock block) {
        public String getString() {
            return string;
        }

        public BasicBlock getBlock() {
            return block;
        }
    }

    private final ArrayList<Pair> records = new ArrayList<>();

    public void add(String string, BasicBlock block) {
        records.add(new Pair(string, block));
    }

    public ArrayList<Pair> getRecords() {
        return records;
    }
}
