#!/usr/bin/env python3
"""
Builds the four bundled typefaces, one per theme.

Run it when the languages change, when a theme changes its face, or to check that the
files in res/font are still what this script would produce:

    python3 -m venv .venv && .venv/bin/pip install fonttools brotli
    .venv/bin/python tools/subset-fonts.py

Why any of this exists. A whole font with Latin and Cyrillic is between 150 and 900
kilobytes, and four of them would be nine tenths of a megabyte on an app that is two.
Sendoku can only ever draw the characters that appear in its own strings, which is 159 of
them, so each face is cut down to exactly those and nothing else. That turns 941 KB into
about 110 KB, and it is the entire reason a face per theme is affordable.

The character set is derived from the shipped strings rather than typed out here. Add a
language and the set grows on its own; the only thing to remember is to run this again.
"""

import os
import re
import sys
import urllib.request
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")
OUT = os.path.join(RES, "font")
CACHE = os.path.join(ROOT, "build/font-sources")
GOOGLE = "https://github.com/google/fonts/raw/main"

# Everything the app can draw that is not in a string: the digits themselves, the clock,
# and the couple of marks the board and the share card add on their own.
EXTRA = "0123456789:/%·…\u00a0"

# What these four faces are not asked to draw.
#
# None of them has a single kana or kanji in it, and the subsets that do exist are around a
# thousand glyphs where a Japanese face is tens of thousands. Bundling Japanese four times
# over would cost more than the whole app. Android carries Noto Sans CJK JP and picks it up
# per character when the face in use cannot draw one, so Japanese renders on every phone
# without a byte from us, and the only thing that changes is which file the shapes come from.
#
# Without this the script stops on 日本語, the name of Japanese in the language picker, which
# is written in Japanese in every language including English.
JAPANESE = [
    (0x3000, 0x303F),  # CJK punctuation
    (0x3040, 0x309F),  # hiragana
    (0x30A0, 0x30FF),  # katakana
    (0x3400, 0x4DBF),  # CJK ideographs, extension A
    (0x4E00, 0x9FFF),  # CJK ideographs
    (0xFF00, 0xFFEF),  # halfwidth and fullwidth forms
]


def is_japanese(character: str) -> bool:
    return any(low <= ord(character) <= high for low, high in JAPANESE)

# One family per theme, and the two weights each one ships. Regular and the heavier of the
# two are real files; anything between them is the nearest of these, which is what Compose
# does on its own. Every one of these is under the SIL Open Font License.
FAMILIES = [
    ("inter", "Inter", [(f"{GOOGLE}/ofl/inter/Inter%5Bopsz,wght%5D.ttf", "regular", 400),
                        (f"{GOOGLE}/ofl/inter/Inter%5Bopsz,wght%5D.ttf", "semibold", 600)]),
    ("pt_serif", "PT Serif", [(f"{GOOGLE}/ofl/ptserif/PT_Serif-Web-Regular.ttf", "regular", None),
                              (f"{GOOGLE}/ofl/ptserif/PT_Serif-Web-Bold.ttf", "bold", None)]),
    ("manrope", "Manrope", [(f"{GOOGLE}/ofl/manrope/Manrope%5Bwght%5D.ttf", "regular", 400),
                            (f"{GOOGLE}/ofl/manrope/Manrope%5Bwght%5D.ttf", "semibold", 600)]),
    ("jetbrains_mono", "JetBrains Mono", [(f"{GOOGLE}/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf", "regular", 400),
                                          (f"{GOOGLE}/ofl/jetbrainsmono/JetBrainsMono%5Bwght%5D.ttf", "bold", 700)]),
]


def charset() -> str:
    """Every character the app's own strings can put on screen."""
    found = set(EXTRA)
    for name in os.listdir(RES):
        path = os.path.join(RES, name, "strings.xml")
        if not name.startswith("values") or not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as handle:
            for body in re.findall(r">([^<>]+)<", handle.read()):
                found |= set(body)
    # A no-break space is not printable by Python's reckoning and is very much something the
    # app draws: French typography puts one before a colon, and a face without it falls back
    # in the middle of a sentence.
    return "".join(sorted(c for c in found if c.isprintable() or c in EXTRA))


def source(url: str) -> str:
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, url.rsplit("/", 1)[-1].replace("%5B", "[").replace("%5D", "]"))
    if not os.path.exists(path):
        print(f"  fetching {os.path.basename(path)}")
        request = urllib.request.Request(url, headers={"User-Agent": "sendoku-build"})
        with urllib.request.urlopen(request, timeout=120) as response, open(path, "wb") as out:
            out.write(response.read())
    return path


def cut(path: str, out: str, text: str, weight):
    from fontTools import subset
    from fontTools.ttLib import TTFont

    font = TTFont(path)
    if weight is not None and "fvar" in font:
        from fontTools.varLib.instancer import instantiateVariableFont

        font = instantiateVariableFont(font, {"wght": weight}, inplace=False, optimize=True)
        # Pinning an axis can leave outline deltas behind for glyphs the subsetter is about
        # to drop, and the writer then fails on a name it cannot find.
        if "gvar" in font:
            del font["gvar"]

    options = subset.Options()
    options.layout_features = ["kern", "liga", "calt", "tnum", "ccmp", "locl", "mark", "mkmk"]
    options.name_IDs = ["*"]
    options.name_legacy = False
    options.drop_tables += ["DSIG"]
    options.hinting = False
    subsetter = subset.Subsetter(options=options)
    subsetter.populate(text=text)
    subsetter.subset(font)
    font.save(out)

    missing = [c for c in text if ord(c) not in {k for t in TTFont(out)["cmap"].tables for k in t.cmap}]
    return os.path.getsize(out), len(zlib.compress(open(out, "rb").read(), 9)), missing


def main() -> int:
    everything = charset()
    text = "".join(c for c in everything if not is_japanese(c))
    japanese = "".join(c for c in everything if is_japanese(c))
    print(f"{len(everything)} characters, taken from the shipped strings")
    print(f"  {len(text)} for the four bundled faces")
    print(f"  {len(japanese)} left to the phone's own Japanese font: {japanese}")
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for slug, label, weights in FAMILIES:
        for url, suffix, weight in weights:
            name = f"{slug}_{suffix}.ttf"
            raw, packed, missing = cut(source(url), os.path.join(OUT, name), text, weight)
            total += packed
            if missing:
                print(f"  {name}: MISSING {''.join(missing)}")
                return 1
            print(f"  {name:28} {raw / 1024:6.1f} KB on disk, {packed / 1024:5.1f} KB in the APK")
    print(f"all four families: {total / 1024:.0f} KB in the APK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
