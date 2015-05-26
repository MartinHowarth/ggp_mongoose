package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import java.util.ArrayList;
import java.util.List;

class PropNode implements LogicObject {
    /*
    Propositional nodes connect propositional gates.
    They have a boolean value.
    They always have exactly one parent gate.
        Not recorded for memory saving.
    They can have any number of child gates.
     */

    public static int numberOfPropNodes = 0;
    PropNet propNet;

    List<LogicGate> children = new ArrayList<>();
    boolean state = false;

    public PropNode(PropNet net) {
        propNet = net;
        numberOfPropNodes++;
    }

    public void clearObject() {
        state = false;
        for (LogicGate child : children) {
            child.clearObject();
        }
    }

    private void addChild(LogicGate child) {
        children.add(child);
        child.parentAdded();
    }

    protected void receiveUpdate(boolean value) {
        state = value;
    }

    public void propagate() {
        for (LogicGate child : children) {
            child.receiveUpdate(state);
            child.propagate();
        }

    }
}
