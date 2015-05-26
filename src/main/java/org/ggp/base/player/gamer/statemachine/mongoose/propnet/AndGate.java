package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

public class AndGate extends LogicGate {
    /*
        The And gate takes any number of parents or children.
        It propagates true if all parents are true.
         */

    public AndGate(PropNet net) {
        super(net);
    }

    protected boolean calculateValue() {
        for (boolean boo : parentValues) {
            if (!boo) {
                return false;
            }
        }
        return true;
    }
}
