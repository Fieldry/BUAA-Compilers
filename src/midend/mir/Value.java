package midend.mir;

import utils.inodelist.IList;
import utils.inodelist.INode;

public class Value extends INode {
    private final IList<Use> useList = new IList<>();
    protected Type type;

    /** The name of virtual register.
     */
    protected String name;

    /** The name of identifier if it exists.
     */
    protected String ident;

    public Value() {}

    public Value(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Value(Type type, String name, String ident) {
        this.type = type;
        this.name = name;
        this.ident = ident;
    }

    public void addUse(User user) { useList.addBack(new Use(user, this)); }

    public void addUse(Use use) { useList.addBack(use); }

    public int useSize() { return useList.size(); }

    public boolean useEmpty() { return useSize() == 0; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Type getType() { return type; }

    public String getIdent() { return ident; }

    @Override
    public String toString() {
        return name;
    }
}
