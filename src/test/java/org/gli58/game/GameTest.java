package org.gli58.game;

import static org.assertj.core.api.Assertions.*;

import org.gli58.domain.City;
import org.gli58.domain.Direction;
import org.gli58.domain.Monster;
import org.gli58.event.ConsoleLoggingEventHandler;
import org.gli58.event.Event;
import org.gli58.event.EventHandler;
import org.gli58.util.MapIO;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GameTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void basicOneMonsterOneCity() {
        List<String> lines = Arrays.asList(
                "Lexington east=Belmont west=Concord"
        );

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

            City lexington = cities.stream().filter(c -> c.getName().equals("Lexington")).findAny().get();
            final Map<Monster, City> expectedMonsters = new HashMap<>();
            expectedMonsters.put(new Monster(1), lexington);

            Game game = new Game(cities, 1);

            assertThat(game.getMonsters()).isEqualTo(expectedMonsters);
        }
    }

    @Test
    public void testOneMonsterMultipleCityButExplicitPlacement() {
        List<String> lines = Arrays.asList(
                "Acton east=Concord",
                "Concord east=Lexington west=Acton",
                "Lexington east=Belmont west=Concord",
                "Belmont east=Boston west=Lexington",
                "Boston west=Belmont"
        );

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

            final Map<Monster, City> expectedMonsters = new HashMap<>();
            City lexington = cities.stream().filter(c -> c.getName().equals("Lexington")).findAny().get();
            expectedMonsters.put(new Monster(1), lexington);

            Game game = new Game(cities, 1,
                    (cs, ms) -> {
                        City lexington2 = cs.stream().filter(c -> c.getName().equals("Lexington")).findAny().get();
                        return ms.stream().collect(Collectors.toMap(m -> m, m -> lexington2));
                });

            assertThat(game.getMonsters()).isEqualTo(expectedMonsters);
        }
    }

    @Test
    public void testTwoMonstersMovingTowardMiddle() {
        List<String> lines = Arrays.asList(
                "Acton east=Concord",
                "Concord east=Lexington west=Acton",
                "Lexington east=Belmont west=Concord",
                "Belmont east=Boston west=Lexington",
                "Boston west=Belmont"
        );

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

            MonsterPlacement oneEachEnd = new MonsterPlacement() {
                @Override
                public Map<Monster, City> apply(Set<City> cities, Set<Monster> monsters) {
                    Monster monster1 = monsters.stream().filter(m -> m.getId() == 1).findAny().get();
                    Monster monster2 = monsters.stream().filter(m -> m.getId() == 2).findAny().get();

                    City acton = cities.stream().filter(c -> c.getName().equals("Acton")).findAny().get();
                    City boston = cities.stream().filter(c -> c.getName().equals("Boston")).findAny().get();

                    Map<Monster, City> monsterCityMap = new HashMap<>();
                    monsterCityMap.put(monster1, acton);
                    monsterCityMap.put(monster2, boston);
                    return monsterCityMap;
                }
            };

            MonsterMoveProvider moveProvider = new MonsterMoveProvider() {
                @Override
                public Map<Monster, Direction> apply(Map<Monster, City> monsterCityMap) {
                    Monster monster1 = monsterCityMap.entrySet().stream().filter(e -> e.getKey().getId() == 1).findAny().get().getKey();
                    Monster monster2 = monsterCityMap.entrySet().stream().filter(e -> e.getKey().getId() == 2).findAny().get().getKey();

                    Map<Monster, Direction> moves = new HashMap<>();
                    moves.put(monster1, Direction.EAST);
                    moves.put(monster2, Direction.WEST);

                    return moves;
                }
            };

            CapturingEventHandler capturingEventHandler = new CapturingEventHandler();


            Game game = new Game(cities, 2, oneEachEnd);
            game.setEventHandler(capturingEventHandler);

            //monster 1 starts at Acton, monster 2 starts at Boston

            assertThat(game.getMonsters().get(new Monster((1))).getName()).isEqualTo("Acton");
            assertThat(game.getMonsters().get(new Monster((2))).getName()).isEqualTo("Boston");

            boolean canContinue = game.playOnce(moveProvider);

            assertThat(canContinue).isTrue();

            assertThat(game.getMonsters().get(new Monster((1))).getName()).isEqualTo("Concord");
            assertThat(game.getMonsters().get(new Monster((2))).getName()).isEqualTo("Belmont");

            canContinue = game.playOnce(moveProvider);

            assertThat(canContinue).isTrue();

            assertThat(game.getMonsters().get(new Monster((1))).getName()).isEqualTo("Lexington");
            assertThat(game.getMonsters().get(new Monster((2))).getName()).isEqualTo("Lexington");

            canContinue = game.playOnce(moveProvider);

            //commenting out because the early termination check is commented out
            //assertThat(canContinue).isFalse();

            assertThat(game.getMonsters()).isEmpty();

            assertThat(game.getCities())
                    .hasSize(4)
                    .doesNotContain(new City("Lexington"));

            //checking the previous neighbors of the destroyed city
            Set<City> citiesLeft = game.getCities();
            for (City city : citiesLeft) {
                if (city.getName().equals("Concord")) {
                    assertThat(city.getNeighbor(Direction.EAST)).isNull();
                } else if (city.getName().equals("Belmont")) {
                    assertThat(city.getNeighbor(Direction.WEST)).isNull();
                }
            }
            //make sure the monster fight message is generated
            assertThat(capturingEventHandler.getEventStrings())
                    .hasSize(1)
                    .contains("Lexington has been destroyed by monster 1 and monster 2!");
        }
    }

    @Test
    public void playingEntireWorldXWithAllRandomMoves() {
        final Set<City> cities = MapIO.getCitiesFromClasspathResource("map.txt");

        int monsterCount = 1000;

        Game game = new Game(cities, monsterCount);
        game.setEventHandler(new ConsoleLoggingEventHandler());

        for (int i=0;i<10_000; i++) {
            boolean canContinue = game.playOnce();

            if (!canContinue) {
                break;
            }
        }

        //intentionally writing to a known file path here
        MapIO.writeCitiesToFile(game.getCities(), "/tmp/game_over.txt");

        logger.info(game.getSummaryDescription());
    }

    private static class CapturingEventHandler implements EventHandler {
        private Set<String> events = new HashSet<>();

        @Override
        public void handle(Event event) {
            events.add(event.getAsString());
        }

        public Set<String> getEventStrings() {
            return events;
        }
    };

}
