package org.gli58.game;

import java.util.List;
import java.util.function.BiFunction;

@FunctionalInterface
interface MonsterPlacementProvider extends BiFunction<List<City>, Monster, City> {
}
