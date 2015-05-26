package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;

public class InputNode extends PropNode {
    /*
    Input nodes have no parents. I.e. they have no inputs.
    Each node corresponds to a legal move. (NB: This may change to be a set of *equivalent* legal inputs.)
     */

    public Role role;
    public Move move;
    public BaseNode legalNode;

    public InputNode(PropNet net, Role newRole, Move newMove, BaseNode legal) {
        super(net);
        net.propagationInitiators.add(this);
        role = newRole;
        move = newMove;
        legalNode = legal;

        net.inputMap.get(role).add(this);
        net.moveMap.get(net.getRoleIndex(role)).put(newMove, this);
    }

}
