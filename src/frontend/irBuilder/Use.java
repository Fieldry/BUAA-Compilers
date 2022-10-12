package frontend.irBuilder;

import frontend.inodelist.INode;

public class Use extends INode {
    User user;
    Value value;

    public Use(User user, Value value) {
        this.user = user;
        this.value = value;
    }
}
