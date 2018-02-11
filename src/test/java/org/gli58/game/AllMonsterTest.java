package org.gli58.game;

import org.gli58.game.util.MapIO;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AllMonsterTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void playingEntireWorldXWithAllRandomMoves() {
        final Set<City> cities = MapIO.getCitiesFromClasspathResource("map.txt");

        int monsterCount = 1000;

        Game game = new Game.Builder(cities, monsterCount)
                .minMoves(10_000)
                .threads(50)
                .build();

        game.startGame();

        //intentionally writing to a known file path here
        MapIO.writeCitiesToFile(game.getCities(), "/tmp/map-after.playing.game.txt");
    }

}
