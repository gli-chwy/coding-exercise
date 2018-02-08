package org.gli58.game;

import org.gli58.BadMonsterPlacementException;
import org.gli58.domain.City;
import org.gli58.domain.Direction;
import org.gli58.event.ConsoleLoggingEventHandler;
import org.gli58.event.EventHandler;
import org.gli58.event.FightEvent;
import org.gli58.domain.Monster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Modeling a game as per the instructions.
 *
 */
public class Game {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * remaining monsters in the game
     */
    private Map<Monster, City> monsters;

    /**
     * remaining cities in the game
     */
    private Set<City> cities;

    /**
     * trapped monsters due to all neighboring cities being destroyed
     */
    private Set<Monster> trappedMonsters = new HashSet<>();

    /**
     * unique ID for a particular monster fight
     */
    private long fightEventId = 1;

    /**
     * the sequence of a particular move in playing the game
     */
    private long moveId = 0;

    /**
     * event handler to handle fight events and other types of events as well
     *
     * This can be injected through some dependency injection framework, but here
     * it is set for simplicity
     */
    private EventHandler eventHandler = new ConsoleLoggingEventHandler();

    private RandomMonsterMoveProvider randomMonsterMoveProvider = new RandomMonsterMoveProvider();

    /**
     * Creates a game in which monsters will be initially distributed randomly.
     *
     * @param cities cities in World X
     * @param monsterCount initial number of monsters in the game
     */
    public Game(Set<City> cities, int monsterCount) {
        this(cities, monsterCount, new RandomMonsterPlacement());
    }

    /**
     * Creates a game specifying how the monsters will be distributed initially.
     *
     * Note this constructor has package access and intended for testability.
     *
     * @param cities
     * @param monsterCount
     * @param monsterDistributor
     */
    Game(Set<City> cities, int monsterCount, MonsterPlacement monsterDistributor) {
        if (cities == null || cities.isEmpty()) {
            throw new IllegalArgumentException("there has to be at least one city");
        }

        if (monsterCount < 1) {
            throw new IllegalArgumentException("there has to be at least one monster");
        }

        this.cities = new HashSet<>(cities);

        monsters = new LinkedHashMap<>();
        for (int i=1; i<=monsterCount; i++) {
            Monster monster = new Monster(i);
            monsters.put(monster, null);
        }

        //basic validation to make sure the monsterDistributor did not modify our data beyong
        //distributing the monsters across the cities
        final Map<Monster, City> monsterCityMap = monsterDistributor.apply(this.cities, monsters.keySet());

        if (!monsterCityMap.keySet().equals(monsters.keySet())) {
            throw new BadMonsterPlacementException(
                    "monster distribution contains different monsters than oiginally supplied");
        }

        if (! this.cities.containsAll(monsterCityMap.values())) {
            throw new BadMonsterPlacementException(
                    "monster distribution contains unknown cities than oiginally supplied");
        }

        monsters = new LinkedHashMap<>(monsterCityMap);
    }

    public Map<Monster, City> getMonsters() {
        return Collections.unmodifiableMap(monsters);
    }

    public Set<City> getCities() {
        return Collections.unmodifiableSet(cities);
    }

    public Set<Monster> getTrappedMonsters() {
        return Collections.unmodifiableSet(trappedMonsters);
    }

    public long getFightEventId() {
        return fightEventId;
    }

    public long getMoveId() {
        return moveId;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        if (eventHandler == null) {
            throw new IllegalArgumentException("eventHandler cannot be null");
        }
        this.eventHandler = eventHandler;
    }

    public boolean playOnce() {
        return playOnce(randomMonsterMoveProvider);
    }

    /**
     *
     * @return flag to indicate whether it's meaningful to continue play. true indicates potential progress,
     *  while false indicates continuing will not make any progress.
     */
    boolean playOnce(MonsterMoveProvider monsterMoveProvider) {
        fight();

        if (logger.isDebugEnabled()) {
            logger.debug(getSummaryDescription());
        }

        //the following two checks make sense as no further progress is possible
        //but commmenting out since they do not match the requirement
        /*
        if (monsters.size() <= 1) {
            return false;
        }

        if (trappedMonsters.size() == monsters.size()) {
            return false;
        }
        */

        //Assumption here is the monster never hides itself in a particular city, that is,
        //it will always move if there is at least one way out of its city

        Map<Monster, Direction> fixedNextMoves = new HashMap<>();

        Map<Monster, City> movesMonstersNeedToChoose = new HashMap<>();

        for (Map.Entry<Monster, City> e : monsters.entrySet()) {
            final List<Direction> directions = new ArrayList(e.getValue().getNeighbors().keySet());

            if (directions.size() == 0) {
                trappedMonsters.add(e.getKey());
            } else if (directions.size() == 1) {
                fixedNextMoves.put(e.getKey(), directions.get(0));
            } else if (directions.size() > 1) {
                movesMonstersNeedToChoose.put(e.getKey(), e.getValue());
            }
        }

        fixedNextMoves.entrySet().forEach(this::processNextMove);

        if (movesMonstersNeedToChoose.size() > 0) {
            Map<Monster, Direction> randomNextMoves = monsterMoveProvider.apply(movesMonstersNeedToChoose);
            randomNextMoves.entrySet().forEach(this::processNextMove);
        }

        moveId++;

        return true;
    }

    private void processNextMove(Map.Entry<Monster, Direction> e) {
        City currentCity = monsters.get(e.getKey());
        Direction neighborDirection = e.getValue();
        City neighbor = currentCity.getNeighbor(neighborDirection);
        if (neighbor != null) {
            monsters.replace(e.getKey(), neighbor);
        }
    }

    private void fight() {
        //aggregate monsters by city
        Map<City, Set<Monster>> monstersByCity = new HashMap<>();
        for (Monster m : monsters.keySet()) {
            City c = monsters.get(m);
            monstersByCity.computeIfAbsent(c, (unused) -> new HashSet<>()).add(m);
        }

        Set<City> citiesToRemove = new HashSet<>();
        Set<Monster> monstersToRemove = new HashSet<>();

        //check if there are cities where multiple monsters meet and fight
        for (City c : monstersByCity.keySet()) {
            Set<Monster> monsters = monstersByCity.get(c);
            if (monsters != null && monsters.size() > 1) {
                FightEvent fightEvent = new FightEvent(fightEventId, c, monsters, moveId);
                eventHandler.handle(fightEvent);

                citiesToRemove.add(c);
                monstersToRemove.addAll(monsters);

                fightEventId++;
            }
        }

        citiesToRemove.stream().forEach(this::removeCity);
        monstersToRemove.stream().forEach(monsters::remove);
    }

    private void removeCity(City city) {
        //first remove link to this city from all its neighbors
        city.getNeighbors().entrySet().stream().forEach(e -> {
            Direction d = e.getKey();
            City c = e.getValue();
            c.removeNeighbor(Direction.opposite(d));
        });

        //remove the city itself
        cities.remove(city);
    }


    public String getSummaryDescription() {
        return "Game{move=" + moveId +
                ", total monsters=" + monsters.size() +
                ", trapped monsters=" + trappedMonsters.size() +
                ", cities=" + cities.size() + "}";
    }
}

