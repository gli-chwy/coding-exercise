package org.gli58.game;

import org.gli58.domain.City;
import org.gli58.domain.Monster;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@FunctionalInterface
public interface MonsterPlacement extends BiFunction<Set<City>, Set<Monster>, Map<Monster, City>> {
}
