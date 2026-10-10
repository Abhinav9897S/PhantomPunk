# Design

## Architecture at a glance

```
desktop  ──►  PhantomGame (composition root, fixed-timestep loop)
                 │
                 ├── ScreenManager (stack) ── TitleScreen, GameScreen, PauseScreen, MapScreen,
                 │                            DialogueScreen, DeathScreen, SettingsScreen, EndingScreen
                 │
                 ├── shared services: SpriteFactory (art), Fonts, AudioManager, SaveManager, KeyboardInput
                 │
GameScreen ──►  GameWorld  (pure logic, no rendering)      WorldRenderer + Hud  (pure drawing)
                 │  EntityManager ── List<GameObject>        ParallaxBackground, TileRenderer,
                 │  Room ── TileMap                          LightingSystem, RenderContext
                 │  CollisionSystem, CombatSystem
                 │  ParticleSystem + Effects, CameraController
                 │  ObjectPool<Projectile>, BossArena, RoomTransition
                 │
                 └── EventBus ──► AudioManager, AchievementTracker, EndingDirector, Hud, saving
```

The **core** module contains the whole game; the **desktop** module is a 30-line launcher plus
developer tools. This is the standard libGDX layout and keeps a path open for other backends.

## Decisions and reasons

### 1. Logic never touches `Gdx.*`
`GameWorld`, every entity, the AI, physics, saving and level loading avoid libGDX statics such
as `Gdx.files` and `Gdx.graphics`. Rendering lives in `render/`, `art/`, `ui/` and `screens/`.
*Why:* the full game simulation runs in JUnit with no window and no GPU. The headless simulation
and traversal tests would be impossible otherwise. Entities receive a `WorldContext` interface
rather than the concrete `GameWorld`, so they depend on an abstraction.

### 2. Hand-written AABB tile collision instead of Box2D
`CollisionSystem.move` resolves X then Y against the tile grid in about 60 lines. Out-of-bounds
counts as solid. *Why:* platformer controls need exact, predictable behaviour (no bouncing,
no sliding off ledges), and every line can be explained in a viva. Speeds are capped below one
tile per tick, so a single leading-edge test per axis cannot tunnel.

### 3. Fixed timestep with interpolation
Logic runs at exactly 60 Hz; rendering interpolates between the last two ticks. *Why:*
deterministic physics (tests, consistent jump height) and smooth motion on any refresh rate.

### 4. Composition first, inheritance where the spec asks
The required `GameObject → Entity → Character → Enemy → Boss` chain carries only shared state.
Variation comes from parts:
* `Player` is assembled from `PlayerMotor`, `PlayerAttack`, `PlayerView`, `AbilitySet`,
  `Inventory`, `SoulMeter` and `Animator`.
* Abilities are `Ability` strategy objects. The player offers a mid-air jump press to each
  unlocked ability in enum order, so wall jump has priority over double jump with no special cases.
* Bosses are lists of `BossAttack` strategies per phase. Three bosses share seven attack classes.

### 5. Enemy AI as state machines
`StateMachine<E>` maps `EnemyState` to `EnemyBehaviour<E>` (usually method references such as
`Brute::charge`). Shared behaviours (`Behaviours.stunned`, `pause`, `canSeePlayer`) remove
duplication. *Why:* each state is small and testable (`EnemyStateTest` checks the transitions).

### 6. Event bus for cross-cutting reactions
Gameplay publishes events; audio, HUD, achievements, endings, boss arenas and saving subscribe.
Events are queued and dispatched once per tick to avoid re-entrancy problems (for example a
dialogue opening while the entity list is being iterated).

### 7. Data-driven rooms with a validator
Rooms are JSON files with an ASCII tile grid, so they are easy to read, edit and diff. The
`RoomLoader` factory validates them and throws `LevelFormatException` with the file name and
position on any error. `LevelValidator` then proves the map can be finished:
* per room: spawns not inside walls, doors on edges with matching doors back, a bench next to every boss;
* for the map: starting with no abilities, it explores each room with `RoomReachability`
  (a breadth-first search over standing and clinging positions using jumps and falls with
  clear "L-shaped" paths), collects shrine and boss rewards when their spot becomes reachable,
  and repeats until nothing new is gained.

The `MovementModel` is derived from the same constants as the real physics (jump velocity,
gravity, run and dash speed) with a safety margin, and wall jumps must push away from the wall,
as they do in the game. Because a model can still disagree with the real physics,
`PlayerPhysicsTest`, `GatingTraversalTest` and `UmbralTraversalTest` also *play* the critical
jumps with scripted input. These tests caught two real level bugs during development (a shaft
that could not be climbed, and a ceiling that made double jumps fall short).

### 8. Ability gating that cannot be bypassed
* **Dash gate** (Cistern Steps): a 9-tile pit over spikes 6 tiles deep. A jump covers about
  6.7 tiles; jump plus dash about 11. The spikes are too deep to pogo out of.
* **Moss Grip gate** (Drowned Chapel, Gearworks Shaft): 6-tile-wide shafts that can only be climbed
  by wall-jumping between the two walls.
* **Wisp Leap gate** (Mist Verge, Hollow Spire): floating one-way ledges 5 tiles apart vertically,
  above Hollow Mist. They cannot be wall-climbed (one-way ledges are not walls), and a single
  jump reaches just under 4 tiles.

### 9. Procedural art through a software rasteriser
Downloaded art was not allowed, so `art/Canvas` rasterises polygons, ellipses, tapered Bezier
strokes and radial glows into a float buffer at 4x resolution, then downsamples for
anti-aliasing. A rim-light pass gives the silhouettes their lit upper-left edges. Painters
(`SpritePainter` implementations) describe each creature as layered shapes; the
`SpriteFactory` packs everything into atlases once at startup (about 1 s of CPU time) and also
generates white silhouettes for hit flashes. Because `Canvas` is plain Java, the art is unit
tested and can be previewed as PNG without a GPU (`./gradlew :core:artPreview`).

### 10. Lighting
Entities call `ctx.light(...)` and `ctx.glow(...)` while rendering. `LightingSystem` draws the
lights additively into a half-resolution frame buffer cleared to the region's ambient colour,
then multiplies it over the scene with "2x" blending (so mid-grey is neutral and lights can
brighten). Glows (eyes, lantern cores, projectiles) and emissive sprites (slash arcs) are added
afterwards so darkness never dims them.

### 11. Saving
`SaveData` is a plain snapshot; `Inventory`, `AbilitySet` and `GameProgress` implement
`Saveable` and copy themselves in and out. `SaveManager` writes JSON to a temporary file and
moves it into place atomically. Loading validates the version (newer: rejected; older than
`MIN_SUPPORTED_VERSION`: rejected; in between: migrated) and repairs missing fields. A bad file
is renamed to `save.json.bad` and the player gets a message. Settings live in a separate file
so a New Game keeps them.

### 12. Respawn rules
Benches save and set the respawn point. On death the player's shards stay with a `Shade` at the
last safe ground position (the death spot itself may be inside spikes), and the player wakes at the
last bench. Spikes and mist send the player back to the last safe ground with one mask of damage.

## Performance notes
* One simulation tick costs about 0.02 ms (measured in `PerformanceSmokeTest`), so logic is
  far below the 16.7 ms budget.
* Projectiles and particles are pooled (`ObjectPool<T>`); per-tick loops avoid allocation
  (the unlocked-ability list is cached, the lighting system stores lights in a float array).
* Tile drawing is culled to the camera view; the light map renders at half resolution.
* All textures, the atlas, frame buffers, fonts, sounds and the batch are disposed in their
  owners' `dispose()` methods.
