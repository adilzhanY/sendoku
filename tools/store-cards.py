#!/usr/bin/env python3
"""
The store screenshots as marketing cards, in the style the listing calls "the working":
a headline, the phone, and a page of sudoku reasoning drawn around it.

    ./tools/store-shots.sh        # render the screens on a device
    python3 tools/store-cards.py  # turn them into store artwork

Reads branding/screenshots/<bucket>/<n>-<name>.png and writes branding/store/<bucket>/.
Deterministic (the jitter is seeded per card) and safe to run again. Needs ImageMagick 7
(magick) and rsvg-convert.

WHY NOT UPLOAD THE RAW GRABS: two reasons, one of them a hard requirement. Play asks phone
screenshots to be 16:9 or 9:16 and a modern phone grab is 9:20, so a raw shot is either
refused or letterboxed by somebody else's idea of a background colour. And a listing is read
at thumbnail size in a scrolling strip, where nobody reads a 9pt technique name: the headline
carries the feature and the phone under it is the proof.

THE SCENE: every card is dressed in the app's own working rather than in stickers. A faint
9x9 grid over the whole ground, pencil pairs and ghost digits in the margins, a colouring
chain running down one side with its elimination in rose, an X-Wing rectangle on the other,
digit tiles peeking out from behind the phone, and two chips overlapping the device edge so
they belong to it instead of hanging in space, which is what the first version got wrong.
Every ornament is a real notation from the game, drawn in the theme the shots were taken in.

The words are English only. Another listing falls back to these unless it is given its own
set, which is a deliberate trade rather than an oversight.
"""
import os
import random
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHOTS = os.path.join(ROOT, "branding", "screenshots")
OUT = os.path.join(ROOT, "branding", "store")
WORK = os.path.join(OUT, ".work")
FONTS = os.path.join(ROOT, "app/src/main/res/font")
BOLD = os.path.join(FONTS, "inter_semibold.ttf")
BOOK = os.path.join(FONTS, "inter_regular.ttf")

# Deep Field, the theme every shot is taken in.
INK, TEAL, PAPER, MUTED, SURFACE, LINE = "#0A0E12", "#4FE8DA", "#E8F0F5", "#7D95A5", "#131A21", "#22303B"
RAISED, ROSE = "#1A222B", "#FF6B7A"

FOOT = "No adverts   ·   No tracking   ·   No account"


class Card:
    """One screenshot and the scene that goes around it."""

    def __init__(self, shot, headline, sub, chips, digit, tiles, themes=False):
        self.shot = shot
        self.headline = headline
        self.sub = sub
        # Two labels, left then right, overlapping the device edges.
        self.chips = chips
        # The digit the card's colouring chain repeats. Chains argue about one digit.
        self.digit = digit
        # Two digits on tilted cells behind the phone, left then right.
        self.tiles = tiles
        self.themes = themes


CARDS = [
    Card("1-hint", "A hint that teaches the rule",
         "It names the technique, lights the cells it rests on, and writes out why. "
         "Then it makes the move, if you still want it to.",
         ("X-Wing", "so the 5 goes"), "5", ("7", "5")),
    Card("2-home", "Eight levels, one ladder",
         "Each one opens when you win the level below it, so the climb is in order "
         "rather than a menu of names you cannot compare.",
         ("Easy", "Nightmare"), "1", ("1", "9")),
    Card("3-beyond", "You will never have to guess",
         "Every puzzle has exactly one answer and every one can be finished by "
         "reasoning alone. The solver proves it before the puzzle ships.",
         ("4,000 puzzles", "one answer each"), "3", ("3", "8")),
    Card("4-glossary", "Every rule, in a sentence",
         "Thirty two techniques, from the naked single to the forcing chain, each "
         "with a lesson behind it and a puzzle that needs it.",
         ("Swordfish", "ALS-XZ"), "6", ("4", "6")),
    Card("5-themes", "Four looks, four typefaces",
         "Deep Field, Ink and Paper, Slate Zen and Terminal. Pick one and the whole "
         "app follows, board and letters together.",
         ("Light", "Dark"), "2", ("2", "5"), themes=True),
    Card("6-stats", "Your record stays here",
         "Times, streaks, clean solves and the rule you lean on most. All of it on "
         "your phone, none of it anywhere else.",
         ("No account", "No server"), "6", ("6", "2")),
    Card("7-settings", "Twelve languages, no adverts",
         "Right to left included. No advertising identifier, no analytics, no crash "
         "reporting, and no internet permission at all.",
         ("Offline", "No tracking"), "7", ("7", "1")),
    Card("8-solved", "See where the time went",
         "After a win the app shows the minute you stalled and the rule that was "
         "sitting there waiting, then offers the whole solution.",
         ("27:41", "clean solve"), "9", ("9", "4")),
]

