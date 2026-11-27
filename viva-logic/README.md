# Phantom: The Umbral Descent (logic edition)

The same game logic as the real game, written as a **plain Java console program**, with no graphics, no
libraries and no art. It is meant for the viva: every file is short, and the comment at the top of each
class names the OOP concept it shows.

## Run it

Needs only a JDK (17 or newer).

```bash
cd viva-logic
javac *.java
java Main              # menu: 1 New Game, 2 Continue, 3 Autoplay, 4 Exit
java Main --autoplay   # watch the computer play to the end
```

Keys in a game: `a` strike, `b` Light Bolt, `h` Focus (heal), `j` Dash (dodge), `e` interact
(bench / talk / shrine), `d` next room, `s` save, `q` quit.

## How the game works (one turn)

1. The player picks a command. It comes from an `InputSource`: `KeyboardInput` (a person) or
   `AutoPilot` (the computer).
2. The command is carried out: strike, use an ability, interact, or move to the next room.
3. Every living enemy in the room takes its turn (`Enemy.takeTurn`).
4. Dead enemies give shards; a beaten boss gives an ability; the last boss ends the game.
5. If the player's health reaches 0, they wake at the last bench they rested on.

The world is 11 rooms in a row. Two rooms are **ability gates**: *Cistern Steps* needs Umbral Dash (from
the first boss) and *Mist Verge* needs Wisp Leap (from the second boss).

## Where each OOP concept is

| Concept | Files |
| --- | --- |
| Encapsulation | `HealthComponent` (private health, only `damage()` / `heal()`), `Inventory.spendShards()` |
| Abstraction: abstract classes | `GameObject`, `Character`, `Enemy`, `Boss` |
| Abstraction: interfaces | `Damageable` (with a `default` method), `Interactable`, `Ability`, `InputSource` |
| Inheritance (multilevel) | `GameObject -> Character -> Enemy -> Boss -> Gnarlwarden` |
| Inheritance (hierarchical) | `Crawler`, `Sentinel`, `Boss` all extend `Enemy` |
| Interface instead of multiple inheritance | `Character extends GameObject implements Damageable` |
| Polymorphism: overriding | `nextState()`, `onHurt()`, `describe()`, `takeDamage()` (`@Override`) |
| Polymorphism: interfaces | `InputSource` is a `KeyboardInput` or an `AutoPilot`; `Ability` objects |
| Overloading | the two `SaveException` constructors |
| `super` / `this` | `super(...)` in every constructor, `super.takeDamage()`, `super.reset()` |
| Composition (has-a) | `Player` has a `HealthComponent`, an `Inventory` and `Ability` objects; `Game` has rooms |
| Strategy pattern | `Ability` -> `Focus`, `LightBolt`, `Dash`, `DoubleJump` |
| Template Method pattern | `Enemy.takeTurn()` (final) calls the hook `nextState()` |
| State (simple) | `EnemyState` enum: PATROL, WINDUP, ATTACK, STUNNED |
| Observer pattern | `EventBus` + `GameEvent`; listeners in `Game` and `Achievements` |
| Factory | `World.create()` builds the rooms; `Player.unlock()` creates the right `Ability` |
| Generics | `EventBus.subscribe(Class<T>, Consumer<T>)` |
| Collections | `ArrayList`, `HashMap`, `EnumMap`, `EnumSet`, `LinkedHashSet`, `List.of` |
| Enums with fields | `AbilityType`, `Command` |
| Records | `GameEvent.*`, `SaveData` |
| Exceptions | `SaveException` (custom checked exception, exception chaining) |
| File handling | `SaveManager` (`Files.writeString`, `Files.readAllLines`) |
| Varargs, lambdas, method references | `Npc(String name, String... lines)`, `e -> ...`, `Enemy::isAboutToAttack` |
