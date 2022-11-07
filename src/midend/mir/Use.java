package midend.mir;

import utils.inodelist.INode;

public class Use extends INode {
    User user;
    Value value;

    public Use(User user, Value value) {
        this.user = user;
        this.value = value;
    }
}
