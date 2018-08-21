package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.IncrementalWorker;
import com.skyplusplus.minesolver.core.ai.UpdateEvent;
import com.skyplusplus.minesolver.core.ai.UpdateEventEntry;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to solve constraint equations in the form of A+B+C = 2, A+C = 1, etc.
 */
public class CSPSolver extends IncrementalWorker<CSPSolver.Variable> {

    private Variable variablesById[];
    private List<Rule> rules = new ArrayList<>();

    public CSPSolver(int nVariables) {
        this(nVariables, null);
    }

    public CSPSolver(int nVariables, UpdateHandler handler) {
        variablesById = new Variable[nVariables];
        for (int i = 0; i < nVariables; i++) {
            variablesById[i] = new Variable(i);
        }
        this.handler = handler;
    }

    public void addRule(int sum, int... varIds) {
        Rule newRule = new Rule(sum, rules.size());
        rules.add(newRule);

        for (int i: varIds) {
            newRule.variables.add(variablesById[i]);
            variablesById[i].rules.add(newRule);
        }
    }

    /**
     * Solves the CSP.
     * @param solution array to put the solution in. solution[i] is the number of solutions where variable(i) is set.
     *                 should be the size of the total number of variables.
     * @return total number of solutions.
     */
    public BigInteger[] solve(BigInteger[][] solution) throws InterruptedException {
        if (false) System.out.println("Starting A*");
        SearchNode finalState = doAStar();

        int maxFrontierSize = 0;
        List<Variable> order = new ArrayList<>();
        Stack<SearchNode> inOrder = new Stack<>();
        while (finalState.parent != null) {
            inOrder.add(finalState);
            maxFrontierSize = Math.max(maxFrontierSize, finalState.maxFrontierSize + 1);
            finalState = finalState.parent;
        }
        while (!inOrder.empty()) {
            SearchNode n = inOrder.pop();
            for (Variable v: n.frontier) {
                if (!order.contains(v)) {
                    order.add(v);
                }
            }
        }
        for (Variable aVariablesById : variablesById) {
            if (!order.contains(aVariablesById)) {
                order.add(aVariablesById);
            }
        }
        if (false) System.out.println(order);

        return getIndependentVariableProbability(solution, order, maxFrontierSize);
    }

