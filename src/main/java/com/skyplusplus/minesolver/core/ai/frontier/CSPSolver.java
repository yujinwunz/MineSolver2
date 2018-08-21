package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.IncrementalWorker;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to solveApproximate constraint equations in the form of A+B+C = 2, A+C = 1, etc.
 */
public class CSPSolver extends IncrementalWorker<CSPSolverUpdate> {

    private final Variable variablesById[];
    private final List<Rule> rules = new ArrayList<>();

    public CSPSolver(int nVariables) {
        this(nVariables, null);
    }

    CSPSolver(int nVariables, UpdateHandler<CSPSolverUpdate> handler) {
        variablesById = new Variable[nVariables];
        for (int i = 0; i < nVariables; i++) {
            variablesById[i] = new Variable(i);
        }
        this.handler = handler;
    }

    /**
     * Adds a new constraint (or rule) to the problem.
     *
     * @param sum    Exactly how many variables in the rule are true?
     * @param varIds ids of the boolean variables (0...N-1)
     */
    public void addRule(int sum, int... varIds) {
        Rule newRule = new Rule(sum, rules.size());
        rules.add(newRule);

        for (int i : varIds) {
            newRule.variables.add(variablesById[i]);
            variablesById[i].rules.add(newRule);
        }
    }

    /**
     * Solves the CSP.
     *
     * @param solution array to put the solution in. solution[i] is the number of solutions where variable(i) is set.
     *                 should be the size of the total number of variables.
     * @return total number of solutions.
     */
    public BigDecimal[] solveApproximate(BigDecimal[][] solution) throws InterruptedException {
        SearchNode solvedState = doAStar();
        if (solvedState == null) {
            throw new IllegalStateException("A* failed to find a valid order to solveApproximate the problem");
        }

        List<Variable> processOrder = getProcessingOrder(solvedState);

        return getIndependentVariableProbability(solution, processOrder);
    }

    private List<Variable> getProcessingOrder(SearchNode solvedState) {

        List<Variable> processOrder = new ArrayList<>();
        Stack<SearchNode> startToFinish = new Stack<>();
        while (solvedState.parent != null) {
            startToFinish.add(solvedState);
            solvedState = solvedState.parent;
        }
        while (!startToFinish.empty()) {
            SearchNode n = startToFinish.pop();
            for (Variable v : n.frontier) {
                if (!processOrder.contains(v)) {
                    processOrder.add(v);
                }
            }
        }
        for (Variable aVariablesById : variablesById) {
            if (!processOrder.contains(aVariablesById)) {
                processOrder.add(aVariablesById);
            }
        }
        return processOrder;
    }

    private BigDecimal[] getIndependentVariableProbability(
            BigDecimal[][] output,
            List<Variable> order
    ) throws InterruptedException {
        List<Variable> frontier = new ArrayList<>();
        Set<Rule> satisfied = new HashSet<>();

        Map<SolutionKey, Solution> solutions = new HashMap<>();

        Solution seedValue = new Solution(order.size());
        seedValue.totalSolutions = BigDecimal.ONE;
        solutions.put(new SolutionKey(0, 0), seedValue);

        int _i = 0;
        for (Variable var : order) {
            _i++;
            final int _fi = _i;
            try {
                solutions = openVariable(solutions, var, frontier, order.size());
            } catch (IllegalStateException e) {
                reportProgressImmediate(new CSPSolverUpdate(
                        "Too many variables at once - cannot solveApproximate with frontier DP method"));
                throw e;
            }
            reportProgress(() -> new CSPSolverUpdate("Processing variable " + _fi + " out of " + order.size()));
            // Add any numbered squares to covered if all its neighbours are in the frontier.
            for (Rule r : var.rules) {
                if (frontier.containsAll(r.variables)) {
                    satisfied.add(r);
                }
            }

            // Remove frontier items that have only covered neighbours. There is no use to keep exploring its
            // possibilities since we don't need it to satisfy any future uncovered squares.
            for (int i = 0; i < frontier.size(); i++) {
                if (satisfied.containsAll(frontier.get(i).rules)) {
                    solutions = closeVariable(solutions, frontier, i, order.size());

                    i--;
                }
            }
            frontier.removeIf(v -> satisfied.containsAll(v.rules));
        }

        BigDecimal[] retVal = new BigDecimal[order.size() + 1];
        for (int i = 0; i <= order.size(); i++) {
            if (solutions.containsKey(new SolutionKey(i, 0))) {
                System.arraycopy(solutions.get(new SolutionKey(i, 0)).setCount, 0, output[i], 0, order.size());
            } else {
                Arrays.fill(output[i], BigDecimal.ZERO);
            }
            retVal[i] = solutions.getOrDefault(new SolutionKey(i, 0), new Solution(0)).totalSolutions;
        }
        return retVal;
    }

