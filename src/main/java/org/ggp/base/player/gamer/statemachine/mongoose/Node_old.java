package org.ggp.base.player.gamer.statemachine.mongoose;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node_old {
    public static int numberNodes = 0;
    public static boolean endExpansion = false;

    Node_old parent = null;
    MachineState nodeState = null;
    Map<Move, Node_old> children;

    /*
    Also store children as siblings, so we can work out which has the minimal score later.
    This assumes that our opponent will work to minimise our score.

    Siblings == the max nodes that a min node has a choice between.
     */
    List<List<Node_old>> siblings = new ArrayList<>();
    int visits = 0;
    int utility = 0;
    int depth = 0;

    protected void initNode(Node_old initParent, MachineState myState) {
        numberNodes += 1;
        parent = initParent;
        nodeState = myState;
    }

    public void expand(StateMachine theMachine, Role theRole) throws MoveDefinitionException, TransitionDefinitionException {
        Map<Move, List<MachineState>> newChildren = theMachine.getNextStates(nodeState, theRole);

        for (Map.Entry<Move, List<MachineState>> newChild : newChildren.entrySet()) {
            List<Node_old> newSiblings = new ArrayList<>();
            for (MachineState childState : newChild.getValue()) {
                Node_old child = new Node_old();
                child.initNode(this, childState);

                children.put(newChild.getKey(), child);

                newSiblings.add(child);
            }
            siblings.add(newSiblings);
        }

    }

    public Node_old selectNextNodeDumb() {
        /*
        If startNode or any of its children have not been explored before, then choose them.
         */
        if (visits == 0) {
            return this;
        }

        for (Node_old child : children.values()) {
            if (child.visits == 0) {
                return child;
            }
        }

        /*
        Otherwise use selectFunction to choose the node to explore further.
         */
        int score = 0;
        Node_old result = this;
        for (Node_old child : children.values()) {
            int newScore = child.selectValue();
            if (newScore > score) {
                score = newScore;
                result = child;
            }
        }

        return result;
    }

    public Node_old selectNextNode() {
        /*
        If startNode or any of its children have not been explored before, then choose them.
         */
        if (visits == 0) {
            return this;
        }

        for (Node_old child : children.values()) {
            if (child.visits == 0) {
                return child;
            }
        }

        /*
        Determine the selection scores of the min nodes.
         */
        Map<Integer, List<Node_old>> siblingGroupScores = new HashMap<>();
        for (List<Node_old> sibs : siblings) {

            // Find worst sibling of group.
            int lowestSibScore = 999999999;
            for (Node_old sib : sibs) {
                int sibValue = sib.selectValue();
                if (sibValue < lowestSibScore) {
                    lowestSibScore = sibValue;
                }
            }
            siblingGroupScores.put(lowestSibScore, sibs);
        }

        /*
        Find the best min node
         */
        List<Node_old> bestSibGroup = new ArrayList<>();
        int score = 0;
        for (Map.Entry<Integer, List<Node_old>> sibGroup : siblingGroupScores.entrySet()) {
            if (sibGroup.getKey() > score) {
                score = sibGroup.getKey();
                bestSibGroup = sibGroup.getValue();
            }
        }

        /*
        Find the worst child resulting from that min node.
         */
        score = 999999999;
        Node_old result = null;
        for (Node_old sib : bestSibGroup) {
            int sibValue = sib.selectValue();
            if (sibValue < score) {
                score = sibValue;
                result = sib;
            }
        }

        return result;


//        /*
//        Otherwise use selectFunction to choose the node to explore further.
//         */
//        int score = 0;
//        MaxNode result = this;
//        for (MaxNode child : children.values()) {
//            int newScore = child.selectValue();
//            if (newScore > score) {
//                score = newScore;
//                result = child;
//            }
//        }
//
//        return result;
    }

    public Node_old selectNextNodeRecursive() {
        Node_old nextNode = selectNextNode();
        if (nextNode.visits == 0) {
            return nextNode;
        }

        // If we've decided to stop expanding the tree (e.g. out of RAM), then return a fringe state when it is reached.
        if (endExpansion) {
            if (children.size() == 0) {
                return nextNode;
            }
        }

        return nextNode.selectNextNodeRecursive();
    }

    public int selectValue() {
        return (int)(utility + Math.sqrt(Math.log(parent.visits) / (double)visits));
    }

    public int selectValueMin() {
        return (int)(utility - Math.sqrt(Math.log(parent.visits) / (double)visits));
    }

    public void backPropagate(Integer score) {
        visits += 1;
        utility = utility + score;
        if (parent != null) {
            backPropagate(score);
        }
    }

    public int simulate(StateMachine theMachine, Role theRole, int numberIterations) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        int cumulativeScore = 0;
        int cumulativeDepth = 0;
        int count = 0;
        int[] depth = new int[1];

        for (int ii = 0; ii < numberIterations; ii++) {
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(nodeState), depth);
            cumulativeScore += theMachine.getGoal(finalState, theRole);
            cumulativeDepth += depth[0];
        }

        this.depth += cumulativeDepth / count;
        return cumulativeScore / count;
    }

    private static Node_old worstSibling(List<Node_old> testSiblings) {
        /*
        Return which of a list of nodes has the lowest utility.
        To be used to predict which joint move our opponent will choose. Assumes they will try to minimise our score.
         */
        int score = 999999999;
        Node_old result = null;
        for (Node_old tSib : testSiblings) {
            if (tSib.utility < score) {
                score = tSib.utility;
                result = tSib;
            }
        }
        return result;
    }

    public Move selectBestMove() {
        List<Node_old> worstSiblings = new ArrayList<>();

        // Get the nodes with the lowest utility for each set of siblings.
        // This corresponds to our opponents trying to minimise our score.
        for (List<Node_old> sibs : siblings) {
            worstSiblings.add(worstSibling(sibs));
        }

        // Find the highest utility over all the worst siblings.
        int score = 0;
        Node_old bestChild = null;
        for (Node_old child : worstSiblings) {
            if (child.utility > score) {
                // TODO: Should this actually decide with the average score, instead of the cumulative?
                score = child.utility;
                bestChild = child;
            }
        }

        // Return the move that leads to the MaxNode chosen above.
        for (Map.Entry<Move, Node_old> child : children.entrySet()) {
            if (child.getValue() == bestChild) {
                return child.getKey();
            }
        }

        return null;
    }

}



















