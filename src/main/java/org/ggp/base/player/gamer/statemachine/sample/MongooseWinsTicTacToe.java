package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public class MongooseWinsTicTacToe extends SampleGamer {
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;
		int myRoleIndex = theMachine.getRoles().indexOf(getRole());
		boolean foundMove = false;

		Move selection = null;
		List<Move> moves;

		// If we can win immediately, return a random winning move.
		moves = findVictoryMovesFromState(theMachine, getCurrentState(), getRole());
		if (moves.size() > 0) {
			selection = (moves.get(new Random().nextInt(moves.size())));
			System.out.println("VICTORY move found: " + selection);
			foundMove = true;
		}

		// Find any moves that would allow an
		// opponent to win immediately (and uncontested) the turn after.
		// Exclude these from subsequent considerations.
		if (!foundMove) {
			moves = theMachine.getLegalMoves(getCurrentState(), getRole());
			List<Move> badMoves = new ArrayList<Move>();

			// For all possible states after my move
			for (List<Move> jointMove : theMachine.getLegalJointMoves(getCurrentState())) {
				MachineState state = theMachine.getNextState(getCurrentState(), jointMove);
				// Check that no other role can win immediately.
				for (Role role : theMachine.getRoles()) {
					if (role != getRole()) {
						if (findVictoryMovesFromState(theMachine, state, role).size() > 0) {
							badMoves.add(jointMove.get(myRoleIndex));
							System.out.println("Excluding bad move: " + jointMove.get(myRoleIndex));
						}
					}
				}
			}

			// Recreate moves without badMoves
			List<Move> tempMoves = new ArrayList<Move>();
			for (Move move : moves) {
				if (!badMoves.contains(move)) {
					tempMoves.add(move);
				}
			}
			moves = tempMoves;

		}

		// Do monte-carlo on all remaining legal moves.
//		if (selection != null) {
		System.out.println("Starting Monte-Carlo...");
		int count = 0;
		if (!foundMove) {
			selection = moves.get(0);
			if (moves.size() > 1) {
	    		int[] moveTotalPoints = new int[moves.size()];
	    		int[] moveTotalAttempts = new int[moves.size()];

	    		// Perform depth charges for each candidate move, and keep track
	    		// of the total score and total attempts accumulated for each move.
	    		for (int i = 0; true; i = (i+1) % moves.size()) {
	    		    if (System.currentTimeMillis() > finishBy)
	    		        break;

	    		    int theScore = performDepthChargeFromMove(getCurrentState(), moves.get(i));
	    		    moveTotalPoints[i] += theScore;
	    		    moveTotalAttempts[i] += 1;
	    		    if (i == 0) { count = count + 1;}
	    		}

	    		// Compute the expected score for each move.
	    		double[] moveExpectedPoints = new double[moves.size()];
	    		for (int i = 0; i < moves.size(); i++) {
	    		    moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];
	    		}

	    		// Find the move with the best expected score.
	    		int bestMove = 0;
	    		double bestMoveScore = moveExpectedPoints[0];
	    		for (int i = 1; i < moves.size(); i++) {
	    		    if (moveExpectedPoints[i] > bestMoveScore) {
	    		        bestMoveScore = moveExpectedPoints[i];
	    		        bestMove = i;
	    		    }
	    		}
	    		selection = moves.get(bestMove);
			}
		}
		System.out.println(count);
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private int[] depth = new int[1];
	int performDepthChargeFromMove(MachineState theState, Move myMove) {
	    StateMachine theMachine = getStateMachine();
	    try {
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(theState, getRole(), myMove), depth);
            return theMachine.getGoal(finalState, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}

	List<Move> findVictoryMovesFromState(StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// Check all moves at given state that lead to immediate victory for given role.
		// Returns a list of all moves that give victory.

		List<Move> victoryMoves = new ArrayList<Move>();
		List<Move> possibleMoves = theMachine.getLegalMoves(theState, theRole);

		for (Move move : possibleMoves) {
			// Get list of possible joint moves that can be taken from this state.
			List<List<Move>> possibleJointMoves = theMachine.getLegalJointMoves(theState, theRole, move);

			// If we win in every possible joint move, then the Move is good. Return the move.
			boolean certainVictory = true;
			boolean victoryMove = false;
			for (List<Move> jointMove : possibleJointMoves) {
				// Get the resulting state for the joint move
				MachineState nextState = theMachine.getNextState(theState, jointMove);
				// Check if this state results in victory for the given role
				if (isVictoryForRole(theMachine, nextState, theRole)) {
					victoryMove = true;
				}
				else {
					certainVictory = false;
				}
			}

			if (certainVictory && victoryMove) {
				victoryMoves.add(move);
			}
		}

		return victoryMoves;
	}

	boolean isVictoryForRole(StateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {
		// Check if the given role is victorious in the given state.
		// Return true is so, otherwise return false.
		if (theMachine.isTerminal(theState)) {
			List<Role> roles = theMachine.getRoles();
			List<Integer> goals = theMachine.getGoals(theState);

			int theRoleIndex = roles.indexOf(theRole);
			int theRoleGoal = goals.get(theRoleIndex);

			for (int ii = 0; ii < roles.size(); ii++) {
				if (goals.get(ii) > theRoleGoal) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
}