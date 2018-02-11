package org.gli58.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class RandomMonsterPlacementProvider implements MonsterPlacementProvider {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public City apply(List<City> cities, Monster monster) {
        final int cityCount = cities.size();
        City city = cities.get(ThreadLocalRandom.current().nextInt(cityCount));
        logger.debug("monster {} placed in city {}", monster.getId(), city);

        return city;
   }
}
