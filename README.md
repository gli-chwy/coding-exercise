#  Coding Exercise

## Main classes

The central abstraction is `Game`. It can be initialized with deterministic
monster locations through `MonsterPlacement`, if not, then game is initialized with monsters randomly
assigned to cities.

At each step of the game, caller can supply deterministic monster moves through `MonsterMoveProvider`. Otherwise, monsters will move randomly. Assumption is that monsters will always move unless they are trapped.

_Note `MonsterPlacement` and `MonsterMoveProvider` are for testability
in this exercise._

Game can be supplied with a `EventHandler`. The abstraction is to plug in
handlers for game events. Currently, the `ConsoleLoggingEventHandler` is
set up by default to log the `FightEvent`, which is required in the
instructions.

`City`, `Monster`, and `Direction` are straightforward.

Parsing map from file and and saving map to file are handled through `MapIO`.
It's a static utility class with good testability.

## Usage

This test case showcases how to play the game (by client code):

```
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
```

A driver program would go through similar steps as above, therefore such driver class has not been provided.

From command line, `mvn test` runs all the tests, and `mvn site` generates the API docs. Please import into IDE to follow the class
structure easily.

## Other tests

There are multiple tests that check the correctness of the code with smaller
map, deterministic monster location and moves.

`GameTest#testTwoMonstersMovingTowardMiddle()` also verifies the output of
`FightEvent`s by customizing game with a `CapturingEventHandler`.

## Notes

* Playing this game does not appear to be amenable to being made parallel, so there has been no concurrency considerations in the
design. Each game should be confined within a single thread.

* I started defining the model classes such as `Monster`, `City` etc, then worked on the parsing and game logic, with testing code all along. In projects these would have been multiple commits, but for the exercise, I made one commit.

* Brief API doc is located [here](target/site/apidocs/index.html)
