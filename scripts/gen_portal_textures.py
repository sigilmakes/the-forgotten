#!/usr/bin/env python3
"""
Generate seamless animated portal textures.
All motion is orbital (sin/cos pairs) so the animation loops perfectly
without palindrome or cross-fade. No borders, no snap.
"""

from PIL import Image
import math
import os

SIZE = 16
FRAMES = 32
TAU = 2 * math.pi
OUT = "/home/sigil/Projects/the-forgotten/src/main/resources/assets/the-forgotten/textures/block"


def orbit(t, speed=1.0, phase=0):
    """Returns (sin, cos) pair — circular motion that loops perfectly."""
    a = (t / FRAMES) * TAU * speed + phase
    return math.sin(a), math.cos(a)


def generate_texture(name, base_rgb, bright_rgb, dark_rgb, alpha_range=(155, 215)):
    img = Image.new("RGBA", (SIZE, SIZE * FRAMES), (0, 0, 0, 0))
    a_lo, a_hi = alpha_range

    for frame in range(FRAMES):
        for y in range(SIZE):
            for x in range(SIZE):
                # Spatial angles (tile-seamless)
                ax = (x / SIZE) * TAU
                ay = (y / SIZE) * TAU

                # Orbital time components — each is a smooth loop
                s1, c1 = orbit(frame, speed=1.0, phase=0)
                s2, c2 = orbit(frame, speed=1.0, phase=TAU/3)
                s3, c3 = orbit(frame, speed=1.0, phase=2*TAU/3)
                # Second harmonic for variety
                s4, c4 = orbit(frame, speed=2.0, phase=0.5)
                s5, c5 = orbit(frame, speed=2.0, phase=2.0)

                # Vertical columns that sway using orbital motion
                # The column positions orbit left-right smoothly
                col1 = math.cos(ax + s1 * 0.4) * 0.5 + 0.5
                col2 = math.cos(ax * 2 + s2 * 0.5) * 0.5 + 0.5
                col3 = math.cos(ax + c3 * 0.35 + 2.0) * 0.5 + 0.5

                # Vertical brightness variation — orbits up/down instead of drifting
                # Using both sin and cos of time creates circular motion
                vert1 = math.cos(ay + s1 * 0.6 + c1 * 0.4) * 0.5 + 0.5
                vert2 = math.cos(ay * 2 + s2 * 0.5 + c2 * 0.3) * 0.5 + 0.5
                vert3 = math.sin(ay + s3 * 0.5 + c3 * 0.5) * 0.5 + 0.5

                # Pulsing glow — orbital breathing
                pulse1 = s1 * 0.3 + c4 * 0.2 + 0.5
                pulse2 = c2 * 0.25 + s5 * 0.15 + 0.5

                # Combine columns with vertical modulation
                w1 = col1 * vert1
                w2 = col2 * vert2
                w3 = col3 * vert3

                # Fine shimmer — fast orbit, high spatial freq
                shimmer_val = (
                    math.sin(ax * 2 + ay * 2 + s4 * 0.7 + c5 * 0.5) * 0.5 + 0.5
                )

                # Overall intensity
                intensity = (
                    w1 * 0.28 +
                    w2 * 0.18 +
                    w3 * 0.14 +
                    pulse1 * 0.10 +
                    pulse2 * 0.05 +
                    shimmer_val * 0.10 +
                    0.10  # floor
                )
                intensity = min(1.0, max(0.0, intensity))

                # Highlight for brightest spots
                highlight = max(0, intensity - 0.55) * 2.5
                highlight = min(1.0, highlight)

                # Color: dark → base → bright
                if intensity < 0.5:
                    tm = intensity * 2
                    r = dark_rgb[0] + (base_rgb[0] - dark_rgb[0]) * tm
                    g = dark_rgb[1] + (base_rgb[1] - dark_rgb[1]) * tm
                    b = dark_rgb[2] + (base_rgb[2] - dark_rgb[2]) * tm
                else:
                    tm = (intensity - 0.5) * 2
                    r = base_rgb[0] + (bright_rgb[0] - base_rgb[0]) * tm
                    g = base_rgb[1] + (bright_rgb[1] - base_rgb[1]) * tm
                    b = base_rgb[2] + (bright_rgb[2] - base_rgb[2]) * tm

                r = r + (bright_rgb[0] - r) * highlight * 0.3
                g = g + (bright_rgb[1] - g) * highlight * 0.3
                b = b + (bright_rgb[2] - b) * highlight * 0.3

                alpha = a_lo + (a_hi - a_lo) * (0.3 + intensity * 0.7)
                alpha = max(a_lo, min(a_hi, alpha))

                img.putpixel((x, frame * SIZE + y), (
                    max(0, min(255, int(r))),
                    max(0, min(255, int(g))),
                    max(0, min(255, int(b))),
                    max(0, min(255, int(alpha)))
                ))

    img.save(os.path.join(OUT, f"{name}.png"))
    print(f"  {name}.png")


print("Generating portal textures (orbital motion)...")

generate_texture("forgotten_portal",
    base_rgb=(120, 140, 170),
    bright_rgb=(185, 205, 235),
    dark_rgb=(55, 65, 85),
    alpha_range=(155, 215)
)

generate_texture("forgotten_portal_overworld",
    base_rgb=(70, 155, 90),
    bright_rgb=(140, 220, 160),
    dark_rgb=(25, 55, 35),
    alpha_range=(155, 210)
)

generate_texture("forgotten_portal_nether",
    base_rgb=(170, 65, 55),
    bright_rgb=(240, 130, 80),
    dark_rgb=(55, 20, 18),
    alpha_range=(155, 210)
)

generate_texture("forgotten_portal_end",
    base_rgb=(130, 75, 175),
    bright_rgb=(195, 145, 245),
    dark_rgb=(40, 22, 60),
    alpha_range=(155, 210)
)

print("Done!")
