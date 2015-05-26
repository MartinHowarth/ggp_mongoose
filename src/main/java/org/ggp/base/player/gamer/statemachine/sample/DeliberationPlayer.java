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

public final class DeliberationPlayer extends SampleGamer
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

	Move bestmove (StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> moves = theMachine.getLegalMoves(theState, theRole);
		Move move = moves.get(0);
		int score = 0;
		for (int i=0; i<moves.size(); i++) {
			List<Move> randJointAction = theMachine.getRandomJointMove(theState, theRole, moves.get(i));
			int result = maxscore(theMachine, theMachine.getNextState(theState, randJointAction), theRole);
			if (result==100) {
				return moves.get(i);
			}
			if (result>score) {
				score = result;
				move = moves.get(i);
			}
		}
		return move;
	}

	int maxscore (StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (theMachine.isTerminal(theState)) {
			return theMachine.getGoal(theState, theRole);
		}
		List<Move> actions = theMachine.getLegalMoves(theState, theRole);
		int score = 0;
		for (int i=0; i<actions.size(); i++) {
			List<Move> randJointAction = theMachine.getRandomJointMove(theState, theRole, actions.get(i));
			int result = maxscore(theMachine, theMachine.getNextState(theState, randJointAction), theRole);
			if (result>score) {
				score = result;
			}
		}
		return score;
	}
}