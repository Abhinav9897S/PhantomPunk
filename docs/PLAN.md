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
| 5 | Abilities and world | `Ability`, `AbilitySet`, `Dash`, `WallCling`, `DoubleJump`, `LightBolt`, `Focus`, `Room`, `RoomData`, `RoomLoader`, `DoorData`, `Bench`, `BreakableWall`, `FallingRock`, `LevelValidator`, `ReachabilityAnalyzer`, `GameWorld`, `RoomTransition` |
| 6 | Art pass 1 | `Canvas`, `ProceduralArt`, `SpriteFactory`, painters, `ParallaxBackground`, `PaletteLibrary`, `ParticleSystem`, `LightingSystem`, `WorldRenderer`, `TileRenderer` |
| 7 | Bosses, regions 2 and 3 | `Boss`, `BossAttack` + attack strategies, `Gnarlwarden`, `GildedWarden`, `HollowHeart`, `BossArena`, `GroundEruption`, 18 room files |
| 8 | Systems | `SaveManager`, `SaveData`, `SaveException`, `GameProgress`, `Inventory`, `MapScreen`, `MapRenderer`, `DialogueScreen`, `Shopkeeper`, `LoreNpc`, `Shade`, `EndingScreen`, `Ending` |
| 9 | Polish | `AudioManager`, `SoundSynth`, `Hud`, `Toasts`, `SettingsScreen`, `PauseScreen`, transitions, screen shake, vignette |
| 10 | Docs, tests, verification | `docs/*`, JUnit suites, headless simulation, `sourceZip` task |

## World layout (18 rooms)

```
Whispering Roots                Gilded Cistern                     Umbral Heart
r1_gate - r1_hollow - r1_shaft  r2_steps - r2_aqueduct - r2_chapel  r3_verge - r3_spire - r3_lantern - r3_heart
                         |  \       ^(dash gap)           |    \   ^(double jump)  |
                  r1_thorn  r1_shrine              r2_secret  r2_gears           r3_secret
                     |         |                               |
                  r1_boss   r1_secret                       r2_boss
```

* Boss 1 (Gnarlwarden, `r1_boss`) grants **Dash**; the first room of the Cistern has a gap only a dash can cross.
* The **Wall Cling** shrine is in the Drowned Chapel; the Gearworks Shaft can only be climbed with it.
* Boss 2 (Gilded Warden, `r2_boss`) grants **Wisp Leap** (double jump); the Mist Verge uses floating
  one-way platforms that cannot be wall-climbed, so it needs the double jump.
* The final encounter (Hollow Heart, `r3_heart`) ends the game. Three hidden Ember Relics unlock the hidden ending.
