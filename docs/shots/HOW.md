# Refreshing the README screenshots

The README is a product page. Its artwork is generated rather than hand made, so when a screen
changes you re-render and rebuild. You never redo the layout.

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew :app:installDebug :app:installDebugAndroidTest
./tools/readme-shots.sh          # writes docs/shots/*.png
python3 tools/frame-shots.py     # writes docs/shots/framed/* and hero.png
```

Needs a running device or emulator, ImageMagick 7 (`magick`) and `rsvg-convert`.

## Nothing is tapped out by hand

`app/src/androidTest/kotlin/com/sendoku/app/ui/ReadmeShotsTest.kt` renders each screen with a
state written down in the test, so the same picture comes out on every run and on any screen
size. There is no staging to do and no demo data to seed: the puzzles are made by the real
generator, the hint is written by the real hint engine, and the statistics are real
`Statistics.of` output over a month of games the test builds.

That is also why the shot list lives in the test rather than here. Adding a screen means adding
a `@Test` that calls `shot("name")`, and the framing script picks it up on its own.

The tests carry the @StoreShot annotation, which keeps them out of the ordinary instrumented
run. They are separate from `StoreShotsTest`, which answers to Play's rules and is shot at
three screen sizes; see `tools/store-shots.sh`.

## What each shot is for

| Name | What it has to show |
|---|---|
| `hint` | The last card of the hint deck: cells lit on the board, the argument, and Do it |
| `home` | A game in progress with its board drawn small, the daily, Killer, and the levels |
| `killer` | Cages with their sums, part filled, so the dashed outlines read |
| `learn` | The course map mid climb, with the next lesson open in the stage you are on |
| `lesson` | A lesson on a real board, at the step where it asks you to find something |
| `you` | The hardest solve, the figures, what has been beaten, the last five weeks |
| `stats` | Time per level, and which rule has been asked about most |
| `won` | The result over the whole screen: the clock, the rule it needed, and the way out |
| `daily`, `technique`, `path`, `glossary` | The four tiles in the More of it row |
| `theme-*` | The same board in each of the four themes |
| `card` | The share card, drawn by the app at 1080x1350. Not a screen, so never framed |

`hero.png` is built from `home`, `hint` and `killer`, so refreshing any of those three updates
the banner on the next run of the framing script.

## The size is fixed on purpose

`tools/readme-shots.sh` sets the device to 1080x2400 at 420 dpi and puts it back afterwards.
That is the phone bucket Play sorts screenshots into and the shape the framing is cut for. On a
different size the frames still build, but the hero stops lining up.
