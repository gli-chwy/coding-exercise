package org.gli58.game;

import org.gli58.game.event.ConsoleLoggingEventHandler;
import org.gli58.game.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * Modeling a game as per the instructions.
 *
 */
public class Game {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * monsters in the game
     */
    private final Set<Monster> monsters;

    /**
     * remaining cities in the game
     */
    private final Set<City> cities =  ConcurrentHashMap.newKeySet();

    /**
     * destroyed cities
     */
    private final Set<City> destroyedCities =  ConcurrentHashMap.newKeySet();

    /**
     * unique ID for a particular monster fight
     */
    private final AtomicLong fightEventId = new AtomicLong(1L);

    /**
     * event handler to handle fight events and other types of events as well
     *
     */
    private final EventHandler eventHandler;

    /**
     * number of monsters in the game
     */
    private final int monsterCount;

    /**
     * minimum number of moves each monster needs to make
     */
    private final int mininumMoveCount;

    /**
     * number of monsters that can move concurrently
     */
    private final int concurrentMonsterThreadCount;

    /**
     * scheduler that makes monsters move
     */
    private final ScheduledExecutorService scheduler;

    /**
     * For specifying monster's next move direction
     */
    private final MonsterMoveProvider monsterMoveProvider;

    /**
     * for specifying monster's initial city
     */
    private final MonsterPlacementProvider monsterPlacementProvider;

    /**
     * for specifying how long each monster will stay at a city
     */
    private final ResidenceDurationProvider residenceDurationProvider;

    /**
     * cities that are already occuplied. used to determine if there is fight
     * when monster successfully enters a city.
     */
    private final ConcurrentMap<City, Monster> citiesOccupied = new ConcurrentHashMap<>();

    /**
     * flag to indicate all monsters have run their course
     */
    private final AtomicBoolean shouldStopGame = new AtomicBoolean(false);

    /**
     * flag to prevent starting a game that is already underway
     */
    private final AtomicBoolean gameStarted = new AtomicBoolean(false);

    /**
     * flag to indicate a game has finished so re-playing is avoided
     */
    private final AtomicBoolean gameFinished = new AtomicBoolean(false);

    /**
     * Used to compare to monster count and terminate the game
     */
    private AtomicLong monsterStatusNotificationCount = new AtomicLong(0);

    /**
     * Made private to prevent direct instaitiation by client.
     *
     * @param cities
     * @param monsterCount
     * @param monsterPlacementProvider
     * @param monsterMoveProvider
     * @param residenceDurationProvider
     * @param mininumMoveCount
     * @param concurrentMonsterThreadCount
     * @param eventHandler
     */
    private Game(Set<City> cities,
                 int monsterCount,
                 MonsterPlacementProvider monsterPlacementProvider,
                 MonsterMoveProvider monsterMoveProvider,
                 ResidenceDurationProvider residenceDurationProvider,
                 int mininumMoveCount,
                 int concurrentMonsterThreadCount,
                 EventHandler eventHandler) {

        if (cities == null || cities.isEmpty()) {
            throw new IllegalArgumentException("there has to be at least one city");
        }

        if (monsterCount < 1) {
            throw new IllegalArgumentException("there has to be at least one monster");
        }

        if (monsterPlacementProvider == null) {
            throw new IllegalArgumentException("monsterPlacementProvider is null");
        }

        if (monsterMoveProvider == null) {
            throw new IllegalArgumentException("monsterMoveProvider is null");
        }

        if (residenceDurationProvider == null) {
            throw new IllegalArgumentException("residenceDurationProvider is null");
        }

        if (eventHandler == null) {
            throw new IllegalArgumentException("eventHandler is null");
        }

        if (mininumMoveCount < 1) {
            throw new IllegalArgumentException("minimum moves needs to be positive integer");
        }

        if (concurrentMonsterThreadCount < 1) {
            throw new IllegalArgumentException("concurrent monster threads needs to be positive integer");
        }

        cities.forEach(c -> this.cities.add(c));

        //regular non-concurrent collection is sufficient here due to the
        // apporach for monsters to only update their status
        this.monsters = new HashSet<>();

        this.monsterCount = monsterCount;
        this.monsterPlacementProvider = monsterPlacementProvider;
        this.monsterMoveProvider = monsterMoveProvider;
        this.residenceDurationProvider = residenceDurationProvider;
        this.mininumMoveCount = mininumMoveCount;
        this.concurrentMonsterThreadCount = concurrentMonsterThreadCount;
        this.scheduler = Executors.newScheduledThreadPool(concurrentMonsterThreadCount);
        this.eventHandler = eventHandler;
    }

    public Set<City> getCities() {
        return Collections.unmodifiableSet(cities);
    }

    public Set<Monster> getMonsters() {
        return Collections.unmodifiableSet(monsters);
    }

    AtomicLong getFightEventId() {
        return fightEventId;
    }

    EventHandler getEventHandler() {
        return eventHandler;
    }

