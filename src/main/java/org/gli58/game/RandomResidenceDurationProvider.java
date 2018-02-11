package org.gli58.game;

import java.util.concurrent.ThreadLocalRandom;

class RandomResidenceDurationProvider implements ResidenceDurationProvider {

    private static final int DURATION_LOWER_LIMIT = 0;
    private static final int DURATION_UPPER_LIMIT = 1000;

    private final int minDuration;
    private final int maxDuration;
    private final int bound;

    public RandomResidenceDurationProvider(int minDuration, int maxDuration) {
        if (minDuration < DURATION_LOWER_LIMIT || maxDuration > DURATION_UPPER_LIMIT) {
            throw new IllegalArgumentException("duration should be between " +
                    DURATION_LOWER_LIMIT  + " and " + DURATION_UPPER_LIMIT + " milliseconds");
        }
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.bound = maxDuration + 1;
    }

    @Override
    public int getDurationInMillis(Monster monster) {
        return ThreadLocalRandom.current().nextInt(minDuration, bound);
    }
}
