package com.skyplusplus.minesolver.core.ai.frontier;

import com.skyplusplus.minesolver.core.ai.MineSweeperAI;
import com.skyplusplus.minesolver.core.ai.Move;
import com.skyplusplus.minesolver.core.ai.UpdateHandler;
import com.skyplusplus.minesolver.core.gamelogic.MineLocation;
import com.skyplusplus.minesolver.core.gamelogic.PlayerState;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class FrontierAI implements MineSweeperAI {

    @Override
    public Move calculate(PlayerState state) {
        return calculate(state, null);
    }

    @Override
    public Move calculate(PlayerState state, UpdateHandler handler) {
        throw new NotImplementedException();
    }

    private class SearchNode {
        private Set<MineLocation> pendingProbed = new HashSet<>();
        private Set<MineLocation> frontierUnknown = new HashSet<>();
        private Set<MineLocation> finishedProbed = new HashSet<>();

        public SearchNode expand(PlayerState state, MineLocation nextProbed) {
            throw new NotImplementedException();
        }

        private SearchNode copy() {
            SearchNode retVal = new SearchNode();
            retVal.finishedProbed = new HashSet<>(finishedProbed);
            retVal.frontierUnknown = new HashSet<>(frontierUnknown);
            retVal.pendingProbed = new HashSet<>(pendingProbed);
            return retVal;
        }
    }
}
