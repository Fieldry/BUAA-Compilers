package frontend.irBuilder;

import frontend.inodelist.IList;

public class User extends Value {
    private final IList<Use> operandList = new IList<>();

    public User() {}

    public User(Type type, String name) {
        super(type, name);
    }

    public User(Type type, String name, Value lValue, Value rValue) {
        super(type, name);
        addOperand(lValue);
        addOperand(rValue);
    }

    public void addOperand(Value value) { operandList.addBack(new Use(this, value)); }

    public int getNumOperands() { return operandList.size(); }

    public IList<Use> getOperandList() { return operandList; }
}
