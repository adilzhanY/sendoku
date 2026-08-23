#!/usr/bin/env python3
"""
Every branding asset, generated from one description of the mark.

The mark is a five by five sudoku grid whose filled cells spell an S. It is the app name and
the app subject in one shape, it survives being shrunk to a launcher icon, and it is drawn
rather than photographed so there is no resampling and no metadata to strip.

Run: python3 tools/make-branding.py
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BRANDING = os.path.join(ROOT, "branding")
RES = os.path.join(ROOT, "app/src/main/res")

# Deep Field, the default theme.
INK = "#0A0E12"
TEAL = "#4FE8DA"
SLATE = "#7D95A5"
PAPER = "#E8F0F5"
MUTED = "#7D95A5"

MARK = [
    ".####",
    "#....",
    ".###.",
    "....#",
    "####.",
]


def cells(side, origin_x, origin_y, gap_ratio=0.11):
    """Returns (filled, empty) rounded square paths for the mark at a given size."""
    n = len(MARK)
    gap = side * gap_ratio / (n - 1)
    cell = (side - gap * (n - 1)) / n
    r = cell * 0.2
    on, off = [], []
    for row, line in enumerate(MARK):
        for col, ch in enumerate(line):
            x = origin_x + col * (cell + gap)
            y = origin_y + row * (cell + gap)
            d = (
                f"M{x + r:.2f},{y:.2f} h{cell - 2 * r:.2f} "
                f"a{r:.2f},{r:.2f} 0 0 1 {r:.2f},{r:.2f} v{cell - 2 * r:.2f} "
                f"a{r:.2f},{r:.2f} 0 0 1 -{r:.2f},{r:.2f} h-{cell - 2 * r:.2f} "
                f"a{r:.2f},{r:.2f} 0 0 1 -{r:.2f},-{r:.2f} v-{cell - 2 * r:.2f} "
                f"a{r:.2f},{r:.2f} 0 0 1 {r:.2f},-{r:.2f} z"
            )
            (on if ch == "#" else off).append(d)
    return on, off


def vector_drawable(on_colour, off_colour, off_alpha):
    """The adaptive icon foreground, sized so a circular mask cannot bite the mark."""
    on, off = cells(side=46, origin_x=31, origin_y=31)
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="108"',
        '    android:viewportHeight="108">',
    ]
    for d in off:
        lines.append(
            f'    <path android:fillColor="{off_colour}" '
            f'android:fillAlpha="{off_alpha}" android:pathData="{d}" />'
        )
    for d in on:
        lines.append(f'    <path android:fillColor="{on_colour}" android:pathData="{d}" />')
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def svg_icon(size=512, pad_ratio=0.16, background=INK):
    """The store icon. No mask here, so the mark can use more of the square."""
    side = size * (1 - 2 * pad_ratio)
    origin = size * pad_ratio
    on, off = cells(side, origin, origin)
    out = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {size} {size}">',
        f'<rect width="{size}" height="{size}" fill="{background}"/>',
    ]
    for d in off:
        out.append(f'<path d="{d}" fill="{SLATE}" fill-opacity="0.28"/>')
    for d in on:
        out.append(f'<path d="{d}" fill="{TEAL}"/>')
    out.append("</svg>")
    return "\n".join(out) + "\n"


def svg_wordmark(width=1600, height=400, background=None):
    """Sendoku, with the mark standing in for nothing. The word is the wordmark."""
    fill = "none" if background is None else background
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<rect width="{width}" height="{height}" fill="{fill}"/>
<text x="{width / 2}" y="{height * 0.62}" text-anchor="middle" font-family="Inter, Noto Sans, sans-serif"
      font-weight="600" font-size="{height * 0.42}" letter-spacing="{height * 0.01}" fill="{PAPER}">Sendoku</text>
<text x="{width / 2}" y="{height * 0.84}" text-anchor="middle" font-family="Inter, Noto Sans, sans-serif"
      font-weight="500" font-size="{height * 0.11}" letter-spacing="{height * 0.035}" fill="{TEAL}">HARDER THAN IT LOOKS</text>
</svg>
"""


