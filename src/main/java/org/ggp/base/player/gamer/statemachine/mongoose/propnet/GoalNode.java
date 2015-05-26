package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import org.ggp.base.util.statemachine.Role;

public class GoalNode extends PropNode {
    /*
    Goal nodes have no children.
    At most one goal node should be true per role in the game in any state.
        In terminal states, exactly one goal node should be true per role.
     */

    public Role role;
    Integer goal;

    public GoalNode(PropNet net, Role newRole, Integer newGoal) {
        super(net);
        role = newRole;
        goal = newGoal;
        net.goalNodes.add(this);
    }

    public int getGoal() {
        return goal;
    }

    public void propagate() {
        /*
        Propagation ends at goal nodes.
         */
    }
}