package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class ViewNode extends PropNode {
    /*
    View nodes connect two gates.
    The incoming gate must not be a transition gate.
     */

    public ViewNode(PropNet net) {
        super(net);
    }

}
