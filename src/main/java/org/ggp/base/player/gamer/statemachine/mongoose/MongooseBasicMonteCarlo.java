package org.ggp.base.player.gamer.statemachine.mongoose;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class MongooseBasicMonteCarlo extends SampleGamer {
    /*
    Define the initial parameters for heuristics.
     */
    int searchDepth = 5;
    int myFocusHeuristic = 0;
    int myMobilityHeuristic = 0;
    int opponentFocusHeuristic = 0;
    int opponentMobilityHeuristic = 0;

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine theMachine = getStateMachine();
        long start = System.currentTimeMillis();
        long finishBy = timeout - 400;
        int myRoleIndex = theMachine.getRoles().indexOf(getRole());
        boolean foundMove = false;

        Move selection = null;
        List<Move> legalMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
        List<Move> moves;

        // If we can win immediately, return a random winning move.
        {
            moves = findVictoryMovesFromState(theMachine, getCurrentState(), getRole());
            if (moves.size() > 0) {
                selection = (moves.get(new Random().nextInt(moves.size())));
                System.out.println("VICTORY move found: " + selection);
                foundMove = true;
            }
        }

        // Find any moves that either cause us to lose immediately, or allow an
        // opponent to win immediately (and uncontested) the turn after.
        // Exclude these from subsequent considerations.
        if (!foundMove) {
            System.out.println("Removing bad moves...");

            List<Move> badMoves = findLosingMovesFromStateForRole(theMachine, getCurrentState(), getRole());

            // Recreate moves without badMoves
            moves = removeMovesFromList(legalMoves, badMoves);

            // If all moves lead to loss, analyse all legal moves instead for "best" loss.
            if (moves.size() == 0) {
                moves = legalMoves;
            }
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
                for (int i = 0; true; i = (i + 1) % moves.size()) {
                    if (System.currentTimeMillis() > finishBy)
                        break;

                    int theScore = performDepthChargeFromMove(getCurrentState(), moves.get(i));
                    moveTotalPoints[i] += theScore;
                    moveTotalAttempts[i] += 1;
                    if (i == 0) {
                        count = count + 1;
                    }
                }

                // Compute the expected score for each move.
                double[] moveExpectedPoints = new double[moves.size()];
                for (int i = 0; i < moves.size(); i++) {
                    moveExpectedPoints[i] = (double) moveTotalPoints[i] / moveTotalAttempts[i];
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
        System.out.println("Monte Carlo iterations completed: " + count);
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
        List<Move> badMoves = new ArrayList<>();
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

    Move bestMoveComplete (StateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        /*
        Implements the alpha-beta minimax player. http://logic.stanford.edu/ggp/chapters/chapter_06.html
        Searches every state until end of game.
        Returns the move that results in the highest score, accounting for opponents trying to reduce our score.
         */
        List<Move> actions = theMachine.getLegalMoves(theState, theRole);
        Move action = actions.get(0);
        int score = 0;
        for (Move action1 : actions) {
            int result = minScoreComplete(theMachine, theState, theRole, action1, 0, 100);
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

    int maxScoreComplete(StateMachine theMachine, MachineState theState, Role theRole, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (theMachine.isTerminal(theState)) {
            return theMachine.getGoal(theState, theRole);
        }
        List<Move> actions = theMachine.getLegalMoves(theState, theRole);
        for (Move action : actions) {
            int result = minScoreComplete(theMachine, theState, theRole, action, alpha, beta);
            alpha = Math.max(alpha, result);
            if (alpha >= beta) {
                return beta;
            }
        }
        return alpha;
    }

    int minScoreComplete(StateMachine theMachine, MachineState theState, Role theRole, Move action, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
        List<Role> roles = theMachine.getRoles();
        // assume one opponent
        Role opponent = roles.get(1 - roles.indexOf(theRole));
        List<Move> actions = theMachine.getLegalMoves(theState, opponent);
        for (Move action1 : actions) {
            List<Move> jointMove = new ArrayList<>();
            if (theRole == roles.get(0)) {
                jointMove.add(0, action);
                jointMove.add(1, action1);
            } else {
                jointMove.add(0, action1);
                jointMove.add(1, action);
            }
            MachineState newstate = theMachine.getNextState(theState, jointMove);
            int result = maxScoreComplete(theMachine, newstate, theRole, alpha, beta);
            beta = Math.min(beta, result);
            if (beta <= alpha) {
                return alpha;
            }
        }
        return beta;
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

    int heuristicValue(StateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {

        return getGoalForRole(theMachine, theState, theRole);
    }

    boolean continueExpansion(StateMachine theMachine, MachineState theState, int depth){
        return depth < searchDepth;
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

}