package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class OrGate extends LogicGate {
    /*
    The Or gate takes any number of children or parents.
    It propagates true if any parent is true.
     */

    public OrGate(PropNet net) {
        super(net);
    }

    protected boolean calculateValue() {
        for (boolean boo : parentValues) {
            if (boo) {
                return true;
            }
        }
        return false;
    }
}
