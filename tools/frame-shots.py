#!/usr/bin/env python3
"""
The README's artwork, built from the raw screenshots.

    ./tools/readme-shots.sh      # render the screens on a device
    python3 tools/frame-shots.py # turn them into artwork

Reads every docs/shots/<name>.png and writes:

  docs/shots/framed/<name>.png   rounded, bezelled and shadowed, like a device
  docs/shots/hero.png            the 2600x1400 banner at the top of the README

It is deterministic and safe to run again. Needs ImageMagick 7 (magick) and rsvg-convert.

WHY FRAME THEM: a raw Android grab dropped on a white README page reads as a bug report.
Rounding the corners, adding a hairline bezel in the app's own board colour and dropping a
soft shadow is the difference between here is a screenshot and here is a product.

The hero is set in the app's own Inter, the subset that ships inside the APK, so the banner
and the screens under it are lettered by the same hand. The subset only carries the
characters the app itself can draw, which is why the words up here are plain ones.

The card is not framed. It is not a screen: the app draws it at 1080x1350 for sending to
somebody, and putting a phone bezel round it would be a lie about what it is.
"""
import importlib.util
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "docs", "shots")
FRAMED = os.path.join(SHOTS, "framed")
WORK = os.path.join(SHOTS, ".work")
FONT_REGULAR = os.path.join(ROOT, "app/src/main/res/font/inter_regular.ttf")
FONT_BOLD = os.path.join(ROOT, "app/src/main/res/font/inter_semibold.ttf")

# Deep Field, the default theme, which is what every shot is taken in.
INK, TEAL, PAPER, SLATE, BOARD = "#0A0E12", "#4FE8DA", "#E8F0F5", "#7D95A5", "#131A21"

# The three phones in the banner, back to front. The hint leads because it is the one thing
# no other sudoku app does.
HERO = [("home", 470), ("killer", 470), ("hint", 620)]

# Drawn at the size the card is not a screen, so it never gets a bezel.
UNFRAMED = {"card"}

HEADLINE = "Sendoku"
KICKER = "HARDER THAN IT LOOKS"
LEAD = [
    "Most sudoku apps stop at Extreme.",
    "This one keeps going, and shows its working.",
]
CLAIMS = [
    "Every puzzle rated by the hardest rule it needs",
    "Hints that teach the technique, never just the digit",
    "45 lessons, Killer sudoku, four themes, no adverts",
]


def run(*args):
    subprocess.run([str(a) for a in args], check=True)


def size(path):
    out = subprocess.run(
        ["magick", str(path), "-format", "%w %h", "info:"],
        capture_output=True, text=True, check=True,
    ).stdout
    return tuple(int(v) for v in out.split())


def mark_svg(side, colour=TEAL, quiet=SLATE):
    """The launcher mark, borrowed from the one script that describes it."""
    spec = importlib.util.spec_from_file_location("branding", os.path.join(ROOT, "tools", "make-branding.py"))
    branding = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(branding)
    on, off = branding.cells(side, 0, 0)
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{side}" height="{side}" viewBox="0 0 {side} {side}">']
    parts += [f'<path d="{d}" fill="{quiet}" fill-opacity="0.30"/>' for d in off]
    parts += [f'<path d="{d}" fill="{colour}"/>' for d in on]
    parts.append("</svg>")
    return "\n".join(parts)


def frame(src, dst, width):
    """Round, bezel and shadow one screenshot."""
    radius = width // 14
    run("magick", src, "-resize", f"{width}x", os.path.join(WORK, "s.png"))
    w, h = size(os.path.join(WORK, "s.png"))
    # `-fill white` matters: a mask drawn on xc:none with no fill uses ImageMagick's default,
    # which is black, and a black CopyOpacity source makes the whole image transparent.
    run("magick", "-size", f"{w}x{h}", "xc:black", "-fill", "white",
        "-draw", f"roundrectangle 0,0,{w - 1},{h - 1},{radius},{radius}", os.path.join(WORK, "mask.png"))
    run("magick", os.path.join(WORK, "s.png"), os.path.join(WORK, "mask.png"),
        "-alpha", "off", "-compose", "CopyOpacity", "-composite", os.path.join(WORK, "rounded.png"))
    bezel = 3
    run("magick", "-size", f"{w + bezel * 2}x{h + bezel * 2}", "xc:none", "-fill", BOARD,
        "-draw", f"roundrectangle 0,0,{w + bezel * 2 - 1},{h + bezel * 2 - 1},{radius + bezel},{radius + bezel}",
        os.path.join(WORK, "bezel.png"))
    run("magick", os.path.join(WORK, "bezel.png"), os.path.join(WORK, "rounded.png"),
        "-geometry", f"+{bezel}+{bezel}", "-compose", "over", "-composite", os.path.join(WORK, "dev.png"))
    run("magick", os.path.join(WORK, "dev.png"),
        "(", "+clone", "-background", "black", "-shadow", "60x30+0+18", ")",
        "+swap", "-background", "none", "-layers", "merge", "+repage", dst)
    return dst


