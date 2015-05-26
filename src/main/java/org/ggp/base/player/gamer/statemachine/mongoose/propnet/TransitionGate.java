package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class TransitionGate extends LogicGate {
    /*
    The Transition gate is a wire with a 1 step time delay. (c.f. a flip flop)
    They only precede base proposition nodes.
    Their action is to "initialise" the child base node with their value from the previous step.
        This is how the propositional net preserves state as the game progresses.
     */

    boolean storedValue;

    public TransitionGate(PropNet net) {
        super(net);
        net.propagationInitiators.add(this);
    }

    protected boolean calculateValue() {
        return storedValue;
    }

    protected void parentAdded() {
        super.parentAdded();
        if (numberParents > 1) {
            throw new IllegalStateException("Second parent added to Transition gate.");
        }
    }

    public void propagate() {
        /*
        Propagation ends at transition gates.
        Store the received value to start the next step.
         */
        storedValue = parentValues.get(0);

        parentReceived = 0;
        parentValues.clear();
    }

}
