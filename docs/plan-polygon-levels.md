# Plan: Apple Silicon revival + polygon levels without HyperLap2D

**Status:** draft
**Branch:** `polygon-levels` (off `main`)
**Source of salvage:** `origin/pr/1` (the deleted `add-hyperlap2d` branch, recovered from the GitHub PR ref)

---

## Background

`main` is the 2021 lineage (`ae4ee68`). The 2023 work — Apple Silicon revival, HyperLap2D
integration, polygon collision/occlusion, and the start of a nav mesh — lived on a branch
called `add-hyperlap2d`, merged into a `master` branch that has since been deleted. It
survives only as a GitHub PR ref:

```sh
git fetch origin '+refs/pull/1/head:refs/remotes/origin/pr/1'
```

`origin/pr/1` is 30 commits ahead of `main`; `main` is 1 commit ahead of it (`ae4ee68 formatting`).
Merge base is `03ecd5e` (2021-02-16).

We want two things off that branch and nothing else:

1. The **Apple Silicon / toolchain fixes** (the game does not currently build or run).
2. The **polygon level data** and the collision / occlusion / pathfinding work built on it.

We are explicitly **dropping HyperLap2D**: the submodule, its custom
`TextureArrayCpuPolygonSpriteBatch` renderer, `project.dt`, the atlases, and the whole
`h2d/` project directory. We'll build our own editor later; H2D's value here was as a
polygon-drawing tool, and that value is now frozen into data we can extract once.

### Why the coupling is cheap to cut

H2D appears in exactly three files on `origin/pr/1`:

| File | Usage |
|---|---|
| `Application.kt` | registers `ResourceManagerLoader`, loads `project.dt` as `AsyncResourceManager` |
| `BattleScreen.kt` | builds `SceneLoader`, `loadScene("MainScene")`, passes `sceneLoader.batch` to two systems, `sceneLoader.engine.process()` |
| `MapManager.kt` | reads `sceneLoader.sceneVO.composite.content[...]` for polys, labels, background |

`sceneLoader.batch` is the only *rendering* dependency, and it exists solely because H2D
needs its own batch type. A plain `SpriteBatch` replaces it.

---

## Phase 0 — Branch setup

```sh
git fetch origin '+refs/pull/1/head:refs/remotes/origin/pr/1'
git switch -c polygon-levels main
```

**Done.** The working tree was dirty with AI-demo junk: `README.md` machine-translated to
German, `sssssss` appended to `LICENSE.md`, and — the one that mattered — `fdasfs`/`dddd`
injected into two rows of tile CSV in `dungeon.tmx`, i.e. corrupt map data. All reverted via

```
stash@{0}  demo junk: german README, corrupt tmx, LICENSE sssssss (pre-polygon-levels)
```

so it's recoverable if any of it turns out to be wanted. The one legitimate change,
`.gitignore` += `.devswarm-temp/`, was kept.

We port by hand rather than merging or cherry-picking, because every interesting commit on
`origin/pr/1` also drags H2D along. The branch is a reference, not a source of commits.

---

## Phase 1 — Apple Silicon / toolchain

**Goal:** `./gradlew desktop:run` launches the *existing tile-based game* on arm64 macOS.
No gameplay or rendering changes. This gives us a running baseline to port polygon work onto.

### Why the current tree cannot build

- Gradle **4.6** (`gradle/wrapper/gradle-wrapper.properties`) cannot run on any JDK past 12.
  Installed JDK is **21.0.11 arm64 (Homebrew)**.
- `compile`/`testCompile` configurations were removed in Gradle 7.
- The LWJGL **2** backend (`gdx-backend-lwjgl`) has no Apple Silicon native build. This is
  the actual arm64 blocker, and the reason `pr/1` switched to LWJGL3.

### Version targets

