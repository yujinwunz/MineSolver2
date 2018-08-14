package com.skyplusplus.minesolver.core;

import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class MineLocationTest {

    @Test
    public void shouldCacheSmallValues() {
        MineLocation zeroZero = MineLocation.ofValue(0, 0);
        MineLocation oneOne = MineLocation.ofValue(1,1);
        MineLocation zeroZero2 = MineLocation.ofValue(0, 0);
        MineLocation oneOne2 = MineLocation.ofValue(1, 1);
        MineLocation oneZero = MineLocation.ofValue(1, 0);
        MineLocation oneZero2 = MineLocation.ofValue(1, 0);
        MineLocation twoTwo = MineLocation.ofValue(2, 2);
        MineLocation zeroOne = MineLocation.ofValue(0, 1);

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
        MineLocation.ofValue(-1, -1);
        MineLocation.ofValue(-100, -100);
        MineLocation.ofValue(1000000, -1000000);
        MineLocation.ofValue(Integer.MAX_VALUE, Integer.MAX_VALUE);
        MineLocation.ofValue(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
}
