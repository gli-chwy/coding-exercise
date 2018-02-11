package org.gli58.game;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * City is immutable in terms of its name, but the neighbor data is mutable.
 * This way we easily have unique cities in the game and also easily update
 * the neighboring relations among them as game is played.
 *
 */
public class City {
    private final String name;

    public final Lock lock = new ReentrantLock();

    //eagerly initialize as majority of the cities should be connected anyway
    private EnumMap<Direction, City> neighbors = new EnumMap<Direction, City>(Direction.class);

    public City(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("city name $name not valid");
        }

        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public City getNeighbor(Direction direction) {
        return neighbors.get(direction);
    }

    public void addNeighbor(Direction direction, City city) {
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }

        if (city == null) {
            throw new IllegalArgumentException("neighboring city cannot be null");
        }

        //TODO check requirement if it is data error if there is already a different city in the same direction
        neighbors.put(direction, city);
    }

    public void removeNeighbor(Direction direction) {
        neighbors.remove(direction);
    }

    public Map<Direction, City> getNeighbors() {
        return Collections.unmodifiableMap(neighbors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Objects.equals(name, city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