| Thing | Current (`main`) | `pr/1` had | **Target** | Rationale |
|---|---|---|---|---|
| Gradle | 4.6 | 7.6.1 | **8.14.5** | 9.6.1 exists but Kotlin plugin support on 9.x is newer than I want under us. 8.14.5 is the last 8.x. |
| Kotlin | 1.2.71 | 1.9.0 | **2.2.21** | Latest stable is 2.4.10, but ktx is compiled against 2.1.10. 2.2.x is a safe forward step; stdlib is backward compatible. |
| libGDX | 1.9.10 | 1.12.0 | **1.13.1** | **Not 1.14.2.** `ktx-app:1.13.1-rc1`'s POM pins `gdx:1.13.1`. Matching ktx's tested pairing beats chasing two minor versions. |
| ktx | 1.9.8-b4 | 1.12.0-rc1 | **1.13.1-rc1** | Newest published. All 10 modules we use exist at this version (verified). ktx has shipped `-rc` as its release channel for years; this is normal. |
| Ashley | 1.7.0 | 1.7.4 | **1.7.4** | Latest. |
| gdx-ai | 1.8.0 | 1.8.1 | **1.8.2** | Latest. |
| Java target | 1.8 | 1.8 | **17** | Kotlin 2.x + Gradle 8 will not target 8. |
| vis-ui | 1.3.0 | 1.3.0 | **1.5.9** | Latest; 1.3.0 predates gdx 1.13 APIs. |
| logback | 1.3.0-alpha4 | same | **1.5.38** | Off the alpha. Brings slf4j-api 2.0.x transitively. |
| slf4j | 1.8.0-beta4 | same | *(transitive)* | Drop the explicit beta pin; let logback bring it. |
| junit | 4.13-beta-2 | same | **4.13.2** | Off the beta. Staying on JUnit 4 — `junit-quickcheck` is a JUnit 4 runner. |
| junit-quickcheck | 0.8.2 | same | **1.0** | Latest. |

### Dependencies to delete outright

Present on `pr/1`, all H2D-related or dead:

- `net.onedaybeard.artemis:artemis-odb` — H2D's ECS. We use Ashley.
- `com.badlogicgames.gdx:gdx-lwjgl3-angle` — see ANGLE note below.
- `org.jetbrains.kotlin:kotlin-script-runtime:1.3.31` — hard-pinned to an ancient Kotlin,
  was for Scratch files.
- `games.rednblack.hyperlap2d:runtime-libgdx` (already commented out) and the
  `hyperlap2d-runtime-libgdx` submodule + its `settings.gradle` entry.

Also worth questioning during implementation: `gdx-bullet` and `gdx-box2d` are pulled in but
I see no usage. If nothing references them, drop them — they're the heaviest natives in the build.

### File changes

**`gradle/wrapper/gradle-wrapper.properties`** — `distributionUrl` → `gradle-8.14.5-bin.zip`.
Regenerate the wrapper jar/scripts via `gradle wrapper` rather than hand-editing.

**`build.gradle`** — version block per table above; `compile` → `api`, `testCompile` →
`testImplementation` throughout; `sourceCompatibility` → Kotlin `jvmToolchain(17)`.
Desktop backend `gdx-backend-lwjgl` → `gdx-backend-lwjgl3`.

**`settings.gradle`** — stays `include 'desktop', 'core'`. (Note: the dirty working-tree
copy already differs from HEAD; make sure we land on the two-module version.)

**`desktop/build.gradle`** — replace the `task run(type: JavaExec)` syntax with
`tasks.register('run', JavaExec)` (Gradle 8 requires it), and add:

```groovy
if (OperatingSystem.current() == OperatingSystem.MAC_OS) {
    jvmArgs += "-XstartOnFirstThread"   // mandatory for LWJGL3 on macOS
}
jvmArgs += "-XX:ReservedCodeCacheSize=2048m"   // from pr/1, for hot-reload reliability
```

**`desktop/src/.../DesktopLauncher.java`** — LWJGL2 → LWJGL3:

```java
Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
config.useVsync(true);                       // pr/1 commit 6d2e5b5 — fixes screen tearing
config.setWindowedMode(1920, 1080);
config.setTitle("CRPG Combat Prototype");
new Lwjgl3Application(new Application(), config);
```

