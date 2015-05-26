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

public class heuristicPlayer_ex73 extends SampleGamer {
    int searchDepth = 2;
    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
        // We get the current start time
        long start = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

        // SampleLegalGamer is very simple : it picks the first legal move
//		Move selection = moves.get(0);
        Move selection = bestMoveVariable(getStateMachine(), getCurrentState(), getRole());

        // We get the end time
        // It is mandatory that stop<timeout
        long stop = System.currentTimeMillis();

        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        return selection;
    }

    Move bestMoveVariable (StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        /*
        Implements the alpha-beta minimax player. http://logic.stanford.edu/ggp/chapters/chapter_06.html
        Searches every state until end of game.
        Returns the move that results in the highest score, accounting for opponents trying to reduce our score.
         */
        List<Move> actions = theMachine.getLegalMoves(theState, theRole);
        Move action = actions.get(0);
        int score = 0;
        for (Move action1 : actions) {
            int result = minScoreVariable(theMachine, theState, theRole, action1, 0, 100, 0);
            if (result == 100) {
                return action1;
            }
            if (result > score) {
                score = result;
                action = action1;
            }
        }
        return action;
    }

    int maxScoreVariable(StateMachine theMachine, MachineState theState, Role theRole, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        /*
        Find the maximum attainable score from the given state for role.
        Keeps track of depth and uses continueExpansion function to determine depth to stop at.
        If it stops before the end of the game, heuristicValue is used to determine the utility of the state.
         */
        if (theMachine.isTerminal(theState)) {
            return theMachine.getGoal(theState, theRole);
        }
        if (!continueExpansion(theMachine, theState, depth)) {
            return heuristicValue(theMachine, theState, theRole);
        }
        List<Move> actions = theMachine.getLegalMoves(theState, theRole);
        for (Move action : actions) {
            int result = minScoreVariable(theMachine, theState, theRole, action, alpha, beta, depth);
            alpha = Math.max(alpha, result);
            if (alpha >= beta) {
                return beta;
            }
        }
        return alpha;
    }

    int minScoreVariable(StateMachine theMachine, MachineState theState, Role theRole, Move action, int alpha, int beta, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

        List<List<Move>> jointMoves = theMachine.getLegalJointMoves(theState, theRole, action);
        for (List<Move> jointMove : jointMoves) {
            MachineState newstate = theMachine.getNextState(theState, jointMove);
            int result = maxScoreVariable(theMachine, newstate, theRole, alpha, beta, depth + 1);
            beta = Math.min(beta, result);
            if (beta <= alpha) {
                return alpha;
            }
        }
        return beta;
    }

    int heuristicValue(StateMachine theMachine, MachineState theState, Role theRole) {

        return 0;
    }

    boolean continueExpansion(StateMachine theMachine, MachineState theState, int depth){
        return depth < searchDepth;
    }
}