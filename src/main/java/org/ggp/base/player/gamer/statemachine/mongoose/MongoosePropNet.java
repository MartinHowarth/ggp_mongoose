package org.ggp.base.player.gamer.statemachine.mongoose;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.mongoose.propnet.MyPropNetGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.PropNetStateMachine;


public class MongoosePropNet extends PropNetGamer {
    /*
    Define the initial parameters for heuristics.
     */
    int searchDepth = 100;
    int myFocusHeuristic = 0;
    int myMobilityHeuristic = 0;
    int opponentFocusHeuristic = 0;
    int opponentMobilityHeuristic = 0;

    int targetNumberOfNodes = 100; // Dynamically adjust nSimulations to get this many nodes.
    int numberMonteCarloPerSimulation = 1;
    int numberNodesToAlphaBetaSearch = 1000;
    int numberNodesRepetitionToBreak = 200;

    long finishBy = 0;

    boolean alphaBetaCompletedFully = true;

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        PropNetStateMachine theMachine = getStateMachine();
        Role theRole = getRole();
        MachineState currentState = getCurrentState();


        List<Move> jointMove = theMachine.getLegalJointMoves(getCurrentState()).get(0);

        MachineState nextState1 = theMachine.getNextState(getCurrentState(), jointMove);
        List<Move> jointMove2 = theMachine.getLegalJointMoves(nextState1).get(0);
        nextState1 = theMachine.getNextState(nextState1, jointMove2);

        theMachine.applyState(getCurrentState());
        theMachine.nextState(jointMove);
        theMachine.nextState(jointMove2);

        MachineState nextState2 = theMachine.getStateFromBase();

        if (nextState1.toString().equals(nextState2.toString())) {
            System.out.println("States are same.");
        }
        else {
            System.out.println("States are different.");
            System.out.println(nextState1.getContents());
            System.out.println(nextState2.getContents());
        }

        while (!theMachine.isTerminal()) {
            theMachine.nextState(theMachine.getLegalJointMoves().get(0));
        }

        MachineState nextState3 = theMachine.getStateFromBase();

