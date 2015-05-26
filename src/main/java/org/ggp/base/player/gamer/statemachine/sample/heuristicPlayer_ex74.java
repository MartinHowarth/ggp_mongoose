package org.ggp.base.player.gamer.statemachine.sample;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class heuristicPlayer_ex74 extends SampleGamer {
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

    int heuristicValue(StateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        return (int)(0.5 * mobility(theMachine, theState, theRole));
    }

    boolean continueExpansion(StateMachine theMachine, MachineState theState, int depth){
        return depth < searchDepth;
    }

    int mobility(StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        /*
        Returns the percentage (integer) of legal moves from given state that are feasible.
        Feasible here means that it doesn't lead to loss within 2 joint moves.
         */
        List<Move> possibleMoves = theMachine.getLegalMoves(theState, theRole);
        List<Move> badMoves = findLosingMovesFromStateForRole(theMachine, getCurrentState(), getRole());

        // Recreate moves without badMoves
        List<Move> feasibleMoves = removeMovesFromList(possibleMoves, badMoves);

        return ((int) ((double) feasibleMoves.size() / (double) possibleMoves.size()) * 100);
    }

    int focus(StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        /*
        Inverse of mobility. Returns the (integer) percentage of legal moves that are not feasible.
         */
        return (100 - mobility(theMachine, theState, theRole));
    }

    List<Move> removeMovesFromList(List<Move> allMoves, List<Move> badMoves) {
        return allMoves.stream().filter(move -> !badMoves.contains(move)).collect(Collectors.toList());
    }

    int getGoalForRole(StateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {
        List<Integer> goals = theMachine.getGoals(theState);
        List<Role> roles = theMachine.getRoles();

        int theRoleIndex = roles.indexOf(theRole);
        return goals.get(theRoleIndex);
    }

    List<Move> findVictoryMovesFromState(StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        /*
        Check all moves at given state that lead to immediate victory for given role.
        Returns a list of all moves that give victory.
        */

        List<Move> victoryMoves = new ArrayList<>();
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
                } else {
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
        /*
        Check if the given role is victorious in the given state.
        Return true is so, otherwise return false.
        */
        if (theMachine.isTerminal(theState)) {
            List<Integer> goals = theMachine.getGoals(theState);
            List<Role> roles = theMachine.getRoles();

            int theRoleGoal = getGoalForRole(theMachine, theState, theRole);

            for (int ii = 0; ii < roles.size(); ii++) {
                if (goals.get(ii) > theRoleGoal) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    List<Move> findLosingMovesFromStateForRole(StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        // TODO: This function can reject states that are immediate losses, but are actually the best score that theRole could get in the rest of the game.
        /*
        Check all legal subsequent states for immediate loss,
        and check for moves that leave theRole open to inevitable loss the turn after.
        */
        List<Move> badMoves = new ArrayList<Move>();
        int theRoleIndex = theMachine.getRoles().indexOf(theRole);
        boolean isBadMove;

        for (List<Move> jointMove : theMachine.getLegalJointMoves(theState)) {
            isBadMove = false;
            MachineState state = theMachine.getNextState(theState, jointMove);

            // If game ends after one joint move, check for victory.
            // In a turn based game this is where theRole takes the last move.
            if (theMachine.isTerminal(state)) {
                if (!isVictoryForRole(theMachine, state, theRole)) {
                    isBadMove = true;
                }
            }

            if (!isBadMove) {
                // If game doesn't end after one joint move,
                // then check that no opponent can win inevitably.
                // In a turn based game, this is where one of theRole's opponents takes the last move.
                for (Role role : theMachine.getRoles()) {
                    if (role != theRole) {
                        if (findVictoryMovesFromState(theMachine, state, role).size() > 0) {
                            isBadMove = true;
                        }
                    }
                }
            }

            if (isBadMove) {
                badMoves.add(jointMove.get(theRoleIndex));
                System.out.println("Excluding bad move: " + jointMove.get(theRoleIndex));
            }
        }
        return badMoves;
    }


}