**Deliberately NOT ported:** `config.setOpenGLEmulation(ANGLE_GLES20, 3, 2)`. That was a
2023 workaround for the H2D texture-array renderer bug documented in `dev-notes.md`
(`GLD_TEXTURE_INDEX_2D is unloadable`). We are deleting that renderer, so the workaround has
no reason to exist, and it drags in the `gdx-lwjgl3-angle` native. Plain GL on LWJGL3 is fine
on Apple Silicon. If the Phase 3 FoV framebuffer misbehaves, this is the first thing to try
re-adding.

### Expected fallout

Kotlin 1.2 → 2.2 is a 7-year jump; expect compile errors, mostly mechanical:

- `maxBy` changed semantics in Kotlin 1.4 (now returns non-null, `maxByOrNull` is the old
  behavior). `MapManager.getTilesInAdjacentGroups` uses `tiles.maxBy { ... }!!` — the `!!`
  will become a warning/error.
- `Map.forEach { pos, group -> }` destructuring in `getTilesInAdjacentGroups` needs
  `{ (pos, group) -> }`.
- `.toMap()` on a `List<Pair<..>>` and other stdlib signature drift.
- `assetManager.setLoader` / ktx extension signature changes across ktx majors.

### Exit criteria — **DONE** (commit `bf38f9f`), with one caveat

- ✅ `./gradlew build` passes; `core:test` 14/14 (`MapManagerTest`, `UtilsKtTest`).
- ✅ `./gradlew desktop:run` opens a window on arm64 and **renders the dungeon correctly** —
  tilemap, entities, and debug geometry filling the full 3840×2160 backbuffer.
- ✅ No `hyperlap2d` string anywhere in the tree.

Bugs found and fixed beyond the planned scope, all from the `minBy` semantic change
(pre-1.4 returned `null` on empty; since 1.7 throws `NoSuchElementException` — same call
site, same signature, no compile error):

- `FieldOfViewSystem` crashed on frame 1 when a ray hit no walls.
- `CombatantSystem` was written `minBy { .. } ?: return` and would have crashed the moment
  either side ran out of units — i.e. on killing the last orc.

#### The rendering bug: leaked GL depth state

The map never drew. Root cause, in `RenderSystem.draw()`:

```kotlin
Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST)
Gdx.gl20.glDepthFunc(GL20.GL_EQUAL)   // ...never disabled
```

This is one half of an **unfinished field-of-view masking scheme**: `FovRenderSystem` renders
the visibility polygons into the depth buffer with colour writes off, then `RenderSystem`
draws sprites with `GL_EQUAL` so only fragments inside the FoV survive.

It cannot work as arranged, because of system order. `FovRenderSystem` is registered *before*
`RenderSystem`, so per frame:

1. clear — **`GL_COLOR_BUFFER_BIT` only**, depth is never cleared here
2. `mapRenderer.render()`
3. `FovRenderSystem` — clears depth, writes the FoV mask
4. `RenderSystem` — enables `GL_EQUAL` depth test, draws sprites, **leaves it enabled**
5. UI

Step 4 leaks the `GL_EQUAL` test into the *next* frame's step 2, where the tilemap is tested
against a stale mask and culled almost everywhere. `FovRenderSystem` had the same
non-restoration problem with `glColorMask(false)`.

Both now restore state at the end of their draw. That necessarily makes the masking inert —
FoV occludes nothing today — which is an accepted trade: Phase 3 replaces the scheme with the
framebuffer renderer. This is almost certainly why the 2023 branch rewrote it (`b963713`,
*"using a framebuffer that we render over the other stuff seems to do the trick"*). **Treat
"this worked in 2019" as unproven** — the evidence says this path never worked.

Also fixed: the render loop never cleared the colour buffer, and no screen overrode `resize()`.

#### A note on method

**macOS `screencapture` cannot see window contents without Screen Recording permission.** It
silently returns byte-identical frames of the desktop and menu bar, which reads convincingly
as "the game renders black". Two wrong diagnoses came out of trusting it — a phantom GL-state
bug and a phantom HiDPI bug.

Use `Screenshot.kt` instead (added in Phase 1), which reads the real framebuffer from inside
the app:

