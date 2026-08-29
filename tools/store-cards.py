#!/usr/bin/env python3
"""
The store screenshots as Play shows them: a phone, on a background, under a sentence.

    ./tools/store-shots.sh        # render the screens on a device
    python3 tools/store-cards.py  # turn them into store artwork

Reads branding/screenshots/<bucket>/<n>-<name>.png and writes branding/store/<bucket>/ with
the same names. Deterministic, safe to run again. Needs ImageMagick 7 (magick).

WHY NOT UPLOAD THE RAW GRABS: two reasons, one of them a hard requirement.

Play asks phone screenshots to be 16:9 or 9:16. A modern phone grab is 9:20, so a raw shot
either gets refused or letterboxed by somebody else's idea of a background colour. Compositing
onto a 1080x1920 canvas settles that.

The other reason is that a store listing is read at thumbnail size in a horizontal carousel.
Nobody reads a 9pt technique name in that strip. The sentence over each shot is what carries
the feature; the picture underneath is the proof.

The words are English only. A listing in another language falls back to these unless it gets
its own set, which is a deliberate trade: twelve sets of artwork to keep in step with every
screen change is a cost the app does not need yet.
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "branding", "screenshots")
OUT = os.path.join(ROOT, "branding", "store")
WORK = os.path.join(OUT, ".work")
FONT = os.path.join(ROOT, "app/src/main/res/font/inter_semibold.ttf")

# Deep Field, the theme every shot was taken in.
INK, TEAL, PAPER, BOARD = "#0A0E12", "#4FE8DA", "#E8F0F5", "#131A21"

# One sentence per screen, in the order Play shows them. The first three are the ones that
# appear in search results, so they carry the three things no other sudoku app has: a hint
# that argues, a ladder that goes further, and a rating that means something.
CAPTIONS = {
    "1-hint": "Hints that teach the technique, not just the digit",
    "2-home": "Eight levels, and the top three go past Extreme",
    "3-beyond": "Master, Insane and Nightmare, all solvable by logic",
    "4-glossary": "Every technique, explained in a sentence",
    "5-themes": "Four themes, each with a typeface of its own",
    "6-stats": "Your record, kept on your phone and nowhere else",
    "7-settings": "Twelve languages. No account, no adverts",
    "8-solved": "After a win, see where the time actually went",
}

# Canvas per bucket. Phone is 9:16 exactly, which is what Play asks for. The tablets keep
# their own shape, which is inside the range Play allows for those.
CANVAS = {"phone": (1080, 1920), "tablet7": (1200, 1920), "tablet10": (1600, 2560)}


def run(*args):
    subprocess.run([str(a) for a in args], check=True)


def size(path):
    out = subprocess.run(["magick", str(path), "-format", "%w %h", "info:"],
                         capture_output=True, text=True, check=True).stdout
    return tuple(int(v) for v in out.split())


def card(src, dst, width, height, caption):
    work = os.path.join(WORK, os.path.basename(dst))
    os.makedirs(work, exist_ok=True)

    # The words first, because how tall they turn out decides how much is left for the phone.
    text_width = int(width * 0.84)
    point = int(width * 0.052)
    text = os.path.join(work, "text.png")
    run("magick", "-background", "none", "-fill", PAPER, "-font", FONT,
        "-pointsize", point, "-interline-spacing", int(point * 0.30),
        "-size", f"{text_width}x", "-gravity", "center", f"caption:{caption}", text)
    _, text_height = size(text)

    top = int(height * 0.055)
    gap = int(height * 0.045)
    bottom = int(height * 0.045)
    room = height - top - text_height - gap - bottom

    shot_width, shot_height = size(src)
    scale = min(room / shot_height, (width * 0.70) / shot_width)
    phone_width = int(shot_width * scale)
    phone_height = int(shot_height * scale)

    radius = int(phone_width * 0.075)
    small = os.path.join(work, "small.png")
    run("magick", src, "-resize", f"{phone_width}x{phone_height}!", small)
    mask = os.path.join(work, "mask.png")
    run("magick", "-size", f"{phone_width}x{phone_height}", "xc:black", "-fill", "white",
        "-draw", f"roundrectangle 0,0,{phone_width - 1},{phone_height - 1},{radius},{radius}", mask)
    rounded = os.path.join(work, "rounded.png")
    run("magick", small, mask, "-alpha", "off", "-compose", "CopyOpacity", "-composite", rounded)

    bezel = max(2, int(phone_width * 0.006))
    frame = os.path.join(work, "frame.png")
    run("magick", "-size", f"{phone_width + bezel * 2}x{phone_height + bezel * 2}", "xc:none",
        "-fill", BOARD, "-draw",
        f"roundrectangle 0,0,{phone_width + bezel * 2 - 1},{phone_height + bezel * 2 - 1},"
        f"{radius + bezel},{radius + bezel}", frame)
    device = os.path.join(work, "device.png")
    run("magick", frame, rounded, "-geometry", f"+{bezel}+{bezel}", "-composite", device)
    shadowed = os.path.join(work, "shadowed.png")
    run("magick", device, "(", "+clone", "-background", "black", "-shadow", "55x40+0+24", ")",
        "+swap", "-background", "none", "-layers", "merge", "+repage", shadowed)

    # The ground: near black with one teal glow behind the phone, the same light the app's
    # own accent throws. Flat black reads as a missing image at thumbnail size.
    base = os.path.join(work, "base.png")
    run("magick", "-size", f"{width}x{height}", f"xc:{INK}", base)
    glow = os.path.join(work, "glow.png")
    run("magick", "-size", f"{width}x{width}", "radial-gradient:#4FE8DA26-none",
        "-resize", f"{int(width * 1.6)}x{int(height * 0.9)}!", glow)
    run("magick", base, glow, "-gravity", "center", "-geometry", f"+0+{int(height * 0.08)}",
        "-composite", base)

    # A teal rule under the sentence, the same mark the app puts under a heading.
    rule_y = top + text_height + int(gap * 0.42)
    rule_w = int(width * 0.10)
    run("magick", base, "-fill", TEAL,
        "-draw", f"rectangle {(width - rule_w) // 2},{rule_y} {(width + rule_w) // 2},{rule_y + max(3, height // 480)}",
        base)

    device_width, device_height = size(shadowed)
    run("magick", base,
        text, "-gravity", "north", "-geometry", f"+0+{top}", "-composite",
        shadowed, "-gravity", "north", "-geometry", f"+0+{top + text_height + gap}", "-composite",
        "-strip", dst)
    for name in os.listdir(work):
        os.remove(os.path.join(work, name))
    os.rmdir(work)
    return device_width, device_height


# The four themes, in the order the settings screen lists them, filling the card left to right.
THEME_ORDER = ["deep_field", "ink", "zen", "terminal"]

# Where the board sits in a shot, as a square: how much of the width it takes, and how far
# down it starts. Four whole phones in one card leaves each board too small to see the point
# of, and the point is the colours and the typeface, both of which live on the board. The
# numbers are per screen size because the board is capped at 560dp and then centred, so it
# fills a phone and floats in the middle of a tablet.
BOARD_BOX = {"phone": (1.0, 0.052), "tablet7": (0.72, 0.060), "tablet10": (0.70, 0.060)}


def themes_card(source, dst, width, height, caption, bucket):
    """Four boards in a two by two, so the claim and the proof are the same picture."""
    work = os.path.join(WORK, "themes")
    os.makedirs(work, exist_ok=True)

    text_width = int(width * 0.84)
    point = int(width * 0.052)
    text = os.path.join(work, "text.png")
    run("magick", "-background", "none", "-fill", PAPER, "-font", FONT,
        "-pointsize", point, "-interline-spacing", int(point * 0.30),
        "-size", f"{text_width}x", "-gravity", "center", f"caption:{caption}", text)
    _, text_height = size(text)

    top = int(height * 0.055)
    gap = int(height * 0.045)
    bottom = int(height * 0.045)
    room = height - top - text_height - gap - bottom
    space = int(width * 0.035)

    raw = [os.path.join(source, f"x-theme-{name}.png") for name in THEME_ORDER]
    for shot in raw:
        if not os.path.exists(shot):
            raise SystemExit(f"the themes card needs {shot}: run ./tools/store-shots.sh first")
    side_fraction, top_fraction = BOARD_BOX[bucket]
    full_width, full_height = size(raw[0])
    square = int(full_width * side_fraction)
    offset_x = (full_width - square) // 2
    offset_y = int(full_height * top_fraction)
    shots = []
    for index, shot in enumerate(raw):
        cropped = os.path.join(work, f"board{index}.png")
        run("magick", shot, "-crop", f"{square}x{square}+{offset_x}+{offset_y}", "+repage", cropped)
        shots.append(cropped)
    shot_width, shot_height = size(shots[0])
    cell_width = (int(width * 0.92) - space) // 2
    cell_height = (room - space) // 2
    scale = min(cell_width / shot_width, cell_height / shot_height)
    one_width, one_height = int(shot_width * scale), int(shot_height * scale)
    radius = int(one_width * 0.075)
    bezel = max(2, int(one_width * 0.008))

    grid_width = one_width * 2 + space + bezel * 4
    grid_height = one_height * 2 + space + bezel * 4
    grid = os.path.join(work, "grid.png")
    run("magick", "-size", f"{grid_width}x{grid_height}", "xc:none", grid)
    for index, shot in enumerate(shots):
        small, mask = os.path.join(work, "s.png"), os.path.join(work, "m.png")
        rounded, frame, device = (os.path.join(work, n) for n in ("r.png", "f.png", "d.png"))
        run("magick", shot, "-resize", f"{one_width}x{one_height}!", small)
        run("magick", "-size", f"{one_width}x{one_height}", "xc:black", "-fill", "white",
            "-draw", f"roundrectangle 0,0,{one_width - 1},{one_height - 1},{radius},{radius}", mask)
        run("magick", small, mask, "-alpha", "off", "-compose", "CopyOpacity", "-composite", rounded)
        run("magick", "-size", f"{one_width + bezel * 2}x{one_height + bezel * 2}", "xc:none",
            "-fill", BOARD, "-draw",
            f"roundrectangle 0,0,{one_width + bezel * 2 - 1},{one_height + bezel * 2 - 1},"
            f"{radius + bezel},{radius + bezel}", frame)
        run("magick", frame, rounded, "-geometry", f"+{bezel}+{bezel}", "-composite", device)
        x = (index % 2) * (one_width + space + bezel * 2)
        y = (index // 2) * (one_height + space + bezel * 2)
        run("magick", grid, device, "-geometry", f"+{x}+{y}", "-composite", grid)

    shadowed = os.path.join(work, "shadowed.png")
    run("magick", grid, "(", "+clone", "-background", "black", "-shadow", "50x36+0+20", ")",
        "+swap", "-background", "none", "-layers", "merge", "+repage", shadowed)

    base = os.path.join(work, "base.png")
    run("magick", "-size", f"{width}x{height}", f"xc:{INK}", base)
    glow = os.path.join(work, "glow.png")
    run("magick", "-size", f"{width}x{width}", "radial-gradient:#4FE8DA26-none",
        "-resize", f"{int(width * 1.6)}x{int(height * 0.9)}!", glow)
    run("magick", base, glow, "-gravity", "center", "-geometry", f"+0+{int(height * 0.08)}",
        "-composite", base)
    rule_y = top + text_height + int(gap * 0.42)
    rule_w = int(width * 0.10)
    run("magick", base, "-fill", TEAL,
        "-draw", f"rectangle {(width - rule_w) // 2},{rule_y} {(width + rule_w) // 2},"
                 f"{rule_y + max(3, height // 480)}", base)
    # The grid is as wide as the card allows, which usually leaves it shorter than the room
    # under the words. Centring it in that room stops the card looking top heavy.
    _, block_height = size(shadowed)
    place = top + text_height + gap + max(0, (room - block_height) // 2)
    run("magick", base,
        text, "-gravity", "north", "-geometry", f"+0+{top}", "-composite",
        shadowed, "-gravity", "north", "-geometry", f"+0+{place}", "-composite",
        "-strip", dst)
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
        for name in sorted(os.listdir(source)):
            # x is the prefix for an ingredient: a render that feeds a card rather than
            # being one. The four theme boards are the only ones today.
            if not name.endswith(".png") or name.startswith("x-"):
                continue
            caption = CAPTIONS.get(os.path.splitext(name)[0])
            if caption is None:
                raise SystemExit(f"{name} has no caption. Add one to CAPTIONS.")
            card(os.path.join(source, name), os.path.join(target, name), width, height, caption)
        themes_card(source, os.path.join(target, "5-themes.png"), width, height,
                    CAPTIONS["5-themes"], bucket)
        print(f"{bucket}: {len(os.listdir(target))} cards at {width}x{height}")
    os.rmdir(WORK)


if __name__ == "__main__":
    main()
