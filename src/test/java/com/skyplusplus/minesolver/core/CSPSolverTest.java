package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.ai.frontier.CSPSolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class CSPSolverTest {
    @Test
    public void shouldSolveSimpleEquations() throws InterruptedException {
        CSPSolver solver = new CSPSolver(3);
        solver.addRule(2, 0, 1, 2);
        solver.addRule(1, 0, 1);
        solver.addRule(1, 0, 2);

        BigDecimal[][] solution = new BigDecimal[3+1][3];
        BigDecimal[] totalSolutions = solver.solveApproximate(solution);

        assertArrayEquals(totalSolutions, new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
        });

        assertEquals(totalSolutions[2], solution[2][1]);
        assertEquals(totalSolutions[2], solution[2][2]);
        assertEquals(BigDecimal.ZERO, solution[2][0]);
        assertNotEquals(BigDecimal.ZERO, totalSolutions[2]);

        solver = new CSPSolver(5);
        solver.addRule(2, 0, 1, 2);
        solver.addRule(1, 3, 4);
        solver.addRule(1, 1, 2, 3);
        solver.addRule(2, 1, 3, 4);
        solution = new BigDecimal[5+1][5];
        totalSolutions = solver.solveApproximate(solution);

        assertNotEquals(BigDecimal.ZERO, totalSolutions[3]);
        assertArrayEquals(totalSolutions, new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        });
        assertArrayEquals(solution[3], new BigDecimal[] {
                totalSolutions[3],
                totalSolutions[3],
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                totalSolutions[3]
        });
    }

    @Test
    public void shouldSolveUncertainEquations() throws InterruptedException {
        CSPSolver solver = new CSPSolver(5);
        solver.addRule(3, 0, 1, 2, 3, 4);
        solver.addRule(1, 0, 1);
        solver.addRule(1, 3, 4);

        BigDecimal[][] solution = new BigDecimal[5+1][5];
        BigDecimal[] totalSolutions = solver.solveApproximate(solution);
        assertArrayEquals(totalSolutions, new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(4),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        });

        assertEquals(BigDecimal.ZERO, totalSolutions[3].remainder(BigDecimal.valueOf(2)));
        BigDecimal totalSolutions2 = totalSolutions[3].divide(BigDecimal.valueOf(2), RoundingMode.DOWN);
        assertArrayEquals(solution[3], new BigDecimal[]{
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

        BigDecimal[][] solution = new BigDecimal[6+1][6];
        BigDecimal[] totalSolutions = solver.solveApproximate(solution);
        assertArrayEquals(totalSolutions, new BigDecimal[]{
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(3),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
        });

        assertEquals(BigDecimal.ZERO, totalSolutions[3].remainder(BigDecimal.valueOf(3)));
        assertEquals(BigDecimal.ZERO, totalSolutions[4].remainder(BigDecimal.valueOf(3)));
        BigDecimal totalSolutions3 = totalSolutions[3].divide(BigDecimal.valueOf(3), RoundingMode.DOWN);
        assertArrayEquals(solution[3], new BigDecimal[]{
                totalSolutions3,
                totalSolutions3,
                totalSolutions3,
                BigDecimal.ZERO,
                totalSolutions[3],
                totalSolutions[3]
        });
        assertArrayEquals(solution[4], new BigDecimal[]{
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
            BigDecimal[][] solution = new BigDecimal[size+1][size];
            BigDecimal[] totalSolutions = solver.solveApproximate(solution);

            for (int i = 0; i <= size; i++) {
                if (i == size/2) {
                    assertNotEquals(BigDecimal.ZERO, totalSolutions[i]);
                } else {
                    assertEquals(BigDecimal.ZERO, totalSolutions[i]);
                }
            }

            assertEquals(BigDecimal.ZERO, totalSolutions[size/2].remainder(BigDecimal.valueOf(2)));
            BigDecimal totalSolutions2 = totalSolutions[size/2].divide(BigDecimal.valueOf(2), BigDecimal.ROUND_DOWN);

            BigDecimal[] correctSolution = new BigDecimal[size];
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

        BigDecimal[][] solution = new BigDecimal[10+1][10];
        assertEquals(BigDecimal.ONE, solver.solveApproximate(solution)[5]);
    }
}