```
./gradlew :desktop:run -Dcrpg.capture=60 -Dcrpg.capture.out=/tmp/frame.png
```

or press **F12** in-game to write `screenshots/crpg-NNNN.png`. `Screenshot.logRenderState()`
dumps logical vs backbuffer size, viewport, camera, and the visible world rect — the numbers
you compare a capture against.

---

## Phase 2 — Extract the level data

**Goal:** the polygon data lives in our own format, read by our own loader. H2D fully gone.

### What's in `MainScene.dt`

Recovered via `git show origin/pr/1:core/assets/scenes/MainScene.dt`:

| Type | Count | Identifiers |
|---|---|---|
| `ColorPrimitiveVO` | 16 | 8 × `COLLISION`, 8 × `OCCLUDER` |
| `LabelVO` | 15 | 2 `PARTY_SPAWN`, 3 `ENEMY_SPAWN`, 8 `TREASURE_SPAWN`, 1 `CART_SPAWN`, 1 `HEALER_SPAWN` |
| `SimpleImageVO` | 1 | `BACKGROUND` → `imageName: "openExterior"` |

Collision polygon vert counts run 4–10; **occluders run 4–18** (they're drawn in more
detail). Background region is 1980×1485 inside a 2048² atlas
(`core/assets/orig/background.atlas`), and it is the *only* region in that atlas — so it
becomes a plain PNG and the atlas is deleted.

### The `polygonizedVertices` field matters

Each `shape` carries **both**:

- `vertices` — the raw outline as drawn, possibly concave
- `polygonizedVertices` — H2D's convex decomposition (1–8 convex pieces per shape)

The `pr/1` `MapManager` built `Polygon(vertices)` and called `.contains(pos)`. **libGDX's
`Polygon.contains` is only correct for convex polygons**, so collision against the concave
shapes was silently wrong. Our format keeps the convex decomposition and does containment
against the pieces. This is a real bug fix carried in, not just a port.

### Coordinate convention

World vert = `shape.vertices[i] + (item.x, item.y)`. The `originX`/`originY` fields (uniformly
`50, 50` on every primitive) are H2D's rotation origin and are **not** part of the world
transform — `pr/1` ignored them and rendered correctly. The converter replicates
`vertex + (x, y)` and we validate visually against the background before trusting it.

`SimpleImageVO` for the background carries no `x`/`y`, so it defaults to origin `(0, 0)`.

**Resolved — intentional, keep as-is.** `BattleScreen`'s hardcoded
`worldWidth = worldHeight = 2048` against 1980×1485 art is deliberate: 2048 is the texture-size
ceiling, and squaring off at it leaves room to swap in differently-sized background assets
without re-deriving world bounds. The slack between the art and the bounds is accepted.

So world bounds stay a **declared property of the level** (`world.width`/`world.height` in the
schema below), not something derived from background extents. Different levels may pair
different art with the same bounds.

Consequence to keep in mind: the occluder bounding ring is built from world dims, so it sits
*outside* the visible art. That's harmless for FoV (it only needs to stop raycasts escaping)
but means the playable area is larger than the art — walking off the edge of the background is
possible unless collision geometry prevents it.

The 2048 ceiling itself is a known annoyance worth revisiting someday (tiled/virtual textures,
or splitting the background into chunks). Out of scope here — noted under Deferred.

### Target format — `core/assets/levels/main.level.json`

```jsonc
{
  "name": "main",
  "world": { "width": 2048, "height": 2048 },
  "background": { "image": "levels/main-background.png", "x": 0, "y": 0 },
  "collision": [
    {
      "outline": [[x, y], ...],           // world-space, for rendering + nav-mesh verts
      "convex": [ [[x, y], ...], ... ]     // convex pieces, for containment tests
    }
  ],
  "occluders": [
    { "outline": [[x, y], ...] }           // FoV only; edges derived at load
  ],
  "spawns": [
    { "kind": "PARTY",  "x": 654, "y": 644 }
  ]
}
```

Flat `[x, y]` pairs rather than `{"x":..,"y":..}` objects — smaller, and trivially
`Vector2`-able.