    ConcurrentMap<City, Monster> getCitiesOccupied() {
        return citiesOccupied;
    }

    boolean isCityDestroyed(City city) {
        return ! cities.contains(city);
    }

    void destroyCity(City city) {
        cities.remove(city);
        destroyedCities.add(city);
    }

    MonsterMoveProvider getMonsterMoveProvider() {
        return monsterMoveProvider;
    }

    MonsterPlacementProvider getMonsterPlacementProvider() {
        return monsterPlacementProvider;
    }

    ResidenceDurationProvider getResidenceDurationProvider() {
        return residenceDurationProvider;
    }

    void scheduleMove(Monster monster) {
        int delayInMillis = getResidenceDurationProvider().getDurationInMillis(monster);
        final ScheduledFuture<?> schedule = scheduler.schedule(monster, delayInMillis, TimeUnit.MILLISECONDS);
    }

    void monsterStatusChanged(Monster monster) {
        //so we compare the total monster count and count of notifications received
        //that should perform much better and serves the purpose compared to counting active monsters

        logger.debug("monster {} reported status {}", monster.getId(), monster.getStatus());

        if (monsterStatusNotificationCount.incrementAndGet() == monsterCount) {
            logger.debug("all monsters have reported. shutdowning game...");
            scheduler.shutdown();

            synchronized (this) {
                shouldStopGame.set(true);
                this.notify();
            }
        }
    }

    //there might be destroyed cities with which neighbor references
    //have not been reset - per our design approach. so we reset all of those
    //when game finishes.
    private void postProcessNeighbors() {
        destroyedCities.stream().forEach(city -> {
            city.getNeighbors().entrySet().stream().forEach(neighbor -> {
                Direction nd = neighbor.getKey();
                City nc = neighbor.getValue();
                nc.removeNeighbor(Direction.opposite(nd));
            });
        });
    }

    public void startGame() {
        if (gameFinished.get()) {
            throw new UnsupportedOperationException("game is desinged to be played once. create a new game please.");
        }

        if (gameStarted.get()) {
            throw new UnsupportedOperationException("game is already playing");
        }

        gameStarted.set(true);

        logger.info("game started. monsters {}, cities {}, threads {}", monsterCount, cities.size(), concurrentMonsterThreadCount);

        long startTime = System.currentTimeMillis();

        for (int i=1; i<=monsterCount; i++) {
            Monster monster = new Monster(i, mininumMoveCount, this);
            monsters.add(monster);
            scheduleMove(monster);
        }

        synchronized (this) {
            while (!shouldStopGame.get()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    logger.warn("waiting for game to finish but got interrupted... going back to wait.");
                    //ignore and re-check the pre-condition
                }
            }
        }

        postProcessNeighbors();

        long gameDuration = System.currentTimeMillis() - startTime;

        gameFinished.set(true);

        final Map<Monster.Status, Long> monstersByStatus = monsters.stream().collect(groupingBy(Monster::getStatus, counting()));
        final int numberOfCitiesLeft = cities.size();

        logger.info("game finished. took {} seconds. monsters {}, cities {}",
                TimeUnit.SECONDS.convert(gameDuration, TimeUnit.MILLISECONDS),
                monstersByStatus,
                numberOfCitiesLeft);
    }

    public static class Builder {
        private Set<City> cities;
        private int monsterCount;

        private int mininumMoveCount = 10_000;
        private int concurrentMonsterThreadCount = 20;
        private MonsterMoveProvider monsterMoveProvider = new RandomMonsterMoveProvider();
        private MonsterPlacementProvider monsterPlacementProvider = new RandomMonsterPlacementProvider();
        private ResidenceDurationProvider residenceDurationProvider = new RandomResidenceDurationProvider(10, 100);
        private EventHandler eventHandler = new ConsoleLoggingEventHandler();

        public Builder(Set<City> cities, int monsterCount) {
            this.cities = cities;
            this.monsterCount = monsterCount;
        }

        Builder minMoves(int mininumMoveCount) {
            this.mininumMoveCount = mininumMoveCount;
            return this;
        }

        Builder threads(int threadCount) {
            this.concurrentMonsterThreadCount = threadCount;
            return this;
        }

        Builder moveProvider(MonsterMoveProvider monsterMoveProvider) {
            this.monsterMoveProvider = monsterMoveProvider;
            return this;
        }

        Builder placementProvider(MonsterPlacementProvider monsterPlacementProvider) {
            this.monsterPlacementProvider = monsterPlacementProvider;
            return this;
        }

        Builder durationProvider(ResidenceDurationProvider residenceDurationProvider) {
            this.residenceDurationProvider = residenceDurationProvider;
            return this;
        }

        Builder eventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Game build() {
            return new Game(cities,
                    monsterCount,
                    monsterPlacementProvider,
                    monsterMoveProvider,
                    residenceDurationProvider,
                    mininumMoveCount,
                    concurrentMonsterThreadCount,
                    eventHandler);
        }
    }
}
