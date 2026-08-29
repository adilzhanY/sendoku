#!/usr/bin/env python3
"""
The store screenshots as marketing cards: a headline, the phone, and the app's own furniture
floating around it.

    ./tools/store-shots.sh        # render the screens on a device
    python3 tools/store-cards.py  # turn them into store artwork

Reads branding/screenshots/<bucket>/<n>-<name>.png and writes branding/store/<bucket>/.
Deterministic and safe to run again. Needs ImageMagick 7 (magick).

WHY NOT UPLOAD THE RAW GRABS: two reasons, one of them a hard requirement.

Play asks phone screenshots to be 16:9 or 9:16 and a modern phone grab is 9:20, so a raw shot
is either refused or letterboxed by somebody else's idea of a background colour.

The other reason is that a listing is read at thumbnail size in a scrolling strip. Nobody
reads a 9pt technique name there. The headline carries the feature, the phone under it is the
proof, and the chips and tiles around it are the app's own vocabulary: technique names it can
explain, and cells off its own board.

Everything is drawn from the theme the app ships in, so the artwork and the product are the
same object. The words are English only; another listing falls back to these unless it is
given its own set, which is a deliberate trade rather than an oversight.
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "branding", "screenshots")
OUT = os.path.join(ROOT, "branding", "store")
WORK = os.path.join(OUT, ".work")
BOLD = os.path.join(ROOT, "app/src/main/res/font/inter_semibold.ttf")
BOOK = os.path.join(ROOT, "app/src/main/res/font/inter_regular.ttf")

# Deep Field, the theme every shot was taken in.
INK, TEAL, PAPER, MUTED, SURFACE, LINE = "#0A0E12", "#4FE8DA", "#E8F0F5", "#7D95A5", "#131A21", "#22303B"

FOOT = "No adverts  .  No tracking  .  No account"


class Card:
    """One screenshot, and everything that goes around it."""

    def __init__(self, shot, headline, sub, chips=(), tiles=(), themes=False):
        self.shot = shot
        self.headline = headline
        self.sub = sub
        self.chips = chips
        # (digit, x, y, size, rotation), positions as fractions of the canvas.
        self.tiles = tiles
        self.themes = themes


CARDS = [
    Card(
        "1-hint",
        "A hint that teaches the rule",
        "It names the technique, lights the cells it rests on, and writes out why. Then it "
        "makes the move, if you still want it to.",
        chips=["X-Wing", "XY-Chain", "Colouring"],
        tiles=[("5", 0.07, 0.30, 0.10, -8), ("7", 0.88, 0.66, 0.085, 11)],
    ),
    Card(
        "2-home",
        "Eight levels, one ladder",
        "Each one opens when you win the level below it, so the climb is in order rather "
        "than a menu of names you cannot compare.",
        chips=["Easy", "Master", "Nightmare"],
        tiles=[("1", 0.08, 0.68, 0.09, 9), ("9", 0.89, 0.31, 0.10, -6)],
    ),
    Card(
        "3-beyond",
        "You will never have to guess",
        "Every puzzle has exactly one answer and every one can be finished by reasoning "
        "alone. The solver proves it before the puzzle ships.",
        chips=["4,000 puzzles", "one answer each"],
        tiles=[("3", 0.07, 0.34, 0.095, 7), ("8", 0.87, 0.72, 0.10, -10)],
    ),
    Card(
        "4-glossary",
        "Every rule, in a sentence",
        "Thirty two techniques, from the naked single to the forcing chain, each with a "
        "lesson behind it and a puzzle that needs it.",
        chips=["Swordfish", "W-Wing", "ALS-XZ"],
        tiles=[("4", 0.08, 0.72, 0.09, -9), ("6", 0.88, 0.33, 0.095, 8)],
    ),
    Card(
        "5-themes",
        "Four looks, four typefaces",
        "Deep Field, Ink and Paper, Slate Zen and Terminal. Pick one and the whole app "
        "follows, board and letters together.",
        chips=["Light", "Dark"],
        tiles=[("2", 0.07, 0.28, 0.09, 10), ("5", 0.89, 0.70, 0.09, -7)],
        themes=True,
    ),
    Card(
        "6-stats",
        "Your record stays here",
        "Times, streaks, clean solves and the rule you lean on most. All of it on your "
        "phone, none of it anywhere else.",
        chips=["No account", "No server"],
        tiles=[("6", 0.08, 0.31, 0.095, -8), ("2", 0.88, 0.68, 0.09, 9)],
    ),
    Card(
        "7-settings",
        "Twelve languages, no adverts",
        "Right to left included. No advertising identifier, no analytics, no crash "
        "reporting, and no internet permission at all.",
        chips=["Offline", "No tracking"],
        tiles=[("7", 0.07, 0.70, 0.09, 8), ("1", 0.89, 0.29, 0.095, -9)],
    ),
    Card(
        "8-solved",
        "See where the time went",
        "After a win the app shows the minute you stalled and the rule that was sitting "
        "there waiting, then offers the whole solution.",
        chips=["27:41", "clean solve"],
        tiles=[("9", 0.08, 0.30, 0.09, 9), ("4", 0.88, 0.71, 0.095, -8)],
    ),
]

# Canvas per bucket. Phone is 9:16 exactly, which is what Play asks for. The tablets keep
# their own shape, which is inside the range Play allows for those.
CANVAS = {"phone": (1080, 1920), "tablet7": (1200, 1920), "tablet10": (1600, 2560)}

# How wide the device may be drawn, as a fraction of the card. A tablet grab is a wider
# picture with the same board inside it, so it needs more of the card to stay readable.
DEVICE_WIDTH = {"phone": 0.62, "tablet7": 0.76, "tablet10": 0.76}

# Where the board sits in a shot, as a square: how much of the width it takes and how far
# down it starts. Only the themes card needs it, and the numbers differ per screen size
# because the board is capped at 560dp and then centred.
BOARD_BOX = {"phone": (1.0, 0.052), "tablet7": (0.72, 0.060), "tablet10": (0.70, 0.060)}

# The four themes, in the order the settings screen lists them.
THEME_ORDER = ["deep_field", "ink", "zen", "terminal"]


def run(*args):
    subprocess.run([str(a) for a in args], check=True)


def size(path):
    out = subprocess.run(["magick", str(path), "-format", "%w %h", "info:"],
                         capture_output=True, text=True, check=True).stdout
    return tuple(int(v) for v in out.split())


def text_block(dst, words, font, point, colour, width, spacing=0.30, centre=True):
    run("magick", "-background", "none", "-fill", colour, "-font", font,
        "-pointsize", point, "-interline-spacing", int(point * spacing),
        "-size", f"{width}x", "-gravity", "center" if centre else "west",
        f"caption:{words}", dst)
    return size(dst)


def rounded(dst, width, height, radius, fill, stroke=None, stroke_width=0):
    args = ["magick", "-size", f"{width}x{height}", "xc:none", "-fill", fill]
    if stroke:
        args += ["-stroke", stroke, "-strokewidth", stroke_width]
    else:
        args += ["-stroke", "none"]
    inset = stroke_width / 2 if stroke else 0
    args += ["-draw",
             f"roundrectangle {inset},{inset},{width - 1 - inset},{height - 1 - inset},{radius},{radius}",
             dst]
    run(*args)


def device(work, src, width, height, name="device"):
    """One screenshot, rounded, bezelled and shadowed, like a phone rather than a grab."""
    radius = int(width * 0.075)
    bezel = max(2, int(width * 0.006))
    small, mask, cut, frame, out = (os.path.join(work, f"{name}-{n}.png")
                                    for n in ("s", "m", "c", "f", "o"))
    run("magick", src, "-resize", f"{width}x{height}!", small)
    run("magick", "-size", f"{width}x{height}", "xc:black", "-fill", "white",
        "-draw", f"roundrectangle 0,0,{width - 1},{height - 1},{radius},{radius}", mask)
    run("magick", small, mask, "-alpha", "off", "-compose", "CopyOpacity", "-composite", cut)
    rounded(frame, width + bezel * 2, height + bezel * 2, radius + bezel, LINE)
    run("magick", frame, cut, "-geometry", f"+{bezel}+{bezel}", "-composite", out)
    return out


def shadow(path, dst, spread="55x40+0+24"):
    run("magick", path, "(", "+clone", "-background", "black", "-shadow", spread, ")",
        "+swap", "-background", "none", "-layers", "merge", "+repage", dst)
    return dst


def chip(work, words, index, scale):
    """A technique name, as the app draws one: teal on a dark pill with a teal hairline."""
    point = int(28 * scale)
    text = os.path.join(work, f"chip{index}-t.png")
    run("magick", "-background", "none", "-fill", TEAL, "-font", BOLD, "-pointsize", point,
        "-kerning", max(1, int(scale)), f"label:{words}", text)
    text_width, text_height = size(text)
    pad_x, pad_y = int(point * 0.95), int(point * 0.62)
    width, height = text_width + pad_x * 2, text_height + pad_y * 2
    pill = os.path.join(work, f"chip{index}-p.png")
    rounded(pill, width, height, height // 2, SURFACE + "F2", TEAL, max(2, int(2 * scale)))
    out = os.path.join(work, f"chip{index}.png")
    run("magick", pill, text, "-gravity", "center", "-composite", out)
    return shadow(out, os.path.join(work, f"chip{index}-s.png"), "60x18+0+10")


def tile(work, digit, side, rotation, index):
    """A cell off the board, tilted. The same rounded square the launcher mark is made of."""
    face = os.path.join(work, f"tile{index}-f.png")
    rounded(face, side, side, int(side * 0.25), SURFACE, LINE, max(2, side // 60))
    point = int(side * 0.52)
    out = os.path.join(work, f"tile{index}.png")
    run("magick", face, "-font", BOLD, "-pointsize", point, "-fill", TEAL,
        "-gravity", "center", "-annotate", "+0+0", digit, out)
    turned = os.path.join(work, f"tile{index}-r.png")
    run("magick", out, "-background", "none", "-rotate", rotation, turned)
    return shadow(turned, os.path.join(work, f"tile{index}-s.png"), "60x22+0+12")


def ground(work, width, height):
    """Near black, with the app's own accent thrown behind the phone from two directions."""
    base = os.path.join(work, "base.png")
    run("magick", "-size", f"{width}x{height}", f"xc:{INK}", base)
    warm = os.path.join(work, "glow1.png")
    run("magick", "-size", f"{width}x{width}", "radial-gradient:#4FE8DA2E-none",
        "-resize", f"{int(width * 1.7)}x{int(height * 0.95)}!", warm)
    run("magick", base, warm, "-gravity", "center", "-geometry", f"+0+{int(height * 0.06)}",
        "-composite", base)
    cool = os.path.join(work, "glow2.png")
    run("magick", "-size", f"{width}x{width}", "radial-gradient:#2A6E9926-none",
        "-resize", f"{int(width * 1.2)}x{int(height * 0.5)}!", cool)
    run("magick", base, cool, "-gravity", "southwest",
        "-geometry", f"-{int(width * 0.25)}-{int(height * 0.1)}", "-composite", base)
    return base


