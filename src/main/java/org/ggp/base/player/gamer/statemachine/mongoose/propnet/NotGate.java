package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class NotGate extends LogicGate {
    /*
    The Not gate inverts the signal passed to it.
    It has only one parent.
     */

    public NotGate(PropNet net) {
        super(net);
    }

    protected boolean calculateValue() {
        return !parentValues.get(0);
    }

    protected void parentAdded() {
        super.parentAdded();
        if (numberParents > 1) {
            throw new IllegalStateException("Second parent added to Not gate.");
        }
    }

}