    /**
     * Solves the equations
     * @param output the number of solutions with variable [index] being set
     * @return the total number of solutions
     */
    public BigInteger[] getIndependentVariableProbability(BigInteger[][] output, List<Variable> order, int maxFrontierSize) {
        List<Variable> frontier = new ArrayList<>();
        Set<Rule> satisfied = new HashSet<>();

        Map<SolutionKey, BigInteger[]> currSolutions = new HashMap<>();
        Map<SolutionKey, BigInteger[]> prevSolutions = new HashMap<>();
        Map<SolutionKey, BigInteger> currNumSolutions = new HashMap<>();
        Map<SolutionKey, BigInteger> prevNumSolutions = new HashMap<>();

        int currI = 0;
        int prevI = 1;

        BigInteger[] zeros = new BigInteger[order.size()];
        Arrays.fill(zeros, BigInteger.ZERO);
        prevSolutions.put(new SolutionKey(0, 0), Arrays.copyOf(zeros, order.size()));
        prevNumSolutions.put(new SolutionKey(0, 0), BigInteger.ONE);

        for (Variable var: order) {
            if (false) System.out.println("Adding variable " + var.id);
            if (false) System.out.println(frontier);

            currSolutions.clear();
            currNumSolutions.clear();

            for (int newval = 0; newval <= 1; newval++) {
                for (Map.Entry<SolutionKey, BigInteger[]> entry: prevSolutions.entrySet()) {
                    boolean possible = true;

                    for (Rule r : var.rules) {
                        int numSatisfied = newval;
                        int numFree = r.variables.size() - 1;
                        for (int i = 0; i < frontier.size(); i++) {
                            if (r.variables.contains(frontier.get(i))) {
                                if ((entry.getKey().frontierSet & (1<<i)) > 0) {
                                    numSatisfied ++;
                                }
                                numFree--;
                            }
                        }
                        if (numSatisfied > r.targetSum || r.targetSum > numSatisfied + numFree) {
                            possible = false;
                        }
                    }


                    if (possible) {
                        long newFrontierSet = (entry.getKey().frontierSet << 1) | newval;
                        SolutionKey newSolutionKey = new SolutionKey(entry.getKey().numTrue + newval, newFrontierSet);
                        if (!currSolutions.containsKey(newSolutionKey)) {
                            currSolutions.put(newSolutionKey, Arrays.copyOf(zeros, order.size()));
                            currNumSolutions.put(newSolutionKey, BigInteger.ZERO);
                        }
                        BigInteger[] currSolution = currSolutions.get(newSolutionKey);
                        for (int i = 0; i < order.size(); i++) {
                            //if (false) System.out.println("" + frontier + " " + i + " " + solution[currI][newBitmap][i] + " " + solution[prevI][bitmap][i]);
                            currSolution[i] = currSolution[i].add(entry.getValue()[i]);
                        }
                        currNumSolutions.put(newSolutionKey, currNumSolutions.get(newSolutionKey).add(prevNumSolutions.get(entry.getKey())));

                        //if (false) System.out.println("    Added " + bitmap + " (" + numSols[prevI][bitmap] +", " + Arrays.deepToString(solution[prevI][bitmap]) + ") to " + newBitSet + " (" + numSols[currI][newBitSet] + ", " + Arrays.deepToString(solution[currI][newBitSet]) + ")");
                    }
                }
            }

            if (false) System.out.println("    Flip");
            Map<SolutionKey, BigInteger[]> temp = prevSolutions;
            prevSolutions = currSolutions;
            currSolutions = temp;
            Map<SolutionKey, BigInteger> temp2 = prevNumSolutions;
            prevNumSolutions = currNumSolutions;
            currNumSolutions = temp2;

            frontier.add(0, var);

            // Add any numbered squares to covered if all its neighbours are in the frontier.
            for (Rule r: var.rules) {
                if (frontier.containsAll(r.variables)) {
                    if (false) System.out.println("Satisfied: " + r);
                    satisfied.add(r);
                }
            }



            // Remove frontier items that have only covered neighbours. There is no use to keep exploring its
            // possibilities since we don't need it to satisfy any future uncovered squares.
            for (int i = 0; i < frontier.size(); i++) {
                if (satisfied.containsAll(frontier.get(i).rules)) {

                    currNumSolutions.clear();
                    currSolutions.clear();

                    if (false) System.out.println("Removing variable " + frontier.get(i).id);
                    if (false) System.out.println(frontier);

                    // Remove the frontier variable, since it's not part of any unexplored rules anymore.
                    for (Map.Entry<SolutionKey, BigInteger[]> entry: prevSolutions.entrySet()) {
                        long leftMask = (1<<i)-1;
                        long rightMask = ((1<<frontier.size()) - 1) & ~((leftMask<<1) | 1);
                        long newFrontierSet = (entry.getKey().frontierSet & leftMask) | ((entry.getKey().frontierSet & rightMask) >> 1);

                        SolutionKey newSolutionKey = new SolutionKey(entry.getKey().numTrue, newFrontierSet);
                        if (!currSolutions.containsKey(newSolutionKey)) {
                            currSolutions.put(newSolutionKey, Arrays.copyOf(zeros, order.size()));
                            currNumSolutions.put(newSolutionKey, BigInteger.ZERO);
                        }
                        BigInteger[] currSolution = currSolutions.get(newSolutionKey);

                        for (int j = 0; j < order.size(); j++) {
                            currSolution[j] = currSolution[j].add(entry.getValue()[j]);
                        }

                        if ((entry.getKey().frontierSet & (1<<i)) > 0) {
                            currSolution[frontier.get(i).id] = currSolution[frontier.get(i).id].add(prevNumSolutions.get(entry.getKey()));
                        }

                        currNumSolutions.put(newSolutionKey, currNumSolutions.get(newSolutionKey).add(prevNumSolutions.get(entry.getKey())));
                        //if (false) System.out.println("    Added " + bitmap + " (" + numSols[prevI][bitmap] +", " + Arrays.deepToString(solution[prevI][bitmap]) + ") to " + newFrontierSet + " (" + numSols[currI][newFrontierSet] + ", " + Arrays.deepToString(solution[currI][newFrontierSet]) + ")");

                    }

                    frontier.remove(i);
                    i--;
                    if (false) System.out.println("    Flip");
                    temp = prevSolutions;
                    prevSolutions = currSolutions;
                    currSolutions = temp;
                    temp2 = prevNumSolutions;
                    prevNumSolutions = currNumSolutions;
                    currNumSolutions = temp2;
                }
            }
            frontier.removeIf(v -> satisfied.containsAll(v.rules));
        }


        BigInteger[] retVal = new BigInteger[order.size() + 1];
        for (int i = 0; i <= order.size(); i++) {
            if (prevSolutions.containsKey(new SolutionKey(i, 0))) {
                System.arraycopy(prevSolutions.get(new SolutionKey(i, 0)), 0, output[i], 0, order.size());
            } else {
                Arrays.fill(output[i], BigInteger.ZERO);
            }
            retVal[i] = prevNumSolutions.getOrDefault(new SolutionKey(i, 0), BigInteger.ZERO);
        }
        return retVal;
    }