### Work items

1. ✅ **Converter** — `tools/convert-h2d-scene.py`, reads `MainScene.dt` and `background.png`
   straight out of `origin/pr/1` via `git show`. Re-runnable, has `--dry-run`, not part of
   the build.
2. ✅ **Background asset** — `core/assets/levels/main-background.png`, 1980×1485, verified
   opaque artwork rather than a blank or offset crop.
3. ⬜ **`Level.kt` / `LevelLoader.kt`** — data classes plus a loader using libGDX's `Json`
   reader (already on the classpath; no new dependency). **Still to do.**
4. ✅ **Delete** `project.dt`, `scenes/`, `orig/` — no-op; those only ever existed on `pr/1`,
   never on this lineage.

Extraction landed in `c66f627` (merged as `f745ebf`). Verified independently:

- 8 collision / 8 occluders / 15 spawns; kind counts PARTY 2, ENEMY 3, TREASURE 8, CART 1,
  HEALER 1.
- Geometry bbox **x [319.2 … 1963.8], y [43.4 … 1482.8]** against 1980×1485 art. The y-max
  landing 2.2px inside the art height is strong evidence the `vertex + (x, y)`,
  ignore-`originX/originY` convention is right.
- Every convex piece is non-degenerate — minimum shoelace area **1348.93 px²**, so H2D's
  decomposer emitted no slivers and the loader needs no area guard.

### Exit criteria

Level loads; a debug `ShapeRenderer` pass draws collision polys in one color and occluders in
another, overlaid on the background, and **the geometry visually lines up with the art**. That
alignment check is the real test of the coordinate convention above.

---

## Phase 3 — Port the polygon systems

**Goal:** collision, FoV, and rendering run off the new `Level`, with a plain `SpriteBatch`.

### `MapManager.kt` — rewrite

Drops the entire `TiledMap` implementation on `main` (`TileCell`, `getCells`,
`getTilesInAdjacentGroups`, `getEdgesFromCornersAndVerts`, `walkDirection`, …). That machinery
existed to *derive* polygon edges from a tile grid — which is exactly what the level editor now
gives us directly. ~300 lines deleted.

Note this also obsoletes most of `MapManagerTest`. The tile-group/corner-walking tests go with
the code they test; do not port them. New tests belong to Phase 4's geometry.

Keeps: `Edge`, `findPath`, the `IndexedGraph` implementation (rebuilt in Phase 4).

New shape:

```kotlin
class MapManager(private val level: Level) : IndexedGraph<PathNode> {
    val collision: List<CollisionPoly>       // outline + convex pieces
    val occluderEdges: List<Edge>            // flattened, + world-bounds ring
    fun spawns(kind: SpawnKind): List<Vector2>
    fun contains(p: Vector2): Boolean        // convex-piece test
}
```

