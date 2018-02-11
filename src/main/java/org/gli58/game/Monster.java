package org.gli58.game;

import org.gli58.game.event.FightEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.partitioningBy;

public class Monster implements Runnable {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public enum Status {ACTIVE, TRAPPED, KILLED, TIRED, ERRORED};

    private final Game game;
    private final long id;
    private final long mininumMoves;

    private City occupiedCity;
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    private long moveId = 1;
    private AtomicReference<Status> status;

    public Monster(long id, long mininumMoves, Game game) {
        if (mininumMoves < 1) {
            throw new IllegalArgumentException("monster is not allowed to move at all");
        }

        if (game == null) {
            throw new IllegalArgumentException("monster needs to belong to a game");
        }

        this.game = game;
        this.id = id;
        this.mininumMoves = mininumMoves;

        this.status = new AtomicReference(Status.ACTIVE);
    }

    public long getId() {
        return id;
    }

    public Status getStatus() {
        return status.get();
    }

    public void setOccupiedCity(City newCity) {
        this.occupiedCity = newCity;
    }

    private void setStatus(Status status) {
        this.status.set(status);
        game.monsterStatusChanged(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Monster monster = (Monster) o;
        return id == monster.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "monster " + id;
    }

    public void gotIntoFightAndKilled() {
        setStatus(Status.KILLED);
        shouldStop.set(true);
    }

    @Override
    public void run() {
        try {
            runWithErrorHandled();

        } catch (Throwable e) {
            logger.error("monster {} encountered error", getId(), e);
            setStatus(Status.ERRORED);
        }
    }

    private void runWithErrorHandled() {
        logger.debug("monster {} running", getId());

        if (shouldStop.get()) {
            logger.debug("monster {} killed", getId());
            return;
        }

        if (moveId > mininumMoves) {
            setStatus(Status.TIRED);
            logger.debug("monster {} tired", getId());
            return;
        }

        //the logic behind this call has been refactored into multiple methods
        //with nested lock acquisition. for better readability.
        tryFindAndOccupy(occupiedCity);

        moveId++;
    }

    private void tryFindAndOccupy(City fromCity) {
        if (fromCity == null) {
            List<City> cityList = new ArrayList(game.getCities());
            City startCity = game.getMonsterPlacementProvider().apply(cityList, this);
            logger.debug("monster {} started in {}", getId(), startCity);
            tryOccupyWithFromCityLocked(null, startCity);
            return;
        }

        boolean fromCityLocked = false;

        try {
            fromCityLocked = fromCity.lock.tryLock();
            if (fromCityLocked) {
                City nextCity = findNextCity(fromCity);

                if (nextCity == null) {
                    //monster is trapped
                    setStatus(Monster.Status.TRAPPED);
                    logger.debug("monster {} trapped", getId());

                    return;
                }

                tryOccupyWithFromCityLocked(fromCity, nextCity);

            } else {
                //give up this round - try same move next round
                game.scheduleMove(this);
            }

        } finally {
            if (fromCityLocked) {
                fromCity.lock.unlock();
            }
        }
    }

    private void tryOccupyWithFromCityLocked(City fromCity, City nextCity) {

        logger.debug("monster {} trying to occupy {}", getId(), nextCity);

        boolean nextCityLocked = false;
        try {
            nextCityLocked = nextCity.lock.tryLock();
            if (nextCityLocked) {
                tryOccupyWithNextCityLocked(fromCity, nextCity);

            } else {
                //try again same move next round
                game.scheduleMove(this);
            }

        } finally {
            if (nextCityLocked) {
                nextCity.lock.unlock();
            }
        }
    }

    private City findNextCity(City fromCity) {
        final Set<Direction> potentialDirections = fromCity.getNeighbors().keySet();
        if (potentialDirections.isEmpty()) {
            return null;
        }

        final List<Direction> directions = new ArrayList(potentialDirections);
        if (directions.size() == 1) {
            Direction direction = directions.get(0);
            City nextCity = fromCity.getNeighbor(direction);

            if (game.isCityDestroyed(nextCity)) {
                fromCity.removeNeighbor(direction);
                return null;
            } else {
                return nextCity;
            }

        } else {
            final Map<Boolean, List<Direction>> groupedDirections = directions.stream()
                    .collect(partitioningBy(d -> game.isCityDestroyed(fromCity.getNeighbor(d))));

            groupedDirections.get(Boolean.TRUE).forEach(d -> fromCity.removeNeighbor(d));

            List<Direction> navigableDirections = groupedDirections.get(Boolean.FALSE);
            if (navigableDirections.size() == 0) {
                return null;
            } else {
                Direction selectedDirection = game.getMonsterMoveProvider().apply(this, Collections.synchronizedList(navigableDirections));
                return fromCity.getNeighbor(selectedDirection);
            }
        }
    }

    private void tryOccupyWithNextCityLocked(City fromCity, City nextCity) {
        ConcurrentMap<City, Monster> citiesOccupied = game.getCitiesOccupied();

        if (game.isCityDestroyed(nextCity)) {
            //next city has been destroyed and not able to move to. try same move next round
            game.scheduleMove(this);
            return;
        }

        Monster existingMonster = citiesOccupied.get(nextCity);
        if (existingMonster == null) {
            //successfully occupied the new city. cycle repeats

            if (fromCity != null) {
                citiesOccupied.remove(fromCity);
            }
            citiesOccupied.put(nextCity, this);

            setOccupiedCity(nextCity);
            game.scheduleMove(this);

            logger.debug("monster {} occupied {}", getId(), nextCity);

        } else {
            //there is alreay monster in this city. they fight
            Set<Monster> monstersFighting = new HashSet<>(Arrays.asList(this, existingMonster));

            FightEvent fightEvent = new FightEvent(game.getFightEventId().getAndIncrement(), nextCity, monstersFighting);
            game.getEventHandler().handle(fightEvent);

            //notify the monster already in city that it's been killed
            existingMonster.gotIntoFightAndKilled();

            //remove city only - not udpating back reference from neighbors
            //because we don't want to acquire all locks of its neighbors
            //rather the approach here is for monster to check if a neighbor 'really' exists
            game.destroyCity(nextCity);

            citiesOccupied.remove(nextCity);

            if (fromCity != null) {
                citiesOccupied.remove(fromCity);
            }

            setStatus(Monster.Status.KILLED);
            logger.debug("monster {} killed", getId());
        }
    }
}
