package org.gli58.game;

import org.gli58.domain.City;
import org.gli58.domain.Direction;
import org.gli58.domain.Monster;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomMonsterMoveProvider implements MonsterMoveProvider {
    @Override
    public Map<Monster, Direction> apply(Map<Monster, City> monsterCityMap) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Map<Monster, Direction> nextMoves = new HashMap<>();

        for (Map.Entry<Monster, City> e : monsterCityMap.entrySet()) {
            final List<Direction> directions = new ArrayList(e.getValue().getNeighbors().keySet());
            nextMoves.put(e.getKey(), directions.get(random.nextInt(directions.size())));
        }

        return nextMoves;
    }
}
