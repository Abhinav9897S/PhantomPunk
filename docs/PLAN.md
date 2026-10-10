# PHANTOM: The Umbral Descent — Build Plan

Engine: **libGDX 1.12.1** (LWJGL3 desktop backend), Java 17, Gradle wrapper, JUnit 5.
Maven Central was reachable from the build environment, so the Java2D fallback was not needed.

The game logic (world, physics, entities, AI, saving, level validation) never touches
`Gdx.*` statics, so it can be run headless in JUnit. Rendering, audio and fonts live in
separate classes that are only created by the desktop application.

## Milestones and the classes each adds

| # | Milestone | Main classes |
|---|-----------|--------------|
| 1 | Project skeleton | `PhantomGame`, `ScreenManager`, `BaseScreen`, `TitleScreen`, `Constants`, `EventBus`, `Events`, `DesktopLauncher` |
| 2 | Core movement | `TileType`, `TileMap`, `CollisionSystem`, `CollisionResult`, `GameObject`, `Entity`, `Character`, `Player`, `PlayerMotor`, `CameraController`, `InputSource`, `ActionInput`, `KeyboardInput`, `ScriptedInput` |
| 3 | Combat | `PlayerAttack`, `CombatSystem`, `HealthComponent`, `Damageable`, `DamageInfo`, `Hitbox`, `Enemy`, `Crawler` |
| 4 | Enemies | `Flutterer`, `Sentinel`, `Brute`, `StateMachine`, `EnemyState`, `EnemyBehaviour`, `Behaviours`, `EnemyFactory`, `Projectile`, `ObjectPool` |
| 5 | Abilities and world | `Ability`, `AbilitySet`, `Dash`, `WallCling`, `DoubleJump`, `LightBolt`, `Focus`, `Room`, `RoomData`, `RoomLoader`, `DoorData`, `Bench`, `BreakableWall`, `FallingRock`, `LevelValidator`, `MovementModel`, `RoomReachability`, `GameWorld`, `RoomTransition` |
| 6 | Art pass 1 | `Canvas`, `ProceduralArt`, `SpriteFactory`, painters, `ParallaxBackground`, `PaletteLibrary`, `ParticleSystem`, `LightingSystem`, `WorldRenderer`, `TileRenderer` |
| 7 | Bosses, regions 2 and 3 | `Boss`, `BossAttack` + attack strategies, `Gnarlwarden`, `GildedWarden`, `HollowHeart`, `BossArena`, `GroundEruption`, 18 room files |
| 8 | Systems | `SaveManager`, `SaveData`, `SaveException`, `GameProgress`, `Inventory`, `MapScreen`, `MapRenderer`, `DialogueScreen`, `Shopkeeper`, `LoreNpc`, `Shade`, `EndingScreen`, `Ending` |
| 9 | Polish | `AudioManager`, `SoundSynth`, `Hud` (toasts and title cards), `SettingsScreen`, `PauseScreen`, transitions, screen shake, vignette |
| 10 | Docs, tests, verification | `docs/*`, JUnit suites, headless simulation, `sourceZip` task |

## World layout (18 rooms, as built)

Generated from `assets/rooms/*.json`:

| Id | Name | Region | Size | Doors | Features | Enemies |
| --- | --- | --- | --- | --- | --- | --- |
| `r1_gate` | Fallen Gate | Whispering Roots | 44x22 | east → r1_hollow | bench, NPC ossian, shard cache | 1 crawler |
| `r1_hollow` | Mossy Hollow | Whispering Roots | 70x20 | west → r1_gate; east → r1_shaft | 1 falling rock | 3 crawler, 1 flutterer |
| `r1_shaft` | Spore Shaft | Whispering Roots | 34x48 | top → r1_hollow; mid → r1_shrine; bottom → r1_thorn | 1 falling rock | 1 crawler, 2 flutterer |
| `r1_shrine` | Root Shrine | Whispering Roots | 40x18 | west → r1_shaft; east → r1_secret | shrine LIGHT_BOLT, breakable wall | 1 crawler, 1 sentinel |
| `r1_secret` | Hidden Burrow | Whispering Roots | 30x17 | west → r1_shrine | relic relic_roots, shard cache | - |
| `r1_thorn` | Thornway | Whispering Roots | 64x20 | west → r1_shaft; east → r1_boss | bench, shop, 1 falling rock | 1 flutterer, 2 sentinel |
| `r1_boss` | Gnarled Den | Whispering Roots | 46x20 | west → r1_thorn; east → r2_steps | boss gnarlwarden (grants DASH) | - |
| `r2_steps` | Cistern Steps | Gilded Cistern | 60x22 | west → r1_boss; east → r2_aqueduct | - | 1 sentinel, 1 brute |
| `r2_aqueduct` | Brass Aqueduct | Gilded Cistern | 80x20 | west → r2_steps; east → r2_chapel | - | 2 flutterer, 2 sentinel, 1 brute |
| `r2_chapel` | Drowned Chapel | Gilded Cistern | 44x40 | west → r2_aqueduct; upper → r2_gears; lower → r2_secret | bench, NPC seraphine, shrine WALL_CLING, shard cache, breakable wall | 2 flutterer |
| `r2_secret` | Flooded Vault | Gilded Cistern | 30x17 | west → r2_chapel | relic relic_cistern, shard cache | - |
| `r2_gears` | Gearworks Shaft | Gilded Cistern | 34x44 | west → r2_chapel; east → r2_boss | bench, shard cache | 1 flutterer, 2 sentinel |
| `r2_boss` | Warden's Gallery | Gilded Cistern | 48x20 | west → r2_gears; east → r3_verge | boss gilded_warden (grants DOUBLE_JUMP) | - |
| `r3_verge` | Mist Verge | Umbral Heart | 64x26 | west → r2_boss; east → r3_spire | - | 2 flutterer |
| `r3_spire` | Hollow Spire | Umbral Heart | 36x44 | west → r3_verge; upper → r3_lantern; lower → r3_secret | breakable wall | 2 flutterer, 1 sentinel, 1 brute |
| `r3_secret` | Starless Hollow | Umbral Heart | 30x17 | west → r3_spire | relic relic_heart, shard cache | - |
| `r3_lantern` | Last Lantern | Umbral Heart | 40x18 | west → r3_spire; east → r3_heart | bench, NPC vesper, shop | - |
| `r3_heart` | The Umbral Heart | Umbral Heart | 56x24 | west → r3_lantern | boss hollow_heart | - |

### Critical path and gates

1. **Fallen Gate → Mossy Hollow → Spore Shaft** (Light Bolt shrine and the first secret are off the
   shaft's middle door) **→ Thornway** (bench, shop) **→ Gnarled Den**: the Gnarlwarden grants **Umbral Dash**.
2. **Cistern Steps** opens with a 9-tile spike pit that only a jump plus dash can cross.
3. **Brass Aqueduct → Drowned Chapel**: the **Moss Grip** shrine, then a wall-jump shaft up to the
   **Gearworks Shaft** (another wall-jump climb, bench at the top) **→ Warden's Gallery**: the Gilded
   Warden grants **Wisp Leap**.
4. **Mist Verge**: floating ledges 5 tiles apart over Hollow Mist need the double jump, then the
   **Hollow Spire** climb **→ Last Lantern** (bench, shop, Vesper) **→ The Umbral Heart** (final boss).
5. Three **Ember Relics** in the secret rooms (behind breakable walls in the Root Shrine, Drowned
   Chapel and Hollow Spire) unlock the hidden ending.
