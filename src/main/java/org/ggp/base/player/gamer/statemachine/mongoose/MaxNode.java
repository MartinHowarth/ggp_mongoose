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


public class MaxNode {
    public static long finishBy = 0;
    public static int numberMaxNodes = 0;
    public static boolean endExpansion = false;
    public static int maxDepth = 99999999;
    public static int explorationParam = 1000;

    MinNode parent = null;
    MachineState nodeState = null;
    Map<Move, MinNode> minNodeMap = new HashMap<>();
    List<MaxNode> allChildren = new ArrayList<>();
    boolean isTerminal = false;

    int visits = 0;
    int utility = 0;
    int depth = 0;

    public static void reInitialise() {
        numberMaxNodes = 0;
        endExpansion = false;
    }

    protected void initNode(StateMachine theMachine, MinNode initParent, MachineState myState) {
        numberMaxNodes += 1;
        parent = initParent;
        nodeState = myState;
        isTerminal = theMachine.isTerminal(nodeState);
    }

    public void expand(StateMachine theMachine, Role theRole) throws MoveDefinitionException, TransitionDefinitionException {
        if (endExpansion || isTerminal) {
            return;
        }

        // Get all possible next states.
        Map<Move, List<MachineState>> newChildren = theMachine.getNextStates(nodeState, theRole);

        // Create a min node for each possible move, populated with a MaxNode for each possible state.
        for (Map.Entry<Move, List<MachineState>> newMinNode : newChildren.entrySet()) {
            MinNode minNode = new MinNode();
            List<MaxNode> nextMaxNodes = new ArrayList<>();
            for (MachineState childState : newMinNode.getValue()) {
                MaxNode newMaxNode = new MaxNode();
                newMaxNode.initNode(theMachine, minNode, childState);
                nextMaxNodes.add(newMaxNode);
                allChildren.add(newMaxNode);
            }

            minNode.initNode(this, nextMaxNodes);

            minNodeMap.put(newMinNode.getKey(), minNode);

        }

    }


    public MaxNode selectNextNode() {
        /*
        If startNode or any of its children have not been explored before, then choose them.
        Assumes that this node has children. If we've stopped expanding, then this might break.
         */
        if (visits == 0 || isTerminal) {
            return this;
        }

        for (MaxNode node : allChildren) {
            if (node.visits == 0) {
                return node;
            }
        }

        int score = 0;
        MinNode nextMinNode = null;
        for (MinNode minNode : minNodeMap.values()) {
            int minNodeScore = selectValue(minNode);
            if (minNodeScore >= score) {
                score = minNodeScore;
                nextMinNode = minNode;
            }
        }

        assert nextMinNode != null;
        return nextMinNode.selectNextNode();
    }

    public MaxNode selectNextNodeRecursive(int currentDepth) {
        MaxNode nextMaxNode = selectNextNode();
//        System.out.println(nextMaxNode + "; Current Depth: "  + currentDepth);
        if (nextMaxNode.visits == 0 || currentDepth > maxDepth || nextMaxNode.isTerminal) {
            return nextMaxNode;
        }

        // If we've decided to stop expanding the tree (e.g. out of RAM), then return a fringe state when it is reached.
        if (endExpansion) {
            if (minNodeMap.size() == 0) {
                return nextMaxNode;
            }
        }

        return nextMaxNode.selectNextNodeRecursive(currentDepth + 1);
    }

    public int selectValue(MinNode node) {
        return (int)(node.utility + explorationParam * Math.sqrt(Math.log(node.parent.visits) / (double)node.visits));
    }

    public void backPropagate(Integer score) {
        visits += 1;
        utility = utility + score;
        if (parent != null) {
            parent.backPropagate(score, this);
        }
    }

    public int simulate(StateMachine theMachine, Role theRole, int numberIterations) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (isTerminal) {
            this.depth += 0;
            return theMachine.getGoal(nodeState, theRole);
        }

        int cumulativeScore = 0;
        int cumulativeDepth = 0;
        int count = 0;
        int[] depth = new int[1];

        for (int ii = 0; ii < numberIterations; ii++) {
            count++;
            if (System.currentTimeMillis() > finishBy) {
                break;
            }
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(nodeState), depth);
            cumulativeScore += theMachine.getGoal(finalState, theRole);
            cumulativeDepth += depth[0];
        }

        this.depth += cumulativeDepth / count;
        return cumulativeScore / count;
    }

    public Move selectBestMove() {
        /*
        Choose the move that leads to the min node with maximum utility.
         */
        int score = 0;
        Move bestMove = null;
        for (Map.Entry<Move, MinNode> nodeEntry : minNodeMap.entrySet()) {
            if (nodeEntry.getValue().utility >= score) {
                score = nodeEntry.getValue().utility;
                bestMove = nodeEntry.getKey();
            }
        }
        return bestMove;
    }

    public String toString() {
        return "Utility: " + utility + "; Average remaining depth: " + depth + "; Visits: " + visits +  "; State: " + nodeState;
    }
}


class MinNode {
    Integer visits = 0;
    MaxNode parent = null;
    List<MaxNode> maxNodes = new ArrayList<>();
    MaxNode currentNextNode = null;
    Integer utility = 0;  // Lowest utility of all next max nodes.
    Integer lastScore = 0;

    public void initNode(MaxNode newParent, List<MaxNode> nextMaxNodes) {
        parent = newParent;
        maxNodes = nextMaxNodes;
        determineCurrentNextNode();
    }

    public void determineCurrentNextNode() {
        /*
        Find the subsequent max node with the lowest utility.
         */
        int score = 2147483647;
        for (MaxNode node : maxNodes) {
            if (node.utility < score) {
                score = node.utility;
                utility = score;
                currentNextNode = node;
            }
        }
    }

    public void backPropagate(int score, MaxNode node) {
        /*
        We want to back propagate the last seen score of the minimum max node.
        This may not be the node that we just considered.
         */
        visits += 1;
        determineCurrentNextNode();

        // If the given score is for the lowest utility node, then back propagate that score.
        if (currentNextNode == node) {
            if (parent != null) {
                parent.backPropagate(score);
                lastScore = score;
            }
        }
        // Otherwise back propagate the score last seen for the lowest utility node.
        else {
            if (parent != null) {
                parent.backPropagate(score);
            }
        }

    }

    public MaxNode selectNextNode() {
        /*
        Determine with next node to return for further exploration.
        Returns the max node which has the least value of selectValueMin.
         */
        int score = 2147483647;
        MaxNode result = null;
        for (MaxNode node : maxNodes) {
            int nodeValue = selectValueMin(node);
            if (nodeValue < score) {
                score = nodeValue;
                result = node;
            }
        }
        return result;
    }

    public int selectValueMin(MaxNode node) {
        return (int)(node.utility - MaxNode.explorationParam * Math.sqrt(Math.log(node.parent.visits) / (double)node.visits));
    }

}
















