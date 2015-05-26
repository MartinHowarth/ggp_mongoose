package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public class Mongoose_1 extends SampleGamer {
	public boolean debug = true;
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		StateMachine theMachine = getStateMachine();

		MyResult bestResult = followBranch(theMachine, getCurrentState(), getRole(), 1, 10);

		if (bestResult.getMove() == null){
			System.out.println("Didn't find a favourable state.");
			return theMachine.getLegalMoves(getCurrentState(), getRole()).get(0);
		}
		return bestResult.getMove();
	}

	private MyResult followBranch(StateMachine theMachine, MachineState state, Role role, int depth, int nOps)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		// For the given state, get possible terminal states that involve one more move for the given role.
		// If no such states are found, call this function again for every non-terminal state.
		// Return minimum number of moves taken to reach a victory state.
		// Return 0 if the state is not victory.

		Role myRole = getRole();
		Map stepsToVictory = new HashMap();
		List<Move> possibleMoves;

		// Get list of possible states after opponent has taken their move
		try {
			possibleMoves = theMachine.getLegalMoves(state, role);
		}
		catch (MoveDefinitionException e) {
			if (debug){
				System.out.println("No legal moves. Must be end of tree?");
			}
			return null;
		}

		Map<Move, List<MachineState>> nextStates = theMachine.getNextStates(state, role);
		List<MyResult> results = new ArrayList<MyResult>();

//		for (Map.Entry<Move, List<MachineState>> testEntry: nextStates.entrySet()){
		for (int ii = 0; ii < nOps && ii < possibleMoves.size(); ii++){
			// For each set of states after opponents turn, look for the one that leads to quickest victory
			Move testMove = possibleMoves.get(ii);
			List<MachineState> testStates = nextStates.get(testMove);

			// For each possible resulting state, repeat
			for (MachineState testState: testStates){
				if (isVictory(theMachine, testState, role)){
					return new MyResult(depth, testMove);
				}
				results.add(followBranch(theMachine, testState, role, depth + 1, nOps));
			}
		}
		int bestResult = Integer.MAX_VALUE;
		Move bestMove = null;

		for (MyResult result : results){
			if (result != null) {
				if (result.getDepth() < bestResult){
					bestResult = result.getDepth();
					bestMove = result.getMove();

					if (debug){
						System.out.println(bestMove);
					}
				}
			}
		}

		return new MyResult(bestResult, bestMove);
	}


	private Boolean isVictory(StateMachine theMachine, MachineState state, Role role){
		// Get goals of all players for the given state.
		// Return true if given role has the highest score (or equal highest).
		// Return false if state is not terminal (no goal exists) or if given role does not have highest score.
		try {
			List<Integer> goals = theMachine.getGoals(state);
			List<Role> roles = theMachine.getRoles();
			int myRoleIndex = roles.indexOf(role);
			int myGoal = goals.get(myRoleIndex);

			for (int ii = 0; ii < goals.size(); ii++){
				if (ii != myRoleIndex){
					if (goals.get(ii) > myGoal) {
						return false;
					}
				}
			}

		} catch (GoalDefinitionException e) {
			return false;
		}

		return true;
	}
}



final class MyResult {
    private final int depth;
    private final Move move;

    public MyResult(int depth, Move move) {
        this.depth = depth;
        this.move = move;
    }

    public int getDepth() {
        return this.depth;
    }

    public Move getMove() {
        return this.move;
    }
}