    private SearchNode doAStar() throws InterruptedException {

        PriorityQueue<SearchNode> domain = new PriorityQueue<>();
        domain.add(new SearchNode(rules.size()));

        HashSet<SearchNode> seen = new HashSet<>();

        int nodesVisited = 0;

        while (!domain.isEmpty()) {
            SearchNode thisNode = domain.remove();
            if (!seen.contains(thisNode)) {
                nodesVisited++;
                final int thisNodesVisited = nodesVisited;
                seen.add(thisNode);

                if (thisNode.numCompleted == rules.size()) {
                    if (false) System.out.print("Found order, total cost: " + thisNode.heuristic + thisNode.cost + "\n");
                    SearchNode current = thisNode;
                    while (current != null && current.parent != null) {
                        BitSet next = (BitSet) current.completedRules.clone();
                        next.xor(current.parent.completedRules);

                        if (false) System.out.println(rules.get(next.nextSetBit(0)));
                        current = current.parent;
                    }
                    return thisNode;
                }

                reportProgress(() -> {
                    List<UpdateEventEntry<Variable>> entries = new ArrayList<>();
                    for (int i = 0; i < variablesById.length; i++) {
                        if (thisNode.completedRules.get(i)) {
                            entries.add(new UpdateEventEntry<>(variablesById[i], "Safe", i));
                        } else {
                            entries.add(new UpdateEventEntry<>(variablesById[i], "Not ", i));
                        }
                    }
                    for (Variable front : thisNode.frontier) {
                        entries.add(new UpdateEventEntry<>(front, "Mine", 0));
                    }
                    return new UpdateEvent<>(entries,
                            "A* in progress. " + "Cost so far: " + (thisNode.cost + thisNode.heuristic) + " Nodes visited: " + thisNodesVisited + ". Queue size: " + domain
                                    .size());
                });
                if (nodesVisited % 1 == 0) if (false) System.out.println("A* in progress. " + "Cost so far: " + thisNode.cost + thisNode.heuristic + " Nodes visited: " + thisNodesVisited + ". Queue size: " + domain
                        .size() + " Current node progress: " + thisNode.completedRules.cardinality());

                domain.addAll(thisNode.getNeighbours(rules));
            }
        }

        return null;
    }

