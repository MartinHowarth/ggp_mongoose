package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class TerminalNode extends PropNode {
    /*
    The terminal node has no children.
    If the terminal node is true in any state, then that state is terminal.
     */

    public TerminalNode(PropNet net) {
        super(net);
        net.terminalNodes.add(this);
    }

    public void propagate() {
        /*
        Propagation ends at terminal nodes.
         */
    }
}