        System.out.println(nextState3);

    }

    @Override
    public PropNetStateMachine getInitialStateMachine() {
        return new PropNetStateMachine();
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void stateMachineStop() {
    }

    @Override
    public void stateMachineAbort() {
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        System.out.println();
        System.out.println("##########################################################################");

        PropNetStateMachine theMachine = getStateMachine();
        Role theRole = getRole();
        MachineState currentState = getCurrentState();
        long start = System.currentTimeMillis();
        finishBy = timeout - 400;
        int myRoleIndex = theMachine.getRoles().indexOf(getRole());
        boolean foundMove = false;
        alphaBetaCompletedFully = true;
        MaxNode.reInitialise();

        Move selection = null;
        List<Move> legalMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
        List<Move> moves;
        System.out.println("Number of legal moves: " + legalMoves.size());

        // If we can win immediately, return a random winning move.
        // Don't do this on single player games, because there is no way to tell if a goal is "good enough".
        //      E.g. Knight tour, can terminate game in 2 moves, but only get a score of 2.
        // Assumption is that in 2 player games, a terminal state is favourable if I have a higher score than the opponent.
        //      Caveat (TODO): cooperative games? Better to take a lower score, with which I win, or try for higher?
        if (theMachine.getRoles().size() > 1){
            moves = findVictoryMovesFromState(theMachine, getCurrentState(), getRole());
            if (moves.size() > 0) {
                selection = (moves.get(new Random().nextInt(moves.size())));
                System.out.println("Victory move found: " + selection);
                foundMove = true;
            }
        }
        else {
            moves = legalMoves;
        }

        /*
        Find any moves that either cause us to lose immediately, or allow an
        opponent to win immediately (and uncontested) the turn after.
        Exclude these from subsequent considerations.

        Note that draws are counted as losses for this consideration.
        Choosing a draw over loss is left for more complicated searches.
         */
        if (!foundMove) {
            System.out.println("Removing bad moves...");

            List<Move> badMoves = findLosingMovesFromStateForRole(theMachine, getCurrentState(), getRole());

            // Recreate moves without badMoves
            moves = removeMovesFromList(legalMoves, badMoves);

            // If all moves lead to loss, analyse all legal moves instead for "best" loss.
            if (moves.size() == 0) {
                System.out.println("All moves lead to immediate loss.");
                moves = legalMoves;
            }
            else {
                System.out.println("Number of non-loss moves: " + moves.size());
            }
        }

        // Do monte-carlo on all remaining legal moves.
        System.out.println("Starting Monte Carlo...");
        if (!foundMove) {
            selection = moves.get(0);
            if (moves.size() > 1) {
                /*
                Perform a Monte Carlo Tree Search. http://logic.stanford.edu/ggp/chapters/chapter_08.html
                If tree stops being expanded, and the tree is sufficiently small
                then break to do a minimax alpha-beta search.
                 */

                boolean doAlphaBeta = false;

                MaxNode headMaxNode = new MaxNode();
                headMaxNode.initNode(theMachine, null, currentState);
                MaxNode.finishBy = finishBy;

                int numberNodesStationaryCounter = 0;
                int lastSeenNumberNodes = 0;
                while (System.currentTimeMillis() < finishBy) {
                    MaxNode nextMaxNode = headMaxNode.selectNextNodeRecursive(0);
                    nextMaxNode.expand(theMachine, theRole);
                    int score = nextMaxNode.simulate(theMachine, theRole, numberMonteCarloPerSimulation);
                    nextMaxNode.backPropagate(score);

                    /*
                    Judge whether to break for alpha-beta search.
                     */
                    if (MaxNode.numberMaxNodes < numberNodesToAlphaBetaSearch) {
                        if (MaxNode.numberMaxNodes == lastSeenNumberNodes) {
                            numberNodesStationaryCounter++;
                            if (numberNodesStationaryCounter > numberNodesRepetitionToBreak) {
                                doAlphaBeta = true;
                                System.out.println("Exiting to do alpha-beta search...");
                                break;
                            }
                        } else {
                            lastSeenNumberNodes = MaxNode.numberMaxNodes;
                            numberNodesStationaryCounter = 0;
                        }
                    }
                }
                System.out.println("Number of nodes in tree: " + MaxNode.numberMaxNodes);
                System.out.println("Utility of head node: " + headMaxNode.utility);
                System.out.println("Node iterations completed: " + headMaxNode.visits);
                selection = headMaxNode.selectBestMove();

                if (MaxNode.numberMaxNodes > (targetNumberOfNodes * 2)) {
                    int factor = (MaxNode.numberMaxNodes / targetNumberOfNodes) / 2;
                    numberMonteCarloPerSimulation *= factor;
                    System.out.println("Increasing number of simulations per node to: " + numberMonteCarloPerSimulation);
                }
                else if (MaxNode.numberMaxNodes < (targetNumberOfNodes / 2)) {
                    System.out.println("Decreasing number of simulations per node.");
                    numberMonteCarloPerSimulation /= 2;
                    if (numberMonteCarloPerSimulation == 0) {
                        numberMonteCarloPerSimulation = 1;
                    }
                }

                if (doAlphaBeta) {
                    System.out.println("Starting alpha-beta search...");
                    long alphaBetaStart = System.currentTimeMillis();
                    Move alphaBetaSelection = bestMoveVariableDepth(theMachine, currentState, theRole);
                    long alphaBetaTime = System.currentTimeMillis() - alphaBetaStart;
                    if (alphaBetaCompletedFully) {
                        selection = alphaBetaSelection;
                        /*
                        If there is enough time left to re-do the alpha-beta search, then double the
                        number of considered nodes for the next turn.
                         */
                        long timeLeft = finishBy - System.currentTimeMillis();
                        if (timeLeft > alphaBetaTime) {
                            System.out.println("Extra time remaining during AB search. Doubling number of nodes.");
                            numberNodesToAlphaBetaSearch *= 2;
                        }
                    }
                    else {
                        /*
                        If we fail to have enough time to search fully, reduce the number of nodes required
                        to break for A-B search.
                         */
                        System.out.println("Ran out of time during AB search. Halving number of nodes.");
                        numberNodesToAlphaBetaSearch /= 2;
                    }
                }
            }
        }
        long stop = System.currentTimeMillis();

        notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        System.out.println("Move selected with " + (timeout - stop) + "ms remaining.");
        return selection;
    }

    List<Move> findVictoryMovesFromState(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
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

    boolean isVictoryForRole(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {
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

    List<Move> findLosingMovesFromStateForRole(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
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
//                System.out.println("Excluding bad move: " + jointMove.get(theRoleIndex));
            }
        }
        return badMoves;
    }

    int mobility(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
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

    int focus(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        /*
        Inverse of mobility. Returns the (integer) percentage of legal moves that are not feasible.
         */
        return (100 - mobility(theMachine, theState, theRole));
    }

    Move bestMoveCompleteTree(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
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

    int maxScoreComplete(PropNetStateMachine theMachine, MachineState theState, Role theRole, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
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

    int minScoreComplete(PropNetStateMachine theMachine, MachineState theState, Role theRole, Move action, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
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

    Move bestMoveVariableDepth(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        /*
        Implements the alpha-beta minimax player. http://logic.stanford.edu/ggp/chapters/chapter_06.html
        Searches every state until continueExpansion is false.
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

    int maxScoreVariable(PropNetStateMachine theMachine, MachineState theState, Role theRole, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
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

    int minScoreVariable(PropNetStateMachine theMachine, MachineState theState, Role theRole, Move action, int alpha, int beta, int depth) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

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

    int heuristicValue(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {

        return getGoalForRole(theMachine, theState, theRole);
    }

    boolean continueExpansion(PropNetStateMachine theMachine, MachineState theState, int depth){
        if (System.currentTimeMillis() > finishBy) {
            System.out.println("Ran out of time during alpha-beta search.");
            alphaBetaCompletedFully = false;
            return false;
        }
        return depth < searchDepth;
    }

    List<Move> removeMovesFromList(List<Move> allMoves, List<Move> badMoves) {
        return allMoves.stream().filter(move -> !badMoves.contains(move)).collect(Collectors.toList());
    }

    int getGoalForRole(PropNetStateMachine theMachine, MachineState theState, Role theRole) throws GoalDefinitionException {
        List<Integer> goals = theMachine.getGoals(theState);
        List<Role> roles = theMachine.getRoles();

        int theRoleIndex = roles.indexOf(theRole);
        return goals.get(theRoleIndex);
    }

}