    /**
     * Adds a variable to the frontier by permutation of its possible values among all the current solutions to the
     * current frontier.
     *
     * @param solutions    current solutions to the current frontier
     * @param var          variable to add to the frontier
     * @param frontier     current frontier to add the variable to
     * @param numVariables total number of variables being solved in the system
     * @return the new solution vector with variable added
     */
    private static Map<SolutionKey, Solution> openVariable(
            Map<SolutionKey, Solution> solutions,
            Variable var,
            List<Variable> frontier,
            int numVariables
    ) throws IllegalStateException {
        Map<SolutionKey, Solution> tempSolutions = new HashMap<>();

        for (int newVal = 0; newVal <= 1; newVal++) {
            for (Map.Entry<SolutionKey, Solution> entry : solutions.entrySet()) {
                boolean possible = true;

                for (Rule r : var.rules) {
                    int numSatisfied = newVal;
                    int numFree = r.variables.size() - 1;
                    for (int i = 0; i < frontier.size(); i++) {
                        if (r.variables.contains(frontier.get(i))) {
                            if ((entry.getKey().frontierSet & (1 << i)) > 0) {
                                numSatisfied++;
                            }
                            numFree--;
                        }
                    }
                    if (numSatisfied > r.targetSum || r.targetSum > numSatisfied + numFree) {
                        possible = false;
                    }
                }


                if (possible) {
                    if (entry.getKey().frontierSet > (1L << 60)) {
                        throw new IllegalStateException(
                                "Too many variables at once - cannot solveApproximate with frontier DP method");
                    }
                    long newFrontierSet = (entry.getKey().frontierSet << 1) | newVal;
                    SolutionKey newSolutionKey = new SolutionKey(entry.getKey().numTrue + newVal, newFrontierSet);
                    Solution currSolution = getOrDefault(tempSolutions, newSolutionKey, numVariables);

                    for (int i = 0; i < numVariables; i++) {
                        currSolution.setCount[i] = currSolution.setCount[i].add(entry.getValue().setCount[i]);
                    }
                    currSolution.totalSolutions = currSolution.totalSolutions.add(entry.getValue().totalSolutions);
                }
            }
        }
        frontier.add(0, var);

        return tempSolutions;
    }

    /**
     * Removes a variable from the frontier, by collapsing each frontier solution into more general solutions that
     * assigns a probability to whether the removed variable is set or not.
     *
     * @param solutions    current solution vector
     * @param frontier     frontier to remove the variable from
     * @param index        index into the frontier to remove
     * @param numVariables total number of variables being solved in the system
     * @return new solution vector with variable removed
     */
    private static Map<SolutionKey, Solution> closeVariable(
            Map<SolutionKey, Solution> solutions,
            List<Variable> frontier,
            int index,
            int numVariables
    ) {
        Map<SolutionKey, Solution> tempSolutions = new HashMap<>();

        // Remove the frontier variable, since it's not part of any unexplored rules anymore.
        for (Map.Entry<SolutionKey, Solution> entry : solutions.entrySet()) {
            long leftMask = (1 << index) - 1;
            long rightMask = ((1 << frontier.size()) - 1) & ~((leftMask << 1) | 1);
            long newFrontierSet = (entry.getKey().frontierSet & leftMask) | ((entry.getKey().frontierSet & rightMask) >> 1);

            SolutionKey newSolutionKey = new SolutionKey(entry.getKey().numTrue, newFrontierSet);
            Solution currSolution = getOrDefault(tempSolutions, newSolutionKey, numVariables);

            for (int i = 0; i < numVariables; i++) {
                currSolution.setCount[i] = currSolution.setCount[i].add(entry.getValue().setCount[i]);
            }

            if ((entry.getKey().frontierSet & (1 << index)) > 0) {
                currSolution.setCount[frontier.get(index).id] = currSolution.setCount[frontier.get(index).id].add(
                        entry.getValue().totalSolutions);
            }

            currSolution.totalSolutions = currSolution.totalSolutions.add(entry.getValue().totalSolutions);
        }

        frontier.remove(index);
        return tempSolutions;
    }

