package org.gli58.game;

import org.gli58.game.util.MapIO;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class OneMonsterTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void basicOneMonsterTrappedInOneCity() {
        List<String> lines = Arrays.asList(
                "Concord east=Lexington west=Acton"
        );

        try (Stream<String> inputCities = lines.stream()) {
            final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

            Game game = new Game.Builder(cities, 1)
                    .build();

            game.startGame();

            Set<Monster> monsters = game.getMonsters();
            assertThat(monsters).hasSize(1);
            assertThat(monsters.iterator().next().getStatus()).isEqualTo(Monster.Status.TRAPPED);
        }
    }
}
