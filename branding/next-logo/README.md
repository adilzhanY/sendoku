# Next logo: "One line"

Chosen on 2026-11-03 out of three directions (One line, Last candidate, The cage). Not shipped
yet. The plan to roll it out is section 63 of `TODO.md`.

## The idea

One line, the 線 in Sendoku, draws an S the way a chain of logic runs through a grid, and
stops one step short of a teal cell. The gap before the cell is the deduction: the app never
hands you the digit, you take the last step yourself.

It keeps what the old icon already owned: the near-black tile, the teal, and one lit cell.

## Construction (256 grid)

- The line is a single stroke, 32 wide, round caps:
  `M198 58 H102 A32 32 0 0 0 102 122 H166 A32 32 0 0 1 166 186 H122`.
  Both turns are semicircles of radius 32, so the S is only straight lines and arcs.
- The cell is a 56 square at (42, 158), corner radius 14. It must never touch the line, and
  it must never be dropped: a rounded S alone is a common shape, the separate cell is what
  makes the mark ours.
- The ink spans x 42 to 214 and y 42 to 214 including the stroke, sitting 6 units above the
  geometric centre on purpose (optical centre).
- Colours: line `#E8F0F5`, cell `#4FE8DA`, tile `#0A0E12`. One colour: everything in ink.
- Wordmark: Inter SemiBold, the app's own font (OFL), turned into outlines.

## Files

| File | What |
|---|---|
| `symbol.svg` | The mark, black, no background |
| `color.svg` | App icon look, mark on the dark tile |
| `lockup.svg` | Mark and wordmark, black |
| `lockup-dark.svg` | Mark and wordmark on the dark background |

These are concepts. The line is still a stroke, so the masters need it expanded to filled
outlines, plus a small size cut for 16 to 24 px.
