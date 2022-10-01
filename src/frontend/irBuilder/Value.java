package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class Value {
    protected final List<Use> useList = new ArrayList<>();
    protected Type type;
    protected String name;

    public Value() {}

    public Value(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Value(Type type, String name, User user) {
        this.type = type;
        this.name = name;
        addUse(new Use(user, this));
    }

    public void addUse(User user) { useList.add(new Use(user, this)); }

    public void addUse(Use use) { useList.add(use); }

    public int useSize() { return useList.size(); }

    public boolean useEmpty() { return useSize() == 0; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Type getType() { return type; }

    @Override
    public String toString() {
        return name;
    }
}
