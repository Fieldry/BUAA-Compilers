package frontend.irBuilder;

import java.util.ArrayList;
import java.util.List;

public class User extends Value {
    protected final List<Use> operandList = new ArrayList<>();

    public User() {}

    public User(Type type, String name) {
        super(type, name);
    }

    public User(Type type, String name, Value lValue, Value rValue) {
        super(type, name);
        addOperand(lValue);
        addOperand(rValue);
    }

    public void addOperand(Value value) { operandList.add(new Use(this, value)); }

    public Use getOperand(int index) { return operandList.get(index); }

    public int getNumOperands() { return operandList.size(); }
}
