#!/usr/bin/env python3
"""
Convert the legacy HyperLap2D scene (MainScene.dt) into our own level format.

This script is NOT part of the gradle build. It exists for provenance: the old
`add-hyperlap2d` branch survives only as a GitHub PR ref, and this is the record
of exactly how its polygon data became `core/assets/levels/main.level.json`.

Sources (all read out of git, nothing needs to be checked out):

    git show origin/pr/1:core/assets/scenes/MainScene.dt
    git show origin/pr/1:core/assets/orig/background.png
    git show origin/pr/1:core/assets/orig/background.atlas

Usage:

    python3 tools/convert-h2d-scene.py                 # write the level + background
    python3 tools/convert-h2d-scene.py --dry-run       # report only, write nothing

Requires Pillow (only for the background crop):  pip install --user pillow
"""

import argparse
import json
import subprocess
import sys
from pathlib import Path

# --------------------------------------------------------------------------
# Configuration
# --------------------------------------------------------------------------

# The recovered branch. Fetch it with:
#   git fetch origin '+refs/pull/1/head:refs/remotes/origin/pr/1'
SOURCE_REF = "origin/pr/1"

SCENE_PATH = "core/assets/scenes/MainScene.dt"
BACKGROUND_PNG_PATH = "core/assets/orig/background.png"

# H2D keys its scene content by fully-qualified runtime class name.
CLASS_COLOR_PRIMITIVE = "games.rednblack.editor.renderer.data.ColorPrimitiveVO"
CLASS_LABEL = "games.rednblack.editor.renderer.data.LabelVO"
CLASS_SIMPLE_IMAGE = "games.rednblack.editor.renderer.data.SimpleImageVO"

# World bounds are a *declared* property of the level, not derived from the art.
# 2048 is the texture-size ceiling; squaring off at it leaves room to swap in
# differently-sized background art without re-deriving world bounds.
WORLD_WIDTH = 2048
WORLD_HEIGHT = 2048

# The sole region in background.atlas. Atlas bounds are top-left origin.
BACKGROUND_REGION = {"x": 1, "y": 1, "w": 1980, "h": 1485}

LEVEL_NAME = "main"
BACKGROUND_ASSET = "levels/main-background.png"

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_LEVEL = REPO_ROOT / "core/assets/levels/main.level.json"
OUT_BACKGROUND = REPO_ROOT / "core/assets/levels/main-background.png"


# --------------------------------------------------------------------------
# Reading the source
# --------------------------------------------------------------------------


def git_show(path, binary=False):
    """Read `<SOURCE_REF>:<path>` straight out of the object store."""
    result = subprocess.run(
        ["git", "show", f"{SOURCE_REF}:{path}"],
        cwd=REPO_ROOT,
        capture_output=True,
        check=True,
    )
    return result.stdout if binary else result.stdout.decode("utf-8")


# --------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------


def to_world(vertices, item):
    """
    Translate H2D shape-local vertices into world space.

    World vert = shape vertex + item (x, y).

    The `originX`/`originY` fields on every primitive (uniformly 50, 50) are
    H2D's *rotation* origin, not part of the world transform. The old renderer
    ignored them and drew correctly, so we ignore them too.

    Coordinates stay in RAW PIXELS. The Kotlin LevelLoader is the single
    pixels -> world-units conversion boundary; nothing is scaled here.
    """
    ox = item.get("x", 0)
    oy = item.get("y", 0)
    return [[v["x"] + ox, v["y"] + oy] for v in vertices]


def convert_primitives(items):
    """Split the ColorPrimitiveVO list into collision polys and occluders."""
    collision = []
    occluders = []

    for item in items:
        shape = item["shape"]
        outline = to_world(shape["vertices"], item)
        identifier = item["itemIdentifier"]

        if identifier == "COLLISION":
            # `vertices` is the raw outline as drawn and may be concave.
            # `polygonizedVertices` is H2D's convex decomposition (1-8 pieces).
            #
            # We keep both: the outline for rendering and nav-mesh verts, the
            # convex pieces for containment. libGDX's Polygon.contains() is only
            # correct for convex polygons -- the old MapManager tested against
            # the raw outline and was silently wrong on the concave shapes.
            convex = [to_world(piece, item) for piece in shape["polygonizedVertices"]]
            collision.append({"outline": outline, "convex": convex})
        elif identifier == "OCCLUDER":
            # Occluders only feed FoV edge derivation, so the outline suffices.
            occluders.append({"outline": outline})
        else:
            raise ValueError(f"unexpected ColorPrimitiveVO identifier: {identifier!r}")

    return collision, occluders


def convert_spawns(items):
    """LabelVO -> spawn point. `kind` is the identifier minus the '_SPAWN' suffix."""
    spawns = []
    for item in items:
        identifier = item["itemIdentifier"]
        if not identifier.endswith("_SPAWN"):
            raise ValueError(f"unexpected LabelVO identifier: {identifier!r}")
        spawns.append(
            {
                "kind": identifier[: -len("_SPAWN")],
                "x": item["x"],
                "y": item["y"],
            }
        )
    return spawns


def bounding_box(level):
    """Bbox over all collision + occluder outline verts. Our coordinate-convention check."""
    points = [p for poly in level["collision"] for p in poly["outline"]]
    points += [p for poly in level["occluders"] for p in poly["outline"]]
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    return min(xs), min(ys), max(xs), max(ys)


