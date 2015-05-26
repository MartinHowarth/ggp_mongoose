package org.ggp.base.player.gamer.statemachine.sample;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public final class MinimaxPlayer extends SampleGamer
{
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		// SampleLegalGamer is very simple : it picks the first legal move
//		Move selection = moves.get(0);
		Move selection = bestmove(getStateMachine(), getCurrentState(), getRole());

		// We get the end time
		// It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	Move bestmove (StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		List<Move> actions = theMachine.getLegalMoves(theState, theRole);
		Move action = actions.get(0);
		int score = 0;
		for (int i=0; i<actions.size(); i++){
			int result = minscore(theMachine, theState, theRole, actions.get(i), 0, 100);
			if (result==100) {
				return actions.get(i);
			}
			if (result>score) {
				score = result; action = actions.get(i);
			}
		}
		return action;
	}

	int maxscore (StateMachine theMachine, MachineState theState, Role theRole, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (theMachine.isTerminal(theState)) {
			return theMachine.getGoal(theState, theRole);
		}
		List<Move> actions = theMachine.getLegalMoves(theState, theRole);
		for (int i=0; i<actions.size(); i++){
	      	int result = minscore(theMachine, theState, theRole, actions.get(i), alpha, beta);
	      	alpha = Math.max(alpha,result);
	      	if (alpha>=beta) {
	      		return beta;
	      	}
		}
	    return alpha;
	}

	int minscore (StateMachine theMachine, MachineState theState, Role theRole, Move action, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Role> roles = theMachine.getRoles();
		// assume one opponent
		Role opponent = roles.get(1 - roles.indexOf(theRole));
		List<Move> actions = theMachine.getLegalMoves(theState, opponent);
		for (int i=0; i<actions.size(); i++){
			List<Move> jointMove = null;
			if (theRole == roles.get(0)) {
				jointMove.add(0, action);
				jointMove.add(1, actions.get(i));
			}
	        else {
				jointMove.add(0, actions.get(i));
				jointMove.add(1, action);
	        }
		    MachineState newstate = theMachine.getNextState(theState, jointMove);
	 	    int result = maxscore(theMachine, newstate, theRole, alpha, beta);
	 	    beta = Math.min(beta,result);
		    if (beta<=alpha){
			    return alpha;
		    }
		}
	    return beta;
	}
}