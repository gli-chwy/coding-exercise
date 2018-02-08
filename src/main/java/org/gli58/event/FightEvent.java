package org.gli58.event;

import org.gli58.domain.City;
import org.gli58.domain.Monster;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Modeling an immutable fight event.
 */
public class FightEvent implements Event {
    private final long id;
    private final City city;
    private final Set<Monster> monsters;
    private final long moveId;

    public FightEvent(long id, City city, Set<Monster> monsters, long moveId) {
        if (city == null) {
            throw new IllegalArgumentException("city cannot be null");
        }

        if (monsters == null || monsters.size() <= 1) {
            throw new IllegalArgumentException("there has to be at least two monsters to get in a fight");
        }

        this.id = id;
        this.city = city;
        this.monsters = Collections.unmodifiableSet(monsters);
        this.moveId = moveId;
    }

    public FightEvent(long id, City city, long moveId, Monster... monsters) {
        this(id, city, new HashSet<>(Arrays.asList(monsters)), moveId);
    }

    public long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public Set<Monster> getMonsters() {
        return monsters;
    }

    public long getMoveId() {
        return moveId;
    }

    /**
     * This returns event summary.
     *
     * For flexibility, the individual fields are exposed as well which the client
     * can use directly.
     *
     * @return event summary as per requirement
     */
    @Override
    public String getAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(city.getName());
        sb.append(" has been destroyed by ");

        //TODO we can output the monsters in a certain way if required
        String monstersText = monsters.stream().map(m -> "monster " + m.getId()).collect(joining(" and "));

        sb.append(monstersText);
        sb.append("!");

        return sb.toString();
    }
}
