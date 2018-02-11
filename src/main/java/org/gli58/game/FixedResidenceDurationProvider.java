package org.gli58.game;

public class FixedResidenceDurationProvider implements ResidenceDurationProvider {

    private static final int DURATION_LOWER_LIMIT = 0;
    private static final int DURATION_UPPER_LIMIT = 1000;
    private final int duration;

    FixedResidenceDurationProvider(int duration) {
        if (duration < DURATION_LOWER_LIMIT || duration > DURATION_UPPER_LIMIT) {
            throw new IllegalArgumentException("duration should be between " +
                    DURATION_LOWER_LIMIT  + " and " + DURATION_UPPER_LIMIT + " milliseconds");
        }
        this.duration = duration;
    }

    @Override
    public int getDurationInMillis(Monster monster) {
        return duration;
    }
}
