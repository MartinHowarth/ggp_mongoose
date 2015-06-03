package org.ggp.base.util.statemachine.implementation.propnet;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class PropNetStateMachine extends SamplePropNetStateMachine {
    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        applyState(state);

        return isTerminal();
    }

    public boolean isTerminal() {
        return propNet.getTerminalProposition().getValue();
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
        applyState(state);

        int val = getGoal(role);
        if (val == -1) {
            throw new GoalDefinitionException(state, role);
        }
        else {
            return val;
        }
    }

    public int getGoal(Role role)
            throws GoalDefinitionException {
        for (Proposition p : propNet.getGoalPropositions().get(role)) {
            if (p.getValue()) {
                return getGoalValue(p);
            }
        }
        return -1;
    }

    protected int getGoalValue(Proposition goalProposition)
    {
        return goalProposition.goal;
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

    public void applyState(MachineState state) {
        applyState(state, false);
    }

    /**
     * Computes the legal moves for role in state.
     */
    public List<Move> getLegalMoves(MachineState state, Role role, boolean verbose)
            throws MoveDefinitionException {
        if (verbose) {System.out.println("Prop net getting legal moves.");}
        applyState(state);
        return getLegalMoves(role, verbose);
    }

    public List<Move> getLegalMoves(Role role, boolean verbose)
            throws MoveDefinitionException {
        if (verbose) {System.out.println("Prop net getting legal moves.");}

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

    public List<Move> getLegalMoves(Role role) throws MoveDefinitionException {
        return getLegalMoves(role, false);
    }

    /**
     * Returns a random move from among the possible legal moves for the
     * given role in the given state.
     */
    @Override
    public Move getRandomMove(MachineState state, Role role) throws MoveDefinitionException
    {
        List<Move> legals = getLegalMoves(state, role);
        return legals.get(new Random().nextInt(legals.size()));
    }

    /**
     * Returns a random move from among the possible legal moves for the
     * given role in the given state.
     */
    public Move getRandomMove(Role role) throws MoveDefinitionException
    {
        List<Move> legals = getLegalMoves(role);
        return legals.get(new Random().nextInt(legals.size()));
    }

    /**
     * Returns a random joint move from among all the possible joint moves in
     * the given state.
     */
    public List<Move> getRandomJointMove() throws MoveDefinitionException
    {
        List<Move> random = new ArrayList<Move>();
        for (Role role : getRoles()) {
            random.add(getRandomMove(role));
        }

        return random;
    }

    public List<List<Move>> getLegalJointMoves() throws MoveDefinitionException
    {
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role role : getRoles()) {
            legals.add(getLegalMoves(role));
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

        return crossProduct;
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
        nextState(moves);

        return getStateFromBase();
    }


    /**
     * Puts the prop net into the next state after given moves are made.
     * @param moves
     */
    public void nextState(List<Move> moves) {
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
    }

    @Override
    public MachineState performDepthCharge(MachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        applyState(state);
//        while(!isTerminal(state)) {
//            nDepth++;
//            getNextState(state, getRandomJointMove(state));
//        }
        while(!isTerminal()) {
            nDepth++;
            nextState(getRandomJointMove());
        }
        if(theDepth != null)
            theDepth[0] = nDepth;
        System.out.println(nDepth);
        return getStateFromBase();
//        return state;
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
}