    private class SearchNode implements Comparable<SearchNode> {
        private BitSet completedRules;
        private double cost;
        private double heuristic;
        private HashSet<Variable> frontier;
        private SearchNode parent;
        private int numCompleted;
        int numRules;
        int maxFrontierSize;

        public SearchNode(int numRules) {
            this(new BitSet(numRules), numRules, null, new HashSet<>(), 0, 0, 0);
        }

        public SearchNode(
                BitSet completedRules,
                int numRules,
                SearchNode parent,
                HashSet<Variable> frontier,
                double cost,
                int numCompleted,
                int maxFrontierSize
        ) {
            this.completedRules = completedRules;
            this.parent = parent;
            this.frontier = frontier;
            this.cost = cost;
            this.numCompleted = numCompleted;
            this.maxFrontierSize = maxFrontierSize;
            this.numRules = numRules;

            if (false) System.out.println("numRules: " + numRules + " numCompleted: " + numCompleted + " frontier size: " + frontier.size());
            heuristic = (numRules - numCompleted) * 500 * (frontier.size() + 5);
        }

        public List<SearchNode> getNeighbours(List<Rule> allRules) {
            List<SearchNode> neighbours = new ArrayList<>();

            HashSet<Rule> candidates = new HashSet<>();
            for (Variable r: frontier) {
                candidates.addAll(r.rules);
            }
            candidates.removeIf(candidate -> completedRules.get(candidate.id));
            if (candidates.isEmpty()) {
                candidates.addAll(allRules);
            }
            candidates.removeIf(candidate -> completedRules.get(candidate.id));

            for (Rule r: candidates) {
                HashSet<Variable> newFrontier = new HashSet<>(frontier);
                newFrontier.addAll(r.variables);
                BitSet newBitSet = (BitSet) completedRules.clone();
                int newMaxFrontierSize = newFrontier.size();
                newFrontier.removeIf(v -> v.rules.stream().allMatch(rr -> newBitSet.get(rr.id)));
                newBitSet.set(r.id);

                neighbours.add(new SearchNode(newBitSet, numRules, this, newFrontier, this.cost + (1 << newFrontier.size()), numCompleted + 1, newMaxFrontierSize));
            }

            return neighbours;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof SearchNode) {
                return ((SearchNode) other).completedRules.equals(this.completedRules);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return completedRules.hashCode();
        }

        @Override
        public int compareTo(SearchNode o) {
            return Double.compare(this.cost + this.heuristic, o.cost + o.heuristic);
        }
    }


    public static class Rule {
        List<Variable> variables = new ArrayList<>();
        int targetSum;
        int id;

        public Rule(int targetSum, int id) {
            this.targetSum = targetSum;
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Rule) {
                return this.id == ((Rule) other).id;
            }
            return false;
        }

        @Override
        public String toString() {
            String varPart = String.join(" + ", variables.stream().map(r -> Integer.toString(r.id)).collect(Collectors.toList()));
            return "(Rule: " + id + ": " + varPart + " = " + targetSum + ")";
        }
    }

    public static class Variable {
        List<Rule> rules = new ArrayList<>();
        int id;

        public Variable(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Variable) {
                return this.id == ((Variable) other).id;
            }
            return false;
        }

        @Override
        public String toString() {
            return "(Var: " + id + ")";
        }
    }

    public static class SolutionKey {
        private int numTrue;
        // Decided on a single long because BitSet cannot shift :( Also when on earth will we be playing a
        // minesweeper game with a group width of > 64 items?
        private long frontierSet;

        public SolutionKey(int numTrue, long frontierSet) {
            this.numTrue = numTrue;
            this.frontierSet = frontierSet;
        }

        public int getNumTrue() {
            return numTrue;
        }

        public long getFrontierSet() {
            return frontierSet;
        }

        @Override
        public int hashCode() {
            return numTrue * 37 + (int)(frontierSet % 1000000009);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof SolutionKey) {
                return this.frontierSet == ((SolutionKey) other).frontierSet && this.numTrue == ((SolutionKey) other).numTrue;
            }
            return false;
        }
    }
}
