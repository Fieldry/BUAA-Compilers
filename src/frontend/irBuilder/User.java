package frontend.irBuilder;

import java.util.List;

public class User extends Value {
    private List<Use> operandList;

    public Use getOperand(int index) { return operandList.get(index); }
    public int getNumOperands() { return operandList.size(); }
}
