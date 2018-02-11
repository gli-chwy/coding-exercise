#  Coding Exercise - version 2

## Changes

My original interpretation of the instructions was that the game is
played step by step sequentially. I decided to try with the monsters
moving, not in unison, but autonomously.

This new implementation is checked in under the branch __autonomous__.
While the major abstractions remain the same, there are also extensive changes. Some of which are:

* `Monster` is now a `Runnable`, to model its behavior that it moves
on its own instead of being driven through API call.

* `Game` becomes essentially a data container, as the game logic
goes into `Monster`. But because there are many more _dependencies_ to
initialize when a game is created, I put in a Builder so that game can
be created conveniently - with the builder supplying the defaults.

  For example, a game (having one monster) can be created as simply as:
  ```
  final Set<City> cities = MapIO.getCitiesFromStream(inputCities);

  Game game = new Game.Builder(cities, 1)
      .build();
  ```

  or a game (with two monsters) can be created by specifying more configuration values:

  ```
  Game game = new Game.Builder(originalCities, 2)
      .minMoves(100)
      .threads(2)
      .placementProvider(placementProvider)
      .moveProvider(moveProvider)
      .durationProvider(durationProvider)
      .eventHandler(capturingEventHandler)
      .build();
  ```

* A new extension point is added through `ResidenceDurationProvider` so that the length of stay of a monster in a city can be supplied. By default, it's a random period. But a deterministic provider can be used in test code:

  ```
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
  ```
  In code snippet above, monster 1 will always stay in the city it occupied for (roughly) 100 milliseconds, while monster 2 will stay for 120 milliseconds. Through these deterministic
  configuration, program behavior can be verified in tests.


## Design considerations

For performance, there is no locking on global data structures. Instead, __atomic__ variables, concurrent data structures (such as `ConcurrentHashMap.newKeySet` for maintaining the cities data) are used where appropriate.

For each move, a monster must __leave__ its current city, and then __occupy__ the next city. Those two events must succeed or fail together,
similar to a transaction. But because there can very well
be another monster trying to move in exactly the opposite direction,
lock ordering deadlock can happen if Java's intrinsic locks were used.
To prevent that, each `City` is associated with its own explicit `ReentrantLock`, and through the result of `trylock` operation, each monster can decide whether to proceed (when both locks are acquired) or back off and try again later (when it
  fails to acquire either lock).

The only place where intrinsic lock being used is for the main thread to wait
for game to finish through `wait` and `notify`.

In summary, through the extensive use of concurrency data structure, explicit locking, and a few other details/tricks, the game is both
thread safe and highly performant.


  ## Usage

  This test case showcases how to play the game:

  ```
  @Test
  public void playingEntireWorldXWithAllRandomMoves() {
      final Set<City> cities = MapIO.getCitiesFromClasspathResource("map.txt");

      int monsterCount = 2000;

      Game game = new Game.Builder(cities, monsterCount)
              .minMoves(10_000)
              .threads(50)
              .build();

      game.startGame();

      //intentionally writing to a known file path here
      MapIO.writeCitiesToFile(game.getCities(), "/tmp/game_over.txt");
  }
  ```

  The above test creates a game with __2000 monsters__ to run in __50 threads__, and
  it will terminate when all monsters are _killed_, _trapped_, or have moved no less than _10000 steps_.

  ## Other tests

  There are multiple tests that check the correctness of the code with smaller
  map, deterministic monster __locations__, __moves__ and __timing__.

  `TwoMonsterTest` also verifies the output of
  `FightEvent`s by customizing game with a `CapturingEventHandler`.