def svg_feature(width=1024, height=500):
    """The store feature graphic. Mark on the left, name and one promise on the right."""
    mark_side = height * 0.52
    on, off = cells(mark_side, width * 0.09, (height - mark_side) / 2)
    out = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">',
        f'<rect width="{width}" height="{height}" fill="{INK}"/>',
    ]
    for d in off:
        out.append(f'<path d="{d}" fill="{SLATE}" fill-opacity="0.28"/>')
    for d in on:
        out.append(f'<path d="{d}" fill="{TEAL}"/>')
    left = width * 0.09 + mark_side + width * 0.07
    out += [
        f'<text x="{left}" y="{height * 0.46}" font-family="Inter, Noto Sans, sans-serif" '
        f'font-weight="600" font-size="{height * 0.19}" fill="{PAPER}">Sendoku</text>',
        f'<text x="{left}" y="{height * 0.60}" font-family="Inter, Noto Sans, sans-serif" '
        f'font-weight="500" font-size="{height * 0.058}" letter-spacing="{height * 0.012}" '
        f'fill="{TEAL}">HARDER THAN IT LOOKS</text>',
        f'<text x="{left}" y="{height * 0.72}" font-family="Inter, Noto Sans, sans-serif" '
        f'font-weight="400" font-size="{height * 0.046}" fill="{MUTED}">'
        f'Rated by technique, not by clue count.</text>',
        "</svg>",
    ]
    return "\n".join(out) + "\n"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        handle.write(text)
    print("wrote", os.path.relpath(path, ROOT))


def render(svg_path, png_path, width, height):
    subprocess.run(
        ["rsvg-convert", "-w", str(width), "-h", str(height), svg_path, "-o", png_path],
        check=True,
    )
    print("wrote", os.path.relpath(png_path, ROOT))


# sBIT records how many bits per channel are meaningful. No text, no timestamp, no origin.
ALLOWED_CHUNKS = {"IHDR", "PLTE", "bKGD", "IDAT", "IEND", "tRNS", "sRGB", "gAMA", "pHYs", "sBIT"}


def check_clean(path):
    """No watermark, no provenance, no timestamp. Fails loudly rather than quietly shipping one."""
    import struct

    data = open(path, "rb").read()
    offset = 8
    found = []
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8].decode("latin1")
        found.append(kind)
        offset += 12 + length
    extra = [k for k in found if k not in ALLOWED_CHUNKS]
    if extra:
        raise SystemExit(f"{path} carries metadata chunks: {extra}")


def main():
    write(os.path.join(RES, "drawable/ic_launcher_foreground.xml"),
          vector_drawable(TEAL, SLATE, "0.28"))
    write(os.path.join(RES, "drawable/ic_launcher_monochrome.xml"),
          vector_drawable("#FFFFFFFF", "#FFFFFFFF", "0.30"))

    write(os.path.join(BRANDING, "icon.svg"), svg_icon())
    write(os.path.join(BRANDING, "wordmark.svg"), svg_wordmark())
    write(os.path.join(BRANDING, "feature.svg"), svg_feature())

    render(os.path.join(BRANDING, "icon.svg"), os.path.join(BRANDING, "icon-512.png"), 512, 512)
    render(os.path.join(BRANDING, "icon.svg"), os.path.join(BRANDING, "icon-48.png"), 48, 48)
    render(os.path.join(BRANDING, "wordmark.svg"), os.path.join(BRANDING, "wordmark.png"), 1600, 400)
    render(os.path.join(BRANDING, "feature.svg"), os.path.join(BRANDING, "feature-1024x500.png"), 1024, 500)

    for name in os.listdir(BRANDING):
        if name.endswith(".png"):
            check_clean(os.path.join(BRANDING, name))
    print("every PNG is free of metadata chunks")


if __name__ == "__main__":
    main()
