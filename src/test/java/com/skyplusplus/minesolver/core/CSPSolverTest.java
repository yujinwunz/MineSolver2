package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.frontier.CSPSolver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class CSPSolverTest {
    @Test
    public void shouldSolveSimpleEquations() throws InterruptedException {
        CSPSolver solver = new CSPSolver(3);
        solver.addRule(2, 0, 1, 2);
        solver.addRule(1, 0, 1);
        solver.addRule(1, 0, 2);

        BigInteger[][] solution = new BigInteger[3+1][3];
        BigInteger[] totalSolutions = solver.solve(solution);

        assertArrayEquals(totalSolutions, new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.ZERO,
        });

        assertEquals(totalSolutions[2], solution[2][1]);
        assertEquals(totalSolutions[2], solution[2][2]);
        assertEquals(BigInteger.ZERO, solution[2][0]);
        assertNotEquals(BigInteger.ZERO, totalSolutions[2]);

        solver = new CSPSolver(5);
        solver.addRule(2, 0, 1, 2);
        solver.addRule(1, 3, 4);
        solver.addRule(1, 1, 2, 3);
        solver.addRule(2, 1, 3, 4);
        solution = new BigInteger[5+1][5];
        totalSolutions = solver.solve(solution);

        assertNotEquals(BigInteger.ZERO, totalSolutions[3]);
        assertArrayEquals(totalSolutions, new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.ZERO,
                BigInteger.ZERO
        });
        assertArrayEquals(solution[3], new BigInteger[] {
                totalSolutions[3],
                totalSolutions[3],
                BigInteger.ZERO,
                BigInteger.ZERO,
                totalSolutions[3]
        });
    }

    @Test
    public void shouldSolveUncertainEquations() throws InterruptedException {
        CSPSolver solver = new CSPSolver(5);
        solver.addRule(3, 0, 1, 2, 3, 4);
        solver.addRule(1, 0, 1);
        solver.addRule(1, 3, 4);

        BigInteger[][] solution = new BigInteger[5+1][5];
        BigInteger[] totalSolutions = solver.solve(solution);
        assertArrayEquals(totalSolutions, new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.valueOf(4),
                BigInteger.ZERO,
                BigInteger.ZERO
        });

        assertEquals(BigInteger.ZERO, totalSolutions[3].remainder(BigInteger.valueOf(2)));
        BigInteger totalSolutions2 = totalSolutions[3].divide(BigInteger.valueOf(2));
        assertArrayEquals(solution[3], new BigInteger[]{
                totalSolutions2,
                totalSolutions2,
                totalSolutions[3],
                totalSolutions2,
                totalSolutions2
        });
    }

    @Test
    public void shouldSolveDisjointEquations() throws InterruptedException {
        CSPSolver solver = new CSPSolver(6);
        solver.addRule(1, 0, 1, 2);
        solver.addRule(2, 4, 5);

        BigInteger[][] solution = new BigInteger[6+1][6];
        BigInteger[] totalSolutions = solver.solve(solution);
        assertArrayEquals(totalSolutions, new BigInteger[]{
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.valueOf(3),
                BigInteger.valueOf(3),
                BigInteger.ZERO,
                BigInteger.ZERO,
        });

        assertEquals(BigInteger.ZERO, totalSolutions[3].remainder(BigInteger.valueOf(3)));
        assertEquals(BigInteger.ZERO, totalSolutions[4].remainder(BigInteger.valueOf(3)));
        BigInteger totalSolutions3 = totalSolutions[3].divide(BigInteger.valueOf(3));
        assertArrayEquals(solution[3], new BigInteger[]{
                totalSolutions3,
                totalSolutions3,
                totalSolutions3,
                BigInteger.ZERO,
                totalSolutions[3],
                totalSolutions[3]
        });
        assertArrayEquals(solution[4], new BigInteger[]{
                totalSolutions3,
                totalSolutions3,
                totalSolutions3,
                totalSolutions[3],
                totalSolutions[3],
                totalSolutions[3]
        });
    }

    @Test
    public void shouldSolveLargeEquations() {
        int size = 116*6;
        CSPSolver solver = new CSPSolver(size);

        List<Integer> randomVars = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            randomVars.add(i);
        }
        Collections.shuffle(randomVars);

        for (int i = 0; i <= size-6; i++) {
            solver.addRule(3, randomVars.get(i), randomVars.get(i+1), randomVars.get(i+2), randomVars.get(i+3), randomVars.get(i+4), randomVars.get(i+5));
        }

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            BigInteger[][] solution = new BigInteger[size+1][size];
            BigInteger[] totalSolutions = solver.solve(solution);

            for (int i = 0; i <= size; i++) {
                if (i == size/2) {
                    assertNotEquals(BigInteger.ZERO, totalSolutions[i]);
                } else {
                    assertEquals(BigInteger.ZERO, totalSolutions[i]);
                }
            }

            assertEquals(BigInteger.ZERO, totalSolutions[size/2].remainder(BigInteger.valueOf(2)));
            BigInteger totalSolutions2 = totalSolutions[size/2].divide(BigInteger.valueOf(2));

            BigInteger[] correctSolution = new BigInteger[size];
            Arrays.fill(correctSolution, totalSolutions2);
            assertArrayEquals(correctSolution, solution[size/2]);
        });
    }

    @Test
    public void shouldSolveComplexEquations() throws InterruptedException {
        //**.**...*.
        //0123456789

        CSPSolver solver = new CSPSolver(10);
        solver.addRule(3, 1, 2, 4, 5, 8, 9);
        solver.addRule(3, 1, 3, 6, 7, 8);
        solver.addRule(3, 1, 3, 4, 6, 9);
        solver.addRule(3, 0, 3, 5, 7, 8, 9);
        solver.addRule(5, 0, 1, 3, 4, 7, 8);
        solver.addRule(2, 3, 5, 6, 7, 8);
        solver.addRule(1, 2, 5, 6, 7, 9, 4);

        BigInteger[][] solution = new BigInteger[10+1][10];
        assertEquals(BigInteger.ONE, solver.solve(solution)[5]);
    }
}
