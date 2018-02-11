package org.gli58.game;

import org.gli58.game.util.MapIO;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TwoMonsterTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MonsterPlacementProvider placementProvider;
    private MonsterMoveProvider moveProvider;
    private CapturingEventHandler capturingEventHandler;

    @Before
    public void setup() {
        placementProvider = new MonsterPlacementProvider() {
            @Override
            public City apply(List<City> cities, Monster monster) {
                City acton = cities.stream().filter(c -> c.getName().equals("Acton")).findAny().get();
                City boston = cities.stream().filter(c -> c.getName().equals("Boston")).findAny().get();

                switch ((int)monster.getId()) {
                    case 1:
                        return acton;
                    case 2:
                        return boston;
                    default:
                        throw new IllegalArgumentException("not possible");
                }
            }
        };

        moveProvider = new MonsterMoveProvider() {
            @Override
            public Direction apply(Monster monster, List<Direction> directions) {
                switch ((int)monster.getId()) {
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.WEST;
                    default:
                        throw new IllegalArgumentException("not possible");
                }
            }
        };

        capturingEventHandler = new CapturingEventHandler();
    }

    @Test
    public void twoMonsterFightInLexington() {
        List<String> lines = Arrays.asList(
                "Acton east=Concord",
                "Concord east=Lexington west=Acton",
                "Lexington east=Belmont west=Concord",
                "Belmont east=Boston west=Lexington",
                "Boston west=Belmont"
        );

        int monsterCount = 2;

        //with this timing, they will meet in Lexington

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> originalCities = MapIO.getCitiesFromStream(inputCities);

            ResidenceDurationProvider durationProvider = new ResidenceDurationProvider() {
                @Override
                public int getDurationInMillis(Monster monster) {
                    switch ((int) monster.getId()) {
                        case 1:
                            return 100;
                        case 2:
                            return 120;
                        default:
                            throw new IllegalArgumentException("not possible");
                    }
                }
            };

            Game game = new Game.Builder(originalCities, 2)
                .minMoves(100)
                .threads(2)
                .placementProvider(placementProvider)
                .moveProvider(moveProvider)
                .durationProvider(durationProvider)
                .eventHandler(capturingEventHandler)
                .build();

            game.startGame();

            assertThat(game.getCities())
                    .hasSize(4)
                    .doesNotContain(new City("Lexington"));

            //make sure the monster fight message is generated
            assertThat(capturingEventHandler.getEventStrings())
                    .hasSize(1)
                    .contains("Lexington has been destroyed by monster 1 and monster 2!");

            Set<Monster> monsters = game.getMonsters();
            assertThat(monsters).hasSize(2);
            final Iterator<Monster> iterator = monsters.iterator();
            assertThat(iterator.next().getStatus()).isEqualTo(Monster.Status.KILLED);
            assertThat(iterator.next().getStatus()).isEqualTo(Monster.Status.KILLED);

            Set<City> remainingCities = game.getCities();

            logger.info(MapIO.writeCitiesAsString(remainingCities));
        }
    }

    @Test
    public void twoMonsterFightInBelmont() {
        List<String> lines = Arrays.asList(
                "Acton east=Concord",
                "Concord east=Lexington west=Acton",
                "Lexington east=Belmont west=Concord",
                "Belmont east=Boston west=Lexington",
                "Boston west=Belmont"
        );

        int monsterCount = 2;

        //with this timing, they will meet in Belmont

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> originalCities = MapIO.getCitiesFromStream(inputCities);

            ResidenceDurationProvider durationProvider = new ResidenceDurationProvider() {
                @Override
                public int getDurationInMillis(Monster monster) {
                    switch ((int) monster.getId()) {
                        case 1:
                            return 100;
                        case 2:
                            return 250;
                        default:
                            throw new IllegalArgumentException("not possible");
                    }
                }
            };

            Game game = new Game.Builder(originalCities, 2)
                    .minMoves(100)
                    .threads(2)
                    .placementProvider(placementProvider)
                    .moveProvider(moveProvider)
                    .durationProvider(durationProvider)
                    .eventHandler(capturingEventHandler)
                    .build();

            game.startGame();

            assertThat(game.getCities())
                    .hasSize(4)
                    .doesNotContain(new City("Belmont"));

            //make sure the monster fight message is generated
            assertThat(capturingEventHandler.getEventStrings())
                    .hasSize(1)
                    .contains("Belmont has been destroyed by monster 1 and monster 2!");

            //rest of assertion is identical as that of Lexington
        }
    }
}
