package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import java.util.ArrayList;
import java.util.List;

abstract class LogicGate implements LogicObject {
    /*
    Logic gates connect propositional nodes or other logic gates.
    They may have any number of children or parents that are nodes.
        Though this number can restricted by the type of gate.
        Not recording parents to keep memory down.
    Logic gates have no state themselves, but propagate a function of their parent states to their children.
     */

    public static int numberOfLogicGates = 0;
    PropNet propNet;

    List<Boolean> parentValues = new ArrayList<>();
    int parentReceived = 0;
    int numberParents = 0;
    List<PropNode> children = new ArrayList<>();

    public LogicGate(PropNet net) {
        propNet = net;
        numberOfLogicGates++;
    }

    public void clearObject() {
        for (PropNode child : children) {
            child.clearObject();
        }
    }

    protected void addChild(PropNode child) {
        children.add(child);
    }

    protected void parentAdded() {
        numberParents++;
    }

    protected void receiveUpdate(boolean value) {
        parentReceived++;
        parentValues.add(value);
    }

    protected final boolean receivedAllParents() {
        return parentReceived == numberParents;
    }

    protected abstract boolean calculateValue();

    public void propagate() {
        if (receivedAllParents()) {
            boolean value = calculateValue();
            for (PropNode child : children) {
                child.receiveUpdate(value);
                child.propagate();
            }

            // Reset the update flags.
            parentReceived = 0;
            parentValues.clear();
        }
    }
}