The world-bounds edge ring from `pr/1` (`49bb765` — "setting a bounding set of edges corrects
the weird FoV behaviour") must be carried over. Without it the FoV raycast escapes the level
and produces garbage polygons.

### `FieldOfViewSystem` — takes `List<Edge>` from occluders instead of tile-derived edges.
Interface is already `List<Edge>` on `main`, so this is mostly a wiring change.

### `FovRenderSystem` — port `pr/1`'s framebuffer version

`main` has a 61-line version; `pr/1` has a 104-line rewrite using an FBO
(`b963713` — "using a framebuffer that we render over the other stuff seems to do the trick").
Port the FBO approach, with two changes:

- Constructor takes a plain `Batch`, not `sceneLoader.batch`.
- The FBO is sized from `viewport.screenWidth/screenHeight` at construction and **never
  recreated on resize** — that's a latent bug on `pr/1`. Add `resize()` handling.

`pr/1` left a comment questioning the whole approach (invert it: draw a black quad and
*subtract* the FoV polygons, rather than masking). Worth trying if the port fights us, but not
in scope for this phase — port first, then improve.

### Carried forward from Phase 1

Nothing outstanding — the rendering problems were traced and fixed in Phase 1 (see below).
There is **no HiDPI bug**: content fills the full 3840×2160 backbuffer correctly.

The one thing to carry in mind is that field-of-view masking is currently **inert**. Phase 1
restored the leaked depth/colour state that was breaking everything else, which necessarily
disabled the depth-buffer masking trick. Restoring the *effect* is this phase's job, via the
framebuffer renderer.

### Known-broken thing to fix

`pr/1` commit `1759d34`: *"the UI rendering breaks the FOV system."* `BattleHealthUiRenderer`
and `FovRenderSystem` both begin/end the shared batch, and system order decides who wins.
Fix by making FoV composite last, or by giving the UI its own batch with a separate
projection matrix.

### `BattleScreen.kt` / `Application.kt` — de-H2D

Delete `SceneConfiguration`, `SceneLoader`, `AsyncResourceManager`, `ResourceManagerLoader`,
the `project.dt` load, and `sceneLoader.engine.process()`. Replace with a `Level` load. Pass
the DI-provided `SpriteBatch` where `sceneLoader.batch` was.

### `SiUnits` — restore it, don't follow `pr/1` here

`pr/1` commit `e06d178` deleted the `SiUnits` object and commented out all ~12 use sites,
because H2D normalized scaling and the pixels↔meters conversion became identity. That
justification dies with H2D. Grep `origin/pr/1` and you'll find only commented-out ghosts
(`// * SiUnits.PIXELS_TO_METER`) scattered through `Application.kt`, `RenderSystem.kt`,
`BattleHealthUiRenderer.kt` — the branch was left mid-decision, in an implicit
"everything is raw pixels" state.

`main` still has it:

```kotlin
object SiUnits {
    const val UNIT = 64                              // minimum resolution of a character
    const val PIXELS_TO_METER = 1.0f / UNIT.toFloat()
}
```

**Keep this.** `UNIT = 64` is not incidental — it's exactly the base of the 64/128/256px
creature size classes the nav mesh is specced around, so the Phase 4 radii are cleanly
`0.5 / 1.0 / 2.0` UNIT rather than three magic pixel constants. Having one declared
world-unit atom is the right call for a game where movement, weapon reach, and FoV all need
to agree on distance.

Decision for implementation: **the level JSON is authored in pixels** (that's what H2D
exported and what the art is in), and `LevelLoader` is the single conversion boundary —
it multiplies by `PIXELS_TO_METER` on load. Everything downstream of the loader is in world
units. This keeps the conversion in exactly one place instead of the ~12 scattered call sites
`main` has today.

That also means restoring the commented-out viewport setup in `Application.kt`
(`OrthographicCamera(w * PIXELS_TO_METER, h * PIXELS_TO_METER)`, `viewport.unitsPerPixel`)
rather than leaving the identity-scale version `pr/1` ended on.

Two hardcoded values from `pr/1` `0505057` to carry over as-is (with the existing TODO intact):
cart drop-zone rect `128×256`, and the orc aggro/melee ranges.

---

## Phase 4 — Finish the nav mesh

**Goal:** the thing that was never written. `pr/1`'s `MapManager` has a `navMesh` block whose
body is a `deriveNavPoint` lambda that literally `println("todo")` and returns `Vector2(0,0)`,
plus an empty loop. The grid nav was commented out in `e06d178`, so the branch has no working
pathfinding at all.

### The design (from the in-code note on `pr/1`, commit `8aaa5fb`)

> Use collision polygon verts as nav nodes. At level load, grab each vert from collision,
> offset those verts by the radius of the various size creatures we'll support (64px, 128px,
> 256px). The offset is a function of the angle (if acute, needs to be further out) and the
> creature radius. Once we've calc'd those offsets, for each vert connect them if they have
> unobstructed line-of-sight. That's our A* graph.

This is a **visibility graph**, and it's the right call for close-quarters tactical movement:

- Paths come out already taut — no post-hoc string-pulling. (Note: there's a local branch
  `add-string-pulling-path-finding`, which is the workaround this design makes unnecessary.)
- Node count scales with wall complexity, not floor area. The grid version was
  `2048/10 × 2048/10 ≈ 42k` nodes for 8 obstacles.