# --------------------------------------------------------------------------
# Background
# --------------------------------------------------------------------------


def crop_background(dry_run):
    """
    Cut the sole atlas region ("openExterior") out of the 2048x2048 sheet.

    Atlas bounds are top-left origin, which is also PIL's convention, so the
    crop box is a direct translation. The result is written as a plain PNG and
    the atlas is dropped -- there was only ever one region in it.
    """
    from PIL import Image
    import io

    png_bytes = git_show(BACKGROUND_PNG_PATH, binary=True)
    sheet = Image.open(io.BytesIO(png_bytes))

    r = BACKGROUND_REGION
    box = (r["x"], r["y"], r["x"] + r["w"], r["y"] + r["h"])
    cropped = sheet.crop(box)

    # Sanity: a correct crop is opaque artwork, not a blank/transparent corner.
    extrema = cropped.convert("RGBA").getchannel("A").getextrema()
    print(f"  background sheet:  {sheet.size[0]}x{sheet.size[1]} {sheet.mode}")
    print(f"  cropped region:    {cropped.size[0]}x{cropped.size[1]} (box {box})")
    print(f"  alpha extrema:     {extrema}  (max 0 would mean fully transparent)")

    if not dry_run:
        OUT_BACKGROUND.parent.mkdir(parents=True, exist_ok=True)
        cropped.save(OUT_BACKGROUND)
        print(f"  wrote {OUT_BACKGROUND.relative_to(REPO_ROOT)}")


# --------------------------------------------------------------------------
# Verification
# --------------------------------------------------------------------------


def verify(level):
    """Assert the counts and shapes we expect, and print the numbers for review."""
    collision = level["collision"]
    occluders = level["occluders"]
    spawns = level["spawns"]

    print(f"  collision polys:   {len(collision)}")
    print(f"  occluders:         {len(occluders)}")
    print(f"  spawns:            {len(spawns)}")

    assert len(collision) == 8, f"expected 8 collision polys, got {len(collision)}"
    assert len(occluders) == 8, f"expected 8 occluders, got {len(occluders)}"
    assert len(spawns) == 15, f"expected 15 spawns, got {len(spawns)}"

    counts = {}
    for s in spawns:
        counts[s["kind"]] = counts.get(s["kind"], 0) + 1
    expected = {"PARTY": 2, "ENEMY": 3, "TREASURE": 8, "CART": 1, "HEALER": 1}
    print(f"  spawn kinds:       {counts}")
    assert counts == expected, f"spawn kinds {counts} != {expected}"

    print("  collision poly  outline_verts  convex_pieces  (verts per piece)")
    for i, poly in enumerate(collision):
        pieces = poly["convex"]
        sizes = [len(p) for p in pieces]
        print(f"    [{i}]           {len(poly['outline']):>2}             {len(pieces):>2}"
              f"           {sizes}")
        assert len(pieces) >= 1, f"collision poly {i} has no convex pieces"
        assert 4 <= len(poly["outline"]) <= 10, (
            f"collision poly {i} outline has {len(poly['outline'])} verts, expected 4-10"
        )

    print("  occluder        outline_verts")
    for i, poly in enumerate(occluders):
        print(f"    [{i}]           {len(poly['outline']):>2}")

    min_x, min_y, max_x, max_y = bounding_box(level)
    print(f"  geometry bbox:     x [{min_x:.1f} .. {max_x:.1f}]  "
          f"y [{min_y:.1f} .. {max_y:.1f}]")
    print(f"  world bounds:      {WORLD_WIDTH} x {WORLD_HEIGHT}")
    print(f"  background art:    {BACKGROUND_REGION['w']} x {BACKGROUND_REGION['h']}")

    assert 0 <= min_x and max_x <= WORLD_WIDTH, "geometry escapes world width"
    assert 0 <= min_y and max_y <= WORLD_HEIGHT, "geometry escapes world height"


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dry-run", action="store_true", help="report only; do not write any files"
    )
    args = parser.parse_args()

    print(f"reading {SOURCE_REF}:{SCENE_PATH}")
    scene = json.loads(git_show(SCENE_PATH))
    content = scene["composite"]["content"]

    collision, occluders = convert_primitives(content[CLASS_COLOR_PRIMITIVE])
    spawns = convert_spawns(content[CLASS_LABEL])

    # The single SimpleImageVO is the background. It carries no x/y, so it sits
    # at the origin; we hardcode that in the schema rather than inferring it.
    images = content[CLASS_SIMPLE_IMAGE]
    assert len(images) == 1, f"expected 1 SimpleImageVO, got {len(images)}"
    assert images[0]["itemIdentifier"] == "BACKGROUND"
    assert images[0]["imageName"] == "openExterior"

    level = {
        "name": LEVEL_NAME,
        "world": {"width": WORLD_WIDTH, "height": WORLD_HEIGHT},
        "background": {"image": BACKGROUND_ASSET, "x": 0, "y": 0},
        "collision": collision,
        "occluders": occluders,
        "spawns": spawns,
    }

    print("verifying level data")
    verify(level)

    print("cropping background")
    crop_background(args.dry_run)

    if args.dry_run:
        print("dry run: nothing written")
        return

    OUT_LEVEL.parent.mkdir(parents=True, exist_ok=True)
    OUT_LEVEL.write_text(json.dumps(level, indent=2) + "\n")
    print(f"wrote {OUT_LEVEL.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
