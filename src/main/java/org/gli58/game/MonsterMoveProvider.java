package org.gli58.game;

import org.gli58.domain.City;
import org.gli58.domain.Direction;
import org.gli58.domain.Monster;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@FunctionalInterface
public interface MonsterMoveProvider extends Function<Map<Monster, City>, Map<Monster, Direction>> {
}
