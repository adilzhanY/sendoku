# Sendoku

A free Android sudoku app with no ads, no tracking, no accounts, and no server.

The point of it is difficulty. Most sudoku apps stop climbing around X-Wing and
XY-Wing territory and call that "Extreme". Sendoku is built around a technique
solver, so a puzzle can be rated by the hardest logic it actually requires, and the
levels can keep going well past where the mainstream apps stop.

## Modules

| Module | What it is |
| --- | --- |
| `engine` | Pure Kotlin. No Android dependency. Solver, uniqueness counter, generator. |
| `app` | Android UI in Jetpack Compose. Currently a placeholder screen. |

The engine is deliberately free of Android imports so it can be tested on the JVM in
milliseconds and reused anywhere later.

## Grid sizes

Every part of the engine takes its shape from `Dimensions(boxWidth, boxHeight)`, so
4x4, 6x6, 9x9 and 16x16 all work today and variants can be added without touching the
solver.

## Building

Needs JDK 21 and an Android SDK with platform 37 installed.

```sh
./gradlew :engine:test      # the part that matters
./gradlew :app:assembleDebug
```

## Status

Engine is done and tested. UI, technique solver, difficulty rating, hints, and the
Killer variant are next. See the roadmap in the project notes.

## Licence

Not chosen yet.