    private static Solution getOrDefault(
            Map<SolutionKey, Solution> solutions,
            SolutionKey key,
            int initSize
    ) {
        if (!solutions.containsKey(key)) {
            solutions.put(key, new Solution(initSize));
        }
        return solutions.get(key);

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

                    SearchNode current = thisNode;
                    while (current.parent != null) {
                        BitSet next = (BitSet) current.completedRules.clone();
                        next.xor(current.parent.completedRules);

                        current = current.parent;
                    }
                    return thisNode;
                }

                reportProgress(() -> new CSPSolverUpdate(
                        "A* in progress. " + "Cost so far: " + (thisNode.cost + thisNode.heuristic) +
                                " Nodes visited: " + thisNodesVisited + ". Queue size: " + domain.size())
                );

                domain.addAll(thisNode.getNeighbours(rules));
            }
        }

        return null;
    }

    private static class SearchNode implements Comparable<SearchNode> {
        final BitSet completedRules;
        final double cost;
        final double heuristic;
        final int numCompleted;
        final HashSet<Variable> frontier;
        final SearchNode parent;
        final int numRules;

        SearchNode(int numRules) {
            this(new BitSet(numRules), numRules, null, new HashSet<>(), 0, 0);
        }

        SearchNode(
                BitSet completedRules,
                int numRules,
                SearchNode parent,
                HashSet<Variable> frontier,
                double cost,
                int numCompleted
        ) {
            this.completedRules = completedRules;
            this.parent = parent;
            this.frontier = frontier;
            this.cost = cost;
            this.numCompleted = numCompleted;
            this.numRules = numRules;


            heuristic = (numRules - numCompleted) * 500 * (frontier.size() + 5);
        }

        List<SearchNode> getNeighbours(List<Rule> allRules) {
            List<SearchNode> neighbours = new ArrayList<>();

            HashSet<Rule> candidates = new HashSet<>();
            for (Variable r : frontier) {
                candidates.addAll(r.rules);
            }
            candidates.removeIf(candidate -> completedRules.get(candidate.id));
            if (candidates.isEmpty()) {
                candidates.addAll(allRules);
            }
            candidates.removeIf(candidate -> completedRules.get(candidate.id));

            for (Rule r : candidates) {
                HashSet<Variable> newFrontier = new HashSet<>(frontier);
                newFrontier.addAll(r.variables);
                BitSet newBitSet = (BitSet) completedRules.clone();
                newFrontier.removeIf(v -> v.rules.stream().allMatch(rr -> newBitSet.get(rr.id)));
                newBitSet.set(r.id);

                neighbours.add(
                        new SearchNode(newBitSet, numRules, this, newFrontier, this.cost + (1 << newFrontier.size()),
                                numCompleted + 1));
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


    private static class Rule {
        final List<Variable> variables = new ArrayList<>();
        final int targetSum;
        final int id;

        Rule(int targetSum, int id) {
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
            String varPart = String.join(" + ",
                    variables.stream().map(r -> Integer.toString(r.id)).collect(Collectors.toList()));
            return "(Rule: " + id + ": " + varPart + " = " + targetSum + ")";
        }
    }

    private static class Variable {
        final List<Rule> rules = new ArrayList<>();
        final int id;

        Variable(int id) {
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

    private static class SolutionKey {
        final int numTrue;
        // Decided on a single long because BitSet cannot shift :( Also when on earth will we be playing a
        // minesweeper game with a group width of > 64 items? If this CSP needs 60 variables in the frontier,
        // it probably won't solveApproximate in time anyway.
        final long frontierSet;

        SolutionKey(int numTrue, long frontierSet) {
            this.numTrue = numTrue;
            this.frontierSet = frontierSet;
        }

        @Override
        public int hashCode() {
            return numTrue * 37 + (int) (frontierSet % 1000000009);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof SolutionKey) {
                return this.frontierSet == ((SolutionKey) other).frontierSet && this.numTrue == ((SolutionKey) other).numTrue;
            }
            return false;
        }
    }

    private static class Solution {

        final BigDecimal[] setCount;
        BigDecimal totalSolutions;

        Solution(int size) {
            this.totalSolutions = BigDecimal.ZERO;
            this.setCount = new BigDecimal[size];
            Arrays.fill(setCount, BigDecimal.ZERO);
        }
    }
}
