package org.ggp.base.util.statemachine.implementation.propnet;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;
import java.util.stream.Collectors;

public class PropNetStateMachine extends SamplePropNetStateMachine {
    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        applyState(state);

        return propNet.getTerminalProposition().getValue();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
//        return state.getGoalForRole(role);
//        // TODO store the value of the goal in the proposition - probably faster?
        applyState(state);

        for (Proposition p : propNet.getGoalPropositions().get(role)) {
            if (p.getValue()) {
                return getGoalValue(p);
            }
        }
        throw new GoalDefinitionException(state, role);
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
        System.out.println("Getting initial Prop net state machine.");
        applyState(null);
        // Update base propositions from their transitions.
        for (Proposition p : propNet.getBasePropositions().values()) {
            p.setValue(p.getSingleInput().getValue());
        }

        return getStateFromBase();
    }

    protected void applyState(MachineState state, boolean verbose) {
        if (verbose) {System.out.println("Applying state...");}
        // Clear out the current state.
        for (Component c : propNet.getPropositions()) {
            c.setValue(false);
        }

        // If state is null, then it is initial state.
        if (state != null) {
            if (verbose) {System.out.println("True bases: ");}
            for (GdlSentence sentence : state.getContents()) {

                Proposition baseProposition = propNet.getBasePropositions().get(sentence);
                baseProposition.setValue(true);
                if (verbose) {System.out.println(sentence);}
            }
        }
        else {
            propNet.getInitProposition().setValue(true);
        }
        propagatePropNet();
    }

    protected void applyState(MachineState state) {
        applyState(state, false);
    }

    /**
     * Computes the legal moves for role in state.
     */
    public List<Move> getLegalMoves(MachineState state, Role role, boolean verbose)
            throws MoveDefinitionException {
        if (verbose) {System.out.println("Prop net getting legal moves.");}
        applyState(state);

        List<Move> result = new ArrayList<>();
        Map<GdlSentence, Proposition> inputPropositions = propNet.getInputPropositions();

        for (Map.Entry<GdlSentence, Proposition> entry : inputPropositions.entrySet()) {
            Proposition legalProposition = propNet.getLegalInputMap().get(entry.getValue());
            if (legalProposition.getValue() && (propNet.getLegalPropositions().get(role).contains(legalProposition))) {
                if (verbose) {System.out.println("Legal: " + legalProposition.getName());}
                result.add(getMoveFromProposition(entry.getValue()));
            }
        }

        return result;
    }

    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
        return getLegalMoves(state, role, false);
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
        applyState(state);

        return getNextState(moves);
    }

    /**
     * Get the next state, starting from whatever state the propnet is currently in.
     * @param moves
     * @return
     * @throws TransitionDefinitionException
     */
    public MachineState getNextState(List<Move> moves)
            throws TransitionDefinitionException {
        // Set all input propositions to false.
        for (Proposition p : propNet.getInputPropositions().values()) {
            p.setValue(false);
        }

        // Get Gdl sentences that correspond to moves.
        List<GdlSentence> inputSentences = toDoes(moves);

        // Set corresponding input propositions to true.
        for (GdlSentence sentence : inputSentences) {
            propNet.getInputPropositions().get(sentence).setValue(true);
        }

        propagatePropNet();

        // Update base propositions from their transitions.
        for (Proposition p : propNet.getBasePropositions().values()) {
            p.setValue(p.getSingleInput().getValue());
        }

        return getStateFromBase();
    }

    protected void propagatePropNet(boolean verbose) {
        if (verbose) {System.out.println("Propagating prop net.");}
        for (Proposition p : ordering) {
            p.setValue(p.getSingleInput().getValue());
//            System.out.println("Setting " + p.getName() + " to: " + p.getValue());
            if (p.getValue()) {
                if (verbose) {System.out.println("True: " + p.getName());}
            }
        }
    }

    protected void propagatePropNet() {
        propagatePropNet(false);
    }

    protected void propagatePropNetOld() {
        System.out.println("Propagating prop net.");
        // Start from all input, init and base propositions and work through outputs.
        // Stop at terminal, goal and base propositions (rather, stop at their transition gate).

        // First update base propositions with the current value of their input transition gate.
        for (Proposition p : propNet.getBasePropositions().values())
        {
            // Transition gates have the value of the view node that is their input.
            p.setValue(p.getSingleInput().getValue());

        }

        // Then push each propositions value into their outputs recursively.
        Set<Component> propagationInitiators = new HashSet<>();
        propagationInitiators.add(propNet.getInitProposition());
        propagationInitiators.addAll(propNet.getInputPropositions().values().stream().collect(Collectors.toList()));

        propagationInitiators.addAll(propNet.getBasePropositions().values().stream().collect(Collectors.toList()));

        propagateListOfPropositions(propagationInitiators);


    }

    private void propagateListOfPropositions(Set<Component> props) {
        for (Component proposition : props) {
            // Stop propagation at terminal proposition.
            if (proposition == propNet.getTerminalProposition()) {
                System.out.println("Stopping at terminal node.");
                continue;
            }

            // Stop propagation at goal propositions.
            if (propNet.getAllGoalPropositions().contains(proposition)) {
                System.out.println("Stopping at goal node: " + proposition);
                continue;
            }

            if (proposition.getOutputs().size() == 0) {
                System.out.println("Missed end node: " + proposition);
            }

            // Propositions have logic gates as outputs.
            for (Component logicGate : proposition.getOutputs()) {
                // Stop propagation at transitions.
                if (logicGate instanceof Transition) {
                    System.out.println("Stopping at transition gate node: " + logicGate);
                    continue;
                }

                // Pass in values to the logic gate until it has received enough inputs.
                if (logicGate.receiveInput()) {
                    logicGate.resetCounter();
                    // Once we have enough information to fully calculate the gate, update its outputs.
                    for (Component out : logicGate.getOutputs()) {
                        out.setValue(logicGate.getValue());
                        if (logicGate.getValue() && out instanceof Proposition) {
                            System.out.println("True: " + out);
                        }
                    }
                    // Finally continue propagation from these propositions.
                    propagateListOfPropositions(logicGate.getOutputs());
                }
            }
        }
    }

}
