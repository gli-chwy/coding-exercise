package org.gli58.game;

import org.gli58.domain.City;
import org.gli58.domain.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RandomMonsterPlacement implements MonsterPlacement {
    @Override
    public Map<Monster, City> apply(Set<City> cities, Set<Monster> monsters) {
        final int cityCount = cities.size();
        final List<City> cityList = new ArrayList<>(cities);
        final ThreadLocalRandom random = ThreadLocalRandom.current().current();

        return monsters.stream().collect(Collectors.toMap(
                m -> m,
                m -> cityList.get(random.nextInt(cityCount))
        ));
    }
}
