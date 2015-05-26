package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.*;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    protected PropNet propNet;
    /** The topological ordering of the propositions */
    protected List<Proposition> ordering;
    /** The player roles */
    protected List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description);
	        roles = propNet.getRoles();
	        ordering = getOrdering();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		// TODO: Compute whether the MachineState is terminal.
		return false;
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
		// TODO: Compute the goal for role in state.
		return -1;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		return null;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// TODO: Compute legal moves.
		return null;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// TODO: Compute the next state.
		return null;
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
    public List<Proposition> getOrdering() {
        return getOrdering(false);
    }
    public List<Proposition> getOrdering(boolean verbose) {
        System.out.println("Getting ordering of propnet.");
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();
        List<Component> tempOrder = new LinkedList<>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        Collection<Component> nextComs = new HashSet<>();
        Collection<Component> newNextComs = new HashSet<>();
        nextComs.add(propNet.getInitProposition());
        for (Component com : propNet.getInitProposition().getOutputs()) {
            Proposition prop = (Proposition) com.getSingleOutput().getSingleOutput();
            if (verbose) {System.out.println(prop.getName());}
        }
        nextComs.addAll(propNet.getInputPropositions().values());
        nextComs.addAll(propNet.getBasePropositions().values());

        int iterationNumber = 0;
        int lastSize = 0;
        System.out.println("Prop net size: " + propNet.getComponents().size());
        // Add all components of the prop net to tempOrder in the correct order.
        // Later remove all unwanted components.
        while (nextComs.size() > 0) {
            if (verbose) {System.out.println("Interation number: " + iterationNumber);}
                if (verbose) {System.out.println("Temp Order size: " + tempOrder.size());}
            for (Component com : nextComs) {
                // We may need to wait until all inputs have been updated.
                if (com.getInputs().size() > 1) {
                    // Work out if all the inputs have been updated already (i.e. already in order)
                    boolean allInputs = true;
                    for (Component inp : com.getInputs()) {
                        if (!tempOrder.contains(inp)) {
                            allInputs = false;
                        }
                    }
                    // If all the inputs are already updated, continue adding outputs.
                    if (allInputs) {
                        for (Component com2 : com.getOutputs()){
                            if (!tempOrder.contains(com2)) {
                                tempOrder.add(com2);
                                if (verbose) {
                                    if (com2 instanceof Proposition) {
                                        System.out.println(((Proposition) com2).getName());
                                    }
                                }
                            }
                        }
                        newNextComs.addAll(com.getOutputs());
                    }
                    // Otherwise try again next cycle.
                    else {
                        newNextComs.add(com);
                    }
                }
                else {
                    for (Component com2 : com.getOutputs()){
                        if (!tempOrder.contains(com2)) {
                            tempOrder.add(com2);
                            if (verbose) {
                                if (com2 instanceof Proposition) {
                                    System.out.println(((Proposition) com2).getName());
                                }
                            }
                        }
                    }
                    newNextComs.addAll(com.getOutputs());
                }
            }
            if (lastSize == tempOrder.size()) {
                System.out.println("No new nodes added. Ending initial ordering.");
                break;
            }
            else {
                lastSize = tempOrder.size();
            }

            // Set nextComs to be newNextComs, but don't do it by reference.
            nextComs.clear();
            nextComs.addAll(newNextComs);
            newNextComs.clear();
            iterationNumber++;
        }

        // Now take all the components that we didn't find above, and add them:
        // - Before the component which is their first occuring output.
        // - After the component which is their last occuring input.
        // Otherwise add to end of list.
        components.removeAll(tempOrder);
        List<Component> nextComponents = new ArrayList<Component>();
        while (components.size() > 0) {
            for (Component com : components) {
                boolean placeFound = false;

                // Add before first output.
                int outputIndex = tempOrder.size() + 10;
                for (Component out : com.getOutputs()) {
                    if (tempOrder.contains(out)) {
                        if (outputIndex > tempOrder.indexOf(out)) {
                            outputIndex = tempOrder.indexOf(out);
                            placeFound = true;
                        }
                    }
                }
                if (placeFound) {
                    tempOrder.add(outputIndex, com);
                    continue;
                }

                // Add after last input
                int inputIndex = 0;
                for (Component inp : com.getInputs()) {
                    if (tempOrder.contains(inp)) {
                        if (inputIndex < tempOrder.indexOf(inp)) {
                            inputIndex = tempOrder.indexOf(inp);
                            placeFound = true;
                        }
                    }
                }
                if (placeFound) {
                    tempOrder.add(inputIndex + 1, com);
                    continue;
                }

                // Otherwise consider it next time.
                nextComponents.add(com);

            }
            components.clear();
            components.addAll(nextComponents);
            nextComponents.clear();

            if (lastSize == tempOrder.size()) {
                System.out.print("Couldn't find any more connected nodes. Adding this (almost certainly from entirely separate net): ");
                if (components.get(0) instanceof Proposition){
                    System.out.println(((Proposition) components.get(0)).getName());
                }
                else {
                    System.out.println(components.get(0));
                }
                tempOrder.add(0, components.get(0));
                components.remove(0);
            }
            lastSize = tempOrder.size();


        }

        if (verbose) {System.out.println("Temp Order size: " + tempOrder.size());}

        // Finally remove all the components that shouldn't be in the ordering.
        // I.e. the input, init and base propositions, and all logic gates.
        tempOrder.remove(propNet.getInitProposition());
        tempOrder.removeAll(propNet.getInputPropositions().values());
        tempOrder.removeAll(propNet.getBasePropositions().values());

        List<Component> toRemove = new LinkedList<>();
        for (Component com : tempOrder) {
            if (!(com instanceof Proposition)) {
                toRemove.add(com);
            }
        }
        tempOrder.removeAll(toRemove);

        for (Component com : tempOrder) {
            if (com instanceof Proposition) {
                order.add((Proposition) com);
                if (verbose) {System.out.println("Added: " + ((Proposition) com).getName());}
            }
            else {
                System.out.println("Error in creating propnet: " + com);
            }
        }

        System.out.println("Number of nodes to update each iteration: " + order.size());

        return order;
    }

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	protected List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    protected int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
//			p.setValue(p.getSingleInput().getValue());

			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}

		return new MachineState(contents);
	}
}