- Corner-hugging movement is what Door Kickers-style play wants.

### Implementation

**1. Vertex offsetting (miter).** For collision vertex `v` with neighbors `prev`/`next`, let
`θ` be the interior angle. Offset outward along the angle bisector by

```
d = r / sin(θ / 2)
```

which is the standard miter offset and gives exactly the "acute ⇒ further out" behavior the
note calls for. Apply a **miter limit** (~`4r`); beyond it the spike is degenerate, so emit two
bevel points instead of one miter point.

Then discard any offset point that falls inside *any* collision polygon (convex-piece test) —
this handles obstacles closer together than `2r`.

**2. Visibility edges.** For each pair of nav points, the segment is traversable if it
intersects no collision edge. Naive is `O(n²)` segment tests over pairs; with ~70 collision
verts across 8 polys that's trivial and needs no acceleration structure. Revisit only if level
complexity grows.

Subtlety: offsetting by `r` makes *vertices* safe but not *edges* — a segment can pass within
`r` of a wall without intersecting it. Correct test is segment-vs-wall distance `≥ r`, not bare
intersection. Cheaper equivalent: test intersection against walls dilated by `r`.

**3. One graph per size class.** Three radii, expressed in `SiUnits.UNIT` rather than raw
pixels (see Phase 3): `0.5`, `1.0`, `2.0` UNIT — i.e. half the 64/128/256px creature
diameters. Build at load, index by size class. `CTransform` already carries a radius, and it
will be in world units once the loader owns the conversion.

**4. A\*.** Keep `IndexedAStarPathFinder` with a Euclidean heuristic. Nodes get a stable
index at build time — this is what the `pr/1` grid version was awkwardly encoding as
sign-packed ints in `pathNodes`, and it goes away.

**5. Dynamic endpoints.** Start and goal aren't graph nodes. Per query, connect each to all
nav nodes it has line-of-sight to, run A*, then discard. Short-circuit: if start sees goal
directly, return the straight segment.

### Testing

This phase is pure geometry, which means it is finally unit-testable without a GL context —
unlike everything above it. Cover: miter offset at acute/right/obtuse/reflex angles; miter-limit
bevel fallback; offset points rejected inside obstacles; visibility rejection through a wall;
the `r`-clearance case (segment passing near a corner); path through a concave obstacle.

### Exit criteria

Click-to-move produces taut paths that round corners at a believable distance, with no
clipping, for all three size classes.

---

## Sequencing

Strictly sequential — each phase depends on the previous one compiling and running. Parallel
workspaces would not help; the phases share the same handful of files (`MapManager.kt`,
`BattleScreen.kt`, `Application.kt`), so concurrent agents would collide.

Commit per phase. Phase 1 in particular should land alone.

## Risk register

| Risk | Mitigation |
|---|---|
| Kotlin 1.2→2.2 breaks more than expected | Phase 1 is isolated and committed alone; bisectable |
| ktx `1.13.1-rc1` API drift across 4 majors | All 10 modules verified present; fix at compile time |
| FoV framebuffer misbehaves without ANGLE | Re-add `setOpenGLEmulation` + `gdx-lwjgl3-angle` |
| Coordinate convention wrong | Phase 2 exit criteria is a *visual* alignment check |
| Unit-conversion errors from restoring `SiUnits` | Loader is the single conversion boundary; nothing downstream touches pixels |
| Visibility graph `O(n²)` too slow | ~70 verts today; revisit only if levels grow |

## Deferred

- Our own level editor (the actual reason H2D goes away).
- **The 2048 texture-size ceiling.** World bounds are squared off at it to accommodate
  varying background art (see Phase 2). It's a real constraint on level size and a known
  annoyance; escaping it means chunked/tiled backgrounds or virtual texturing. Someday.
- Dynamic obstacles / doors in the nav graph — doors matter a lot for the Door Kickers side
  of this, and a static visibility graph doesn't model them. Design once static nav works.
- Deleting `dungeon.tmx` / `dungeon-tileset.tsx` / the Tiled dependency, once nothing reads them.
