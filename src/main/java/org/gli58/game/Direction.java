package org.gli58.game;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    //Note it would be nice to have per enum behavior, i.e defining the opposite
    //as an abstract op on Direction, but that required forward reference which
    //is not allowed. The static method appears to be the next best option.
    public static Direction opposite(Direction direction) {
        switch (direction) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
        }
        throw new IllegalArgumentException("unknown direction " + direction);
    }
}
