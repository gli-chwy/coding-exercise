package org.gli58.game;

@FunctionalInterface
interface ResidenceDurationProvider {
    int getDurationInMillis(Monster monster);
}
