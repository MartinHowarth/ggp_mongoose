package org.ggp.base.player.gamer.statemachine.mongoose.propnet;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;

import java.util.*;

public class PropNet {
    private List<Gdl> gameDescription;

    Map<Role, List<InputNode>> inputMap = new HashMap<>(); // Mapping for all inputs for a role
    List<Map<Move, InputNode>> moveMap = new ArrayList<>(); // List of hashmaps, in role order, for mappings from moves to input nodes.
    List<TerminalNode> terminalNodes = new ArrayList<>();
    List<GoalNode> goalNodes = new ArrayList<>();

    // Nodes from which propagation begins. Input nodes & transition gates.
    List<LogicObject> propagationInitiators = new ArrayList<>();

    List<BaseNode> baseNodes = new ArrayList<>();
    Integer nBaseNodes;

    List<Role> roles = new ArrayList<>();
    int numberRoles = 0;
    Role role;

    public PropNet() {
    }

    public void initialize(List<Gdl> description) {
        gameDescription = description;
        // role = Role;

        numberRoles = roles.size();
        for (int ii = 0; ii < numberRoles; ii++) {
            moveMap.add(new HashMap<>());
        }
        initInputs();
        System.out.println("Initializing propnet.");
    }

    public StatePropNet getInitialState() {
        // Todo make this real.
        return new StatePropNet(this);
    }

    public StatePropNet getPropNetFromSentenceList(Set<GdlSentence> description) {
        // Todo this.
        return null;
    }

    public int getRoleIndex(Role role) {
        for (int ii = 0; ii < numberRoles; ii++) {
            if (roles.get(ii) == role) {
                return ii;
            }
        }
        return -1;
    }

    public Role getRoleFromConstant(GdlConstant constant) {
        return new Role(constant);
    }
    public Move getMoveFromTerm(GdlTerm term) {
        return new Move(term);
    }

    /** Override this to perform some extra work (like trimming a cache) once per move.
     * <p>
     * CONTRACT: Should be called once per move.
     */
    public void doPerMoveWork() {}

    public void initInputs() {
        for (Role role : roles) {
            inputMap.keySet().add(role);
        }
    }

    public void startPropagation() {
        for (LogicObject obj : propagationInitiators) {
            obj.propagate();
        }
    }

    public boolean isTerminal(StatePropNet state) {
        applyState(state);
        for (TerminalNode node : terminalNodes) {
            if (node.state) {
                return true;
            }
        }
        return false;
    }

    public boolean isLegal(InputNode node) {
        return node.legalNode.state;
    }

    void markActions(List<InputNode> jointMove) {
        for (InputNode node : jointMove) {
            node.state = true;
        }
    }

    /**
     * Set propagation initiators to false. When we propagate next, other nodes will be overwritten accordingly.
     */
    void clearNet() {
        for (LogicObject object : propagationInitiators) {
            object.clearObject();
        }
    }

    void applyState(StatePropNet statePropNet) {
        statePropNet.applyState();
    }

    /**
     * Setup the propnet in the given state. Then return a list of moves corresponding to the true legal nodes.
     */
    public Map<Role, List<Move>> getLegalMoves(StatePropNet state) {
        applyState(state);
        Map<Role, List<Move>> legalMoves = new HashMap<>();

        List<InputNode> inputNodes;
        for (Map.Entry<Role, List<InputNode>> inputMapEntry : inputMap.entrySet()) {
            for (InputNode inputNode : inputMapEntry.getValue()) {
                if (isLegal(inputNode)) {
                    legalMoves.get(inputNode.role).add(inputNode.move);
                }
            }
        }
        return legalMoves;
    }

    public List<Move> getLegalMoves(StatePropNet state, Role role) {
        return getLegalMoves(state).get(role);
    }

    public List<List<Move>> getLegalJointMoves(StatePropNet state, Role role, Move move) {
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role r : getRoles()) {
            if (r.equals(role)) {
                List<Move> m = new ArrayList<Move>();
                m.add(move);
                legals.add(m);
            } else {
                legals.add(getLegalMoves(state, r));
            }
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

        return crossProduct;
    }

    public List<List<Move>> getLegalJointMoves(StatePropNet state) {
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role role : getRoles()) {
            legals.add(getLegalMoves(state, role));
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

        return crossProduct;
    }

    public Map<Role, Integer> getGoals(StatePropNet state) {
        applyState(state);
        Map<Role, Integer> result = new HashMap<>();
        for (GoalNode goalNode : goalNodes) {
            result.put(goalNode.role, goalNode.getGoal());
        }
        return result;
    }

    public Integer getGoalForRole(StatePropNet state, Role role) {
        return getGoals(state).get(role);
    }

    /**
     * Returns the goals for given state in a list whose order matches the order of the roles.
     */
    public List<Integer> getGoalsList(StatePropNet state) {
        Map<Role, Integer> goalsRoles = getGoals(state);
        List<Integer> result = new ArrayList<>();
        for (Role role: getRoles()) {
            result.add(goalsRoles.get(role));
        }
        return result;
    }

    public StatePropNet getNextState(StatePropNet state, List<Move> jointMove) {
        applyState(state);
        List<InputNode> jointNode = new ArrayList<>();
        for (int ii = 0; ii < numberRoles; ii++) {
            InputNode roleNode = moveMap.get(ii).get(jointMove.get(ii));
            jointNode.add(roleNode);
        }

        markActions(jointNode);
        startPropagation();
        return new StatePropNet(this);
    }

    public StatePropNet getNextStateDestructively(StatePropNet state, List<Move> jointMove) {
        return getNextState(state, jointMove);
    }


    public StatePropNet getCurrentState() {
        return new StatePropNet(this);
    }

    public List<Role> getRoles() {
        return roles;
    }

    public Role getRole() {
        return role;
    }


    protected void crossProductLegalMoves(List<List<Move>> legals, List<List<Move>> crossProduct, LinkedList<Move> partial)
    {
        if (partial.size() == legals.size()) {
            crossProduct.add(new ArrayList<Move>(partial));
        } else {
            for (Move move : legals.get(partial.size())) {
                partial.addLast(move);
                crossProductLegalMoves(legals, crossProduct, partial);
                partial.removeLast();
            }
        }
    }


}