def annotate(img, x, y, text, points, colour, font, kerning=0):
    run("magick", img, "-font", font, "-pointsize", points, "-fill", colour,
        "-kerning", kerning, "-annotate", f"+{x}+{y}", text, img)


def build_hero():
    width, height = 2600, 1400
    hero = os.path.join(SHOTS, "hero.png")
    phones = {}
    for name, phone_width in HERO:
        src = os.path.join(SHOTS, f"{name}.png")
        if not os.path.exists(src):
            sys.exit(f"the banner needs {src}: run ./tools/readme-shots.sh first")
        phones[name] = frame(src, os.path.join(WORK, f"h-{name}.png"), phone_width)

    run("magick", "-size", f"{width}x{height}", f"xc:{INK}", os.path.join(WORK, "base.png"))
    # A teal glow behind the phones, so the right half is not a black slab.
    run("magick", "-size", "1400x1400", "radial-gradient:#4FE8DA33-none",
        "-resize", "1600x1400!", os.path.join(WORK, "glow.png"))
    run("magick", os.path.join(WORK, "base.png"), os.path.join(WORK, "glow.png"),
        "-geometry", "+1150+0", "-composite", os.path.join(WORK, "base.png"))
    # No dimmed watermark behind the words. A huge mark bleeding off the corner is nine
    # rounded squares, and nine rounded squares half out of frame read as blobs rather than
    # as a sudoku box. The mark is drawn once, at the top, at a size where it is legible.

    hw, hh = size(phones["home"])
    kw, kh = size(phones["killer"])
    sw, sh = size(phones["hint"])
    # Three phones, overlapping by about a fifth each, with the hint in front and in the
    # middle. Enough of the two behind has to stay visible to read as a screen rather than
    # as a stripe, which is what decides the gaps.
    run("magick", os.path.join(WORK, "base.png"),
        phones["home"], "-geometry", f"+1100+{(height - hh) // 2 - 20}", "-composite",
        phones["killer"], "-geometry", f"+2050+{(height - kh) // 2 - 20}", "-composite",
        phones["hint"], "-geometry", f"+1500+{(height - sh) // 2}", "-composite",
        hero)

    with open(os.path.join(WORK, "logo.svg"), "w") as handle:
        handle.write(mark_svg(150))
    run("rsvg-convert", "-w", "150", "-h", "150", os.path.join(WORK, "logo.svg"),
        "-o", os.path.join(WORK, "logo150.png"))
    run("magick", hero, os.path.join(WORK, "logo150.png"), "-geometry", "+150+300", "-composite", hero)
    annotate(hero, 150, 600, HEADLINE, 180, PAPER, FONT_BOLD, -4)
    annotate(hero, 152, 690, KICKER, 46, TEAL, FONT_BOLD, 8)
    for index, line in enumerate(LEAD):
        annotate(hero, 150, 800 + index * 62, line, 46, SLATE, FONT_REGULAR)
    run("magick", hero, "-fill", TEAL, "-draw", "rectangle 150,930 330,938", hero)
    for index, line in enumerate(CLAIMS):
        annotate(hero, 150, 1020 + index * 66, line, 40, PAPER, FONT_REGULAR)
    print(f"hero.png  {size(hero)[0]}x{size(hero)[1]}")


def main():
    os.makedirs(FRAMED, exist_ok=True)
    os.makedirs(WORK, exist_ok=True)
    for name in sorted(os.listdir(SHOTS)):
        if not name.endswith(".png") or name == "hero.png":
            continue
        if os.path.splitext(name)[0] in UNFRAMED:
            continue
        frame(os.path.join(SHOTS, name), os.path.join(FRAMED, name), 620)
        print(f"framed/{name}")
    build_hero()
    for name in os.listdir(WORK):
        os.remove(os.path.join(WORK, name))
    os.rmdir(WORK)


if __name__ == "__main__":
    main()
