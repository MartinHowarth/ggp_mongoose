package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class BaseNode extends PropNode {
    /*
    The base node keeps a value from the previous iteration. This is populated by its parent transition gate.
    Base nodes always have a transition gate as a parent.
     */

    public BaseNode(PropNet net) {
        super(net);
        net.baseNodes.add(this);
        net.nBaseNodes++;
    }

}