def theme_board(work, source, bucket, index, name, side):
    """One theme's board, cropped out of its screenshot, for the four up card."""
    raw = os.path.join(source, f"x-theme-{name}.png")
    if not os.path.exists(raw):
        raise SystemExit(f"the themes card needs {raw}: run ./tools/store-shots.sh first")
    fraction, top = BOARD_BOX[bucket]
    full_width, full_height = size(raw)
    square = int(full_width * fraction)
    cropped = os.path.join(work, f"board{index}.png")
    run("magick", raw, "-crop", f"{square}x{square}+{(full_width - square) // 2}+{int(full_height * top)}",
        "+repage", cropped)
    return device(work, cropped, side, side, name=f"board{index}")


def build(card, source, dst, width, height, bucket):
    work = os.path.join(WORK, os.path.splitext(os.path.basename(dst))[0])
    os.makedirs(work, exist_ok=True)
    scale = width / 1080

    top = int(height * 0.048)
    headline = os.path.join(work, "headline.png")
    _, headline_height = text_block(headline, card.headline, BOLD, int(width * 0.072), PAPER,
                                    int(width * 0.86), spacing=0.22)
    sub = os.path.join(work, "sub.png")
    _, sub_height = text_block(sub, card.sub, BOOK, int(width * 0.033), MUTED, int(width * 0.80))

    words_height = headline_height + int(height * 0.014) + sub_height
    foot = os.path.join(work, "foot.png")
    _, foot_height = text_block(foot, FOOT, BOOK, int(width * 0.026), MUTED, int(width * 0.9))
    foot_top = height - int(height * 0.042) - foot_height

    gap = int(height * 0.040)
    room = foot_top - int(height * 0.020) - (top + words_height + gap)

    base = ground(work, width, height)

    if card.themes:
        space = int(width * 0.035)
        cell = min((int(width * 0.86) - space) // 2, (room - space) // 2)
        boards = [theme_board(work, source, bucket, i, name, cell)
                  for i, name in enumerate(THEME_ORDER)]
        block_width = cell * 2 + space + int(cell * 0.012) * 4
        block = os.path.join(work, "block.png")
        run("magick", "-size", f"{block_width}x{block_width}", "xc:none", block)
        for index, board in enumerate(boards):
            x = (index % 2) * (cell + space)
            y = (index // 2) * (cell + space)
            run("magick", block, board, "-geometry", f"+{x}+{y}", "-composite", block)
        stage = shadow(block, os.path.join(work, "stage.png"), "50x36+0+20")
    else:
        shot_width, shot_height = size(os.path.join(source, card.shot + ".png"))
        fit = min(room / shot_height, (width * DEVICE_WIDTH[bucket]) / shot_width)
        stage = shadow(device(work, os.path.join(source, card.shot + ".png"),
                              int(shot_width * fit), int(shot_height * fit)),
                       os.path.join(work, "stage.png"))

    # The drop shadow grows the picture past the size it was fitted to, which used to push
    # the phone over the line of text at the bottom. Fit the shadowed block, not the bare one.
    stage_width, stage_height = size(stage)
    if stage_height > room:
        run("magick", stage, "-resize", f"x{room}", stage)
        stage_width, stage_height = size(stage)
    stage_top = top + words_height + gap + max(0, (room - stage_height) // 2)

    args = ["magick", base,
            headline, "-gravity", "north", "-geometry", f"+0+{top}", "-composite",
            sub, "-gravity", "north",
            "-geometry", f"+0+{top + headline_height + int(height * 0.014)}", "-composite"]

    # Tiles go under the phone, chips over it. A cell peeking out from behind the device
    # reads as depth; a technique name half hidden behind it reads as a mistake.
    for index, (digit, x, y, side, rotation) in enumerate(card.tiles):
        art = tile(work, digit, int(width * side), rotation, index)
        art_width, art_height = size(art)
        args += [art, "-gravity", "northwest",
                 "-geometry", f"+{int(width * x) - art_width // 2}+{int(height * y) - art_height // 2}",
                 "-composite"]

    args += [stage, "-gravity", "north", "-geometry", f"+0+{stage_top}", "-composite"]

    for index, words in enumerate(card.chips):
        art = chip(work, words, index, scale)
        art_width, art_height = size(art)
        # Alternating sides, spread down the height of the phone, hanging off its edges.
        left = index % 2 == 0
        x = int(width * 0.035) if left else width - art_width - int(width * 0.035)
        span = stage_height - art_height
        y = stage_top + int(span * (0.16 + 0.30 * index)) if len(card.chips) > 1 else stage_top + span // 2
        args += [art, "-gravity", "northwest", "-geometry", f"+{x}+{y}", "-composite"]

    args += [foot, "-gravity", "north", "-geometry", f"+0+{foot_top}", "-composite",
             "-strip", dst]
    run(*args)
    for name in os.listdir(work):
        os.remove(os.path.join(work, name))
    os.rmdir(work)


def main():
    os.makedirs(WORK, exist_ok=True)
    for bucket, (width, height) in CANVAS.items():
        source = os.path.join(SHOTS, bucket)
        if not os.path.isdir(source):
            continue
        target = os.path.join(OUT, bucket)
        os.makedirs(target, exist_ok=True)
        for card in CARDS:
            build(card, source, os.path.join(target, card.shot + ".png"), width, height, bucket)
        print(f"{bucket}: {len(os.listdir(target))} cards at {width}x{height}")
    os.rmdir(WORK)


if __name__ == "__main__":
    main()
