package org.gli58.game;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class RandomMonsterMoveProvider implements MonsterMoveProvider {
    @Override
    public Direction apply(Monster monster, List<Direction> directions) {
        return directions.get(ThreadLocalRandom.current().nextInt(directions.size()));
    }
}