# Canvas per bucket. Phone is 9:16 exactly, which is what Play asks for.
CANVAS = {"phone": (1080, 1920), "tablet7": (1200, 1920), "tablet10": (1600, 2560)}

# How wide the device may be drawn, as a fraction of the card. A tablet grab is a wider
# picture with the same board inside it, so it needs more of the card to stay readable.
DEVICE_WIDTH = {"phone": 0.62, "tablet7": 0.74, "tablet10": 0.74}

# Where the board sits in a themes-card shot, as a square: width fraction and top offset.
# Per screen size because the board is capped at 560dp and then centred.
BOARD_BOX = {"phone": (1.0, 0.052), "tablet7": (0.72, 0.060), "tablet10": (0.70, 0.060)}

THEME_ORDER = ["deep_field", "ink", "zen", "terminal"]

# The themes card labels its four boards by name, like a legend, instead of the two
# generic chips every other card gets. (label, side, height fraction of the block); the
# heights match the quadrant each theme lands in under THEME_ORDER.
THEME_LABELS = [("Deep Field", 0, 0.10), ("Ink and Paper", 1, 0.18),
                ("Slate Zen", 0, 0.62), ("Terminal", 1, 0.72)]


def run(*args, env=None):
    merged = None
    if env:
        merged = dict(os.environ)
        merged.update(env)
    subprocess.run([str(a) for a in args], check=True, env=merged)


def size(path):
    out = subprocess.run(["magick", str(path), "-format", "%w %h", "info:"],
                         capture_output=True, text=True, check=True).stdout
    return tuple(int(v) for v in out.split())


def fontconfig():
    """Points rsvg-convert at the app's own Inter subset, so SVG text matches everything else."""
    path = os.path.join(WORK, "fonts.conf")
    cache = os.path.join(WORK, "font-cache")
    os.makedirs(cache, exist_ok=True)
    with open(path, "w") as handle:
        handle.write(
            '<?xml version="1.0"?>\n'
            '<!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd">\n'
            f"<fontconfig><dir>{FONTS}</dir><cachedir>{cache}</cachedir></fontconfig>\n"
        )
    return path


def render_svg(svg, dst, width, height, fonts):
    src = dst + ".svg"
    with open(src, "w") as handle:
        handle.write(svg)
    run("rsvg-convert", "-w", width, "-h", height, src, "-o", dst,
        env={"FONTCONFIG_FILE": fonts})
    os.remove(src)


def text_block(dst, words, font, point, colour, width, spacing=0.30):
    run("magick", "-background", "none", "-fill", colour, "-font", font,
        "-pointsize", point, "-interline-spacing", int(point * spacing),
        "-size", f"{width}x", "-gravity", "center", f"caption:{words}", dst)
    return size(dst)


def rounded(dst, width, height, radius, fill, stroke=None, stroke_width=0):
    args = ["magick", "-size", f"{width}x{height}", "xc:none", "-fill", fill]
    args += ["-stroke", stroke, "-strokewidth", stroke_width] if stroke else ["-stroke", "none"]
    inset = stroke_width / 2 if stroke else 0
    args += ["-draw",
             f"roundrectangle {inset},{inset},{width - 1 - inset},{height - 1 - inset},{radius},{radius}",
             dst]
    run(*args)


