package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import clojure.lang.Obj;
import org.ggp.base.util.gdl.grammar.GdlSentence;

import java.util.Set;

public class StatePropNet {

    PropNet propNet;
    Boolean[] baseValues;
    Boolean isTerminal;

    public StatePropNet(PropNet net) {
        propNet = net;
        baseValues = new Boolean[propNet.nBaseNodes];
        recordState();
    }

    void recordState() {
        for (int ii = 0; ii < propNet.nBaseNodes; ii++) {
            baseValues[ii] = propNet.baseNodes.get(ii).state;
        }
    }

    void applyState() {
        for (int ii = 0; ii < propNet.nBaseNodes; ii++) {
            propNet.baseNodes.get(ii).state = baseValues[ii];
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof StatePropNet)) {
            return false;
        }
        for (int ii = 0; ii < propNet.nBaseNodes; ii++) {
            if (baseValues[ii] != ((StatePropNet) other).baseValues[ii]) {
                return false;
            }
        }
        return true;
    }

    /**
     * getContents returns the GDL sentences which determine the current state
     * of the game being played. Two given states with identical GDL sentences
     * should be identical states of the game.
     */
    public Set<GdlSentence> getContents() {
        // TODO convert prop net state to gdl state
        Set<GdlSentence> contents = null;
        return contents;
    }
}
