package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.BoardCoord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class BoardCoordTest {

    @Test
    public void shouldCacheSmallValues() {
        BoardCoord zeroZero = BoardCoord.ofValue(0, 0);
        BoardCoord oneOne = BoardCoord.ofValue(1,1);
        BoardCoord zeroZero2 = BoardCoord.ofValue(0, 0);
        BoardCoord oneOne2 = BoardCoord.ofValue(1, 1);
        BoardCoord oneZero = BoardCoord.ofValue(1, 0);
        BoardCoord oneZero2 = BoardCoord.ofValue(1, 0);
        BoardCoord twoTwo = BoardCoord.ofValue(2, 2);
        BoardCoord zeroOne = BoardCoord.ofValue(0, 1);

        assertEquals(0, oneZero.getY());
        assertEquals(1, oneZero2.getX());
        assertEquals(2, twoTwo.getX());
        assertEquals(1, zeroOne.getY());

        assertSame(zeroZero, zeroZero2);
        assertSame(oneOne, oneOne2);
        assertSame(oneZero, oneZero2);
        assertNotSame(zeroZero, oneZero);
        assertNotSame(zeroZero2, oneOne2);
        assertNotSame(oneZero2, twoTwo);
        assertNotSame(oneOne, oneZero);
    }

    @Test
    public void shouldNotCrashOnLargeValues() {
        BoardCoord.ofValue(-1, -1);
        BoardCoord.ofValue(-100, -100);
        BoardCoord.ofValue(1000000, -1000000);
        BoardCoord.ofValue(Integer.MAX_VALUE, Integer.MAX_VALUE);
        BoardCoord.ofValue(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
}