def device(work, src, width, height, name="device"):
    """One screenshot, rounded and bezelled, like a phone rather than a grab."""
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
    """A label as the app draws one: teal on a dark pill with a teal hairline."""
    point = int(30 * scale)
    text = os.path.join(work, f"chip{index}-t.png")
    run("magick", "-background", "none", "-fill", TEAL, "-font", BOLD, "-pointsize", point,
        "-kerning", max(1, int(scale)), f"label:{words}", text)
    text_width, text_height = size(text)
    pad_x, pad_y = int(point * 0.95), int(point * 0.60)
    width, height = text_width + pad_x * 2, text_height + pad_y * 2
    pill = os.path.join(work, f"chip{index}-p.png")
    rounded(pill, width, height, height // 2, SURFACE + "F5", TEAL, max(2, int(2.2 * scale)))
    out = os.path.join(work, f"chip{index}.png")
    run("magick", pill, text, "-gravity", "center", "-composite", out)
    return shadow(out, os.path.join(work, f"chip{index}-s.png"), "60x16+0+8")


def tile(work, digit, side, rotation, index, rose=False):
    """A cell off the board, tilted. The same rounded square the launcher mark is made of."""
    face = os.path.join(work, f"tile{index}-f.png")
    # The raised surface, not the flat one: half of each tile sits in the device's drop
    # shadow, and on the flat surface colour the shadow swallowed it whole.
    rounded(face, side, side, int(side * 0.25), RAISED, "#2E3E4B", max(2, side // 55))
    out = os.path.join(work, f"tile{index}.png")
    run("magick", face, "-font", BOLD, "-pointsize", int(side * 0.52),
        "-fill", ROSE if rose else TEAL, "-gravity", "center", "-annotate", "+0+0", digit, out)
    turned = os.path.join(work, f"tile{index}-r.png")
    run("magick", out, "-background", "none", "-rotate", rotation, turned)
    return shadow(turned, os.path.join(work, f"tile{index}-s.png"), "60x20+0+10")


def scene_svg(card, width, height, box_top, box_height, margin, rng):
    """
    Everything behind the phone, as one SVG: the glows, the grid, the notation.

    [box_top, box_top + box_height] is where the device will sit and [margin] is the width
    of the empty strip either side of it, which is where the notation lives. Chains and
    rectangles argue in the margins; only faint things may cross the middle.
    """
    cell = width / 9.0
    left_cx = margin / 2
    right_cx = width - margin / 2
    chain_left = rng.random() < 0.5
    chain_cx = left_cx if chain_left else right_cx
    rect_cx = right_cx if chain_left else left_cx

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">',
        "<defs>",
        f'<radialGradient id="g1"><stop offset="0%" stop-color="{TEAL}" stop-opacity="0.16"/>'
        f'<stop offset="100%" stop-color="{TEAL}" stop-opacity="0"/></radialGradient>',
        f'<radialGradient id="g2"><stop offset="0%" stop-color="#2A6E99" stop-opacity="0.15"/>'
        f'<stop offset="100%" stop-color="#2A6E99" stop-opacity="0"/></radialGradient>',
        "</defs>",
        f'<rect width="{width}" height="{height}" fill="{INK}"/>',
        f'<ellipse cx="{width * 0.5:.0f}" cy="{height * 0.38:.0f}" rx="{width * 0.85:.0f}" '
        f'ry="{height * 0.48:.0f}" fill="url(#g1)"/>',
        f'<ellipse cx="{width * 0.12:.0f}" cy="{height * 0.92:.0f}" rx="{width * 0.62:.0f}" '
        f'ry="{height * 0.28:.0f}" fill="url(#g2)"/>',
    ]

    # The grid, full bleed, with the box line every third heavier: the one detail that says
    # sudoku rather than graph paper.
    lines = []
    step = 0
    x = 0.0
    while x <= width + 1:
        heavy = step % 3 == 0
        lines.append(f'<line x1="{x:.0f}" y1="0" x2="{x:.0f}" y2="{height}" '
                     f'stroke="{MUTED}" stroke-opacity="{0.10 if heavy else 0.05}" '
                     f'stroke-width="{2 if heavy else 1}"/>')
        x += cell
        step += 1
    y = 0.0
    step = 0
    while y <= height + 1:
        heavy = step % 3 == 0
        lines.append(f'<line x1="0" y1="{y:.0f}" x2="{width}" y2="{y:.0f}" '
                     f'stroke="{MUTED}" stroke-opacity="{0.10 if heavy else 0.05}" '
                     f'stroke-width="{2 if heavy else 1}"/>')
        y += cell
        step += 1
    parts += lines

    def jitter(amount):
        return (rng.random() * 2 - 1) * amount

    # Ghost givens: two big faint digits in the margins, one per side.
    ghost = int(width * 0.085)
    for cx, fraction in ((left_cx, 0.20 + jitter(0.06)), (right_cx, 0.82 + jitter(0.05))):
        parts.append(
            f'<text x="{cx + jitter(margin * 0.15):.0f}" '
            f'y="{box_top + box_height * fraction:.0f}" text-anchor="middle" '
            f'font-family="Inter" font-weight="600" font-size="{ghost}" '
            f'fill="{PAPER}" fill-opacity="0.06">{rng.randint(1, 9)}</text>')

    # Pencil pairs: the two-candidate scribbles a margin fills up with.
    pair_size = int(width * 0.024)
    heights = [-0.04, 0.34, 0.58, 0.96, 1.06]
    for index, fraction in enumerate(heights):
        cx = left_cx if index % 2 == 0 else right_cx
        a, b = rng.randint(1, 9), rng.randint(1, 9)
        parts.append(
            f'<text x="{cx + jitter(margin * 0.28):.0f}" '
            f'y="{box_top + box_height * (fraction + jitter(0.02)):.0f}" text-anchor="middle" '
            f'font-family="Inter" font-weight="600" font-size="{pair_size}" '
            f'letter-spacing="{pair_size * 0.45:.0f}" fill="{MUTED}" fill-opacity="0.35">'
            f"{a} {b}</text>")

    if not card.themes:
        # The colouring chain: the same digit three times down one margin, linked by a
        # dashed path, and the last one in rose because it is the one the chain removes.
        radius = width * 0.0145
        nodes = []
        for fraction, drift in ((0.10, 0.20), (0.44, -0.24), (0.76, 0.30)):
            nodes.append((chain_cx + margin * (drift + jitter(0.06)),
                          box_top + box_height * (fraction + jitter(0.02))))
        dash = f"{width * 0.006:.0f} {width * 0.006:.0f}"
        parts.append(
            f'<path d="M{nodes[0][0]:.0f} {nodes[0][1]:.0f} L{nodes[1][0]:.0f} {nodes[1][1]:.0f} '
            f'L{nodes[2][0]:.0f} {nodes[2][1]:.0f}" stroke="{TEAL}" stroke-opacity="0.5" '
            f'stroke-width="{max(2, width * 0.0018):.1f}" stroke-dasharray="{dash}" fill="none"/>')
        for index, (nx, ny) in enumerate(nodes):
            last = index == len(nodes) - 1
            colour = ROSE if last else TEAL
            parts.append(
                f'<circle cx="{nx:.0f}" cy="{ny:.0f}" r="{radius:.0f}" fill="{SURFACE}" '
                f'stroke="{colour}" stroke-opacity="0.85" stroke-width="{max(2, width * 0.0016):.1f}"/>')
            if last:
                # Struck through, exactly as the board strikes a dying mark.
                arm = radius * 0.5
                parts.append(
                    f'<line x1="{nx - arm:.0f}" y1="{ny + arm:.0f}" x2="{nx + arm:.0f}" '
                    f'y2="{ny - arm:.0f}" stroke="{ROSE}" stroke-width="{max(2, width * 0.0016):.1f}"/>')
            parts.append(
                f'<text x="{nx:.0f}" y="{ny + radius * 0.38:.0f}" text-anchor="middle" '
                f'font-family="Inter" font-weight="600" font-size="{radius * 1.15:.0f}" '
                f'fill="{colour}">{card.digit}</text>')

        # The X-Wing rectangle in the other margin: four corners, two of them marked.
        rect_width, rect_height = width * 0.055, width * 0.125
        rect_cy = box_top + box_height * (0.32 + jitter(0.08))
        angle = (8 if chain_left else -8) + jitter(3)
        dot = max(3, width * 0.004)
        parts.append(f'<g transform="rotate({angle:.0f} {rect_cx:.0f} {rect_cy:.0f})">')
        parts.append(
            f'<rect x="{rect_cx - rect_width / 2:.0f}" y="{rect_cy - rect_height / 2:.0f}" '
            f'width="{rect_width:.0f}" height="{rect_height:.0f}" rx="{width * 0.006:.0f}" '
            f'stroke="{ROSE}" stroke-opacity="0.55" stroke-width="{max(2, width * 0.0016):.1f}" '
            f'fill="none"/>')
        for corner_x, corner_y in ((rect_cx - rect_width / 2, rect_cy - rect_height / 2),
                                   (rect_cx + rect_width / 2, rect_cy + rect_height / 2)):
            parts.append(f'<circle cx="{corner_x:.0f}" cy="{corner_y:.0f}" r="{dot:.0f}" '
                         f'fill="{ROSE}" fill-opacity="0.85"/>')
        parts.append("</g>")

    parts.append("</svg>")
    return "\n".join(parts)


def theme_board(work, source, bucket, index, name, side):
    """One theme's board, cropped out of its screenshot, for the four up card."""
    raw = os.path.join(source, f"x-theme-{name}.png")
    if not os.path.exists(raw):
        raise SystemExit(f"the themes card needs {raw}: run ./tools/store-shots.sh first")
    fraction, top = BOARD_BOX[bucket]
    full_width, full_height = size(raw)
    square = int(full_width * fraction)
    cropped = os.path.join(work, f"board{index}.png")
    run("magick", raw, "-crop",
        f"{square}x{square}+{(full_width - square) // 2}+{int(full_height * top)}",
        "+repage", cropped)
    return device(work, cropped, side, side, name=f"board{index}")


def build(card, source, dst, width, height, bucket, fonts):
    work = os.path.join(WORK, os.path.splitext(os.path.basename(dst))[0])
    os.makedirs(work, exist_ok=True)
    rng = random.Random(card.shot)
    scale = width / 1080

    top = int(height * 0.048)
    headline = os.path.join(work, "headline.png")
    _, headline_height = text_block(headline, card.headline, BOLD, int(width * 0.072),
                                    PAPER, int(width * 0.86), spacing=0.22)
    sub = os.path.join(work, "sub.png")
    _, sub_height = text_block(sub, card.sub, BOOK, int(width * 0.033), MUTED, int(width * 0.80))
    words_height = headline_height + int(height * 0.014) + sub_height

    foot = os.path.join(work, "foot.png")
    _, foot_height = text_block(foot, FOOT, BOOK, int(width * 0.026), MUTED, int(width * 0.9))
    foot_top = height - int(height * 0.042) - foot_height

    gap = int(height * 0.040)
    room = foot_top - int(height * 0.020) - (top + words_height + gap)

    # The device, sized to its room. Fitted at 95 percent so the drop shadow it grows later
    # still lands inside; the safety resize below only fires if that estimate is ever wrong.
    if card.themes:
        space = int(width * 0.035)
        cell = min((int(width * 0.88) - space) // 2, (int(room * 0.95) - space) // 2)
        boards = [theme_board(work, source, bucket, i, name, cell)
                  for i, name in enumerate(THEME_ORDER)]
        board_side = size(boards[0])[0]
        block_side = board_side * 2 + space
        block = os.path.join(work, "block.png")
        run("magick", "-size", f"{block_side}x{block_side}", "xc:none", block)
        for index, board in enumerate(boards):
            run("magick", block, board,
                "-geometry", f"+{(index % 2) * (board_side + space)}+{(index // 2) * (board_side + space)}",
                "-composite", block)
        bare_width = bare_height = block_side
        stage = shadow(block, os.path.join(work, "stage.png"), "50x36+0+20")
    else:
        shot_width, shot_height = size(os.path.join(source, card.shot + ".png"))
        fit = min(room * 0.95 / shot_height, (width * DEVICE_WIDTH[bucket]) / shot_width)
        bare_width, bare_height = int(shot_width * fit), int(shot_height * fit)
        stage = shadow(device(work, os.path.join(source, card.shot + ".png"),
                              bare_width, bare_height),
                       os.path.join(work, "stage.png"))

    stage_width, stage_height = size(stage)
    if stage_height > room:
        squeeze = room / stage_height
        run("magick", stage, "-resize", f"x{room}", stage)
        stage_width, stage_height = size(stage)
        bare_width, bare_height = int(bare_width * squeeze), int(bare_height * squeeze)
    stage_top = top + words_height + gap + max(0, (room - stage_height) // 2)

    # Where the visible device actually is, inside the shadowed picture. Everything in the
    # scene is anchored to this box, which is what keeps the ornaments from floating.
    device_left = (width - bare_width) // 2
    device_top = stage_top + int((stage_height - bare_height) * 0.35)

    back = os.path.join(work, "back.png")
    render_svg(scene_svg(card, width, height, device_top, bare_height,
                         device_left, rng), back, width, height, fonts)

    args = ["magick", back,
            headline, "-gravity", "north", "-geometry", f"+0+{top}", "-composite",
            sub, "-gravity", "north",
            "-geometry", f"+0+{top + headline_height + int(height * 0.014)}", "-composite"]

    # Tiles go under the phone, chips over it. A cell peeking out from behind the device
    # reads as depth; a label half hidden behind it would read as a mistake.
    tile_side = int(width * 0.10)
    placements = [(card.tiles[0], device_left - tile_side * 0.28,
                   device_top + bare_height * (0.62 + rng.random() * 0.1),
                   -8 - rng.random() * 4, False),
                  (card.tiles[1], device_left + bare_width + tile_side * 0.28,
                   device_top + bare_height * (0.16 + rng.random() * 0.1),
                   8 + rng.random() * 4, card.shot == "1-hint")]
    for index, (digit, x, y, rotation, rose) in enumerate(placements):
        art = tile(work, digit, tile_side, f"{rotation:.0f}", index, rose)
        art_width, art_height = size(art)
        args += [art, "-gravity", "northwest",
                 "-geometry", f"+{int(x - art_width / 2)}+{int(y - art_height / 2)}", "-composite"]

    args += [stage, "-gravity", "north", "-geometry", f"+0+{stage_top}", "-composite"]

    # The chips hang off the device edges so they belong to it. Most cards get two, at
    # heights that never collide with the tiles on the same side; the themes card gets a
    # name beside each of its four boards instead, because there they are a legend.
    if card.themes:
        labels = [(words, side, fraction) for words, side, fraction in THEME_LABELS]
    else:
        labels = [(card.chips[0], 0, 0.22 + rng.random() * 0.06),
                  (card.chips[1], 1, 0.52 + rng.random() * 0.08)]
    # How far a chip may lean out past the device edge. The themes block runs nearly the
    # whole card wide, so its legend chips lean inward instead; leaning out clipped them
    # against the canvas and cut the theme names in half.
    lean = 0.16 if card.themes else 0.55
    for index, (words, side, fraction) in enumerate(labels):
        art = chip(work, words, index, scale)
        art_width, art_height = size(art)
        if side == 0:
            x = device_left - int(art_width * lean)
        else:
            x = device_left + bare_width - int(art_width * (1 - lean))
        y = device_top + int(bare_height * fraction)
        args += [art, "-gravity", "northwest", "-geometry", f"+{x}+{y}", "-composite"]

    args += [foot, "-gravity", "north", "-geometry", f"+0+{foot_top}", "-composite",
             "-strip", dst]
    run(*args)
    for name in os.listdir(work):
        os.remove(os.path.join(work, name))
    os.rmdir(work)


def main():
    os.makedirs(WORK, exist_ok=True)
    fonts = fontconfig()
    for bucket, (width, height) in CANVAS.items():
        source = os.path.join(SHOTS, bucket)
        if not os.path.isdir(source):
            continue
        target = os.path.join(OUT, bucket)
        os.makedirs(target, exist_ok=True)
        for card in CARDS:
            build(card, source, os.path.join(target, card.shot + ".png"),
                  width, height, bucket, fonts)
        print(f"{bucket}: {len(os.listdir(target))} cards at {width}x{height}")
    os.remove(fonts)
    for name in os.listdir(os.path.join(WORK, "font-cache")):
        os.remove(os.path.join(WORK, "font-cache", name))
    os.rmdir(os.path.join(WORK, "font-cache"))
    os.rmdir(WORK)


if __name__ == "__main__":
    main()
